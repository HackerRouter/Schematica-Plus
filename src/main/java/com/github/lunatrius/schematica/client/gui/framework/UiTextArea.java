// SPDX-License-Identifier: LGPL-3.0-only
// MaLiLib GuiTextFieldMultiLine, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.framework;

import java.util.List;
import java.util.function.Consumer;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;

/** A multi-line text box with soft word wrap; Enter inserts a line break. */
public final class UiTextArea extends UiWidget {
    private static final int PADDING = 4, LINE_HEIGHT = 9;

    private final FontRenderer font;
    private final TextAreaModel model;
    private final Consumer<String> changed;
    private int scroll, blink;
    private boolean dragging;

    public UiTextArea(FontRenderer font, int maxLength, int maxLines, Consumer<String> changed) {
        this.font = font;
        this.model = new TextAreaModel(maxLength, maxLines);
        this.changed = changed;
    }

    public String text() { return model.text(); }

    public void setText(String text) {
        model.setText(text);
        scroll = 0;
        changed.accept(model.text());
    }

    private int textWidth() { return Math.max(1, bounds().width - PADDING * 2 - 4); }
    private int visibleLines() { return Math.max(1, (bounds().height - PADDING * 2) / LINE_HEIGHT); }

    private List<TextAreaModel.Line> lines() {
        model.setWrap(textWidth(), font::getStringWidth);
        return model.lines();
    }

    private int maxScroll() { return Math.max(0, lines().size() - visibleLines()); }

    private void revealCursor() {
        lines();
        int line = model.lineOf(model.cursor());
        if (line < scroll) scroll = line;
        else if (line >= scroll + visibleLines()) scroll = line - visibleLines() + 1;
        scroll = Math.max(0, Math.min(maxScroll(), scroll));
    }

    @Override public boolean isFocusable() { return true; }

    @Override public void tick() { blink++; }

    @Override
    public void draw(UiDraw draw, int mouseX, int mouseY) {
        UiBounds b = bounds();
        draw.fill(b, UiTheme.FIELD);
        draw.border(b, isFocused() ? UiTheme.FOCUS : UiTheme.BORDER);
        List<TextAreaModel.Line> lines = lines();
        scroll = Math.max(0, Math.min(maxScroll(), scroll));
        int x = b.x + PADDING, top = b.y + PADDING;
        int color = isEnabled() ? UiTheme.TEXT : UiTheme.DISABLED;
        int from = model.selectionStart(), to = model.selectionEnd();
        for (int i = scroll; i < Math.min(lines.size(), scroll + visibleLines()); i++) {
            TextAreaModel.Line line = lines.get(i);
            int y = top + (i - scroll) * LINE_HEIGHT;
            String content = model.text().substring(line.start, line.end);
            int selStart = Math.max(from, line.start), selEnd = Math.min(to, line.end);
            boolean breakSelected = from <= line.end && to > line.end;
            if (selStart < selEnd || breakSelected) {
                int left = x + font.getStringWidth(model.text().substring(line.start, Math.max(line.start, selStart)));
                int right = x + font.getStringWidth(model.text().substring(line.start, selEnd)) + (breakSelected ? 3 : 0);
                if (selStart >= selEnd) left = right - 3;
                draw.fill(new UiBounds(left, y - 1, Math.max(1, right - left), LINE_HEIGHT), UiTheme.SELECTED);
            }
            draw.text(content, x, y, color);
        }
        if (isFocused() && blink / 6 % 2 == 0) {
            int line = model.lineOf(model.cursor());
            if (line >= scroll && line < scroll + visibleLines()) {
                int cx = x + model.pixelOf(model.cursor()), cy = top + (line - scroll) * LINE_HEIGHT;
                draw.fill(new UiBounds(cx, cy - 1, 1, LINE_HEIGHT), UiTheme.FOCUS);
            }
        }
        if (maxScroll() > 0) {
            int track = b.height - 2;
            int thumb = Math.max(8, track * visibleLines() / lines.size());
            int offset = (track - thumb) * scroll / maxScroll();
            draw.fill(new UiBounds(b.right() - 4, b.y + 1 + offset, 3, thumb), UiTheme.BORDER);
        }
    }

    private int positionAt(int mouseX, int mouseY) {
        int line = scroll + Math.max(0, (mouseY - bounds().y - PADDING)) / LINE_HEIGHT;
        if (line >= lines().size()) return model.text().length();
        return model.positionAt(line, mouseX - bounds().x - PADDING);
    }

    @Override
    public boolean mouseDown(int x, int y, int button) {
        if (button != 0) return false;
        model.moveTo(positionAt(x, y), GuiScreen.isShiftKeyDown());
        dragging = true;
        blink = 0;
        return true;
    }

    @Override
    public void mouseDrag(int x, int y, int button) {
        if (!dragging) return;
        model.moveTo(positionAt(x, y), true);
        revealCursor();
    }

    @Override public void mouseUp(int x, int y, int button) { dragging = false; }
    @Override public void cancelMouse() { dragging = false; }

    @Override
    public boolean scroll(int x, int y, int amount) {
        if (maxScroll() == 0) return false;
        scroll = Math.max(0, Math.min(maxScroll(), scroll - Integer.signum(amount)));
        return true;
    }

    @Override
    public boolean keyTyped(char character, int keyCode) {
        if (!isEnabled()) return false;
        boolean shift = GuiScreen.isShiftKeyDown(), ctrl = GuiScreen.isCtrlKeyDown();
        boolean edited = false;
        lines();
        if (ctrl && keyCode == Keyboard.KEY_A) model.selectAll();
        else if (ctrl && keyCode == Keyboard.KEY_C) GuiScreen.setClipboardString(model.selectedText());
        else if (ctrl && keyCode == Keyboard.KEY_X) {
            GuiScreen.setClipboardString(model.selectedText());
            edited = model.insert("");
        } else if (ctrl && keyCode == Keyboard.KEY_V) edited = model.insert(GuiScreen.getClipboardString());
        else switch (keyCode) {
            case Keyboard.KEY_RETURN: case Keyboard.KEY_NUMPADENTER: edited = model.insert("\n"); break;
            case Keyboard.KEY_BACK: edited = model.erase(-1, ctrl); break;
            case Keyboard.KEY_DELETE: edited = model.erase(1, ctrl); break;
            case Keyboard.KEY_LEFT:
                if (!shift && model.hasSelection() && !ctrl) model.moveTo(model.selectionStart(), false);
                else model.moveTo(ctrl ? model.wordFrom(model.cursor(), -1) : model.cursor() - 1, shift);
                break;
            case Keyboard.KEY_RIGHT:
                if (!shift && model.hasSelection() && !ctrl) model.moveTo(model.selectionEnd(), false);
                else model.moveTo(ctrl ? model.wordFrom(model.cursor(), 1) : model.cursor() + 1, shift);
                break;
            case Keyboard.KEY_UP: model.moveLines(-1, shift); break;
            case Keyboard.KEY_DOWN: model.moveLines(1, shift); break;
            case Keyboard.KEY_PRIOR: model.moveLines(-visibleLines(), shift); break;
            case Keyboard.KEY_NEXT: model.moveLines(visibleLines(), shift); break;
            case Keyboard.KEY_HOME: if (ctrl) model.moveTo(0, shift); else model.lineStart(shift); break;
            case Keyboard.KEY_END: if (ctrl) model.moveTo(model.text().length(), shift); else model.lineEnd(shift); break;
            default:
                if (character < ' ' || character == 127) return false;
                edited = model.insert(String.valueOf(character));
        }
        blink = 0;
        revealCursor();
        if (edited) changed.accept(model.text());
        return true;
    }
}

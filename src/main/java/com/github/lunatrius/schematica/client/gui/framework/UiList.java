// SPDX-License-Identifier: LGPL-3.0-only
// Litematica/MaLiLib visual conventions, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.framework;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.input.Keyboard;

public final class UiList<T> extends UiWidget {

    private final UiListModel<T> model;
    private final Consumer<T> activated;
    private String emptyText;
    private boolean dragging;
    private int dragOffset;
    private int lastClickIndex = -1;
    private long lastClickTime;
    private Function<T, UiSprite> icons;
    private Function<T, String> displayName;
    private Function<T, UiSprite> badges;
    private Set<T> chosen;
    private int anchor = -1;

    private Function<T, List<String>> entryTooltip;

    /** Replaces the label tooltip of hovered entries. */
    public void setEntryTooltip(Function<T, List<String>> tooltip) { this.entryTooltip = tooltip; }

    public void setFileStyle(Function<T, UiSprite> icons, Function<T, String> displayName) {
        this.icons = icons;
        this.displayName = displayName;
    }

    public void setBadges(Function<T, UiSprite> badges) { this.badges = badges; }

    public void setEmptyText(String text) { this.emptyText = text; }

    public void enableMultiSelect() { chosen = new LinkedHashSet<>(); }

    public Set<T> chosen() {
        if (chosen == null) return model.selectedIndex() < 0 ? Collections.<T>emptySet() : Collections.singleton(model.selected());
        chosen.retainAll(model.entries());
        return Collections.unmodifiableSet(chosen);
    }

    public UiList(UiListModel<T> model, String emptyText, Consumer<T> activated) {
        this.model = model;
        this.emptyText = emptyText;
        this.activated = activated;
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        model.setViewportHeight(Math.max(0, height - (icons == null ? 2 : 0)));
    }

    @Override
    public boolean isFocusable() {
        return true;
    }

    private UiBounds content() {
        return icons == null ? bounds().inset(1) : bounds();
    }

    private UiBounds thumb() {
        UiBounds content = content();
        int total = Math.max(content.height, model.entries().size() * model.rowHeight());
        int height = Math.min(content.height, Math.max(12, (int) ((long) content.height * content.height / Math.max(1, total))));
        int travel = content.height - height;
        int y = model.maxOffset() == 0 ? 0 : (int) ((long) model.offset() * travel / model.maxOffset());
        return new UiBounds(content.right() - 6, content.y + y, 6, height);
    }

    @Override
    public void draw(UiDraw draw, int mouseX, int mouseY) {
        UiBounds content = content();
        if (icons == null) {
            draw.fill(bounds(), UiTheme.FIELD);
            draw.border(bounds(), isFocused() ? UiTheme.FOCUS : UiTheme.BORDER);
        }
        try (UiDraw.Clip ignored = draw.clip(content)) {
            int first = model.offset() / model.rowHeight();
            int end = Math.min(model.entries().size(), first + content.height / model.rowHeight() + 2);
            int selected = model.selectedIndex();
            if (chosen != null) chosen.retainAll(model.entries());
            int hovered = content.contains(mouseX, mouseY) ? model.indexAt(mouseY - content.y) : -1;
            int rowWidth = Math.max(0, content.width - (icons != null ? 12 : model.maxOffset() > 0 ? 7 : 0));
            for (int index = first; index < end; index++) {
                int y = content.y + index * model.rowHeight() - model.offset();
                int background = index == selected ? UiTheme.SELECTED
                    : index == hovered ? UiTheme.HOVER : (index % 2 == 0 ? UiTheme.ROW : UiTheme.PANEL);
                UiBounds row = new UiBounds(content.x, y, rowWidth, model.rowHeight());
                if (icons != null) {
                    T entry = model.entries().get(index);
                    boolean picked = chosen == null ? index == selected : chosen.contains(entry);
                    draw.fill(row, picked || index == hovered ? 0x70FFFFFF : index % 2 == 1 ? 0x20FFFFFF : 0x38FFFFFF);
                    if (picked) draw.border(row, 0xEEEEEEEE);
                    icons.apply(entry).draw(draw, content.x, y + 1, false, false);
                    UiSprite badge = badges == null ? null : badges.apply(entry);
                    int nameWidth = rowWidth - 18;
                    if (badge != null) {
                        badge.draw(draw, content.x + rowWidth - badge.width - 2, y + (model.rowHeight() - badge.height) / 2, true, false);
                        nameWidth -= badge.width + 4;
                    }
                    draw.text(draw.trim(displayName.apply(entry), nameWidth), content.x + 16, y + 3, 0xFFFFFFFF);
                    continue;
                }
                draw.fill(row, background);
                String label = draw.trim(model.label(model.entries().get(index)), rowWidth - 8);
                draw.text(label, content.x + 4, y + (model.rowHeight() - 8) / 2,
                    isEnabled() ? UiTheme.TEXT : UiTheme.DISABLED);
            }
            if (model.entries().isEmpty()) {
                draw.text(draw.trim(emptyText, content.width - 8), content.x + 4, content.y + 6, UiTheme.DISABLED);
            }
            if (model.maxOffset() > 0) {
                draw.fill(new UiBounds(content.right() - 6, content.y, 6, content.height), UiTheme.CONTROL);
                draw.fill(thumb(), dragging ? UiTheme.FOCUS : UiTheme.BORDER);
            }
        }
    }

    @Override
    public List<String> tooltip(int x, int y) {
        int index = model.indexAt(y - content().y);
        if (index >= 0 && x < content().right() - 7) {
            T entry = model.entries().get(index);
            return entryTooltip != null ? entryTooltip.apply(entry) : Collections.singletonList(model.label(entry));
        }
        return super.tooltip(x, y);
    }

    @Override
    public boolean mouseDown(int x, int y, int button) {
        if (button != 0 || !content().contains(x, y)) return false;
        if (model.maxOffset() > 0 && x >= thumb().x) {
            UiBounds thumb = thumb();
            dragOffset = thumb.contains(x, y) ? y - thumb.y : thumb.height / 2;
            dragging = true;
            mouseDrag(x, y, button);
            return true;
        }
        int index = model.indexAt(y - content().y);
        if (index >= 0 && chosen != null && (GuiScreen.isCtrlKeyDown() || GuiScreen.isShiftKeyDown())) {
            T entry = model.entries().get(index);
            if (GuiScreen.isShiftKeyDown() && anchor >= 0 && anchor < model.entries().size()) {
                if (!GuiScreen.isCtrlKeyDown()) chosen.clear();
                for (int i = Math.min(anchor, index); i <= Math.max(anchor, index); i++) chosen.add(model.entries().get(i));
            } else {
                if (!chosen.remove(entry)) chosen.add(entry);
                anchor = index;
            }
            model.select(index);
            lastClickIndex = -1;
            return true;
        }
        if (index >= 0) {
            if (chosen != null) {
                chosen.clear();
                chosen.add(model.entries().get(index));
                anchor = index;
            }
            boolean doubleClick = index == lastClickIndex && index == model.selectedIndex()
                && System.nanoTime() - lastClickTime < 250_000_000L;
            model.select(index);
            lastClickIndex = index;
            lastClickTime = System.nanoTime();
            if (doubleClick) {
                lastClickIndex = -1;
                activated.accept(model.selected());
            }
        }
        return true;
    }

    @Override
    public void mouseDrag(int x, int y, int button) {
        if (dragging) {
            int travel = content().height - thumb().height;
            double fraction = (double) (y - content().y - dragOffset) / Math.max(1, travel);
            model.setOffset((int) Math.round(Math.max(0, Math.min(1, fraction)) * model.maxOffset()));
        }
    }

    @Override
    public void mouseUp(int x, int y, int button) {
        dragging = false;
    }

    @Override
    public void cancelMouse() {
        dragging = false;
        lastClickIndex = -1;
    }

    @Override
    public boolean scroll(int x, int y, int amount) {
        model.setOffset(model.offset() - Integer.signum(amount) * model.rowHeight() * 3);
        lastClickIndex = -1;
        return true;
    }

    @Override
    public boolean keyTyped(char character, int keyCode) {
        int page = Math.max(1, content().height / model.rowHeight());
        switch (keyCode) {
            case Keyboard.KEY_UP: model.moveSelection(-1); return true;
            case Keyboard.KEY_DOWN: model.moveSelection(1); return true;
            case Keyboard.KEY_PRIOR: model.moveSelection(-page); return true;
            case Keyboard.KEY_NEXT: model.moveSelection(page); return true;
            case Keyboard.KEY_HOME: model.select(0); model.revealSelection(); return true;
            case Keyboard.KEY_END: model.select(model.entries().size() - 1); model.revealSelection(); return true;
            case Keyboard.KEY_RETURN:
            case Keyboard.KEY_NUMPADENTER:
                if (model.selectedIndex() >= 0) activated.accept(model.selected());
                return true;
            default: return false;
        }
    }
}

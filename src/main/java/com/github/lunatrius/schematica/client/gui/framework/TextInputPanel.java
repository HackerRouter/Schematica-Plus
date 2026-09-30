// SPDX-License-Identifier: LGPL-3.0-only
// MaLiLib GuiTextInputBase layout, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.framework;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.gui.FontRenderer;

public final class TextInputPanel extends UiPanel {

    private final FontRenderer font;
    private final UiInput input;
    private final String title;
    private final Function<String, String> apply;
    private final UiTextField text;
    private final List<UiButton> buttons = new ArrayList<>();
    private String error = "";
    private List<String> errorLines = new ArrayList<>();
    private UiBounds screen;

    public TextInputPanel(FontRenderer font, UiInput input, String title, String initial, Function<String, String> apply) {
        this.font = font;
        this.input = input;
        this.title = title;
        this.apply = apply;
        text = add(new UiTextField(font, 256, value -> {}) {
            @Override public boolean keyTyped(char character, int keyCode) {
                if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                    submit();
                    return true;
                }
                return super.keyTyped(character, keyCode);
            }
        });
        text.setText(initial);
        button("ok", this::submit);
        button("reset", () -> {
            text.setText(initial);
            error = "";
            layout(screen);
            input.focus(text);
        });
        button("cancel", input::popModal);
    }

    private void button(String name, Runnable action) {
        buttons.add(add(new UiButton(() -> UiTranslations.format("malilib.gui.button." + name), mouse -> {
            if (mouse == 0) action.run();
        })));
    }

    private void submit() {
        String result = apply.apply(text.text());
        if (result == null) input.popModal();
        else {
            error = result;
            layout(screen);
            input.focus(text);
        }
    }

    @Override
    public void layout(UiBounds screen) {
        this.screen = screen;
        int width = Math.max(80, Math.min(260, screen.width - 16));
        errorLines = error.isEmpty() ? new ArrayList<>() : font.listFormattedStringToWidth(error, width - 20);
        int height = Math.min(screen.height - 8, 80 + errorLines.size() * 11);
        setBounds((screen.width - width) / 2, (screen.height - height) / 2, width, height);
        text.setBounds(bounds().x + 12, bounds().y + 20, width - 20, 20);
        int x = bounds().x + 10;
        for (UiButton button : buttons) {
            int w = Math.max(40, font.getStringWidth(button.label()) + 10);
            button.setBounds(x, bounds().y + 50, w, 20);
            x += w + 2;
        }
    }

    @Override
    public void draw(UiDraw draw, int mouseX, int mouseY) {
        draw.fill(bounds(), 0xE0000000);
        draw.border(bounds(), UiTheme.BORDER);
        draw.text(draw.trim(title, bounds().width - 20), bounds().x + 10, bounds().y + 4, UiTheme.TEXT);
        int y = bounds().y + 76;
        for (String line : errorLines) {
            draw.text(line, bounds().x + 10, y, 0xFFFF6666);
            y += 11;
        }
        super.draw(draw, mouseX, mouseY);
    }
}

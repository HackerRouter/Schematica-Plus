// SPDX-License-Identifier: LGPL-3.0-only
// Litematica/MaLiLib visual conventions, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.framework;

import java.util.function.IntConsumer;
import java.util.function.Supplier;

import org.lwjgl.input.Keyboard;

public class UiButton extends UiWidget {

    private final Supplier<String> label;
    private final IntConsumer action;
    private final UiIcon icon;
    private UiSprite sprite;
    private boolean background = true;
    private int pressed = -1;
    /** The label did not fit when last drawn; it is then shown in full on hover (MaLiLib buttons are never cut). */
    private boolean cut;

    public UiButton(Supplier<String> label, IntConsumer action) {
        this(label, null, action);
    }

    public UiButton(Supplier<String> label, UiIcon icon, IntConsumer action) {
        this.label = label;
        this.icon = icon;
        this.action = action;
    }

    @Override
    public boolean isFocusable() {
        return true;
    }

    public UiButton setSprite(UiSprite sprite) {
        this.sprite = sprite;
        return this;
    }

    public UiButton setBackground(boolean background) {
        this.background = background;
        return this;
    }

    public String label() {
        return label.get();
    }

    public int preferredWidth(int textWidth) {
        return textWidth + (sprite == null ? 10 : sprite.width + 16);
    }

    @Override
    public void draw(UiDraw draw, int mouseX, int mouseY) {
        UiBounds box = bounds();
        boolean hovered = containsVisible(mouseX, mouseY);
        if (background) {
            int v = !isEnabled() ? 46 : hovered ? 86 : 66;
            int left = box.width / 2;
            draw.texture("minecraft:textures/gui/widgets.png", new UiBounds(box.x, box.y, left, box.height),
                0, v, Math.min(left, 200), 20, 256, 256);
            int right = box.width - left;
            draw.texture("minecraft:textures/gui/widgets.png", new UiBounds(box.x + left, box.y, right, box.height),
                200 - Math.min(right, 200), v, Math.min(right, 200), 20, 256, 256);
            if (isFocused() && isEnabled() && UiInput.focusVisible()) draw.border(box, 0xFFE0E0E0);
        }
        int color = !isEnabled() ? 0xFFA0A0A0 : hovered ? 0xFFFFFFFF : 0xFFE0E0E0;
        if (sprite != null) {
            sprite.draw(draw, box.x + (background ? 4 : 0), box.y + (box.height - sprite.height) / 2,
                isEnabled(), hovered);
            String shown = draw.trim(label.get(), box.width - sprite.width - 12);
            cut = !shown.equals(label.get());
            draw.text(shown, box.x + sprite.width + 8, box.y + (box.height - 8) / 2, color);
            drawIconHighlight(draw, hovered);
            return;
        }
        String text = draw.trim(label.get(), box.width - (icon == null ? 8 : 21));
        cut = !text.equals(label.get());
        if (icon != null) icon.draw(draw, box.x + 5, box.y + (box.height - 7) / 2, color);
        int x = icon == null ? box.x + (box.width - draw.textWidth(text)) / 2 : box.x + 17;
        draw.text(text, x, box.y + (box.height - 8) / 2, color);
        drawIconHighlight(draw, hovered);
    }

    private void drawIconHighlight(UiDraw draw, boolean hovered) {
        if (!background && isEnabled() && (hovered || isFocused() && UiInput.focusVisible())) {
            draw.fill(bounds(), 0x20C0C0C0);
            draw.border(bounds(), 0xE0FFFFFF);
        }
    }

    @Override
    public java.util.List<String> tooltip(int mouseX, int mouseY) {
        java.util.List<String> lines = super.tooltip(mouseX, mouseY);
        return lines.isEmpty() && cut ? java.util.Collections.singletonList(label.get()) : lines;
    }

    @Override
    public boolean mouseDown(int x, int y, int button) {
        if (button != 0 && button != 1) return false;
        pressed = button;
        return true;
    }

    @Override
    public void mouseUp(int x, int y, int button) {
        boolean activate = button == pressed && containsVisible(x, y);
        pressed = -1;
        if (activate) action.accept(button);
    }

    @Override
    public void cancelMouse() {
        pressed = -1;
    }

    @Override
    public boolean keyTyped(char character, int keyCode) {
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER || keyCode == Keyboard.KEY_SPACE) {
            action.accept(0);
            return true;
        }
        return false;
    }
}

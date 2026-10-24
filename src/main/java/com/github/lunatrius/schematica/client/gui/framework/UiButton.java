// SPDX-License-Identifier: LGPL-3.0-only
// Litematica/MaLiLib visual conventions, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.framework;

import java.util.Collections;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import org.lwjgl.input.Keyboard;

public class UiButton extends UiWidget {

    private final Supplier<String> label;
    private final IntConsumer action;
    private final UiIcon icon;
    private UiSprite sprite;
    private int spriteWidth, spriteHeight;
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

    public UiButton setSpriteSize(int width, int height) {
        spriteWidth = Math.max(1, width);
        spriteHeight = Math.max(1, height);
        return this;
    }

    public String label() {
        return label.get();
    }

    public int preferredWidth(int textWidth) {
        return textWidth + (sprite == null ? 10 : slot() + 16);
    }

    private int slot() {
        return background ? Math.max(14, sprite.width) : sprite.width;
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
            int width = spriteWidth > 0 ? spriteWidth : sprite.width;
            int height = spriteHeight > 0 ? spriteHeight : sprite.height;
            sprite.draw(draw, new UiBounds(box.x + (background ? 4 : 0) + (slot() - width) / 2,
                box.y + (box.height - height) / 2, width, height), isEnabled(), hovered);
            String shown = draw.trim(label.get(), box.width - slot() - 12);
            cut = !shown.equals(label.get());
            draw.shadowText(shown, box.x + slot() + 8, box.y + (box.height - 8) / 2, color);
            drawIconHighlight(draw, hovered);
            return;
        }
        String text = draw.trim(label.get(), box.width - (icon == null ? 8 : 21));
        cut = !text.equals(label.get());
        if (icon != null) icon.draw(draw, box.x + 5, box.y + (box.height - 7) / 2, color);
        int x = icon == null ? box.x + (box.width - draw.textWidth(text)) / 2 : box.x + 17;
        draw.shadowText(text, x, box.y + (box.height - 8) / 2, color);
        drawIconHighlight(draw, hovered);
    }

    private void drawIconHighlight(UiDraw draw, boolean hovered) {
        if (!background && isEnabled() && (hovered || isFocused() && UiInput.focusVisible())) {
            draw.fill(bounds(), 0x20C0C0C0);
            draw.border(bounds(), 0xE0FFFFFF);
        }
    }

    @Override
    public List<String> tooltip(int mouseX, int mouseY) {
        List<String> lines = super.tooltip(mouseX, mouseY);
        return lines.isEmpty() && cut ? Collections.singletonList(label.get()) : lines;
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

// SPDX-License-Identifier: LGPL-3.0-only
// MaLiLib WidgetSlider drawing, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.config;

import net.minecraftforge.common.config.Property;

import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.client.gui.framework.UiDraw;
import com.github.lunatrius.schematica.client.gui.framework.UiInput;
import com.github.lunatrius.schematica.client.gui.framework.UiWidget;

import org.lwjgl.input.Keyboard;

public final class UiConfigSlider extends UiWidget {
    private final ConfigPropertyDraft draft;

    public UiConfigSlider(ConfigPropertyDraft draft) { this.draft = draft; }

    @Override
    public boolean isFocusable() { return true; }

    private int knobWidth() {
        int usable = bounds().width - 4;
        long steps = draft.property.getType() == Property.Type.INTEGER
            ? Long.parseLong(draft.property.getMaxValue()) - Long.parseLong(draft.property.getMinValue()) + 1 : Integer.MAX_VALUE;
        return (int) Math.max(8, Math.min(usable / 2, usable / Math.max(1, steps)));
    }

    private static void button(UiDraw draw, UiBounds box, int v) {
        int left = box.width / 2, right = box.width - left;
        draw.texture("minecraft:textures/gui/widgets.png", new UiBounds(box.x, box.y, left, 20), 0, v, left, 20, 256, 256);
        draw.texture("minecraft:textures/gui/widgets.png", new UiBounds(box.x + left, box.y, right, 20), 200 - right, v, right, 20, 256, 256);
    }

    @Override
    public void draw(UiDraw draw, int mouseX, int mouseY) {
        UiBounds box = bounds();
        button(draw, new UiBounds(box.x + 1, box.y, box.width - 3, 20), 46);
        int knob = knobWidth();
        button(draw, new UiBounds(box.x + 2 + (int) (draft.fraction() * (box.width - 4 - knob)), box.y, knob, 20), 66);
        String label = draft.property.getType() == Property.Type.DOUBLE && draft.valid()
            ? String.format("%.4f", Double.parseDouble(draft.text())) : draft.text();
        label = draw.trim(label, box.width - 6);
        draw.text(label, box.x + box.width / 2 - draw.textWidth(label) / 2, box.y + 6, 0xFFFFFFA0);
        if (isFocused() && UiInput.focusVisible()) draw.border(box, 0xFFE0E0E0);
    }

    @Override
    public boolean mouseDown(int x, int y, int button) {
        if (button != 0) return false;
        mouseDrag(x, y, button);
        return true;
    }

    @Override
    public void mouseDrag(int x, int y, int button) {
        int knob = knobWidth();
        draft.setFraction((double) (x - bounds().x - knob / 2) / Math.max(1, bounds().width - knob - 4));
    }

    @Override
    public boolean keyTyped(char character, int keyCode) {
        if (keyCode != Keyboard.KEY_LEFT && keyCode != Keyboard.KEY_RIGHT) return false;
        double range = Double.parseDouble(draft.property.getMaxValue()) - Double.parseDouble(draft.property.getMinValue());
        double step = draft.property.getType() == Property.Type.INTEGER ? 1 / range : 0.01;
        draft.setFraction(draft.fraction() + (keyCode == Keyboard.KEY_LEFT ? -step : step));
        return true;
    }
}

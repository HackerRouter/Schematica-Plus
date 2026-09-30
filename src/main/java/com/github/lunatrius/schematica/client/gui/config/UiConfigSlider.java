// SPDX-License-Identifier: LGPL-3.0-only
// MaLiLib WidgetSlider drawing, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.config;

import org.lwjgl.input.Keyboard;

import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.client.gui.framework.UiDraw;
import com.github.lunatrius.schematica.client.gui.framework.UiWidget;

public final class UiConfigSlider extends UiWidget {
    private final ConfigPropertyDraft draft;

    public UiConfigSlider(ConfigPropertyDraft draft) { this.draft = draft; }

    @Override
    public boolean isFocusable() { return true; }

    @Override
    public void draw(UiDraw draw, int mouseX, int mouseY) {
        UiBounds box = bounds();
        draw.texture("minecraft:textures/gui/widgets.png", box, 0, 46, 200, 20, 256, 256);
        int x = box.x + 2 + (int) (draft.fraction() * Math.max(0, box.width - 12));
        draw.texture("minecraft:textures/gui/widgets.png", new UiBounds(x, box.y, 4, box.height),
            0, 66, 4, 20, 256, 256);
        draw.texture("minecraft:textures/gui/widgets.png", new UiBounds(x + 4, box.y, 4, box.height),
            196, 66, 4, 20, 256, 256);
        String label = draw.trim(draft.text(), box.width - 6);
        draw.text(label, box.x + (box.width - draw.textWidth(label)) / 2, box.y + 6, 0xFFFFFFA0);
        if (isFocused()) draw.border(box, 0xFFE0E0E0);
    }

    @Override
    public boolean mouseDown(int x, int y, int button) {
        if (button != 0) return false;
        mouseDrag(x, y, button);
        return true;
    }

    @Override
    public void mouseDrag(int x, int y, int button) {
        draft.setFraction((double) (x - bounds().x - 4) / Math.max(1, bounds().width - 12));
    }

    @Override
    public boolean keyTyped(char character, int keyCode) {
        if (keyCode != Keyboard.KEY_LEFT && keyCode != Keyboard.KEY_RIGHT) return false;
        double range = Double.parseDouble(draft.property.getMaxValue()) - Double.parseDouble(draft.property.getMinValue());
        double step = draft.property.getType() == net.minecraftforge.common.config.Property.Type.INTEGER ? 1 / range : 0.01;
        draft.setFraction(draft.fraction() + (keyCode == Keyboard.KEY_LEFT ? -step : step));
        return true;
    }
}

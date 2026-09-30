// SPDX-License-Identifier: LGPL-3.0-only
// MaLiLib HSV selector and bars, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.config;

import org.lwjgl.input.Keyboard;

import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.client.gui.framework.UiDraw;
import com.github.lunatrius.schematica.client.gui.framework.UiWidget;
import com.github.lunatrius.schematica.client.gui.config.ColorPickerModel.Channel;

public final class UiColorSurface extends UiWidget {
    private final ColorPickerModel model;
    private final Channel channel;
    private final boolean vertical;
    private final Runnable changed;

    public UiColorSurface(ColorPickerModel model, Channel channel, boolean vertical, Runnable changed) {
        this.model = model;
        this.channel = channel;
        this.vertical = vertical;
        this.changed = changed;
    }

    @Override public boolean isFocusable() { return true; }

    @Override public void draw(UiDraw draw, int mouseX, int mouseY) {
        UiBounds box = bounds().inset(1);
        int columns = channel == null ? 1 : channel == Channel.H && !vertical ? 6 : 1;
        int rows = channel == null ? 102 : vertical ? 6 : 1;
        int[] colors = new int[(columns + 1) * (rows + 1)];
        for (int y = 0; y <= rows; y++) for (int x = 0; x <= columns; x++) {
            colors[y * (columns + 1) + x] = channel == null ? model.squareColor((float) x / columns, (float) y / rows)
                : vertical ? model.hueColor((float) y / rows) : model.barColor(channel, (float) x / columns);
        }
        draw.fill(box, 0xFF000000);
        draw.colorGrid(box, columns, rows, colors);
        draw.border(bounds(), isFocused() ? 0xFFFFFFFF : 0xC0FFFFFF);
        if (channel == null) {
            int x = box.x + Math.round(model.fraction(Channel.V) * Math.max(0, box.width - 1));
            int y = box.y + Math.round((1 - model.fraction(Channel.S)) * Math.max(0, box.height - 1));
            draw.fill(new UiBounds(x, box.y, 1, box.height), 0xFFFFFFFF);
            draw.fill(new UiBounds(box.x, y, box.width, 1), 0xFFFFFFFF);
        }
    }

    public void drawMarkers(UiDraw draw) {
        if (channel == null) return;
        UiBounds box = bounds().inset(1);
        if (vertical) {
            int y = box.y + (int) ((1 - model.fraction(channel)) * box.height);
            int x = bounds().x;
            draw.triangle(x - 2, y - 2, x - 2, y + 2, x + 2, y, 0xFFFFFFFF);
            x += bounds().width;
            draw.triangle(x + 2, y - 2, x - 2, y, x + 2, y + 2, 0xFFFFFFFF);
        } else {
            float x = box.x + model.fraction(channel) * box.width;
            float y = box.y - 1.5f;
            draw.triangle(x - 2, y - 2, x, y + 2, x + 2, y - 2, 0xFFFFFFFF);
            y += box.height + 3;
            draw.triangle(x - 2, y + 2, x + 2, y + 2, x, y - 2, 0xFFFFFFFF);
        }
    }

    @Override public boolean mouseDown(int x, int y, int button) {
        if (button != 0) return false;
        mouseDrag(x, y, button);
        return true;
    }

    @Override public void mouseDrag(int x, int y, int button) {
        if (button != 0) return;
        UiBounds box = bounds().inset(1);
        float relX = (float) (x - box.x) / Math.max(1, box.width - 1);
        float relY = (float) (y - box.y) / Math.max(1, box.height - 1);
        if (channel == null) model.setSquare(relX, relY);
        else model.setFraction(channel, vertical ? 1 - relY : relX);
        changed.run();
    }

    @Override public boolean keyTyped(char character, int keyCode) {
        boolean up = keyCode == Keyboard.KEY_UP;
        boolean down = keyCode == Keyboard.KEY_DOWN;
        boolean left = keyCode == Keyboard.KEY_LEFT;
        boolean right = keyCode == Keyboard.KEY_RIGHT;
        if (!up && !down && !left && !right) return false;
        Channel target = channel == null ? up || down ? Channel.S : Channel.V : channel;
        model.setComponent(target, model.component(target) + (up || right ? 1 : -1));
        changed.run();
        return true;
    }
}

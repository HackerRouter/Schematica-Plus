// SPDX-License-Identifier: LGPL-3.0-only
// MaLiLib WidgetDirectoryNavigation path bar, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.framework;

import java.util.List;
import java.util.function.Supplier;

public final class UiPathBar extends UiWidget {
    private final Supplier<List<String>> elements;
    private final Runnable activate;

    public UiPathBar(Supplier<List<String>> elements) { this(elements, null); }

    public UiPathBar(Supplier<List<String>> elements, Runnable activate) {
        this.elements = elements;
        this.activate = activate;
    }

    @Override
    public boolean mouseDown(int x, int y, int button) {
        if (button != 0 || activate == null) return false;
        activate.run();
        return true;
    }

    @Override
    public void draw(UiDraw draw, int mouseX, int mouseY) {
        draw.fill(bounds(), 0x20FFFFFF);
        int max = Math.max(0, bounds().width - 6);
        String path = String.join(" / ", elements.get());
        if (draw.textWidth(path) > max) {
            while (!path.isEmpty() && draw.textWidth("... " + path) > max) path = path.substring(1);
            path = "... " + path;
        }
        draw.text(path, bounds().x + 3, bounds().y + 3, 0xC0C0C0C0);
    }
}

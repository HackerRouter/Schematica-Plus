// SPDX-License-Identifier: LGPL-3.0-only
// MaLiLib GuiScrollBar, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.framework;

final class UiScrollBar {
    static final int WIDTH = 8;
    private static final int MIN_THUMB = 8;

    private UiScrollBar() {}

    static UiBounds thumb(UiBounds track, int total, int offset, int maxOffset) {
        int slide = Math.max(0, track.height - 2);
        int height = Math.min(slide, Math.max(MIN_THUMB, (int) ((long) slide * slide / Math.max(1, Math.max(slide, total)))));
        int y = maxOffset <= 0 ? 0 : (int) ((long) offset * (slide - height) / maxOffset);
        return new UiBounds(track.x + 1, track.y + 1 + y, track.width - 2, height);
    }

    static int offset(UiBounds track, UiBounds thumb, int y, int grab, int maxOffset) {
        int travel = Math.max(1, track.height - 2 - thumb.height);
        return (int) Math.round(Math.max(0, Math.min(1, (double) (y - track.y - 1 - grab) / travel)) * maxOffset);
    }

    static void draw(UiDraw draw, UiBounds track, UiBounds thumb) {
        draw.fill(track, 0x44FFFFFF);
        draw.fill(thumb, 0xFFFFFFFF);
    }
}

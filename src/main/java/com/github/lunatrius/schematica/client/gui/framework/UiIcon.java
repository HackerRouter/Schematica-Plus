package com.github.lunatrius.schematica.client.gui.framework;

public enum UiIcon {
    BACK, CLOSE, PLUS, MINUS;

    public void draw(UiDraw draw, int x, int y, int color) {
        if (this == CLOSE) {
            for (int i = 0; i < 7; i++) {
                draw.fill(new UiBounds(x + i, y + i, 1, 1), color);
                draw.fill(new UiBounds(x + 6 - i, y + i, 1, 1), color);
            }
        } else {
            draw.fill(new UiBounds(x, y + 3, 7, 1), color);
            if (this == PLUS) draw.fill(new UiBounds(x + 3, y, 1, 7), color);
            if (this == BACK) {
                for (int i = 0; i < 4; i++) {
                    draw.fill(new UiBounds(x + i, y + 3 - i, 1, 1), color);
                    draw.fill(new UiBounds(x + i, y + 3 + i, 1, 1), color);
                }
            }
        }
    }
}

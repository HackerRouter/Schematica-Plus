package com.github.lunatrius.schematica.client.gui.framework;

public interface UiDraw {

    void fill(UiBounds bounds, int color);

    void text(String text, int x, int y, int color);

    int textWidth(String text);

    String trim(String text, int width);

    Clip clip(UiBounds bounds);

    void texture(String texture, UiBounds destination, int u, int v, int sourceWidth,
        int sourceHeight, int textureWidth, int textureHeight);

    default void border(UiBounds bounds, int color) {
        if (bounds.isEmpty()) return;
        fill(new UiBounds(bounds.x, bounds.y, bounds.width, 1), color);
        fill(new UiBounds(bounds.x, bounds.bottom() - 1, bounds.width, 1), color);
        fill(new UiBounds(bounds.x, bounds.y, 1, bounds.height), color);
        fill(new UiBounds(bounds.right() - 1, bounds.y, 1, bounds.height), color);
    }

    interface Clip extends AutoCloseable {
        @Override
        void close();
    }
}

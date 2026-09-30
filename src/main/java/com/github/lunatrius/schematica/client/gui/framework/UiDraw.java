package com.github.lunatrius.schematica.client.gui.framework;

import net.minecraft.item.ItemStack;

public interface UiDraw {

    void fill(UiBounds bounds, int color);

    void colorGrid(UiBounds bounds, int columns, int rows, int[] colors);

    void triangle(float x1, float y1, float x2, float y2, float x3, float y3, int color);

    void text(String text, int x, int y, int color);

    int textWidth(String text);

    String trim(String text, int width);

    void item(ItemStack stack, int x, int y);

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

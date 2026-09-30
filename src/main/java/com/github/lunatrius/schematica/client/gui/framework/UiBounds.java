package com.github.lunatrius.schematica.client.gui.framework;

public final class UiBounds {

    public final int x;
    public final int y;
    public final int width;
    public final int height;

    public UiBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
    }

    public int right() {
        return x + width;
    }

    public int bottom() {
        return y + height;
    }

    public boolean contains(int px, int py) {
        return px >= x && py >= y && px < right() && py < bottom();
    }

    public boolean isEmpty() {
        return width == 0 || height == 0;
    }

    public UiBounds intersect(UiBounds other) {
        int left = Math.max(x, other.x);
        int top = Math.max(y, other.y);
        return new UiBounds(left, top, Math.min(right(), other.right()) - left,
            Math.min(bottom(), other.bottom()) - top);
    }

    public UiBounds inset(int amount) {
        return new UiBounds(x + amount, y + amount, width - amount * 2, height - amount * 2);
    }

    public UiBounds toScissor(int scale, int displayWidth, int displayHeight) {
        if (scale < 1) throw new IllegalArgumentException("Invalid GUI scale");
        return new UiBounds(x * scale, displayHeight - bottom() * scale, width * scale, height * scale)
            .intersect(new UiBounds(0, 0, displayWidth, displayHeight));
    }
}

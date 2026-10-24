package com.github.lunatrius.schematica.client.renderer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class RenderSectionIndex<T> {
    private final int width, height, length;
    private final List<T> sections;

    RenderSectionIndex(int width, int height, int length, List<T> sections) {
        if (width < 1 || height < 1 || length < 1 || (long) width * height * length != sections.size()) {
            throw new IllegalArgumentException("Invalid render section dimensions");
        }
        this.width = width;
        this.height = height;
        this.length = length;
        this.sections = new ArrayList<>(sections);
    }

    void visit(long minX, long minY, long minZ, long maxX, long maxY, long maxZ, Consumer<T> visitor) {
        if (minX > maxX || minY > maxY || minZ > maxZ) return;
        long x0 = Math.max(0, minX >> 4), y0 = Math.max(0, minY >> 4), z0 = Math.max(0, minZ >> 4);
        long x1 = Math.min(width - 1L, maxX >> 4), y1 = Math.min(height - 1L, maxY >> 4), z1 = Math.min(length - 1L, maxZ >> 4);
        for (long y = y0; y <= y1; y++) for (long z = z0; z <= z1; z++) for (long x = x0; x <= x1; x++) {
            visitor.accept(sections.get((int) ((y * length + z) * width + x)));
        }
    }
}

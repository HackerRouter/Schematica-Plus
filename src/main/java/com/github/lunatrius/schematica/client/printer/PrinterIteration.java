// Printer iteration area and order after the behavior of litematica-printer (shape, axis order, axis reversal), by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.printer;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.handler.PrinterRange;

/** The block offsets around the player that one printer pass visits, in the configured order. */
public final class PrinterIteration {
    public static final String[] SHAPES = {"sphere", "octahedron", "cube"};
    /** The first axis changes fastest: xzy fills a layer along x, then z, before moving to the next y. */
    public static final String[] ORDERS = {"xzy", "xyz", "yxz", "yzx", "zxy", "zyx"};

    private static String cachedKey;
    private static List<int[]> cached = Collections.emptyList();

    private PrinterIteration() {}

    /** Resume a time-limited scan so distant offsets are eventually visited. */
    static final class Cursor {
        private int index;
        int next(int size) {
            if (index >= size) index = 0;
            return index++;
        }
        void reset() { index = 0; }
    }

    public static synchronized List<int[]> offsets(int radius, String shape, String order, boolean reverseX, boolean reverseY, boolean reverseZ) {
        radius = clampRadius(radius);
        return offsets(radius, shape, order, reverseX, reverseY, reverseZ,
            new int[] {-radius, -radius, -radius}, new int[] {radius, radius, radius});
    }

    static synchronized List<int[]> offsets(int radius, String shape, String order, boolean reverseX, boolean reverseY, boolean reverseZ, int[] min, int[] max) {
        String key = radius + shape + order + reverseX + reverseY + reverseZ + Arrays.toString(min) + Arrays.toString(max);
        if (!key.equals(cachedKey)) {
            cached = build(radius, shape, order, reverseX, reverseY, reverseZ, min, max);
            cachedKey = key;
        }
        return cached;
    }

    static List<int[]> offsets(List<SchematicWorld> placements, int x, int y, int z, int radius) {
        int[] min = {radius + 1, radius + 1, radius + 1}, max = {-radius - 1, -radius - 1, -radius - 1};
        for (SchematicWorld placement : placements) {
            long[] start = {(long) placement.position.x - x, (long) placement.position.y - y, (long) placement.position.z - z};
            int[] size = {placement.getWidth(), placement.getHeight(), placement.getLength()};
            boolean intersects = true;
            for (int i = 0; i < 3; i++) if (start[i] > radius || start[i] + size[i] - 1 < -radius) intersects = false;
            if (!intersects) continue;
            for (int i = 0; i < 3; i++) {
                min[i] = Math.min(min[i], (int) Math.max(-radius, start[i]));
                max[i] = Math.max(max[i], (int) Math.min(radius, start[i] + size[i] - 1));
            }
        }
        min[1] = Math.max(min[1], -y); max[1] = Math.min(max[1], 255 - y);
        return PrinterIteration.offsets(radius, ConfigurationHandler.printerIteratorShape,
            ConfigurationHandler.printerIteratorMode, ConfigurationHandler.printerXAxisReverse,
            ConfigurationHandler.printerYAxisReverse, ConfigurationHandler.printerZAxisReverse, min, max);
    }

    static List<int[]> build(int radius, String shape, String order, boolean reverseX, boolean reverseY, boolean reverseZ) {
        radius = clampRadius(radius);
        return build(radius, shape, order, reverseX, reverseY, reverseZ,
            new int[] {-radius, -radius, -radius}, new int[] {radius, radius, radius});
    }

    static List<int[]> build(int radius, String shape, String order, boolean reverseX, boolean reverseY, boolean reverseZ, int[] min, int[] max) {
        radius = clampRadius(radius);
        min = min.clone(); max = max.clone();
        for (int i = 0; i < 3; i++) {
            min[i] = Math.max(-radius, min[i]); max[i] = Math.min(radius, max[i]);
            if (min[i] > max[i]) return Collections.emptyList();
        }
        return new Offsets(radius, valid(shape, SHAPES, "sphere"), valid(order, ORDERS, "xzy"),
            new boolean[] {reverseX, reverseY, reverseZ}, min, max);
    }

    /** Index rows instead of allocating a coordinate array for every block in the volume. */
    private static final class Offsets extends AbstractList<int[]> {
        private final int[] axes, min, max, ends, starts;
        private final boolean[] reverse;
        private final int middleSize;

        Offsets(int radius, String shape, String order, boolean[] reverse, int[] min, int[] max) {
            this.axes = new int[] {axis(order.charAt(0)), axis(order.charAt(1)), axis(order.charAt(2))};
            this.reverse = reverse; this.min = min; this.max = max;
            middleSize = max[axes[1]] - min[axes[1]] + 1;
            int rows = middleSize * (max[axes[2]] - min[axes[2]] + 1);
            ends = new int[rows + 1]; starts = new int[rows];
            for (int row = 0; row < rows; row++) {
                int middle = coordinate(row % middleSize, axes[1]), outer = coordinate(row / middleSize, axes[2]);
                int reach = radius;
                if ("octahedron".equals(shape)) reach -= Math.abs(middle) + Math.abs(outer);
                else if ("sphere".equals(shape)) {
                    int squared = radius * radius - middle * middle - outer * outer;
                    reach = squared < 0 ? -1 : (int) Math.sqrt(squared);
                }
                int first = Math.max(min[axes[0]], -reach), last = Math.min(max[axes[0]], reach);
                starts[row] = reverse[axes[0]] ? last : first;
                ends[row + 1] = ends[row] + Math.max(0, last - first + 1);
            }
        }

        private int coordinate(int i, int axis) { return reverse[axis] ? max[axis] - i : min[axis] + i; }

        @Override public int size() { return ends[ends.length - 1]; }

        @Override public int[] get(int index) {
            if (index < 0 || index >= size()) throw new IndexOutOfBoundsException(Integer.toString(index));
            int low = 0, high = ends.length - 1;
            while (low < high) {
                int middle = (low + high) >>> 1;
                if (ends[middle] <= index) low = middle + 1; else high = middle;
            }
            int row = low - 1;
            int[] offset = new int[3];
            offset[axes[0]] = starts[row] + (reverse[axes[0]] ? -1 : 1) * (index - ends[row]);
            offset[axes[1]] = coordinate(row % middleSize, axes[1]);
            offset[axes[2]] = coordinate(row / middleSize, axes[2]);
            return offset;
        }
    }

    private static int clampRadius(int radius) { return Math.max(0, Math.min((int) PrinterRange.MAX, radius)); }

    private static int axis(char c) { return c == 'x' ? 0 : c == 'y' ? 1 : 2; }

    public static String valid(String value, String[] values, String fallback) {
        String lower = value == null ? "" : value.toLowerCase(Locale.ROOT);
        for (String candidate : values) if (candidate.equals(lower)) return candidate;
        return fallback;
    }
}

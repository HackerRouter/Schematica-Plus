// Printer iteration area and order after the behavior of litematica-printer (shape, axis order, axis reversal), by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.printer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** The block offsets around the player that one printer pass visits, in the configured order. */
public final class PrinterIteration {
    public static final String[] SHAPES = {"sphere", "octahedron", "cube"};
    /** The first axis changes fastest: xzy fills a layer along x, then z, before moving to the next y. */
    public static final String[] ORDERS = {"xzy", "xyz", "yxz", "yzx", "zxy", "zyx"};

    private static String cachedKey;
    private static List<int[]> cached = Collections.emptyList();

    private PrinterIteration() {}

    public static synchronized List<int[]> offsets(int radius, String shape, String order, boolean reverseX, boolean reverseY, boolean reverseZ) {
        String key = radius + shape + order + reverseX + reverseY + reverseZ;
        if (!key.equals(cachedKey)) {
            cached = Collections.unmodifiableList(build(radius, shape, order, reverseX, reverseY, reverseZ));
            cachedKey = key;
        }
        return cached;
    }

    static List<int[]> build(int radius, String shape, String order, boolean reverseX, boolean reverseY, boolean reverseZ) {
        radius = Math.max(0, radius);
        String axes = valid(order, ORDERS, "xzy");
        String form = valid(shape, SHAPES, "sphere");
        boolean[] reverse = {reverseX, reverseY, reverseZ};
        int[] index = {axis(axes.charAt(0)), axis(axes.charAt(1)), axis(axes.charAt(2))};
        int size = 2 * radius + 1;
        List<int[]> result = new ArrayList<>();
        int[] offset = new int[3];
        for (int outer = 0; outer < size; outer++) {
            offset[index[2]] = step(outer, radius, reverse[index[2]]);
            for (int middle = 0; middle < size; middle++) {
                offset[index[1]] = step(middle, radius, reverse[index[1]]);
                for (int inner = 0; inner < size; inner++) {
                    offset[index[0]] = step(inner, radius, reverse[index[0]]);
                    if (inside(form, offset, radius)) result.add(offset.clone());
                }
            }
        }
        return result;
    }

    private static int step(int i, int radius, boolean reverse) { return reverse ? radius - i : i - radius; }

    private static int axis(char c) { return c == 'x' ? 0 : c == 'y' ? 1 : 2; }

    private static boolean inside(String shape, int[] o, int radius) {
        switch (shape) {
            case "octahedron": return Math.abs(o[0]) + Math.abs(o[1]) + Math.abs(o[2]) <= radius;
            case "cube": return true;
            default: return o[0] * o[0] + o[1] * o[1] + o[2] * o[2] <= radius * radius;
        }
    }

    public static String valid(String value, String[] values, String fallback) {
        String lower = value == null ? "" : value.toLowerCase(Locale.ROOT);
        for (String candidate : values) if (candidate.equals(lower)) return candidate;
        return fallback;
    }
}

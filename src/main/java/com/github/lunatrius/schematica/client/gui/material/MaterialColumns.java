// SPDX-License-Identifier: LGPL-3.0-only
// Litematica material column spacing, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.material;

public final class MaterialColumns {
    private MaterialColumns() {}

    public static int[] positions(int rowWidth, int nameWidth, int[] countWidths, int ignoreWidth) {
        int available = Math.max(0, rowWidth - ignoreWidth - 8);
        int[] widths = { nameWidth + 40, countWidths[0] + 20, countWidths[1] + 20, countWidths[2] + 20 };
        int total = widths[0] + widths[1] + widths[2] + widths[3];
        if (total > available) {
            widths[0] = Math.max(48, widths[0] - (total - available));
            total = widths[0] + widths[1] + widths[2] + widths[3];
            if (total > available) {
                int remaining = Math.max(0, available - widths[0]);
                int counts = widths[1] + widths[2] + widths[3];
                widths[1] = (int) ((long) widths[1] * remaining / Math.max(1, counts));
                widths[2] = (int) ((long) widths[2] * remaining / Math.max(1, counts));
                widths[3] = remaining - widths[1] - widths[2];
                widths[0] = Math.min(widths[0], available);
            }
        }
        int[] positions = new int[5];
        positions[0] = 4;
        for (int i = 0; i < 4; i++) positions[i + 1] = positions[i] + widths[i];
        return positions;
    }

    /** How many single (27 slots) and double (54 slots) chests a count fills. */
    public static String storage(long count, int stackSize, java.util.function.BiFunction<String, Object[], String> format) {
        long perChest = 27L * Math.max(1, stackSize);
        return format.apply("schematica.ui.material.storage.value", new Object[] {
            String.format(java.util.Locale.ROOT, "%.2f", count / (double) perChest), String.format(java.util.Locale.ROOT, "%.2f", count / (2.0 * perChest))});
    }

    public static String stackCount(long count, int stackSize) {
        int size = Math.max(1, stackSize);
        if (size == 1 || count <= size) return Long.toString(count);
        return count + " = " + count / size + " x " + size + (count % size == 0 ? "" : " + " + count % size);
    }
}

package com.github.lunatrius.schematica.client.renderer;

import java.util.concurrent.CancellationException;

final class QuadOrder {
    static final class Result {
        final int[] vertices, order;
        Result(int[] vertices, int[] order) { this.vertices = vertices; this.order = order; }
    }

    private QuadOrder() {}

    static Result sort(int[] vertices, double x, double y, double z) {
        if (vertices.length % 32 != 0 || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("Invalid quad data or camera");
        }
        int quads = vertices.length / 32;
        int[] order = new int[quads], scratch = new int[quads];
        double[] distances = new double[quads];
        for (int i = 0; i < quads; i++) {
            double cx = 0, cy = 0, cz = 0;
            for (int v = 0; v < 4; v++) {
                int base = i * 32 + v * 8;
                cx += Float.intBitsToFloat(vertices[base]);
                cy += Float.intBitsToFloat(vertices[base + 1]);
                cz += Float.intBitsToFloat(vertices[base + 2]);
            }
            cx = cx / 4 - x; cy = cy / 4 - y; cz = cz / 4 - z;
            distances[i] = cx * cx + cy * cy + cz * cz;
            order[i] = i;
        }
        for (int width = 1; width < quads; width *= 2) {
            if (Thread.currentThread().isInterrupted()) throw new CancellationException();
            for (int start = 0; start < quads; start += width * 2) {
                int mid = Math.min(quads, start + width), end = Math.min(quads, mid + width);
                int a = start, b = mid;
                for (int out = start; out < end; out++) {
                    boolean left = b == end || a < mid && (distances[order[a]] > distances[order[b]]
                        || distances[order[a]] == distances[order[b]] && order[a] < order[b]);
                    scratch[out] = order[left ? a++ : b++];
                }
            }
            int[] swap = order; order = scratch; scratch = swap;
        }
        int[] sorted = new int[vertices.length];
        for (int i = 0; i < quads; i++) System.arraycopy(vertices, order[i] * 32, sorted, i * 32, 32);
        return new Result(sorted, order);
    }
}

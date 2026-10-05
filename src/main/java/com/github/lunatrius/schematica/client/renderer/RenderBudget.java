// Schematic render distance that shrinks while the frame rate is low, after Buildprint's FPS guard, by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.renderer;

/**
 * schematicRenderDistance limits how far from the camera schematic chunks are drawn and rebuilt (0: no limit).
 * With adaptiveRenderingMinFps, every second below that frame rate shrinks the distance by a quarter (not below
 * MIN_DISTANCE); after three seconds well above it the distance grows again up to the configured one.
 */
public final class RenderBudget {
    public static final double MIN_DISTANCE = 32, UNLIMITED = Double.MAX_VALUE;
    static final double START_LIMIT = 512;

    public static volatile int configuredDistance, minFps = 30;

    private double limit = UNLIMITED;
    private long second = -1;
    private int frames, goodSeconds;
    private boolean reduced;

    /** Counts a frame; returns true when the distance shrank for the first time since it was last restored. */
    public boolean frame(long nanos) {
        long now = nanos / 1_000_000_000L;
        if (second < 0) second = now;
        frames++;
        if (now == second) return false;
        if (now - second > 2) {
            // frames were not counted in between (nothing drawn): start over
            frames = 0;
            second = now;
            return false;
        }
        int fps = (int) (frames / (now - second));
        frames = 0;
        second = now;
        return update(fps);
    }

    boolean update(int fps) {
        double configured = configuredDistance > 0 ? configuredDistance : UNLIMITED;
        if (minFps <= 0) {
            limit = configured;
            reduced = false;
            goodSeconds = 0;
            return false;
        }
        if (fps < minFps) {
            goodSeconds = 0;
            double base = Math.min(limit, Math.min(configured, START_LIMIT));
            double next = Math.max(MIN_DISTANCE, base * 0.75);
            if (next >= limit) return false;
            limit = next;
            boolean first = !reduced;
            reduced = true;
            return first;
        }
        if (fps >= minFps * 1.25 && limit < configured && ++goodSeconds >= 3) {
            goodSeconds = 0;
            limit = limit * 1.25 >= Math.min(configured, START_LIMIT) ? configured : limit * 1.25;
            if (limit == configured) reduced = false;
        }
        if (limit > configured) limit = configured;
        return false;
    }

    /** The distance in blocks to draw and rebuild schematic chunks within. */
    public double limit() {
        double configured = configuredDistance > 0 ? configuredDistance : UNLIMITED;
        return minFps <= 0 ? configured : Math.min(limit, configured);
    }

    public boolean reduced() { return reduced && limit() < (configuredDistance > 0 ? configuredDistance : UNLIMITED); }

    /** The distance from a point to a box. */
    public static double distance(double x, double y, double z, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        double dx = Math.max(0, Math.max(minX - x, x - maxX)), dy = Math.max(0, Math.max(minY - y, y - maxY)), dz = Math.max(0, Math.max(minZ - z, z - maxZ));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}

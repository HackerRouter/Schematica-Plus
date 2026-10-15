// SPDX-License-Identifier: LGPL-3.0-only
package com.github.lunatrius.schematica.handler;

public final class PrinterRange {
    public static final double MAX = 32;

    private PrinterRange() {}

    public static double clamp(double range) {
        return Double.isFinite(range) ? Math.max(0, Math.min(MAX, range)) : 0;
    }

    public static double effective(double requested, double nativeReach, double serverMax) {
        double limit = serverMax > 0 ? clamp(serverMax) : nativeReach;
        return Math.min(requested > 0 ? clamp(requested) : nativeReach, limit);
    }

    public static boolean contains(double eyeX, double eyeY, double eyeZ, int x, int y, int z, double range) {
        if (y < 0 || y > 255 || Math.abs((long) x) >= 30_000_000 || Math.abs((long) z) >= 30_000_000) return false;
        double dx = x + 0.5 - eyeX, dy = y + 0.5 - eyeY, dz = z + 0.5 - eyeZ;
        return dx * dx + dy * dy + dz * dz <= range * range;
    }
}

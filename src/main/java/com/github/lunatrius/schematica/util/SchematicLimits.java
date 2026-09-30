package com.github.lunatrius.schematica.util;

/** Allocation and coordinate limits shared by disk, network and editing entry points. */
public final class SchematicLimits {
    public static final long MAX_BLOCKS = 16L * 1024 * 1024;
    public static final long MAX_NBT_BYTES = 128L * 1024 * 1024;
    public static final int MAX_NBT_DEPTH = 64;

    private SchematicLimits() {}

    public static int volume(long width, long height, long length) {
        if (width <= 0 || height <= 0 || length <= 0
            || width > Short.MAX_VALUE || height > Short.MAX_VALUE || length > Short.MAX_VALUE) {
            throw new IllegalArgumentException("Schematic dimensions must be between 1 and 32767");
        }
        long volume = width * height * length;
        if (volume > MAX_BLOCKS || width * height > 1024L * 1024) {
            throw new IllegalArgumentException("Schematic exceeds the allocation limit (16777216 blocks)");
        }
        return (int) volume;
    }

    public static int dimension(int min, int max) {
        long size = (long) max - min + 1;
        if (size <= 0 || size > Short.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid schematic extent");
        }
        return (int) size;
    }

    public static void worldBounds(long minX, long minY, long minZ, long maxX, long maxY, long maxZ) {
        if (minX < -30000000 || minZ < -30000000 || maxX >= 30000000 || maxZ >= 30000000
            || minY < 0 || maxY >= 256 || minX > maxX || minY > maxY || minZ > maxZ) {
            throw new IllegalArgumentException("Selection is outside world bounds (Y must be 0..255)");
        }
        volume(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1);
    }
}

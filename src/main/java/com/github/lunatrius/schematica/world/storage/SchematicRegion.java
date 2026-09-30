package com.github.lunatrius.schematica.world.storage;

import com.github.lunatrius.schematica.util.SchematicTransform;

public final class SchematicRegion {
    public final String name;
    public final int minX, minY, minZ, maxX, maxY, maxZ;

    public SchematicRegion(String name, int ax, int ay, int az, int bx, int by, int bz) {
        if (name == null || name.trim().isEmpty() || name.length() > 200) throw new IllegalArgumentException("Invalid region name");
        this.name = name;
        minX = Math.min(ax, bx); minY = Math.min(ay, by); minZ = Math.min(az, bz);
        maxX = Math.max(ax, bx); maxY = Math.max(ay, by); maxZ = Math.max(az, bz);
    }

    public boolean contains(int x, int y, int z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public SchematicRegion offset(int x, int y, int z) {
        return new SchematicRegion(name, Math.addExact(minX, x), Math.addExact(minY, y), Math.addExact(minZ, z),
            Math.addExact(maxX, x), Math.addExact(maxY, y), Math.addExact(maxZ, z));
    }

    public SchematicRegion transform(char operation, int width, int height, int length) {
        double[] a = SchematicTransform.point(operation, minX, minY, minZ, width - 1, height - 1, length - 1);
        double[] b = SchematicTransform.point(operation, maxX, maxY, maxZ, width - 1, height - 1, length - 1);
        return new SchematicRegion(name, (int) a[0], (int) a[1], (int) a[2], (int) b[0], (int) b[1], (int) b[2]);
    }
}

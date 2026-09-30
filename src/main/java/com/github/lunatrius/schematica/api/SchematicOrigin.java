package com.github.lunatrius.schematica.api;

public final class SchematicOrigin {
    public static final SchematicOrigin ZERO = new SchematicOrigin(0, 0, 0);
    public final int x, y, z;

    public SchematicOrigin(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }

    public boolean isZero() { return x == 0 && y == 0 && z == 0; }
    public int[] coordinates() { return new int[] {x, y, z}; }

    public SchematicOrigin atMinimum(int minX, int minY, int minZ) {
        return new SchematicOrigin(Math.addExact(minX, x), Math.addExact(minY, y), Math.addExact(minZ, z));
    }

    public SchematicOrigin minimumAt(SchematicOrigin worldOrigin) {
        return new SchematicOrigin(Math.subtractExact(worldOrigin.x, x), Math.subtractExact(worldOrigin.y, y), Math.subtractExact(worldOrigin.z, z));
    }

    public SchematicOrigin restoredMinimum(int x, int y, int z, int[] savedOrigin) {
        if (savedOrigin == null) return new SchematicOrigin(x, y, z);
        if (savedOrigin.length != 3) throw new IllegalArgumentException("Invalid saved placement origin");
        return minimumAt(new SchematicOrigin(savedOrigin[0], savedOrigin[1], savedOrigin[2]));
    }

    public SchematicOrigin transform(char operation, int width, int height, int length) {
        switch (operation) {
            case 'X': return new SchematicOrigin(x, z, Math.subtractExact(height - 1, y));
            case 'Y': return new SchematicOrigin(Math.subtractExact(length - 1, z), y, x);
            case 'Z': return new SchematicOrigin(y, Math.subtractExact(width - 1, x), z);
            case 'x': return new SchematicOrigin(Math.subtractExact(width - 1, x), y, z);
            case 'y': return new SchematicOrigin(x, Math.subtractExact(height - 1, y), z);
            case 'z': return new SchematicOrigin(x, y, Math.subtractExact(length - 1, z));
            default: throw new IllegalArgumentException("Unknown schematic transform");
        }
    }
}

package com.github.lunatrius.schematica.util;

/** Uppercase operations rotate; lowercase operations mirror. Dimensions use continuous coordinates. */
public final class SchematicTransform {
    private SchematicTransform() {}

    public static double[] point(char operation, double x, double y, double z, double w, double h, double l) {
        switch (operation) {
            case 'X': return new double[] {x, z, h - y};
            case 'Y': return new double[] {l - z, y, x};
            case 'Z': return new double[] {y, w - x, z};
            case 'x': return new double[] {w - x, y, z};
            case 'y': return new double[] {x, h - y, z};
            case 'z': return new double[] {x, y, l - z};
            default: throw new IllegalArgumentException("Unknown schematic transform: " + operation);
        }
    }
}

package com.github.lunatrius.schematica.util;

import java.lang.reflect.Array;
import net.minecraftforge.common.util.ForgeDirection;

/** Uppercase operations rotate; lowercase operations mirror. Dimensions use continuous coordinates. */
public final class SchematicTransform {
    private SchematicTransform() {}

    public static ForgeDirection direction(char operation, ForgeDirection side) {
        if (side == ForgeDirection.UNKNOWN) return side;
        double[] vector = point(operation, side.offsetX, side.offsetY, side.offsetZ, 0, 0, 0);
        for (ForgeDirection candidate : ForgeDirection.VALID_DIRECTIONS) {
            if (candidate.offsetX == vector[0] && candidate.offsetY == vector[1] && candidate.offsetZ == vector[2]) return candidate;
        }
        throw new IllegalArgumentException("Invalid transformed direction");
    }

    public static int sideMask(char operation, int mask) {
        int result = mask & ~63;
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            if ((mask & side.flag) != 0) result |= direction(operation, side).flag;
        }
        return result;
    }

    public static void sides(char operation, Object values) {
        if (!values.getClass().isArray() || Array.getLength(values) != 6) {
            throw new IllegalArgumentException("Expected six directional values");
        }
        Object copy = Array.newInstance(values.getClass().getComponentType(), 6);
        System.arraycopy(values, 0, copy, 0, 6);
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            Array.set(values, direction(operation, side).ordinal(), Array.get(copy, side.ordinal()));
        }
    }

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

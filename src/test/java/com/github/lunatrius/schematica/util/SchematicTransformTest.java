package com.github.lunatrius.schematica.util;

import org.junit.Test;
import net.minecraftforge.common.util.ForgeDirection;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

public class SchematicTransformTest {
    @Test public void sideTransformsFollowNeighborCoordinatesAndRoundTrip() {
        for (char operation : "XYZxyz".toCharArray()) {
            int[] channels = {4, 8, 16, 32, 64, 128};
            int[] original = channels.clone();
            for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
                double[] origin = SchematicTransform.point(operation, 3, 4, 5, 10, 10, 10);
                double[] neighbor = SchematicTransform.point(operation, 3 + side.offsetX, 4 + side.offsetY,
                    5 + side.offsetZ, 10, 10, 10);
                ForgeDirection transformed = SchematicTransform.direction(operation, side);
                assertArrayEquals(new double[] {transformed.offsetX, transformed.offsetY, transformed.offsetZ},
                    new double[] {neighbor[0] - origin[0], neighbor[1] - origin[1], neighbor[2] - origin[2]}, 0);
            }
            SchematicTransform.sides(operation, channels);
            for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
                assertEquals(original[side.ordinal()], channels[SchematicTransform.direction(operation, side).ordinal()]);
            }
            int mask = 0x80 | ForgeDirection.NORTH.flag | ForgeDirection.UP.flag;
            int steps = Character.isUpperCase(operation) ? 4 : 2;
            for (int i = 0; i < steps; i++) mask = SchematicTransform.sideMask(operation, mask);
            for (int i = 1; i < steps; i++) SchematicTransform.sides(operation, channels);
            assertEquals(0x80 | ForgeDirection.NORTH.flag | ForgeDirection.UP.flag, mask);
            assertArrayEquals(original, channels);
            assertEquals(ForgeDirection.UNKNOWN, SchematicTransform.direction(operation, ForgeDirection.UNKNOWN));
        }
    }

    @Test public void rotationsPreserveFractionalEntityPositionAndRoundTrip() {
        double[] p = {1.25, 2.5, 3.75};
        for (char axis : "XYZ".toCharArray()) {
            double[] q = p.clone();
            for (int i = 0; i < 4; i++) q = SchematicTransform.point(axis, q[0], q[1], q[2], 10, 10, 10);
            assertArrayEquals(p, q, 0.000001);
        }
    }

    @Test public void mirrorsRoundTripAndMixedRotationOrderMatters() {
        double[] p = {1, 2, 3};
        for (char axis : "xyz".toCharArray()) {
            double[] q = SchematicTransform.point(axis, p[0], p[1], p[2], 10, 10, 10);
            q = SchematicTransform.point(axis, q[0], q[1], q[2], 10, 10, 10);
            assertArrayEquals(p, q, 0);
        }
        double[] x = SchematicTransform.point('X', 1, 2, 3, 10, 10, 10);
        double[] xy = SchematicTransform.point('Y', x[0], x[1], x[2], 10, 10, 10);
        double[] y = SchematicTransform.point('Y', 1, 2, 3, 10, 10, 10);
        double[] yx = SchematicTransform.point('X', y[0], y[1], y[2], 10, 10, 10);
        assertFalse(java.util.Arrays.equals(xy, yx));
    }
}

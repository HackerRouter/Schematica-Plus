package com.github.lunatrius.schematica.util;

import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;

public class SchematicTransformTest {
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

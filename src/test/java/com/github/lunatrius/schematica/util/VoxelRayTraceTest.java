package com.github.lunatrius.schematica.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Vec3;
import org.junit.Test;
import static org.junit.Assert.*;

public class VoxelRayTraceTest {
    @Test public void visitsNegativeCoordinatesWithoutSteppingPastTheEndpoint() {
        List<Integer> visited = new ArrayList<>();
        assertNull(VoxelRayTrace.trace(v(-0.5, 64.5, 0.5), v(-10.5, 64.5, 0.5), (x, y, z) -> {
            assertEquals(64, y);
            assertEquals(0, z);
            visited.add(x);
            return null;
        }));
        assertEquals(11, visited.size());
        for (int i = 0; i < visited.size(); i++) assertEquals(-1 - i, (int) visited.get(i));
    }

    @Test public void handlesVerticalRaysDiagonalsAndExactBoundaries() {
        assertEquals(Integer.valueOf(62), VoxelRayTrace.trace(v(0.5, 64, 0.5), v(0.5, 54, 0.5), (x, y, z) -> y == 62 ? y : null));
        List<String> cells = new ArrayList<>();
        String hit = VoxelRayTrace.trace(v(0.5, 0.5, 0.5), v(10.5, 10.5, 10.5), (x, y, z) -> {
            String position = x + "," + y + "," + z;
            assertFalse(cells.contains(position));
            cells.add(position);
            return x == 10 && y == 10 && z == 10 ? position : null;
        });
        assertEquals("10,10,10", hit);
        assertEquals(31, cells.size());
    }

    @Test public void stopsAtFirstShapeHitAndAllowsWorldBorderCrossing() {
        List<Integer> cells = new ArrayList<>();
        assertEquals(Integer.valueOf(29999999), VoxelRayTrace.trace(v(29999998.5, 64, 0), v(30000008.5, 64, 0), (x, y, z) -> {
            cells.add(x);
            return x == 29999999 ? x : null;
        }));
        assertEquals(2, cells.size());
        assertEquals(Integer.valueOf(1), VoxelRayTrace.trace(v(0.5, 0, 0), v(10.5, 0, 0), (x, y, z) -> x == 0 ? null : x));
    }

    @Test public void rejectsInvalidRaysAndBoundsWorkForLongOrStationaryRays() {
        int[] visits = {0};
        assertNull(VoxelRayTrace.trace(v(Double.NaN, 0, 0), v(1, 1, 1), (x, y, z) -> ++visits[0]));
        assertNull(VoxelRayTrace.trace(v(0, 0, 0), v(Double.POSITIVE_INFINITY, 1, 1), (x, y, z) -> ++visits[0]));
        assertEquals(0, visits[0]);
        VoxelRayTrace.trace(v(0.5, 0.5, 0.5), v(0.5, 0.5, 0.5), (x, y, z) -> { visits[0]++; return null; });
        assertEquals(1, visits[0]);
        visits[0] = 0;
        VoxelRayTrace.trace(v(0.5, 0, 0), v(100000, 0, 0), (x, y, z) -> { visits[0]++; return null; });
        assertEquals(256, visits[0]);
    }

    private static Vec3 v(double x, double y, double z) { return Vec3.createVectorHelper(x, y, z); }
}

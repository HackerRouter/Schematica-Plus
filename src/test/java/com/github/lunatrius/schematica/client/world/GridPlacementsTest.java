package com.github.lunatrius.schematica.client.world;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GridPlacementsTest {
    private static GridSettings grid(int size, int negative, int positive) {
        GridSettings grid = new GridSettings();
        grid.setDefaultSize(new int[] {4, 2, 4});
        grid.setSize(new int[] {size, size, size});
        grid.setRepeatNegative(new int[] {negative, 0, negative});
        grid.setRepeatPositive(new int[] {positive, 0, positive});
        grid.toggleEnabled();
        return grid;
    }

    private static Set<String> keys(List<int[]> points) {
        Set<String> keys = new HashSet<>();
        for (int[] point : points) keys.add(point[0] + "," + point[1] + "," + point[2]);
        return keys;
    }

    @Test public void sizeNeverShrinksBelowThePlacementAndRoundTripsThroughJson() {
        GridSettings grid = new GridSettings();
        grid.setDefaultSize(new int[] {5, 3, 7});
        assertArrayEquals(new int[] {5, 3, 7}, grid.size());
        grid.setSize(0, 2);
        assertArrayEquals(new int[] {5, 3, 7}, grid.size());
        grid.setSize(1, 10);
        grid.setRepeatPositive(new int[] {2, 0, -4});
        assertArrayEquals(new int[] {2, 0, 0}, grid.repeatPositive());
        grid.toggleEnabled();
        GridSettings read = new GridSettings();
        read.setDefaultSize(new int[] {5, 3, 7});
        read.fromJson(grid.toJson());
        assertEquals(grid, read);
        assertTrue(read.isInitialized());
        grid.setDefaultSize(new int[] {8, 3, 7});
        assertArrayEquals(new int[] {8, 10, 7}, grid.size());
        grid.resetSize();
        assertArrayEquals(new int[] {8, 3, 7}, grid.size());
    }

    @Test public void pointsCoverTheRepeatsInsideTheLoadedArea() {
        int[] base = {0, 64, 0, 3, 65, 3};
        Set<String> all = keys(GridPlacements.points(base, grid(10, 1, 2), new int[] {-1000, 0, -1000, 1000, 255, 1000}));
        assertEquals(15, all.size());
        assertFalse(all.contains("0,0,0"));
        assertTrue(all.contains("-1,0,-1"));
        assertTrue(all.contains("2,0,2"));
        assertFalse(all.contains("3,0,0"));
        Set<String> near = keys(GridPlacements.points(base, grid(10, 1, 2), new int[] {-5, 0, -5, 5, 255, 5}));
        assertEquals(new HashSet<>(java.util.Arrays.asList("-1,0,-1", "-1,0,0", "0,0,-1")), near);
        assertTrue(GridPlacements.points(base, new GridSettings(), new int[] {-1000, 0, -1000, 1000, 255, 1000}).isEmpty());
    }
}

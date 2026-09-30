package com.github.lunatrius.schematica.world.storage;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public class RegionSelectionTest {
    @Test public void unionsOverlapsWithoutCapturingTheGapAndRetainsNegativeWorldOrigin() {
        RegionSelection selection = new RegionSelection(Arrays.asList(
            new SchematicRegion("First", -10, 64, -3, -8, 64, -3),
            new SchematicRegion("Overlap", -9, 64, -3, -8, 64, -3),
            new SchematicRegion("Last", -2, 64, -3, -2, 64, -3)));
        assertEquals(-10, selection.minX);
        assertEquals(-2, selection.maxX);
        BitSet mask = RegionMask.create(selection.localRegions, 9, 1, 1);
        assertEquals(4, mask.cardinality());
        assertFalse(mask.get(3));
        assertTrue(mask.get(8));
        assertThrows(UnsupportedOperationException.class, () -> selection.localRegions.clear());
    }

    @Test public void allRotationsAndMirrorsKeepTheExactSelectedCells() {
        SchematicRegion region = new SchematicRegion("Pipes", 0, 1, 1, 1, 2, 3);
        for (char op : new char[] {'X', 'Y', 'Z', 'x', 'y', 'z'}) {
            int w = 4, h = 3, l = 5;
            SchematicRegion transformed = region.transform(op, w, h, l);
            for (int x = 0; x < w; x++) for (int y = 0; y < h; y++) for (int z = 0; z < l; z++) {
                double[] p = com.github.lunatrius.schematica.util.SchematicTransform.point(op, x, y, z, w - 1, h - 1, l - 1);
                assertEquals(region.contains(x, y, z), transformed.contains((int) p[0], (int) p[1], (int) p[2]));
            }
        }
    }

    @Test public void rejectsEmptyOversizedAndOutOfBoundsSelectionsBeforeAllocation() {
        assertThrows(IllegalArgumentException.class, () -> new RegionSelection(Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> new RegionSelection(Arrays.asList(
            new SchematicRegion("A", 0, 0, 0, 0, 0, 0), new SchematicRegion("B", 100000, 0, 0, 100000, 0, 0))));
        assertThrows(IllegalArgumentException.class, () -> RegionMask.create(Collections.singletonList(
            new SchematicRegion("Bad", -1, 0, 0, 0, 0, 0)), 2, 2, 2));
    }
}

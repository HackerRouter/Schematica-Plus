package com.github.lunatrius.schematica.client.gui.material;

import org.junit.Test;
import static org.junit.Assert.*;

public class MaterialColumnsTest {
    @Test public void preservesUpstreamSpacingWhenTheColumnsFit() {
        assertArrayEquals(new int[] {4, 144, 194, 254, 324},
            MaterialColumns.positions(600, 100, new int[] {30, 40, 50}, 60));
    }

    @Test public void longNamesAndLargeCountsCannotOverlapTheIgnoreButton() {
        for (int width : new int[] {100, 280, 400, 600}) {
            int[] columns = MaterialColumns.positions(width, 900, new int[] {110, 110, 110}, 50);
            assertEquals(4, columns[0]);
            for (int i = 0; i < 4; i++) assertTrue(columns[i] <= columns[i + 1]);
            assertTrue(columns[4] <= width - 50 - 4);
        }
    }

    @Test public void stackDetailsUseTheItemsActualStackLimitWithoutOverflow() {
        assertEquals("65 = 1 x 64 + 1", MaterialColumns.stackCount(65, 64));
        assertEquals("32 = 2 x 16", MaterialColumns.stackCount(32, 16));
        assertEquals("5", MaterialColumns.stackCount(5, 1));
        assertEquals("5", MaterialColumns.stackCount(5, 0));
        assertEquals("6000000000 = 93750000 x 64", MaterialColumns.stackCount(6000000000L, 64));
    }
}

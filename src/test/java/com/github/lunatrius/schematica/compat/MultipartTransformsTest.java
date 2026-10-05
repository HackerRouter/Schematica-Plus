package com.github.lunatrius.schematica.compat;

import org.junit.Test;

import static org.junit.Assert.*;

public class MultipartTransformsTest {
    private static int slot(String id, int slot, String ops) {
        for (char op : ops.toCharArray()) slot = MultipartTransforms.slot(id, slot, op);
        return slot;
    }

    private static int orient(int orient, String ops) {
        for (char op : ops.toCharArray()) orient = MultipartTransforms.orientation(orient, op);
        return orient;
    }

    @Test public void microblockSlotsTurnAndReturn() {
        assertEquals(5, slot("mcr_face", 2, "Y"));
        assertEquals(2, slot("mcr_edge", 0, "Y"));
        assertEquals(4, slot("mcr_cnr", 0, "x"));
        assertEquals(2, slot("mcr_post", 1, "Y"));
        String[] ids = {"mcr_face", "mcr_hollow", "mcr_cnr", "mcr_edge", "mcr_post"};
        int[] counts = {6, 6, 8, 12, 3};
        for (int i = 0; i < ids.length; i++) {
            for (int s = 0; s < counts[i]; s++) {
                for (String ops : new String[] {"XXXX", "YYYY", "ZZZZ", "xx", "yy", "zz"}) {
                    assertEquals(ids[i] + " " + s + " " + ops, s, slot(ids[i], s, ops));
                }
                for (String op : new String[] {"X", "Y", "Z", "x", "y", "z"}) {
                    int turned = slot(ids[i], s, op);
                    assertTrue(turned >= 0 && turned < counts[i]);
                }
            }
        }
    }

    @Test public void gateOrientationsKeepSideAndFront() {
        for (int orient = 0; orient < 24; orient++) {
            for (String ops : new String[] {"XXXX", "YYYY", "ZZZZ", "xx", "yy", "zz"}) {
                assertEquals(orient + " " + ops, orient, orient(orient, ops));
            }
        }
        // a gate on the floor (side 0) pointing north (rotation 2: sideRotMap[2] = 2) turns to point east
        assertEquals(0 << 2 | 3, orient(0 << 2 | 2, "Y"));
    }
}

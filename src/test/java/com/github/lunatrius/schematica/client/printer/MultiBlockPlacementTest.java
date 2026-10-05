package com.github.lunatrius.schematica.client.printer;

import org.junit.Test;

import com.github.lunatrius.schematica.client.printer.MultiBlockPlacement.Kind;

import static org.junit.Assert.*;

public class MultiBlockPlacementTest {
    @Test public void facingFollowsTheVanillaItems() {
        // yaw 0 looks south: doors face 1, beds point their head south (0), sunflowers face the player (2)
        assertEquals(1, MultiBlockPlacement.facing(Kind.DOOR, 0));
        assertEquals(0, MultiBlockPlacement.facing(Kind.BED, 0));
        assertEquals(2, MultiBlockPlacement.facing(Kind.DOUBLE_PLANT, 0));
        assertEquals(2, MultiBlockPlacement.facing(Kind.DOOR, 90));
        assertEquals(1, MultiBlockPlacement.facing(Kind.BED, 90));
        assertEquals(0, MultiBlockPlacement.facing(Kind.DOOR, -90));
    }

    @Test public void everyFacingHasAHorizontalLook() {
        for (Kind kind : Kind.values()) {
            for (int facing = 0; facing < 4; facing++) {
                PrinterLook look = MultiBlockPlacement.look(kind, 37, facing);
                assertNotNull(look);
                assertEquals(0, look.pitch, 0);
                assertEquals(facing, MultiBlockPlacement.facing(kind, look.yaw));
            }
        }
    }

    @Test public void secondBlockOffsetsAndComparedBits() {
        assertArrayEquals(new int[] {0, 1, 0}, MultiBlockPlacement.secondOffset(Kind.DOOR, 2));
        assertArrayEquals(new int[] {0, 0, 1}, MultiBlockPlacement.secondOffset(Kind.BED, 0));
        assertArrayEquals(new int[] {-1, 0, 0}, MultiBlockPlacement.secondOffset(Kind.BED, 9));
        assertTrue(MultiBlockPlacement.secondary(8));
        assertEquals(0xB, MultiBlockPlacement.stateMask(Kind.DOOR, 1, false));
        assertEquals(0x8, MultiBlockPlacement.stateMask(Kind.DOOR, 9, false));
        assertEquals(0xF, MultiBlockPlacement.stateMask(Kind.DOOR, 1, true));
    }
}

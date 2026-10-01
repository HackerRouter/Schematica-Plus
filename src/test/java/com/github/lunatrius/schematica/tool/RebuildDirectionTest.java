package com.github.lunatrius.schematica.tool;

import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Test;
import static org.junit.Assert.*;

public class RebuildDirectionTest {
    @Test public void centerQuarterTargetsTheHitFace() {
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            assertEquals(side, RebuildDirection.targeted(side, ForgeDirection.NORTH, 0.5, 0.5, 0.5));
            assertEquals(side, RebuildDirection.targeted(side, ForgeDirection.EAST, 0.7, 0.3, 0.7));
        }
    }

    @Test public void horizontalFacesUseTheirEdgesRelativeToTheFace() {
        assertEquals(ForgeDirection.WEST, RebuildDirection.targeted(ForgeDirection.NORTH, ForgeDirection.SOUTH, 0.1, 0.5, 0));
        assertEquals(ForgeDirection.EAST, RebuildDirection.targeted(ForgeDirection.NORTH, ForgeDirection.SOUTH, 0.9, 0.5, 0));
        assertEquals(ForgeDirection.EAST, RebuildDirection.targeted(ForgeDirection.SOUTH, ForgeDirection.NORTH, 0.9, 0.5, 1));
        assertEquals(ForgeDirection.NORTH, RebuildDirection.targeted(ForgeDirection.EAST, ForgeDirection.WEST, 1, 0.5, 0.05));
        assertEquals(ForgeDirection.SOUTH, RebuildDirection.targeted(ForgeDirection.WEST, ForgeDirection.EAST, 0, 0.5, 0.95));
        assertEquals(ForgeDirection.DOWN, RebuildDirection.targeted(ForgeDirection.SOUTH, ForgeDirection.NORTH, 0.5, 0.05, 1));
        assertEquals(ForgeDirection.UP, RebuildDirection.targeted(ForgeDirection.EAST, ForgeDirection.WEST, 1, 0.95, 0.5));
    }

    @Test public void verticalFacesFollowThePlayerFacing() {
        assertEquals(ForgeDirection.NORTH, RebuildDirection.targeted(ForgeDirection.UP, ForgeDirection.NORTH, 0.5, 1, 0.05));
        assertEquals(ForgeDirection.SOUTH, RebuildDirection.targeted(ForgeDirection.UP, ForgeDirection.NORTH, 0.5, 1, 0.95));
        assertEquals(ForgeDirection.EAST, RebuildDirection.targeted(ForgeDirection.UP, ForgeDirection.NORTH, 0.95, 1, 0.5));
        assertEquals(ForgeDirection.WEST, RebuildDirection.targeted(ForgeDirection.UP, ForgeDirection.NORTH, 0.05, 1, 0.5));
        assertEquals(ForgeDirection.EAST, RebuildDirection.targeted(ForgeDirection.UP, ForgeDirection.EAST, 0.95, 1, 0.5));
        assertEquals(ForgeDirection.SOUTH, RebuildDirection.targeted(ForgeDirection.DOWN, ForgeDirection.NORTH, 0.5, 0, 0.95));
        assertEquals(ForgeDirection.NORTH, RebuildDirection.targeted(ForgeDirection.DOWN, ForgeDirection.NORTH, 0.5, 0, 0.05));
    }
}

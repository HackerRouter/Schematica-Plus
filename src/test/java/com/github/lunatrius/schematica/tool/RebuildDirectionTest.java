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

    @Test public void overlayFacePointsInvertTheHitPositionMapping() {
        ForgeDirection[] facings = {ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.EAST};
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) for (ForgeDirection facing : facings) {
            for (double h = 0; h <= 1; h += 0.125) for (double v = 0; v <= 1; v += 0.125) {
                double[] point = RebuildDirection.facePoint(side, facing, h, v, 0);
                double[] back = RebuildDirection.facePosition(side, facing, point[0], point[1], point[2]);
                assertEquals(h, back[0], 1.0E-9);
                assertEquals(v, back[1], 1.0E-9);
                double[] outside = RebuildDirection.facePoint(side, facing, h, v, 0.01);
                assertEquals(side.offsetX * 0.01, outside[0] - point[0], 1.0E-9);
                assertEquals(side.offsetY * 0.01, outside[1] - point[1], 1.0E-9);
                assertEquals(side.offsetZ * 0.01, outside[2] - point[2], 1.0E-9);
            }
        }
        assertEquals(RebuildDirection.Part.CENTER, RebuildDirection.part(0.6, 0.4));
        assertEquals(RebuildDirection.Part.LEFT, RebuildDirection.part(0.1, 0.4));
        assertEquals(RebuildDirection.Part.TOP, RebuildDirection.part(0.5, 0.9));
    }
}

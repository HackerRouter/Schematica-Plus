package com.github.lunatrius.schematica.client.printer;

import java.util.List;

import net.minecraftforge.common.util.ForgeDirection;
import org.junit.Test;

import static org.junit.Assert.*;

public class PrinterIterationTest {
    @Test public void firstAxisChangesFastestAndReversalFlipsAnAxis() {
        List<int[]> xzy = PrinterIteration.build(1, "cube", "xzy", false, false, false);
        assertEquals(27, xzy.size());
        assertArrayEquals(new int[] {-1, -1, -1}, xzy.get(0));
        assertArrayEquals(new int[] {0, -1, -1}, xzy.get(1));
        assertArrayEquals(new int[] {-1, -1, 0}, xzy.get(3));
        // the whole bottom layer comes before the next one
        for (int i = 0; i < 9; i++) assertEquals(-1, xzy.get(i)[1]);
        List<int[]> topDown = PrinterIteration.build(1, "cube", "xzy", false, true, false);
        assertEquals(1, topDown.get(0)[1]);
        List<int[]> yxz = PrinterIteration.build(1, "cube", "yxz", false, false, false);
        assertArrayEquals(new int[] {-1, 0, -1}, yxz.get(1));
    }

    @Test public void shapesLimitTheArea() {
        assertEquals(7, PrinterIteration.build(1, "sphere", "xzy", false, false, false).size());
        assertEquals(7, PrinterIteration.build(1, "octahedron", "xzy", false, false, false).size());
        assertEquals(125, PrinterIteration.build(2, "cube", "xzy", false, false, false).size());
        assertEquals(33, PrinterIteration.build(2, "sphere", "xzy", false, false, false).size());
        assertEquals(25, PrinterIteration.build(2, "octahedron", "xzy", false, false, false).size());
        for (int[] o : PrinterIteration.build(5, "sphere", "zyx", true, true, true)) assertTrue(o[0] * o[0] + o[1] * o[1] + o[2] * o[2] <= 25);
    }

    @Test public void unknownValuesFallBackToTheDefaults() {
        assertEquals(PrinterIteration.build(2, "sphere", "xzy", false, false, false).size(),
            PrinterIteration.build(2, "blob", "abc", false, false, false).size());
        assertSame(PrinterIteration.offsets(3, "cube", "xyz", false, false, false), PrinterIteration.offsets(3, "cube", "xyz", false, false, false));
    }

    @Test public void lookCandidatesCoverEveryOrientationStartingWithTheCurrentYaw() {
        PrinterLook[] looks = PrinterLook.candidates(181);
        assertEquals(180f, looks[0].yaw, 0);
        java.util.Set<ForgeDirection> directions = java.util.EnumSet.noneOf(ForgeDirection.class);
        for (PrinterLook look : looks) directions.add(PrinterLook.orientation(look.yaw, look.pitch));
        assertEquals(6, directions.size());
        assertEquals(ForgeDirection.SOUTH, PrinterLook.orientation(0, 0));
        assertEquals(ForgeDirection.WEST, PrinterLook.orientation(90, 0));
        assertEquals(ForgeDirection.NORTH, PrinterLook.orientation(-180, 0));
        assertEquals(ForgeDirection.DOWN, PrinterLook.orientation(0, 90));
    }
}

package com.github.lunatrius.schematica.client.printer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraftforge.common.util.ForgeDirection;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.proxy.CommonProxy;
import com.github.lunatrius.schematica.proxy.ServerProxy;
import com.github.lunatrius.schematica.world.storage.Schematic;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

public class PrinterIterationTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private CommonProxy previousProxy;

    @Before public void proxy() {
        previousProxy = SchematicaPlus.proxy;
        if (SchematicaPlus.proxy == null) SchematicaPlus.proxy = new ServerProxy() {
            @Override public File getDataDirectory() { return temporary.getRoot(); }
        };
    }

    @After public void reset() { SchematicaPlus.proxy = previousProxy; }

    @Test public void maximumRadiusSupportsRandomAccessWithoutMaterializingEveryCoordinate() {
        List<int[]> cube = PrinterIteration.build(128, "cube", "xzy", false, false, false);
        assertEquals(257 * 257 * 257, cube.size());
        assertArrayEquals(new int[] {-128, -128, -128}, cube.get(0));
        assertArrayEquals(new int[] {0, 0, 0}, cube.get(cube.size() / 2));
        assertArrayEquals(new int[] {128, 128, 128}, cube.get(cube.size() - 1));
        cube.get(0)[0] = 500;
        assertArrayEquals(new int[] {-128, -128, -128}, cube.get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> cube.get(cube.size()));
        assertThrows(IndexOutOfBoundsException.class, () -> cube.get(-1));
        for (String shape : new String[] {"sphere", "octahedron"}) {
            List<int[]> shaped = PrinterIteration.build(128, shape, "xzy", false, false, false);
            assertArrayEquals(new int[] {0, -128, 0}, shaped.get(0));
            assertArrayEquals(new int[] {0, 128, 0}, shaped.get(shaped.size() - 1));
            for (int i = 0; i < shaped.size(); i += 997) {
                int[] o = shaped.get(i);
                assertTrue(o[0] * o[0] + o[1] * o[1] + o[2] * o[2] <= 128 * 128);
                if (shape.equals("octahedron")) assertTrue(Math.abs(o[0]) + Math.abs(o[1]) + Math.abs(o[2]) <= 128);
            }
        }
    }

    @Test public void clippedShapesPreserveAllAxisOrdersAndReversals() {
        for (String shape : PrinterIteration.SHAPES) for (String order : PrinterIteration.ORDERS) for (int mask = 0; mask < 8; mask++) {
            boolean x = (mask & 1) != 0, y = (mask & 2) != 0, z = (mask & 4) != 0;
            List<int[]> expected = new ArrayList<>();
            for (int[] o : PrinterIteration.build(3, "cube", order, x, y, z)) {
                if (o[0] < -2 || o[0] > 1 || o[1] < 0 || o[1] > 2 || o[2] < -1) continue;
                if (shape.equals("sphere") && o[0] * o[0] + o[1] * o[1] + o[2] * o[2] > 9) continue;
                if (shape.equals("octahedron") && Math.abs(o[0]) + Math.abs(o[1]) + Math.abs(o[2]) > 3) continue;
                expected.add(o);
            }
            List<int[]> actual = PrinterIteration.build(3, shape, order, x, y, z, new int[] {-2, 0, -1}, new int[] {1, 2, 20});
            assertEquals(expected.size(), actual.size());
            for (int i = 0; i < actual.size(); i++) assertArrayEquals(expected.get(i), actual.get(i));
        }
        assertTrue(PrinterIteration.build(128, "cube", "xyz", false, false, false, new int[] {129, 0, 0}, new int[] {130, 0, 0}).isEmpty());
        assertEquals(1, PrinterIteration.build(0, "sphere", "xyz", true, true, true).size());
    }

    @Test public void scanBoundsIncludeAllNearbyPlacementsAndExcludeDistantOnes() {
        SchematicWorld near = new SchematicWorld(new Schematic(null, 3, 1, 1));
        near.position.set(127, 255, 0);
        SchematicWorld far = new SchematicWorld(new Schematic(null, 3, 1, 1));
        far.position.set(1000, 255, 0);
        String old = ConfigurationHandler.printerIteratorShape;
        try {
            ConfigurationHandler.printerIteratorShape = "cube";
            List<int[]> offsets = PrinterIteration.offsets(Arrays.asList(near, far), 0, 255, 0, 128);
            assertEquals(2, offsets.size());
            Set<Integer> xs = new HashSet<>();
            for (int[] o : offsets) { xs.add(o[0]); assertEquals(0, o[1]); assertEquals(0, o[2]); }
            assertEquals(new HashSet<>(Arrays.asList(127, 128)), xs);
            assertTrue(PrinterIteration.offsets(Collections.singletonList(far), 0, 255, 0, 128).isEmpty());
        } finally { ConfigurationHandler.printerIteratorShape = old; }
    }

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

    @Test public void interruptedScansEventuallyVisitTheWholeRange() {
        PrinterIteration.Cursor cursor = new PrinterIteration.Cursor();
        List<int[]> offsets = PrinterIteration.build(32, "cube", "xzy", false, false, false);
        Set<Integer> visited = new HashSet<>();
        for (int pass = 0; pass < 300; pass++) {
            for (int i = 0; i < 1000; i++) visited.add(cursor.next(offsets.size()));
        }
        assertEquals(offsets.size(), visited.size());
        assertTrue(cursor.next(27) < 27);
        cursor.reset();
        assertEquals(0, cursor.next(27));
    }

    @Test public void unknownValuesFallBackToTheDefaults() {
        assertEquals(PrinterIteration.build(2, "sphere", "xzy", false, false, false).size(),
            PrinterIteration.build(2, "blob", "abc", false, false, false).size());
        assertSame(PrinterIteration.offsets(3, "cube", "xyz", false, false, false), PrinterIteration.offsets(3, "cube", "xyz", false, false, false));
    }

    @Test public void lookCandidatesCoverEveryOrientationStartingWithTheCurrentYaw() {
        PrinterLook[] looks = PrinterLook.candidates(181);
        assertEquals(180f, looks[0].yaw, 0);
        Set<ForgeDirection> directions = EnumSet.noneOf(ForgeDirection.class);
        for (PrinterLook look : looks) directions.add(PrinterLook.orientation(look.yaw, look.pitch));
        assertEquals(6, directions.size());
        assertEquals(ForgeDirection.SOUTH, PrinterLook.orientation(0, 0));
        assertEquals(ForgeDirection.WEST, PrinterLook.orientation(90, 0));
        assertEquals(ForgeDirection.NORTH, PrinterLook.orientation(-180, 0));
        assertEquals(ForgeDirection.DOWN, PrinterLook.orientation(0, 90));
    }
}

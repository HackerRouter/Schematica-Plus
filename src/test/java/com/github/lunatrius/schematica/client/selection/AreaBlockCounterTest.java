package com.github.lunatrius.schematica.client.selection;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;
import com.github.lunatrius.schematica.api.SchematicRegion;
import com.github.lunatrius.schematica.client.world.RenderLayerRange;

public class AreaBlockCounterTest {
    private static AreaBlockCounter.Reader<String> reader(Set<String> positions) {
        return new AreaBlockCounter.Reader<String>() {
            @Override public boolean loaded(int x, int y, int z) { return true; }
            @Override public String read(int x, int y, int z) {
                assertTrue("Duplicate position", positions.add(x + "," + y + "," + z));
                return "stone";
            }
        };
    }

    @Test public void overlappingBoxesCountOnceAndGapsAreNeverRead() {
        AreaBlockCounter<String> scan = new AreaBlockCounter<>(Arrays.asList(
            new SchematicRegion("a", -3, 60, 5, -1, 60, 5),
            new SchematicRegion("b", -2, 60, 5, 0, 60, 5),
            new SchematicRegion("c", 4, 60, 5, 4, 60, 5)));
        Set<String> positions = new HashSet<>();
        scan.step(reader(positions), 100, Long.MAX_VALUE);
        assertTrue(scan.done()); assertEquals(100, scan.percent());
        assertEquals(5, scan.selected()); assertEquals(Integer.valueOf(5), scan.counts().get("stone"));
        assertFalse(positions.contains("1,60,5"));
        assertTrue(positions.contains("4,60,5"));
        assertThrows(UnsupportedOperationException.class, () -> scan.counts().clear());
    }

    @Test public void budgetedScanResumesWithoutRepeatingOrSkippingPositions() {
        AreaBlockCounter<String> scan = new AreaBlockCounter<>(Collections.singletonList(new SchematicRegion("box", 0, 1, 2, 2, 2, 3)));
        Set<String> positions = new HashSet<>();
        AreaBlockCounter.Reader<String> reader = reader(positions);
        scan.step(reader, 0, Long.MAX_VALUE);
        assertEquals(0, scan.percent());
        scan.step(reader, 3, Long.MAX_VALUE);
        assertEquals(25, scan.percent()); assertFalse(scan.done());
        while (!scan.done()) scan.step(reader, 2, Long.MAX_VALUE);
        assertEquals(12, positions.size());
        scan.step(reader, 20, Long.MAX_VALUE);
        assertEquals(12, scan.selected());
    }

    @Test public void unloadedAirAndFailedReadsRemainDistinct() {
        AreaBlockCounter<String> scan = new AreaBlockCounter<>(Collections.singletonList(new SchematicRegion("box", 0, 0, 0, 3, 0, 0)));
        scan.step(new AreaBlockCounter.Reader<String>() {
            @Override public boolean loaded(int x, int y, int z) { return x != 1; }
            @Override public String read(int x, int y, int z) {
                assertNotEquals(1, x);
                if (x == 2) return null;
                if (x == 3) throw new IllegalStateException("Unsupported block");
                return "machine";
            }
        }, 100, Long.MAX_VALUE);
        assertEquals(4, scan.selected()); assertEquals(1, scan.unverified()); assertEquals(1, scan.skipped());
        assertEquals(Integer.valueOf(1), scan.counts().get("machine")); assertTrue(scan.done());
    }

    @Test public void scanSnapshotsRegionsAndAbsoluteRenderLayers() {
        java.util.List<SchematicRegion> regions = new java.util.ArrayList<>();
        regions.add(new SchematicRegion("box", -3, 60, 5, -1, 62, 6));
        RenderLayerRange range = new RenderLayerRange();
        range.setMode(RenderLayerRange.Mode.SINGLE_LAYER); range.setHere(61);
        AreaBlockCounter<String> scan = new AreaBlockCounter<>(regions, range);
        regions.clear(); range.setHere(200);
        Set<String> positions = new HashSet<>();
        scan.step(reader(positions), 100, Long.MAX_VALUE);
        assertEquals(6, scan.selected()); assertEquals(6, positions.size());
        assertTrue(positions.contains("-3,61,5")); assertFalse(positions.contains("-3,60,5"));
    }

    @Test public void expiredDeadlineYieldsAfterOnePosition() {
        AreaBlockCounter<String> scan = new AreaBlockCounter<>(Collections.singletonList(new SchematicRegion("box", 0, 0, 0, 99, 0, 0)));
        Set<String> positions = new HashSet<>();
        scan.step(reader(positions), 100, System.nanoTime() - 1);
        assertEquals(1, positions.size()); assertFalse(scan.done());
    }

    @Test public void emptyAndOversizedAreasFailBeforeReadingWorld() {
        assertThrows(IllegalArgumentException.class, () -> new AreaBlockCounter<>(Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> new AreaBlockCounter<>(Collections.singletonList(
            new SchematicRegion("large", 0, 0, 0, 1023, 255, 1023))));
        assertThrows(IllegalArgumentException.class, () -> new AreaBlockCounter<>(Collections.singletonList(
            new SchematicRegion("outside", 0, -1, 0, 2, 3, 4))));
    }

    @Test public void xAndZLayerFiltersCanProduceEmptyResults() {
        for (RenderLayerRange.Axis axis : new RenderLayerRange.Axis[] {RenderLayerRange.Axis.X, RenderLayerRange.Axis.Z}) {
            RenderLayerRange range = new RenderLayerRange();
            range.setAxis(axis); range.setMode(RenderLayerRange.Mode.ALL_ABOVE); range.setHere(10);
            AreaBlockCounter<String> scan = new AreaBlockCounter<>(Collections.singletonList(new SchematicRegion("box", 0, 60, 0, 2, 62, 2)), range);
            scan.step(reader(new HashSet<>()), 100, Long.MAX_VALUE);
            assertTrue(scan.done()); assertEquals(0, scan.selected()); assertEquals(0, scan.unverified());
        }
    }
}

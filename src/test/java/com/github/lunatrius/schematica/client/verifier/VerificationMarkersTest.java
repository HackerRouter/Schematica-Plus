package com.github.lunatrius.schematica.client.verifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import net.minecraft.util.Vec3;
import com.github.lunatrius.schematica.client.verifier.VerificationScan.Group;
import com.github.lunatrius.schematica.client.verifier.VerificationScan.Marker;
import com.github.lunatrius.schematica.client.verifier.VerificationScan.State;
import com.github.lunatrius.schematica.client.verifier.VerificationScan.Type;
import com.github.lunatrius.schematica.util.VoxelRayTrace;

public class VerificationMarkersTest {
    private static final State STONE = new State("minecraft:stone", 0), DIRT = new State("minecraft:dirt", 0);
    private static class Reader implements VerificationScan.Reader {
        @Override public boolean included(int x, int y, int z) { return true; }
        @Override public boolean loaded(int x, int z) { return true; }
        @Override public State expected(int x, int y, int z) { return STONE; }
        @Override public State found(int x, int y, int z) { return State.AIR; }
    }
    private static VerificationScan line(int length) { return new VerificationScan(-17, 64, -3, length, 1, 1, new int[] {0, 0, 0, length, 1, 1}); }
    private static void finish(VerificationScan scan, Reader reader) {
        for (int i = 0; !scan.done() && i < 100; i++) scan.step(reader, 10000, Long.MAX_VALUE);
        assertTrue(scan.done());
    }
    private static List<Marker> closest(VerificationScan scan, VerificationSelection selection, double x, double y, double z, int count) {
        VerificationScan.MarkerSearch search = scan.closest(selection, x, y, z, count);
        for (int i = 0; !search.done() && i < 10000; i++) search.step(31, Long.MAX_VALUE);
        assertTrue(search.done());
        return search.result();
    }
    private static void update(VerificationMarkers markers, VerificationScan scan, int x, int limit) {
        markers.update(scan, x, 64, -3, limit, 100000, Long.MAX_VALUE);
    }

    @Test public void categoryAndEntrySelectionFollowUpstreamMultiSelectRules() {
        VerificationScan scan = line(3);
        finish(scan, new Reader() { @Override public State found(int x, int y, int z) { return x == -17 ? State.AIR : x == -16 ? DIRT : STONE; } });
        Group missing = scan.at(-17, 64, -3), wrong = scan.at(-16, 64, -3), correct = scan.at(-15, 64, -3);
        VerificationSelection selection = new VerificationSelection();
        selection.toggle(Type.ALL); selection.toggle(Type.CORRECT); selection.toggle(correct);
        assertTrue(selection.empty());
        selection.toggle(missing); selection.toggle(wrong);
        assertTrue(selection.entry(missing)); assertTrue(selection.entry(wrong));
        selection.toggle(Type.MISSING);
        assertTrue(selection.category(Type.MISSING)); assertFalse(selection.entry(missing)); assertTrue(selection.entry(wrong));
        selection.toggle(missing);
        assertFalse(selection.category(Type.MISSING)); assertTrue(selection.entry(missing));
        selection.toggle(missing);
        assertFalse(selection.includes(missing)); assertTrue(selection.includes(wrong));
        selection.clear(); assertTrue(selection.empty());
    }

    @Test public void nearestMarkersMatchBruteForceAcrossNegativeChunksAndCroppedLayers() {
        VerificationScan scan = new VerificationScan(-35, 62, -20, 70, 5, 42, new int[] {2, 1, 3, 67, 4, 40});
        finish(scan, new Reader() {
            @Override public boolean included(int x, int y, int z) { return (x + z) % 7 != 0; }
            @Override public State found(int x, int y, int z) { return (x * 7 + z * 3 + y) % 3 == 0 ? State.AIR : DIRT; }
        });
        VerificationSelection selection = new VerificationSelection(); selection.toggle(Type.MISSING);
        for (double[] eye : new double[][] {{-16.5, 63.5, -0.5}, {31.5, 255, 19.5}, {-100, 0, -100}, {0.5, 64.5, 0.5}}) {
            List<int[]> expected = new ArrayList<>();
            for (int x = -33; x < 32; x++) for (int y = 63; y < 66; y++) for (int z = -17; z < 20; z++) {
                Group group = scan.at(x, y, z);
                if (group != null && selection.includes(group)) expected.add(new int[] {x, y, z});
            }
            expected.sort(Comparator.comparingDouble((int[] p) -> distance(p, eye)).thenComparingInt(p -> p[0])
                .thenComparingInt(p -> p[1]).thenComparingInt(p -> p[2]));
            List<Marker> actual = closest(scan, selection, eye[0], eye[1], eye[2], 37);
            assertEquals(37, actual.size());
            for (int i = 0; i < actual.size(); i++) assertArrayEquals(expected.get(i), new int[] {actual.get(i).x, actual.get(i).y, actual.get(i).z});
        }
    }

    private static double distance(int[] p, double[] eye) {
        double x = p[0] + 0.5 - eye[0], y = p[1] + 0.5 - eye[1], z = p[2] + 0.5 - eye[2];
        return x * x + y * y + z * z;
    }

    @Test public void searchHonorsWorkAndTimeBudgetsAndRejectsInvalidInputs() {
        VerificationScan scan = line(34); finish(scan, new Reader());
        VerificationSelection selection = new VerificationSelection(); selection.toggle(Type.MISSING);
        VerificationScan.MarkerSearch search = scan.closest(selection, -17, 64, -3, 100);
        search.step(0, Long.MAX_VALUE); assertFalse(search.done()); assertTrue(search.result().isEmpty());
        search.step(100, System.nanoTime() - 1); assertTrue(search.result().isEmpty());
        search.step(1, Long.MAX_VALUE); assertEquals(1, search.result().size()); assertFalse(search.done());
        assertThrows(UnsupportedOperationException.class, () -> search.result().clear());
        assertThrows(IllegalArgumentException.class, () -> scan.closest(selection, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> scan.closest(selection, Double.NaN, 0, 0, 1));
    }

    @Test public void sparseResultsSkipCorrectChunksAndEmptySelections() {
        VerificationScan scan = line(10000);
        finish(scan, new Reader() { @Override public State found(int x, int y, int z) { return x == 4096 ? State.AIR : STONE; } });
        VerificationSelection selection = new VerificationSelection();
        assertTrue(scan.closest(selection, 0, 64, 0, 1000).done());
        selection.toggle(Type.MISSING);
        VerificationScan.MarkerSearch search = scan.closest(selection, 0, 64, 0, 1000);
        search.step(17, Long.MAX_VALUE);
        assertTrue(search.done()); assertEquals(1, search.result().size()); assertEquals(4096, search.result().get(0).x);
        scan.ignore(scan.at(4096, 64, -3));
        assertTrue(scan.closest(selection, 0, 64, 0, 1000).done());
    }

    @Test public void correctionsIgnoresAndResetsRemoveStaleMarkersAndRestoreCurrentData() {
        VerificationScan scan = line(34); finish(scan, new Reader());
        VerificationMarkers markers = new VerificationMarkers(); markers.selection.toggle(Type.MISSING);
        update(markers, scan, -17, 2); assertEquals(-17, markers.markers().get(0).x);
        scan.changed(-17, 64, -3, -17, 64, -3);
        finish(scan, new Reader() { @Override public State found(int x, int y, int z) { return x == -17 ? STONE : State.AIR; } });
        update(markers, scan, -17, 2);
        assertNull(markers.at(-17, 64, -3)); assertEquals(-16, markers.markers().get(0).x);
        scan.ignore(scan.at(-16, 64, -3)); update(markers, scan, -17, 2);
        assertTrue(markers.markers().isEmpty());
        scan.resetIgnored(); update(markers, scan, -17, 2); assertEquals(2, markers.markers().size());
        markers.selection.toggle(Type.MISSING); update(markers, scan, -17, 2);
        assertTrue(markers.markers().isEmpty()); assertNull(markers.at(-16, 64, -3));
    }

    @Test public void cameraMovementAndLimitChangesRecomputeNearestResults() {
        VerificationScan scan = line(34); finish(scan, new Reader());
        VerificationMarkers markers = new VerificationMarkers(); markers.selection.toggle(Type.MISSING);
        update(markers, scan, -17, 2); assertEquals(-17, markers.markers().get(0).x);
        update(markers, scan, 16, 1); assertEquals(1, markers.markers().size()); assertEquals(16, markers.markers().get(0).x);
        assertNull(markers.at(-17, 64, -3)); assertNotNull(markers.at(16, 64, -3));
        assertNull(markers.at(16, 320, -3)); assertNull(markers.at(16, -192, -3));
        assertThrows(UnsupportedOperationException.class, () -> markers.markers().clear());
    }

    @Test public void changingSelectionDuringPartialSearchDiscardsOldResults() {
        VerificationScan scan = line(34);
        finish(scan, new Reader() { @Override public State found(int x, int y, int z) { return x < 0 ? State.AIR : DIRT; } });
        VerificationMarkers markers = new VerificationMarkers(); markers.selection.toggle(Type.MISSING);
        markers.update(scan, -17, 64, -3, 10, 1, Long.MAX_VALUE);
        markers.selection.toggle(Type.MISSING); markers.selection.toggle(Type.WRONG_BLOCK);
        update(markers, scan, -17, 10);
        assertEquals(10, markers.markers().size());
        for (Marker marker : markers.markers()) assertEquals(Type.WRONG_BLOCK, marker.group.type);
    }

    @Test public void updatesDuringSearchAreReconciledWithoutRestartStarvation() {
        VerificationScan scan = line(34); finish(scan, new Reader());
        VerificationMarkers markers = new VerificationMarkers(); markers.selection.toggle(Type.MISSING);
        markers.update(scan, -17, 64, -3, 2, 1, Long.MAX_VALUE);
        scan.changed(-17, 64, -3, -17, 64, -3);
        finish(scan, new Reader() { @Override public State found(int x, int y, int z) { return x == -17 ? STONE : State.AIR; } });
        update(markers, scan, 16, 2);
        assertNull(markers.at(-17, 64, -3));
        update(markers, scan, 16, 2);
        assertEquals(16, markers.markers().get(0).x);
    }

    @Test public void rayTargetsNearestSelectedCellThroughOtherBlocksAndFromInside() {
        VerificationScan scan = line(34); finish(scan, new Reader());
        VerificationMarkers markers = new VerificationMarkers(); markers.selection.toggle(Type.MISSING);
        update(markers, scan, 16, 2);
        Marker hit = VoxelRayTrace.trace(Vec3.createVectorHelper(-20, 64.5, -2.5), Vec3.createVectorHelper(108, 64.5, -2.5), markers::at);
        assertNotNull(hit); assertEquals(15, hit.x);
        assertEquals(16, VoxelRayTrace.trace(Vec3.createVectorHelper(16.5, 64.5, -2.5), Vec3.createVectorHelper(-100, 64.5, -2.5), markers::at).x);
        assertNull(VoxelRayTrace.trace(Vec3.createVectorHelper(-20, 65.5, -2.5), Vec3.createVectorHelper(108, 65.5, -2.5), markers::at));
    }

    @Test public void resetAndScanReplacementReleaseOldCoordinatesAndSelections() {
        VerificationScan scan = line(34); finish(scan, new Reader());
        VerificationMarkers markers = new VerificationMarkers(); markers.selection.toggle(Type.MISSING);
        update(markers, scan, -17, 2); assertNotNull(markers.at(-17, 64, -3));
        VerificationScan next = new VerificationScan(10, 255, 9, 1, 1, 1, new int[] {0, 0, 0, 1, 1, 1});
        finish(next, new Reader()); update(markers, next, -17, 2);
        assertNull(markers.at(-17, 64, -3)); assertNotNull(markers.at(10, 255, 9));
        markers.clear(); assertTrue(markers.selection.empty()); assertTrue(markers.markers().isEmpty()); assertNull(markers.at(10, 255, 9));
    }
}

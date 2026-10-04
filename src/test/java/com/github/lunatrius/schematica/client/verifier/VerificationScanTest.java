package com.github.lunatrius.schematica.client.verifier;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;
import com.github.lunatrius.schematica.client.verifier.VerificationScan.State;
import com.github.lunatrius.schematica.client.verifier.VerificationScan.Type;

public class VerificationScanTest {
    private static final State STONE = new State("minecraft:stone", 0);
    private static final State DIRT = new State("minecraft:dirt", 0);
    private static class Reader implements VerificationScan.Reader {
        @Override public boolean included(int x, int y, int z) { return true; }
        @Override public boolean loaded(int x, int z) { return true; }
        @Override public State expected(int x, int y, int z) { return STONE; }
        @Override public State found(int x, int y, int z) { return STONE; }
    }
    private static VerificationScan line(int length) { return new VerificationScan(0, 0, 0, length, 1, 1, new int[] {0, 0, 0, length, 1, 1}); }
    private static void finish(VerificationScan scan, VerificationScan.Reader reader) {
        for (int i = 0; !scan.done() && i < 1000; i++) scan.step(reader, 100, Long.MAX_VALUE);
        assertTrue("Scan did not finish", scan.done());
    }

    @Test public void classifiesAllFiveResultsWithoutCountingAirAsCorrectOrAsMaterial() {
        VerificationScan scan = line(6);
        finish(scan, new Reader() {
            @Override public State expected(int x, int y, int z) { return x == 3 || x == 5 ? State.AIR : STONE; }
            @Override public State found(int x, int y, int z) {
                return x == 0 || x == 5 ? State.AIR : x == 1 ? DIRT : x == 2 ? new State(STONE.block, 1) : STONE;
            }
        });
        for (Type type : Type.values()) assertEquals(type.name(), type == Type.ALL ? 4 : type == Type.DIFF_BLOCK ? 0 : 1, scan.count(type));
        assertEquals(4, scan.expectedBlocks());
        assertEquals(6, scan.checked());
        assertEquals(0, scan.skipped());
    }

    @Test public void ignoresOnlyExactExpectedFoundPairAndResetRestoresItsCurrentCount() {
        VerificationScan scan = line(3);
        Reader reader = new Reader() { @Override public State found(int x, int y, int z) { return x < 2 ? DIRT : State.AIR; } };
        finish(scan, reader);
        VerificationScan.Group wrong = scan.groups(Type.WRONG_BLOCK).get(0);
        assertEquals(2, wrong.count());
        scan.ignore(wrong);
        assertEquals(1, scan.count(Type.ALL));
        assertEquals(3, scan.expectedBlocks());
        assertTrue(scan.groups(Type.WRONG_BLOCK).isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> scan.ignored().clear());
        VerificationScan next = line(3);
        next.addIgnored(scan.ignored());
        finish(next, reader);
        assertEquals(1, next.count(Type.ALL));
        next.resetIgnored();
        assertEquals(3, next.count(Type.ALL));
        assertEquals(2, next.groups(Type.WRONG_BLOCK).get(0).count());
    }

    @Test public void budgetedNegativeCrossChunkScanVisitsEveryWorldPositionOnce() {
        VerificationScan scan = new VerificationScan(-17, 63, -1, 34, 2, 18, new int[] {0, 0, 0, 34, 2, 18});
        Set<String> positions = new HashSet<>();
        Reader reader = new Reader() {
            @Override public State found(int x, int y, int z) {
                assertTrue(positions.add(x + "," + y + "," + z));
                assertTrue(x >= -17 && x <= 16 && y >= 63 && y <= 64 && z >= -1 && z <= 16);
                return STONE;
            }
        };
        assertEquals(12, scan.totalChunks());
        scan.step(reader, 3, Long.MAX_VALUE);
        assertEquals(3, scan.checked());
        finish(scan, reader);
        assertEquals(34 * 2 * 18, positions.size());
        assertEquals(positions.size(), scan.count(Type.CORRECT));
        assertEquals(scan.total(), scan.checked());
        scan.step(reader, 100, Long.MAX_VALUE);
        assertEquals(34 * 2 * 18, positions.size());
    }

    @Test public void renderBoundsAreCopiedAndCoordinatesRemainRelativeToTheFullSchematic() {
        int[] bounds = {1, 1, 2, 5, 2, 4};
        VerificationScan scan = new VerificationScan(-3, 60, 9, 6, 3, 5, bounds);
        bounds[0] = 0;
        Set<String> positions = new HashSet<>();
        finish(scan, new Reader() {
            @Override public State expected(int x, int y, int z) {
                assertTrue(x >= 1 && x < 5 && y == 1 && z >= 2 && z < 4);
                return STONE;
            }
            @Override public State found(int x, int y, int z) {
                positions.add(x + "," + y + "," + z);
                assertTrue(x >= -2 && x < 2 && y == 61 && z >= 11 && z < 13);
                return STONE;
            }
        });
        assertEquals(8, positions.size());
        assertEquals(8, scan.total());
        assertEquals(8, scan.count(Type.CORRECT));
    }

    @Test public void gapsDoNotRequireLoadedChunksAndUnloadedSelectedPositionsWait() {
        VerificationScan scan = line(33);
        Set<Integer> loaded = new HashSet<>();
        loaded.add(0);
        Reader reader = new Reader() {
            @Override public boolean included(int x, int y, int z) { return x == 0 || x == 32; }
            @Override public boolean loaded(int x, int z) { assertNotEquals(1, x); return loaded.contains(x); }
            @Override public State found(int x, int y, int z) { assertTrue(loaded.contains(x >> 4)); return STONE; }
        };
        scan.step(reader, 100, Long.MAX_VALUE);
        assertFalse(scan.done());
        assertEquals(1, scan.remainingChunks());
        assertEquals(1, scan.count(Type.CORRECT));
        assertEquals(0, scan.count(Type.MISSING));
        assertEquals(32, scan.checked());
        loaded.add(2);
        finish(scan, reader);
        assertEquals(2, scan.count(Type.CORRECT));
        assertEquals(33, scan.checked());
    }

    @Test public void unloadedChunksDoNotStarveLoadedChunksBehindThem() {
        VerificationScan scan = line(32);
        scan.step(new Reader() {
            @Override public boolean loaded(int x, int z) { return x == 1; }
            @Override public State found(int x, int y, int z) { assertTrue(x >= 16); return STONE; }
        }, 1000, Long.MAX_VALUE);
        assertFalse(scan.done());
        assertEquals(1, scan.remainingChunks());
        assertEquals(16, scan.count(Type.CORRECT));
        assertEquals(16, scan.checked());
    }

    @Test public void rechecksReplaceCountsAndRecoverFailedReadsWithoutDoubleCounting() {
        VerificationScan scan = line(4);
        final boolean[] fixed = {false};
        Reader reader = new Reader() {
            @Override public State found(int x, int y, int z) {
                if (!fixed[0] && x == 0) throw new IllegalStateException("Unsupported mod block");
                return fixed[0] ? STONE : DIRT;
            }
        };
        finish(scan, reader);
        assertEquals(1, scan.skipped());
        assertEquals(3, scan.count(Type.WRONG_BLOCK));
        fixed[0] = true;
        for (int i = 0; i < 20; i++) scan.changed(0, 0, 0, 3, 0, 0);
        assertEquals(1, scan.remainingChunks());
        finish(scan, reader);
        assertEquals(0, scan.skipped());
        assertEquals(4, scan.count(Type.CORRECT));
        assertEquals(0, scan.count(Type.ALL));
        assertEquals(4, scan.checked());
        assertEquals(4, scan.expectedBlocks());
    }

    @Test public void updateDuringPartialChunkIsNotLost() {
        VerificationScan scan = line(3);
        final boolean[] changed = {false};
        Reader reader = new Reader() {
            @Override public State found(int x, int y, int z) { return changed[0] && x == 0 ? State.AIR : STONE; }
        };
        scan.step(reader, 1, Long.MAX_VALUE);
        changed[0] = true;
        scan.changed(0, 0, 0, 0, 0, 0);
        finish(scan, reader);
        assertEquals(1, scan.count(Type.MISSING));
        assertEquals(2, scan.count(Type.CORRECT));
        assertEquals(3, scan.checked());
    }

    @Test public void dirtyUnloadedChunksCannotBeReportedAsFinished() {
        VerificationScan scan = line(2);
        finish(scan, new Reader());
        scan.changed(0, 0, 0, 1, 0, 0);
        scan.step(new Reader() { @Override public boolean loaded(int x, int z) { return false; } }, 100, Long.MAX_VALUE);
        assertFalse(scan.done());
        assertEquals(1, scan.remainingChunks());
        finish(scan, new Reader() { @Override public State found(int x, int y, int z) { return DIRT; } });
        assertEquals(0, scan.count(Type.CORRECT));
        assertEquals(2, scan.count(Type.WRONG_BLOCK));
    }

    @Test public void ignoresChangesOutsideItsWorldHeightAndChunkRange() {
        VerificationScan scan = line(2);
        finish(scan, new Reader());
        scan.changed(32, 0, 0, 33, 0, 0);
        scan.changed(0, 2, 0, 1, 2, 0);
        scan.changed(-100, 0, -100, -50, 0, -50);
        assertTrue(scan.done());
        assertEquals(2, scan.count(Type.CORRECT));
    }

    @Test public void deadlineZeroBudgetAndEmptyRangeNeverReadWorld() {
        VerificationScan scan = line(4);
        Reader reader = new Reader() { @Override public boolean included(int x, int y, int z) { fail("Read outside budget"); return false; } };
        scan.step(reader, 0, Long.MAX_VALUE);
        scan.step(reader, 100, System.nanoTime() - 1);
        assertEquals(0, scan.checked());
        VerificationScan empty = new VerificationScan(0, -1, 0, 4, 1, 1, new int[] {0, 0, 0, 0, 1, 1});
        empty.step(reader, 100, Long.MAX_VALUE);
        empty.changed(0, 0, 0, 100, 100, 100);
        assertTrue(empty.done());
        assertEquals(0, empty.totalChunks());
    }

    @Test public void rejectsUnboundedAllocationAndInvalidWorldRangesBeforeScanning() {
        assertThrows(IllegalArgumentException.class, () -> new VerificationScan(0, 0, 0, 1024, 256, 1024, new int[] {0, 0, 0, 1, 1, 1}));
        assertThrows(IllegalArgumentException.class, () -> new VerificationScan(0, 255, 0, 1, 2, 1, new int[] {0, 0, 0, 1, 2, 1}));
        assertThrows(IllegalArgumentException.class, () -> new VerificationScan(Integer.MAX_VALUE, 0, 0, 2, 1, 1, new int[] {0, 0, 0, 2, 1, 1}));
        assertThrows(IllegalArgumentException.class, () -> new VerificationScan(0, 0, 0, 2, 1, 1, new int[] {1, 0, 0, 0, 1, 1}));
    }

    @Test public void ignorableExistingBlocksAreNotExtra() {
        VerificationScan scan = line(3);
        State water = new State("minecraft:water", 0);
        finish(scan, new Reader() {
            @Override public State expected(int x, int y, int z) { return x == 2 ? STONE : State.AIR; }
            @Override public State found(int x, int y, int z) { return x == 0 ? water : x == 1 ? DIRT : State.AIR; }
            @Override public boolean ignorable(State found) { return found.equals(water); }
        });
        assertEquals(1, scan.count(Type.EXTRA));
        assertEquals(1, scan.count(Type.MISSING));
        assertNull(scan.at(0, 0, 0));
    }

    @Test public void pendingChunksAreListedClosestFirst() {
        VerificationScan scan = new VerificationScan(0, 0, 0, 48, 1, 16, new int[] {0, 0, 0, 48, 1, 16});
        java.util.List<int[]> chunks = scan.pendingChunks(40, 8);
        assertEquals(3, chunks.size());
        assertArrayEquals(new int[] {2, 0}, chunks.get(0));
        assertArrayEquals(new int[] {0, 0}, chunks.get(2));
        finish(scan, new Reader());
        assertTrue(scan.pendingChunks(0, 0).isEmpty());
    }

    @Test public void singleSelectedCategory() {
        VerificationSelection selection = new VerificationSelection();
        assertNull(selection.single());
        selection.toggle(Type.MISSING);
        assertEquals(Type.MISSING, selection.single());
        selection.toggle(Type.EXTRA);
        assertNull(selection.single());
    }
}

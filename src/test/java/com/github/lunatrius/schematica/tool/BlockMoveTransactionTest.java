package com.github.lunatrius.schematica.tool;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.*;

public class BlockMoveTransactionTest {
    private static final class Grid implements BlockMoveTransaction.Access<String> {
        final Map<String, String> cells = new HashMap<>();
        int writes, failAfter = -1;
        boolean failRead;
        String key(int x, int y, int z) { return x + "," + y + "," + z; }
        @Override public String read(int x, int y, int z) {
            if (failRead) throw new IllegalArgumentException("Unloaded");
            return cells.getOrDefault(key(x, y, z), "air");
        }
        @Override public void write(int x, int y, int z, String value) {
            writes++;
            if (value.equals("air")) cells.remove(key(x, y, z)); else cells.put(key(x, y, z), value);
            if (writes == failAfter) throw new IllegalStateException("Injected failure after mutation");
        }
        @Override public void changed(int x, int y, int z) {}
    }
    private static BlockMoveTransaction<String> job(int dx, BitSet mask) { return new BlockMoveTransaction<>(-2, 64, -1, 3, 1, 1, dx, 0, 0, mask, "air"); }
    private static BitSet all() { BitSet bits = new BitSet(); bits.set(0, 3); return bits; }
    private static void finish(BlockMoveTransaction<String> job, Grid grid, boolean cancel) {
        for (int i = 0; i < 1000; i++) if (job.step(grid, cancel)) return;
        fail("Move did not terminate");
    }
    @Test public void overlappingMovesUseSnapshotAndRetainPerBlockDataInBothDirections() {
        for (int dx : new int[] {-1, 1}) {
            Grid grid = new Grid();
            for (int x = -2; x <= 0; x++) grid.write(x, 64, -1, "tileNBT" + x);
            BlockMoveTransaction<String> job = job(dx, all()); finish(job, grid, false);
            assertTrue(job.successful());
            for (int x = -2; x <= 0; x++) assertEquals("tileNBT" + x, grid.read(x + dx, 64, -1));
            assertEquals("air", grid.read(dx > 0 ? -2 : 0, 64, -1));
        }
    }
    @Test public void gapsAreUntouchedAndAirOverwritesDestination() {
        Grid grid = new Grid(); grid.write(-1, 64, -1, "gap"); grid.write(3, 64, -1, "target");
        BitSet bits = all(); bits.clear(1);
        BlockMoveTransaction<String> job = job(5, bits); finish(job, grid, false);
        assertEquals("gap", grid.read(-1, 64, -1)); assertEquals("air", grid.read(3, 64, -1));
    }
    @Test public void cancellationRestoresEveryOverlapAndOriginalDestination() {
        for (BlockMoveTransaction.Phase phase : new BlockMoveTransaction.Phase[] {BlockMoveTransaction.Phase.CAPTURE, BlockMoveTransaction.Phase.CLEAR, BlockMoveTransaction.Phase.PLACE, BlockMoveTransaction.Phase.UPDATES}) {
            Grid grid = new Grid(); for (int x = -3; x <= 1; x++) grid.write(x, 64, -1, "before" + x);
            Map<String, String> original = new HashMap<>(grid.cells); BlockMoveTransaction<String> job = job(1, all());
            while (job.phase() != phase) assertFalse(job.step(grid, false));
            assertFalse(job.step(grid, false)); finish(job, grid, true);
            assertFalse(job.successful()); assertEquals(original, grid.cells);
        }
    }
    @Test public void failedReadsDoNotWriteAndFailedWritesRollBack() {
        Grid missing = new Grid(); missing.failRead = true;
        BlockMoveTransaction<String> uncaptured = job(1, all()); finish(uncaptured, missing, false);
        assertEquals(0, missing.writes); assertNotNull(uncaptured.failure());
        for (int fault = 1; fault <= 6; fault++) {
            Grid grid = new Grid(); for (int x = -2; x <= 1; x++) grid.write(x, 64, -1, "before" + x);
            Map<String, String> original = new HashMap<>(grid.cells); grid.failAfter = grid.writes + fault;
            BlockMoveTransaction<String> job = job(1, all()); finish(job, grid, false);
            assertEquals(original, grid.cells); assertNotNull(job.failure()); assertFalse(job.successful());
        }
    }
    @Test public void largeOrInvalidAllocationsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new BlockMoveTransaction<>(0, 0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 1, 0, 0, all(), "air"));
        assertThrows(IllegalArgumentException.class, () -> new BlockMoveTransaction<>(0, 0, 0, 1, 1, 1, 1, 0, 0, all(), "air"));
    }
}

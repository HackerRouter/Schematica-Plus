package com.github.lunatrius.schematica.client.verifier;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import static org.junit.Assert.*;

public class VerificationAsyncTest {
    private static final VerificationScan.State STONE = new VerificationScan.State("minecraft:stone", 0);
    private static final VerificationScan.State DIRT = new VerificationScan.State("minecraft:dirt", 0);

    private static class Reader implements VerificationScan.Reader {
        final Thread owner = Thread.currentThread();
        final Set<String> reads = new HashSet<>();
        VerificationScan.State value = STONE;
        boolean loaded = true;
        void main() { assertSame("World access escaped the owner thread", owner, Thread.currentThread()); }
        @Override public boolean included(int x, int y, int z) { main(); return true; }
        @Override public boolean loaded(int x, int z) { main(); return loaded; }
        @Override public VerificationScan.State expected(int x, int y, int z) { main(); return STONE; }
        @Override public VerificationScan.State found(int x, int y, int z) { main(); reads.add(x + "," + y + "," + z); return value; }
    }

    private VerificationScan scan(int width, int height) {
        return new VerificationScan(0, 0, 0, width, height, 1, new int[] {0, 0, 0, width, height, 1});
    }

    private VerificationScan.Result compute(VerificationScan.Batch batch) throws Exception {
        FutureTask<VerificationScan.Result> task = new FutureTask<>(batch::compare);
        Thread thread = new Thread(task); thread.setDaemon(true); thread.start();
        return task.get(5, TimeUnit.SECONDS);
    }

    private void finish(VerificationScan scan, Reader reader) {
        for (int i = 0; !scan.done() && i < 1000; i++) scan.step(reader, 4096, Long.MAX_VALUE);
        assertTrue(scan.done());
    }

    @Test public void workerUsesTheCapturedStatesAndMergesOnlyOnOwnerThread() throws Exception {
        VerificationScan scan = scan(16, 16);
        Reader reader = new Reader();
        VerificationScan.Batch batch = scan.capture(reader, 4096, Long.MAX_VALUE, new VerificationRules());
        reader.value = DIRT;
        VerificationScan.Result result = compute(batch);
        assertEquals(0, scan.checked());
        assertTrue(scan.apply(result, reader));
        assertEquals(256, scan.count(VerificationScan.Type.CORRECT));
        assertFalse(scan.apply(result, reader));
    }

    @Test public void oneBatchIncludesMultipleThinColumnsAndVerticalSections() throws Exception {
        VerificationScan scan = scan(64, 32);
        Reader reader = new Reader();
        VerificationScan.Batch batch = scan.capture(reader, 4096, Long.MAX_VALUE, new VerificationRules());
        assertEquals(2048, reader.reads.size());
        assertEquals(4, scan.remainingChunks());
        assertEquals(4, scan.pendingChunks(0, 0).size());
        assertTrue(scan.apply(compute(batch), reader));
        assertEquals(2048, scan.checked());
        assertTrue(scan.done());
        assertEquals(0, scan.remainingChunks());
    }

    @Test public void changedColumnRejectsOnlyItsOwnPartOfABatch() throws Exception {
        VerificationScan scan = scan(48, 32);
        Reader reader = new Reader();
        VerificationScan.Batch batch = scan.capture(reader, 4096, Long.MAX_VALUE, new VerificationRules());
        reader.value = DIRT;
        scan.changed(20, 1, 0, 20, 1, 0);
        assertTrue(scan.apply(compute(batch), reader));
        assertEquals(1024, scan.checked());
        assertEquals(1, scan.remainingChunks());
        reader.reads.clear(); finish(scan, reader);
        assertEquals(512, reader.reads.size());
        assertEquals(512, scan.count(VerificationScan.Type.WRONG_BLOCK));
        assertEquals(1024, scan.count(VerificationScan.Type.CORRECT));
    }

    @Test public void changesWhileComparingRejectTheOldResultAndRescanTheSection() throws Exception {
        VerificationScan scan = scan(16, 32);
        Reader reader = new Reader();
        VerificationScan.Batch batch = scan.capture(reader, 4096, Long.MAX_VALUE, new VerificationRules());
        reader.value = DIRT;
        scan.changed(0, 3, 0, 0, 3, 0);
        assertFalse(scan.apply(compute(batch), reader));
        assertEquals(0, scan.checked());
        finish(scan, reader);
        assertEquals(512, scan.count(VerificationScan.Type.WRONG_BLOCK));
    }

    @Test public void cancelledAndUnloadedResultsCannotModifyCounts() throws Exception {
        VerificationScan scan = scan(4, 1);
        Reader reader = new Reader();
        VerificationScan.Batch old = scan.capture(reader, 4, Long.MAX_VALUE, new VerificationRules());
        scan.cancelBatch();
        VerificationScan.Batch next = scan.capture(reader, 4, Long.MAX_VALUE, new VerificationRules());
        assertFalse(scan.apply(compute(old), reader));
        reader.loaded = false;
        assertFalse(scan.apply(compute(next), reader));
        assertEquals(0, scan.checked());
        assertFalse(scan.done());
        reader.loaded = true;
        finish(scan, reader);
        assertEquals(4, scan.checked());
    }

    @Test public void distantAndVerticalChangesDoNotScanTheUntouchedGap() {
        VerificationScan scan = scan(80, 64);
        Reader reader = new Reader(); finish(scan, reader); reader.reads.clear();
        VerificationChanges changes = new VerificationChanges();
        for (int i = 0; i < 40; i++) {
            changes.add(1, 1, 0, 1, 1, 0);
            changes.add(65, 49, 0, 65, 49, 0);
        }
        changes.drain().apply(scan);
        reader.value = DIRT; finish(scan, reader);
        assertEquals(512, reader.reads.size());
        assertEquals(512, scan.count(VerificationScan.Type.WRONG_BLOCK));
        assertEquals(80 * 64 - 512, scan.count(VerificationScan.Type.CORRECT));
        assertFalse(reader.reads.contains("32,32,0"));
        assertFalse(reader.reads.contains("0,32,0"));
        changes.drain().apply(scan);
        assertTrue(scan.done());
    }

    @Test public void oversizedChangeQueuesFallBackToABoundedFullInvalidation() {
        VerificationScan scan = scan(4, 32);
        Reader reader = new Reader(); finish(scan, reader); reader.reads.clear();
        VerificationChanges changes = new VerificationChanges();
        changes.add(Integer.MIN_VALUE, 0, Integer.MIN_VALUE, Integer.MAX_VALUE, 255, Integer.MAX_VALUE);
        changes.drain().apply(scan); finish(scan, reader);
        assertEquals(128, reader.reads.size());
    }

    @Test public void cancellationAfterPartialMergePreservesCountsWhenResumed() throws Exception {
        VerificationScan scan = scan(16, 16);
        Reader reader = new Reader();
        VerificationScan.Batch first = scan.capture(reader, 10, Long.MAX_VALUE, new VerificationRules());
        scan.apply(compute(first), reader);
        VerificationScan.Batch second = scan.capture(reader, 10, Long.MAX_VALUE, new VerificationRules());
        scan.cancelBatch();
        assertFalse(scan.apply(compute(second), reader));
        finish(scan, reader);
        assertEquals(256, scan.checked());
        assertEquals(256, scan.count(VerificationScan.Type.CORRECT));
    }
}

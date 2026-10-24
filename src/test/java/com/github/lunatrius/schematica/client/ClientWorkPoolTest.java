package com.github.lunatrius.schematica.client;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import static org.junit.Assert.*;

public class ClientWorkPoolTest {
    @Test public void boundsCompletedResultsUntilTheyAreConsumedAndReleasesCancellation() throws Exception {
        ClientWorkPool pool = new ClientWorkPool(1, 2);
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        ClientWorkPool.Job<Integer> first = pool.submit(() -> { entered.countDown(); release.await(); return 1; });
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        ClientWorkPool.Job<Integer> second = pool.submit(() -> 2);
        try {
            assertNull(pool.submit(() -> 3));
            release.countDown();
            assertEquals(Integer.valueOf(1), first.get(5, TimeUnit.SECONDS));
            assertEquals(Integer.valueOf(2), second.get(5, TimeUnit.SECONDS));
            assertFalse(pool.available());
            first.close(); first.close();
            assertEquals(1, pool.availableSlots());
            ClientWorkPool.Job<Integer> next = pool.submit(() -> 3);
            assertNotNull(next); next.close();
        } finally { release.countDown(); first.close(); second.close(); }
        assertEquals(2, pool.availableSlots());
    }

    @Test public void cancellationRemovesQueuedWorkWithoutCallingIt() throws Exception {
        ClientWorkPool pool = new ClientWorkPool(1, 2);
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        ClientWorkPool.Job<Integer> first = pool.submit(() -> { entered.countDown(); release.await(); return 1; });
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        ClientWorkPool.Job<Integer> second = pool.submit(() -> { fail("Cancelled queued work ran"); return 2; });
        second.close(); release.countDown();
        try { assertEquals(Integer.valueOf(1), first.get(5, TimeUnit.SECONDS)); }
        finally { first.close(); }
        assertEquals(2, pool.availableSlots());
    }
}

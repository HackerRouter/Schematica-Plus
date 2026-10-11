// Machine update lock regressions, by HackerRouter, 2026.
package com.github.lunatrius.schematica.compat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.Test;
import static org.junit.Assert.*;

public class MachineUpdateLockTest {
    @Test public void competingMachineScanCannotEnterDuringAnEdit() throws Exception {
        ReentrantLock lock = new ReentrantLock();
        ExecutorService scanner = Executors.newSingleThreadExecutor();
        try {
            MachineUpdateLock.withLock(lock, () -> {
                try {
                    assertFalse(scanner.submit(() -> {
                        boolean acquired = lock.tryLock(100, TimeUnit.MILLISECONDS);
                        if (acquired) lock.unlock();
                        return acquired;
                    }).get(5, TimeUnit.SECONDS));
                } catch (Exception e) { throw new AssertionError(e); }
            });
            assertTrue(scanner.submit(() -> {
                boolean acquired = lock.tryLock(1, TimeUnit.SECONDS);
                if (acquired) lock.unlock();
                return acquired;
            }).get(5, TimeUnit.SECONDS));
        } finally {
            scanner.shutdownNow();
        }
    }

    @Test public void nestedEditsPreserveTheExistingTickLockOnFailure() {
        ReentrantLock lock = new ReentrantLock();
        lock.lock();
        try {
            try {
                MachineUpdateLock.withLock(lock, () -> {
                    assertEquals(2, lock.getHoldCount());
                    throw new IllegalStateException("edit failed");
                });
                fail("Expected edit failure");
            } catch (IllegalStateException expected) { assertEquals("edit failed", expected.getMessage()); }
            assertEquals(1, lock.getHoldCount());
        } finally {
            lock.unlock();
        }
        assertFalse(lock.isLocked());
    }

    @Test public void absentGregTechDoesNotPreventEdits() {
        int[] calls = {0};
        MachineUpdateLock.withLock(null, () -> calls[0]++);
        assertEquals(1, calls[0]);
    }
}

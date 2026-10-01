package com.github.lunatrius.schematica.task;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import static org.junit.Assert.*;

public class TaskRegistryTest {
    private final TaskRegistry registry = new TaskRegistry();
    private final UUID owner = UUID.randomUUID();

    private TaskRegistry.Task task(int dimension) {
        return registry.start(owner, dimension, TaskRegistry.Kind.SAVE, TaskRegistry.Backend.CLIENT, "test.schemplus");
    }

    @Test public void filtersByPlayerAndDimensionAndReturnsDetachedLists() {
        TaskRegistry.Task local = task(0);
        TaskRegistry.Task otherDimension = task(-1);
        TaskRegistry.Task otherPlayer = registry.start(UUID.randomUUID(), 0, TaskRegistry.Kind.FILL,
            TaskRegistry.Backend.SERVER, "0, 64, 0");
        assertEquals(Collections.singletonList(local), registry.tasks(owner, 0));
        assertEquals(Collections.singletonList(otherDimension), registry.tasks(owner, -1));
        registry.tasks(owner, 0).clear();
        assertEquals(1, registry.tasks(owner, 0).size());
        assertFalse(local.cancel(otherPlayer.owner));
        assertTrue(local.progress().cancellable);
    }

    @Test public void cancelledSaveNeverEntersWritingAndProgressCannotReactivateIt() {
        TaskRegistry.Task task = task(0);
        task.update(TaskRegistry.Stage.CAPTURE, 2, 8, 0, 0);
        TaskRegistry.Progress before = task.progress();
        assertTrue(task.cancel(owner));
        task.update(TaskRegistry.Stage.CAPTURE, 3, 8, 0, 0);
        assertTrue(task.progress().cancelling);
        assertFalse(task.progress().cancellable);
        assertFalse(task.beginWrite());
        assertFalse(task.cancel(owner));
        assertFalse(before.cancelling);
        assertEquals(2, before.completed);
        assertEquals(1, registry.tasks(owner, 0).size());
        task.finish();
        assertTrue(registry.tasks(owner, 0).isEmpty());
    }

    @Test public void staleRowCannotCancelReplacementOrReappearAfterCompletion() {
        TaskRegistry.Task old = task(0);
        old.finish();
        TaskRegistry.Task replacement = task(0);
        assertFalse(old.cancel(owner));
        assertFalse(old.beginWrite());
        old.update(TaskRegistry.Stage.CAPTURE, 1, 1, 0, 0);
        old.finish();
        assertTrue(old.progress().finished);
        assertEquals(Collections.singletonList(replacement), registry.tasks(owner, 0));
        assertTrue(replacement.progress().cancellable);
    }

    @Test public void writingCannotBeCancelledAndFinishReleasesTheRecord() {
        TaskRegistry.Task task = task(0);
        assertTrue(task.beginWrite());
        assertEquals(TaskRegistry.Stage.WRITE, task.progress().stage);
        assertFalse(task.cancel(owner));
        assertFalse(task.beginWrite());
        assertFalse(task.progress().cancellable);
        task.finish();
        assertTrue(registry.tasks(owner, 0).isEmpty());
        assertFalse(task.cancel(owner));
    }

    @Test public void cancelAndFileWriteHaveExactlyOneWinnerAcrossThreads() throws Exception {
        ExecutorService threads = Executors.newFixedThreadPool(2);
        try {
            for (int attempt = 0; attempt < 64; attempt++) {
                TaskRegistry.Task task = task(0);
                CyclicBarrier gate = new CyclicBarrier(2);
                Future<Boolean> cancel = threads.submit(() -> { gate.await(2, TimeUnit.SECONDS); return task.cancel(owner); });
                Future<Boolean> write = threads.submit(() -> { gate.await(2, TimeUnit.SECONDS); return task.beginWrite(); });
                assertNotEquals(cancel.get(3, TimeUnit.SECONDS), write.get(3, TimeUnit.SECONDS));
                assertFalse(task.progress().cancellable);
                task.finish();
            }
            assertTrue(registry.tasks(owner, 0).isEmpty());
        } finally { threads.shutdownNow(); }
    }

    @Test public void progressIsPublishedAsOneConsistentSnapshot() throws Exception {
        TaskRegistry.Task task = task(0);
        ExecutorService thread = Executors.newSingleThreadExecutor();
        try {
            Future<?> updates = thread.submit(() -> {
                for (int i = 1; i <= 10000; i++) task.update(TaskRegistry.Stage.CAPTURE, i, 10000, i * 2L, i * 3L);
            });
            for (int i = 0; i < 10000; i++) {
                TaskRegistry.Progress progress = task.progress();
                assertEquals(progress.completed * 2, progress.affected);
                assertEquals(progress.completed * 3, progress.entities);
            }
            updates.get(3, TimeUnit.SECONDS);
            assertEquals(10000, task.progress().completed);
        } finally { thread.shutdownNow(); task.finish(); }
    }
}

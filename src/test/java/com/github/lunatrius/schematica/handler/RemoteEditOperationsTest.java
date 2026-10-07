// Remote edit cancellation regressions, by HackerRouter, 2026.
package com.github.lunatrius.schematica.handler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.github.lunatrius.schematica.task.TaskRegistry;
import com.github.lunatrius.schematica.tool.WorldEditJob;
import org.junit.Test;

import static org.junit.Assert.*;

public class RemoteEditOperationsTest {
    private final UUID owner = UUID.randomUUID();
    private final List<Object> submitted = new ArrayList<>();
    private final List<Object> cancelled = new ArrayList<>();
    private final RemoteEditOperations<Object> operations = new RemoteEditOperations<>(cancelled::add);
    private final Queue<Runnable> decoder = new ArrayDeque<>();
    private final Queue<Runnable> server = new ArrayDeque<>();

    private RemoteEditOperations.Operation<Object> upload(long id, Object job) {
        RemoteEditOperations.Operation<Object> operation = operations.begin(owner, id);
        assertNotNull(operation);
        operations.uploaded(operation);
        decoder.add(() -> server.add(() -> operations.start(operation, job, submitted::add)));
        return operation;
    }

    private void drain() {
        while (!decoder.isEmpty()) decoder.remove().run();
        while (!server.isEmpty()) server.remove().run();
    }

    @Test public void cancellationWhileDecodingPreventsWorldSubmission() {
        upload(1, new Object());
        operations.cancel(owner, 1);
        drain();
        assertTrue(submitted.isEmpty());
    }

    @Test public void cancellationAfterDecodeBeforeServerTickPreventsSubmission() {
        upload(1, new Object());
        decoder.remove().run();
        operations.cancel(owner, 1);
        drain();
        assertTrue(submitted.isEmpty());
    }

    @Test public void staleDecodeAndCompletionCannotReplaceOrRemoveTheNextOperation() {
        RemoteEditOperations.Operation<Object> first = upload(1, new Object());
        operations.cancel(owner, 1);
        Object nextJob = new Object();
        RemoteEditOperations.Operation<Object> second = upload(2, nextJob);
        assertFalse(operations.finish(first));
        drain();
        assertEquals(java.util.Collections.singletonList(nextJob), submitted);
        assertTrue(operations.active(second));
        operations.cancel(owner, 1);
        assertTrue(cancelled.isEmpty());
        assertTrue(operations.finish(second));
        assertNotNull(operations.begin(owner, 3));
    }

    @Test public void ownershipRemainsBusyAcrossUploadDecodeQueueAndExecution() {
        RemoteEditOperations.Operation<Object> operation = operations.begin(owner, 1);
        assertNull(operations.begin(owner, 2));
        operations.uploaded(operation);
        assertNull(operations.begin(owner, 2));
        assertEquals(RemoteEditOperations.Start.STARTED, operations.start(operation, new Object(), submitted::add));
        assertNull(operations.begin(owner, 2));
        assertEquals(RemoteEditOperations.Start.DISCARDED, operations.start(operation, new Object(), submitted::add));
        assertEquals(1, submitted.size());
        assertTrue(operations.finish(operation));
        assertNotNull(operations.begin(owner, 2));
    }

    @Test public void decodeFailureAndQueueRejectionReleaseOnlyTheirOwnToken() {
        RemoteEditOperations.Operation<Object> failed = upload(1, new Object());
        assertTrue(operations.finish(failed));
        RemoteEditOperations.Operation<Object> next = operations.begin(owner, 2);
        assertNotNull(next);
        assertFalse(operations.finish(failed));
        drain();
        assertTrue(submitted.isEmpty());
        assertTrue(operations.active(next));
    }

    @Test public void busyWorldQueueReleasesTheOperation() {
        RemoteEditOperations.Operation<Object> operation = operations.begin(owner, 1);
        operations.uploaded(operation);
        assertEquals(RemoteEditOperations.Start.BUSY, operations.start(operation, new Object(), job -> false));
        assertFalse(operations.active(operation));
        assertNotNull(operations.begin(owner, 2));
        assertTrue(cancelled.isEmpty());
    }

    @Test public void submissionFailureReleasesTheOperation() {
        RemoteEditOperations.Operation<Object> operation = operations.begin(owner, 1);
        operations.uploaded(operation);
        assertThrows(IllegalStateException.class, () -> operations.start(operation, new Object(), job -> {
            throw new IllegalStateException("queue unavailable");
        }));
        assertFalse(operations.active(operation));
        assertNotNull(operations.begin(owner, 2));
    }

    @Test public void logoutInvalidatesQueuedWorkAndAllowsReconnection() {
        upload(1, new Object());
        decoder.remove().run();
        operations.forget(owner);
        Object nextJob = new Object();
        upload(2, nextJob);
        drain();
        assertEquals(java.util.Collections.singletonList(nextJob), submitted);
    }

    @Test public void serverClearInvalidatesDecodingAndQueuedWork() {
        upload(1, new Object());
        decoder.remove().run();
        UUID another = UUID.randomUUID();
        RemoteEditOperations.Operation<Object> other = operations.begin(another, 2);
        operations.uploaded(other);
        operations.clear();
        drain();
        assertEquals(RemoteEditOperations.Start.DISCARDED, operations.start(other, new Object(), submitted::add));
        assertTrue(submitted.isEmpty());
        assertNotNull(operations.begin(owner, 3));
    }

    @Test public void runningCancellationUsesTheCapturedJobOnceAndReleasesOwnership() {
        Object job = new Object();
        upload(1, job);
        drain();
        operations.cancel(owner, 1);
        operations.cancel(owner, 1);
        assertEquals(java.util.Collections.singletonList(job), cancelled);
        assertNotNull(operations.begin(owner, 2));
    }

    @Test public void startAndCancelCannotLeaveAnUntrackedRunningJob() throws Exception {
        ExecutorService threads = Executors.newFixedThreadPool(2);
        try {
            for (int attempt = 0; attempt < 32; attempt++) {
                Object job = new Object();
                RemoteEditOperations.Operation<Object> operation = operations.begin(owner, attempt);
                operations.uploaded(operation);
                CyclicBarrier gate = new CyclicBarrier(2);
                Future<RemoteEditOperations.Start> start = threads.submit(() -> {
                    gate.await(2, TimeUnit.SECONDS);
                    return operations.start(operation, job, submitted::add);
                });
                final long id = attempt;
                Future<?> cancel = threads.submit(() -> {
                    gate.await(2, TimeUnit.SECONDS);
                    operations.cancel(owner, id);
                    return null;
                });
                RemoteEditOperations.Start result = start.get(3, TimeUnit.SECONDS);
                cancel.get(3, TimeUnit.SECONDS);
                assertFalse(operations.active(operation));
                if (result == RemoteEditOperations.Start.STARTED) assertTrue(cancelled.contains(job));
                else assertFalse(submitted.contains(job));
            }
        } finally { threads.shutdownNow(); }
    }

    @Test public void cancellationWaitsForSubmissionAndThenCancelsThatJob() throws Exception {
        ExecutorService threads = Executors.newFixedThreadPool(2);
        CountDownLatch insideSubmit = new CountDownLatch(1), releaseSubmit = new CountDownLatch(1), cancelling = new CountDownLatch(1);
        Object job = new Object();
        RemoteEditOperations.Operation<Object> operation = operations.begin(owner, 1);
        operations.uploaded(operation);
        try {
            Future<RemoteEditOperations.Start> start = threads.submit(() -> operations.start(operation, job, value -> {
                insideSubmit.countDown();
                try { assertTrue(releaseSubmit.await(2, TimeUnit.SECONDS)); }
                catch (InterruptedException e) { throw new AssertionError(e); }
                return submitted.add(value);
            }));
            assertTrue(insideSubmit.await(2, TimeUnit.SECONDS));
            Future<?> cancel = threads.submit(() -> { cancelling.countDown(); operations.cancel(owner, 1); });
            assertTrue(cancelling.await(2, TimeUnit.SECONDS));
            releaseSubmit.countDown();
            assertEquals(RemoteEditOperations.Start.STARTED, start.get(3, TimeUnit.SECONDS));
            cancel.get(3, TimeUnit.SECONDS);
            assertEquals(java.util.Collections.singletonList(job), cancelled);
            assertFalse(operations.active(operation));
        } finally { releaseSubmit.countDown(); threads.shutdownNow(); }
    }

    @Test public void staleRemoteCancellationCannotCancelAnotherQueueEntryForTheSameOwner() {
        WorldEditQueue queue = new WorldEditQueue();
        WorldEditJob first = new WorldEditJob(owner, 0, WorldEditJob.Kind.FILL, 0, 64, 0, 1, 1, 1, null, 0, null, 0);
        WorldEditJob next = new WorldEditJob(owner, 0, WorldEditJob.Kind.FILL, 0, 64, 0, 1, 1, 1, null, 0, null, 0);
        try {
            assertTrue(queue.submit(null, next));
            assertFalse(queue.cancel(owner, first));
            assertFalse(next.cancelled);
            assertFalse(TaskRegistry.INSTANCE.tasks(owner, 0).get(0).progress().cancelling);
            assertTrue(queue.cancel(owner, next));
            assertTrue(next.cancelled);
            assertTrue(TaskRegistry.INSTANCE.tasks(owner, 0).get(0).progress().cancelling);
        } finally { queue.clear(); }
    }
}

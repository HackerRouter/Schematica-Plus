package com.github.lunatrius.schematica.client;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class ClientWorkPool {
    public static final ClientWorkPool INSTANCE = new ClientWorkPool(2, 8);
    private final Semaphore slots;
    private final ThreadPoolExecutor executor;

    public ClientWorkPool(int threads, int capacity) {
        slots = new Semaphore(capacity);
        executor = new ThreadPoolExecutor(threads, threads, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(capacity), task -> {
            Thread thread = new Thread(task, "Schematica Plus worker");
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            return thread;
        });
        executor.allowCoreThreadTimeOut(true);
    }

    public boolean available() { return slots.availablePermits() > 0; }
    public int availableSlots() { return slots.availablePermits(); }

    public <T> Job<T> submit(Callable<T> computation) {
        if (!slots.tryAcquire()) return null;
        Job<T> job = new Job<>(computation);
        try { executor.execute(job); return job; }
        catch (RejectedExecutionException error) { job.close(); return null; }
    }

    public final class Job<T> extends FutureTask<T> implements AutoCloseable {
        private boolean closed;
        private Job(Callable<T> computation) { super(computation); }

        public T result() {
            if (!isDone()) throw new IllegalStateException("Work is not complete");
            try { return get(); }
            catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new IllegalStateException(error); }
            catch (ExecutionException error) { throw new IllegalStateException("Background computation failed", error.getCause()); }
        }

        @Override public synchronized void close() {
            if (closed) return;
            closed = true;
            cancel(true);
            executor.remove(this);
            slots.release();
        }
    }
}

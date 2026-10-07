// SPDX-License-Identifier: LGPL-3.0-only
// Remote edit lifecycle, by HackerRouter, 2026.
package com.github.lunatrius.schematica.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

final class RemoteEditOperations<J> {
    enum Start { STARTED, BUSY, DISCARDED }

    static final class Operation<J> {
        final UUID owner;
        final long id;
        private J job;
        private boolean uploaded;

        Operation(UUID owner, long id) { this.owner = owner; this.id = id; }
    }

    private final Map<UUID, Operation<J>> current = new HashMap<>();
    private final Consumer<J> cancelJob;

    RemoteEditOperations(Consumer<J> cancelJob) { this.cancelJob = cancelJob; }

    synchronized Operation<J> begin(UUID owner, long id) {
        if (current.containsKey(owner)) return null;
        Operation<J> operation = new Operation<>(owner, id);
        current.put(owner, operation);
        return operation;
    }

    synchronized void uploaded(Operation<J> operation) {
        if (active(operation)) operation.uploaded = true;
    }

    synchronized boolean active(Operation<J> operation) { return current.get(operation.owner) == operation; }

    synchronized Start start(Operation<J> operation, J job, Predicate<J> submit) {
        if (!active(operation) || !operation.uploaded || operation.job != null) return Start.DISCARDED;
        try {
            if (!submit.test(job)) { finish(operation); return Start.BUSY; }
            operation.job = job;
            return Start.STARTED;
        } catch (RuntimeException e) {
            finish(operation);
            throw e;
        }
    }

    synchronized boolean finish(Operation<J> operation) { return current.remove(operation.owner, operation); }

    synchronized void cancel(UUID owner, long id) {
        Operation<J> operation = current.get(owner);
        if (operation != null && operation.id == id) discard(operation);
    }

    synchronized void forget(UUID owner) {
        Operation<J> operation = current.get(owner);
        if (operation != null) discard(operation);
    }

    synchronized void clear() {
        for (Operation<J> operation : current.values()) if (operation.job != null) cancelJob.accept(operation.job);
        current.clear();
    }

    private void discard(Operation<J> operation) {
        current.remove(operation.owner, operation);
        if (operation.job != null) cancelJob.accept(operation.job);
    }
}

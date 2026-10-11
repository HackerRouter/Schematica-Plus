package com.github.lunatrius.schematica.compat;

import java.util.concurrent.locks.Lock;

import com.github.lunatrius.schematica.reference.Reference;

/** Serializes queued world edits with GregTech's asynchronous machine scans. */
public final class MachineUpdateLock {
    private static final Lock LOCK = findLock();

    private MachineUpdateLock() {}

    private static Lock findLock() {
        try {
            Object proxy = Class.forName("gregtech.GTMod").getField("proxy").get(null);
            Object lock = proxy.getClass().getField("TICK_LOCK").get(proxy);
            return lock instanceof Lock ? (Lock) lock : null;
        } catch (ClassNotFoundException | NoSuchFieldException absent) {
            return null;
        } catch (ReflectiveOperationException | LinkageError error) {
            Reference.logger.warn("Could not find the machine update lock", error);
            return null;
        }
    }

    public static void run(Runnable action) { withLock(LOCK, action); }

    static void withLock(Lock lock, Runnable action) {
        if (lock == null) { action.run(); return; }
        lock.lock();
        try {
            action.run();
        } finally {
            lock.unlock();
        }
    }
}

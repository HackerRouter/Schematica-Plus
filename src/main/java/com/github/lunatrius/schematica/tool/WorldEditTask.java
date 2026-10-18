package com.github.lunatrius.schematica.tool;

import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.world.WorldServer;

import com.github.lunatrius.schematica.task.TaskRegistry;
import com.github.lunatrius.schematica.util.MessageType;
import com.github.lunatrius.schematica.util.PlayerMessage;

public abstract class WorldEditTask {
    public enum Outcome {
        FINISHED, PARTIAL, FAILED;

        public boolean completed() { return this != FAILED; }
    }

    public final UUID player;
    public final int dimension, x, y, z;
    public volatile boolean cancelled;
    public int blockCount, entityCount;
    public Consumer<Outcome> completion = outcome -> {};
    /** Called on the server thread with each published progress, for remote clients. */
    public Consumer<TaskRegistry.Progress> progress;

    protected WorldEditTask(UUID player, int dimension, int x, int y, int z) {
        this.player = player; this.dimension = dimension; this.x = x; this.y = y; this.z = z;
    }
    public abstract boolean step(WorldServer world);
    public abstract void flushBlockChanges(WorldServer world);
    public abstract TaskRegistry.Kind taskKind();
    public abstract void publishProgress(TaskRegistry.Task task);
    public boolean needsRollback() { return false; }
    public RuntimeException failure() { return null; }

    public Outcome outcome() {
        return cancelled || failure() != null ? Outcome.FAILED : Outcome.FINISHED;
    }

    /** The upstream task end message for this edit, or null when upstream shows none. */
    public PlayerMessage finishedMessage(boolean success) {
        String key = taskKind().finishedKey(success);
        return key == null ? null : new PlayerMessage(success ? MessageType.SUCCESS : MessageType.ERROR, key);
    }
}

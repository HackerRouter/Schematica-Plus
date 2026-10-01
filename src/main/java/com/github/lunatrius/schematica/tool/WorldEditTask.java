package com.github.lunatrius.schematica.tool;

import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.world.WorldServer;

import com.github.lunatrius.schematica.task.TaskRegistry;

public abstract class WorldEditTask {
    public final UUID player;
    public final int dimension, x, y, z;
    public volatile boolean cancelled;
    public int blockCount, entityCount;
    public Consumer<Boolean> completion = success -> {};

    protected WorldEditTask(UUID player, int dimension, int x, int y, int z) {
        this.player = player; this.dimension = dimension; this.x = x; this.y = y; this.z = z;
    }
    public abstract boolean step(WorldServer world);
    public abstract void flushBlockChanges(WorldServer world);
    public abstract TaskRegistry.Kind taskKind();
    public abstract void publishProgress(TaskRegistry.Task task);
    public boolean needsRollback() { return false; }
    public RuntimeException failure() { return null; }

    /** The upstream task end message for this edit, or null when upstream shows none. */
    public net.minecraft.util.IChatComponent finishedMessage(boolean success) {
        String key = taskKind().finishedKey(success);
        return key == null ? null : new net.minecraft.util.ChatComponentTranslation(key);
    }
}

package com.github.lunatrius.schematica.handler;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import com.github.lunatrius.schematica.util.MessageException;
import net.minecraft.world.WorldServer;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.tool.WorldEditJob;
import com.github.lunatrius.schematica.task.TaskRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Integrated-server edits enter here from the client and execute exclusively on server ticks. */
public final class WorldEditQueue {
    public static final WorldEditQueue INSTANCE = new WorldEditQueue();
    private final AtomicReference<Edit> pending = new AtomicReference<>();

    private static final class Edit {
        final MinecraftServer server;
        final WorldEditJob job;
        final TaskRegistry.Task task;

        Edit(MinecraftServer server, WorldEditJob job) {
            this.server = server;
            this.job = job;
            this.task = TaskRegistry.INSTANCE.start(job.player, job.dimension, job.taskKind(), TaskRegistry.Backend.SERVER,
                job.x + ", " + job.y + ", " + job.z);
        }
    }

    public synchronized void clear() {
        Edit edit = pending.getAndSet(null);
        if (edit != null) {
            edit.job.cancelled = true;
            edit.task.finish();
        }
    }

    public boolean cancel(UUID player) {
        Edit edit = pending.get();
        if (edit == null || !edit.job.player.equals(player)) return false;
        edit.task.cancel(player);
        edit.job.cancelled = true;
        return true;
    }

    public synchronized boolean submit(MinecraftServer server, WorldEditJob job) {
        Edit edit = pending.get();
        if (edit != null && edit.server != server) clear();
        if (pending.get() != null) return false;
        pending.set(new Edit(server, job));
        return true;
    }

    private void finish(Edit edit) {
        pending.compareAndSet(edit, null);
        edit.task.finish();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Edit edit = pending.get();
        if (edit == null) return;
        WorldEditJob job = edit.job;
        MinecraftServer server = MinecraftServer.getServer();
        if (server != edit.server || server == null) { finish(edit); return; }
        EntityPlayerMP player = null;
        for (EntityPlayerMP candidate : server.getConfigurationManager().playerEntityList) {
            if (candidate.getUniqueID().equals(job.player)) { player = candidate; break; }
        }
        if (player == null || player.dimension != job.dimension) { finish(edit); return; }
        if (!player.capabilities.isCreativeMode || !player.canCommandSenderUseCommand(2, "setblock")) {
            player.addChatMessage(new ChatComponentTranslation("schematica.message.edit.permissions"));
            finish(edit);
            return;
        }
        WorldServer world = server.worldServerForDimension(job.dimension);
        try {
            long deadline = System.nanoTime() + 8_000_000L;
            boolean done = false;
            try {
                for (int i = 0; i < 2048 && System.nanoTime() < deadline; i++) {
                    if (edit.task.progress().cancelling) job.cancelled = true;
                    if (job.step(world)) { done = true; break; }
                }
            } finally {
                job.flushBlockChanges(world);
            }
            job.publishProgress(edit.task);
            if (done) {
                player.addChatMessage(new ChatComponentTranslation(job.cancelled
                    ? "schematica.message.edit.cancelled" : "schematica.message.edit.finished", job.blockCount, job.entityCount));
                finish(edit);
            }
        } catch (Exception e) {
            Reference.logger.error("World edit stopped after partial completion", e);
            player.addChatMessage(new ChatComponentTranslation("schematica.message.edit.stopped", e instanceof MessageException
                ? new ChatComponentTranslation(((MessageException) e).key(), ((MessageException) e).arguments())
                : new ChatComponentTranslation("schematica.message.edit.see_log")));
            finish(edit);
        }
    }
}

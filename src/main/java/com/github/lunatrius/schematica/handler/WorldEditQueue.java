package com.github.lunatrius.schematica.handler;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.WorldServer;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.tool.WorldEditJob;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Integrated-server edits enter here from the client and execute exclusively on server ticks. */
public final class WorldEditQueue {
    public static final WorldEditQueue INSTANCE = new WorldEditQueue();
    private final AtomicReference<WorldEditJob> pending = new AtomicReference<>();
    private volatile MinecraftServer owner;

    public void clear() { pending.set(null); owner = null; }

    public boolean cancel(UUID player) {
        WorldEditJob job = pending.get();
        if (job == null || !job.player.equals(player)) return false;
        job.cancelled = true;
        return true;
    }

    public synchronized boolean submit(MinecraftServer server, WorldEditJob job) {
        if (owner != server) pending.set(null);
        owner = server;
        if (!pending.compareAndSet(null, job)) return false;
        return true;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        WorldEditJob job = pending.get();
        if (job == null) return;
        MinecraftServer server = MinecraftServer.getServer();
        if (server != owner || server == null) { pending.compareAndSet(job, null); return; }
        EntityPlayerMP player = null;
        for (EntityPlayerMP candidate : server.getConfigurationManager().playerEntityList) {
            if (candidate.getUniqueID().equals(job.player)) { player = candidate; break; }
        }
        if (player == null || player.dimension != job.dimension) { pending.compareAndSet(job, null); return; }
        if (!player.capabilities.isCreativeMode || !player.canCommandSenderUseCommand(2, "setblock")) {
            player.addChatMessage(new ChatComponentText("[Schematica] Editing requires creative mode and command permission."));
            pending.compareAndSet(job, null);
            return;
        }
        WorldServer world = server.worldServerForDimension(job.dimension);
        try {
            long deadline = System.nanoTime() + 8_000_000L;
            boolean done = false;
            try {
                for (int i = 0; i < 2048 && System.nanoTime() < deadline; i++) {
                    if (job.step(world)) { done = true; break; }
                }
            } finally {
                job.flushBlockChanges(world);
            }
            if (done) {
                player.addChatMessage(new ChatComponentText("[Schematica] Edit " + (job.cancelled ? "cancelled" : "finished")
                    + ": " + job.blockCount + " blocks, " + job.entityCount + " entities. Changes already made are retained."));
                pending.compareAndSet(job, null);
            }
        } catch (Exception e) {
            Reference.logger.error("World edit stopped after partial completion", e);
            player.addChatMessage(new ChatComponentText("[Schematica] Edit stopped: " + e.getMessage()
                + ". Changes already made are retained."));
            pending.compareAndSet(job, null);
        }
    }
}

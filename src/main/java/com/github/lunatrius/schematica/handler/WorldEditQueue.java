package com.github.lunatrius.schematica.handler;

import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.compat.MachineUpdateLock;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.task.TaskRegistry;
import com.github.lunatrius.schematica.tool.WorldEditTask;
import com.github.lunatrius.schematica.util.MessageException;
import com.github.lunatrius.schematica.util.MessageType;
import com.github.lunatrius.schematica.util.PlayerMessage;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Integrated-server edits enter here from the client and execute exclusively on server ticks. */
public final class WorldEditQueue {
    public static final WorldEditQueue INSTANCE = new WorldEditQueue();
    private final AtomicReference<Edit> pending = new AtomicReference<>();
    private final ArrayBlockingQueue<Runnable> requests = new ArrayBlockingQueue<>(16);

    public boolean enqueue(Runnable request) { return requests.offer(request); }

    public static boolean mayEdit(EntityPlayerMP player) {
        return player.capabilities.isCreativeMode && player.canCommandSenderUseCommand(2, "setblock");
    }

    private static final class Edit {
        final MinecraftServer server;
        final WorldEditTask job;
        final TaskRegistry.Task task;

        Edit(MinecraftServer server, WorldEditTask job) {
            this.server = server;
            this.job = job;
            this.task = TaskRegistry.INSTANCE.start(job.player, job.dimension, job.taskKind(), TaskRegistry.Backend.SERVER,
                job.x + ", " + job.y + ", " + job.z);
        }
    }

    public synchronized void clear() {
        requests.clear();
        Edit edit = pending.getAndSet(null);
        if (edit != null) {
            edit.job.cancelled = true;
            edit.task.finish();
        }
    }

    public boolean cancel(UUID player) {
        return cancel(player, null);
    }

    public boolean cancel(UUID player, WorldEditTask expected) {
        Edit edit = pending.get();
        if (edit == null || !edit.job.player.equals(player) || (expected != null && edit.job != expected)) return false;
        edit.task.cancel(player);
        edit.job.cancelled = true;
        return true;
    }

    public synchronized boolean submit(MinecraftServer server, WorldEditTask job) {
        Edit edit = pending.get();
        if (edit != null && edit.server != server) clear();
        if (pending.get() != null) return false;
        pending.set(new Edit(server, job));
        return true;
    }

    private void finish(Edit edit) { finish(edit, WorldEditTask.Outcome.FAILED); }
    private void finish(Edit edit, WorldEditTask.Outcome outcome) {
        pending.compareAndSet(edit, null); edit.task.finish(); edit.job.completion.accept(outcome);
    }

    public void rollbackBeforeShutdown(MinecraftServer server) {
        MachineUpdateLock.run(() -> rollback(server));
    }

    private void rollback(MinecraftServer server) {
        Edit edit = pending.get();
        if (edit == null || edit.server != server || !edit.job.needsRollback()) return;
        edit.job.cancelled = true;
        WorldServer world = server.worldServerForDimension(edit.job.dimension);
        try { while (!edit.job.step(world)) {} }
        finally { edit.job.flushBlockChanges(world); finish(edit); }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (requests.isEmpty() && pending.get() == null) return;
        MachineUpdateLock.run(this::processTick);
    }

    private void processTick() {
        Runnable request = requests.poll();
        if (request != null) request.run();
        Edit edit = pending.get();
        if (edit == null) return;
        WorldEditTask job = edit.job;
        MinecraftServer server = MinecraftServer.getServer();
        if (server != edit.server || server == null) { finish(edit); return; }
        EntityPlayerMP player = null;
        for (EntityPlayerMP candidate : server.getConfigurationManager().playerEntityList) {
            if (candidate.getUniqueID().equals(job.player)) { player = candidate; break; }
        }
        boolean authorized = player != null && player.dimension == job.dimension && mayEdit(player);
        if (!authorized) {
            if (player != null && !job.cancelled) SchematicaPlus.proxy.sendMessage(player, new PlayerMessage(MessageType.ERROR, "schematica.message.edit.permissions"));
            job.cancelled = true;
            if (!job.needsRollback()) { finish(edit); return; }
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
            if (job.progress != null) job.progress.accept(edit.task.progress());
            if (done) {
                WorldEditTask.Outcome outcome = job.outcome();
                if (job.failure() != null) Reference.logger.error("World edit finished with errors", job.failure());
                SchematicaPlus.proxy.sendMessage(player, job.finishedMessage(outcome == WorldEditTask.Outcome.FINISHED));
                finish(edit, outcome);
            }
        } catch (Exception e) {
            Reference.logger.error("World edit stopped after partial completion", e);
            SchematicaPlus.proxy.sendMessage(player, job.finishedMessage(false));
            if (e instanceof MessageException) {
                SchematicaPlus.proxy.sendMessage(player, new PlayerMessage(MessageType.ERROR, ((MessageException) e).key(), ((MessageException) e).arguments()));
            }
            finish(edit);
        }
    }
}

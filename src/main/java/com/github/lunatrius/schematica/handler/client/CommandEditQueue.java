package com.github.lunatrius.schematica.handler.client;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ClientChatReceivedEvent;

import com.github.lunatrius.schematica.client.util.InfoUtils;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.task.TaskRegistry;
import com.github.lunatrius.schematica.tool.WorldEditJob;
import com.github.lunatrius.schematica.tool.WorldEditTask;
import com.github.lunatrius.schematica.util.MessageException;
import com.github.lunatrius.schematica.util.MessageType;
import com.github.lunatrius.schematica.util.PlayerMessage;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Vanilla-server fallback, waiting for native command replies before advancing. */
public final class CommandEditQueue {
    public static final CommandEditQueue INSTANCE = new CommandEditQueue();
    private static final int REPLY_TIMEOUT = 200;
    private static final Set<String> JOB_ERRORS = new HashSet<>(Arrays.asList(
        "commands.generic.permission", "commands.generic.notFound", "commands.generic.usage", "commands.generic.snytax"));
    interface Client {
        boolean valid(WorldEditJob job, World world);
        String command(WorldEditJob job, int index, World world);
        void send(String command);
        void message(PlayerMessage message);
    }
    private final Client client;
    private WorldEditJob job;
    private World world;
    private TaskRegistry.Task task;
    private int cursor, sent, delay, pending, waiting;
    private final ArrayDeque<Integer> pendingCells = new ArrayDeque<>();

    public CommandEditQueue() {
        this(new Client() {
            public boolean valid(WorldEditJob job, World world) {
                Minecraft mc = Minecraft.getMinecraft();
                return mc.thePlayer != null && mc.theWorld == world && job.player.equals(mc.thePlayer.getUniqueID())
                    && job.dimension == mc.thePlayer.dimension && mc.thePlayer.capabilities.isCreativeMode;
            }
            public String command(WorldEditJob job, int index, World world) { return job.command(index, world); }
            public void send(String command) { Minecraft.getMinecraft().thePlayer.sendChatMessage(command); }
            public void message(PlayerMessage message) { InfoUtils.showGuiOrInGameMessage(message); }
        });
    }

    CommandEditQueue(Client client) { this.client = client; }

    public synchronized boolean cancel() { return end(WorldEditTask.Outcome.FAILED); }

    public synchronized boolean busy() { return job != null; }

    private boolean end(WorldEditTask.Outcome outcome) {
        if (job == null) return false;
        WorldEditJob finished = job;
        task.finish();
        task = null;
        job = null;
        world = null;
        pendingCells.clear();
        finished.completion.accept(outcome);
        return true;
    }

    public synchronized void submit(WorldEditJob next, World targetWorld) {
        if (job != null) throw new MessageException("schematica.message.edit.busy");
        if (next.kind == WorldEditJob.Kind.PASTE && ConfigurationHandler.pasteIgnoreBlockEntitiesEntirely) next.dropTiles();
        next.validateCommandFallback(); // Preflight before sending the first command.
        if (next.removesEntities()) {
            client.message(new PlayerMessage(MessageType.WARNING, "schematica.message.edit.entities_require_singleplayer"));
        }
        task = TaskRegistry.INSTANCE.start(next.player, next.dimension, next.taskKind(), TaskRegistry.Backend.COMMANDS,
            next.x + ", " + next.y + ", " + next.z);
        job = next; world = targetWorld; cursor = 0; sent = 0; delay = 0; pending = 0; waiting = 0;
        pendingCells.clear();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public synchronized void onChat(ClientChatReceivedEvent event) {
        if (job == null || pending == 0) return;
        String key = reply(event.message);
        if ("commands.setblock.success".equals(key) || "commands.summon.success".equals(key)) {
            pending--;
            if (pendingCells.removeFirst() < job.volume) job.blockCount++;
            else job.entityCount++;
            waiting = 0;
        } else if (key != null && job.kind == WorldEditJob.Kind.PASTE && !JOB_ERRORS.contains(key)) {
            pending--;
            int index = pendingCells.removeFirst();
            if (index >= job.volume) {
                job.recordEntityFailure(new MessageException("schematica.message.edit.entity_failed", job.entityId(index - job.volume)));
            } else {
                job.recordBlockFailure(index, new MessageException("schematica.message.edit.command_failed",
                    job.x + index % job.width, job.y + index / job.width / job.length, job.z + index / job.width % job.length, event.message));
            }
            waiting = 0;
        } else if (key != null) {
            client.message(job.finishedMessage(false));
            cancel();
        }
    }

    private static String reply(IChatComponent message) {
        if (message == null) return null;
        if (message instanceof ChatComponentTranslation) {
            String key = ((ChatComponentTranslation) message).getKey();
            if (key.startsWith("commands.setblock.") || key.startsWith("commands.summon.") || key.startsWith("commands.generic.")
                || key.startsWith("commands.block.") || "commands.give.notFound".equals(key)) return key;
            // Admin broadcasts contain another player's command result, not this queue's reply.
            return null;
        }
        for (Object sibling : message.getSiblings()) {
            String key = reply((IChatComponent) sibling);
            if (key != null) return key;
        }
        return null;
    }

    @SubscribeEvent
    public synchronized void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || job == null) return;
        if (task.progress().cancelling) { cancel(); return; }
        if (!client.valid(job, world)) { cancel(); return; }
        if (pending != 0) {
            if (++waiting >= REPLY_TIMEOUT) {
                client.message(new PlayerMessage(MessageType.ERROR, "schematica.message.edit.commands_unconfirmed", sent));
                cancel();
            }
            return;
        }
        if (delay-- > 0) return;
        try {
            // commandLimitPerTick commands every commandTaskInterval ticks
            int limit = sent == 0 ? 1 : ConfigurationHandler.commandLimitPerTick, issued = 0;
            for (int scanned = 0; scanned < 65536 && cursor < job.commandCount() && issued < limit; scanned++) {
                int index = cursor++;
                boolean queued = false;
                try {
                    String command = client.command(job, index, world);
                    if (command != null) {
                        pending++; pendingCells.addLast(index); queued = true;
                        issued++;
                        client.send(command);
                        sent++;
                    }
                } catch (RuntimeException | LinkageError error) {
                    if (job.kind != WorldEditJob.Kind.PASTE) throw error;
                    if (queued) { pending--; pendingCells.removeLast(); }
                    else issued++;
                    if (index >= job.volume) job.recordEntityFailure(error);
                    else job.recordBlockFailure(index, error);
                }
            }
            delay = ConfigurationHandler.commandTaskInterval - 1;
            task.update(TaskRegistry.Stage.COMMANDS, cursor, job.commandCount(), job.blockCount, job.entityCount);
            if (cursor == job.commandCount() && pending == 0) {
                job.completedTraversal = true;
                WorldEditTask.Outcome outcome = job.outcome();
                boolean success = outcome == WorldEditTask.Outcome.FINISHED;
                client.message(success && job.kind == WorldEditJob.Kind.PASTE && job.blockCount != 0
                    ? new PlayerMessage(MessageType.SUCCESS, "litematica.message.schematic_pasted_using_setblock", job.blockCount)
                    : job.finishedMessage(success));
                if (!success) Reference.logger.error("Command paste finished with errors", job.failure());
                end(outcome);
            }
        } catch (RuntimeException e) {
            Reference.logger.error("Command edit stopped after partial completion", e);
            client.message(job.finishedMessage(false));
            cancel();
        }
    }
}

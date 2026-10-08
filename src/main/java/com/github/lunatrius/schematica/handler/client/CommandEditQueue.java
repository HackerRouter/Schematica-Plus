package com.github.lunatrius.schematica.handler.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import com.github.lunatrius.schematica.tool.WorldEditJob;
import com.github.lunatrius.schematica.task.TaskRegistry;
import com.github.lunatrius.schematica.util.MessageException;
import com.github.lunatrius.schematica.reference.Reference;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Vanilla-server fallback, waiting for native command replies before advancing. */
public final class CommandEditQueue {
    public static final CommandEditQueue INSTANCE = new CommandEditQueue();
    private static final int REPLY_TIMEOUT = 200;
    interface Client {
        boolean valid(WorldEditJob job, World world);
        String command(WorldEditJob job, int index, World world);
        void send(String command);
        void message(IChatComponent message);
    }
    private final Client client;
    private WorldEditJob job;
    private World world;
    private TaskRegistry.Task task;
    private int cursor, sent, delay, pending, waiting;

    public CommandEditQueue() {
        this(new Client() {
            public boolean valid(WorldEditJob job, World world) {
                Minecraft mc = Minecraft.getMinecraft();
                return mc.thePlayer != null && mc.theWorld == world && job.player.equals(mc.thePlayer.getUniqueID())
                    && job.dimension == mc.thePlayer.dimension && mc.thePlayer.capabilities.isCreativeMode;
            }
            public String command(WorldEditJob job, int index, World world) { return job.command(index, world); }
            public void send(String command) { Minecraft.getMinecraft().thePlayer.sendChatMessage(command); }
            public void message(IChatComponent message) {
                if (message != null && Minecraft.getMinecraft().thePlayer != null) {
                    Minecraft.getMinecraft().thePlayer.addChatMessage(message);
                }
            }
        });
    }

    CommandEditQueue(Client client) { this.client = client; }

    public synchronized boolean cancel() { return end(false); }

    public synchronized boolean busy() { return job != null; }

    private boolean end(boolean success) {
        if (job == null) return false;
        WorldEditJob finished = job;
        task.finish();
        task = null;
        job = null;
        world = null;
        finished.completion.accept(success);
        return true;
    }

    public synchronized void submit(WorldEditJob next, World targetWorld) {
        if (job != null) throw new MessageException("schematica.message.edit.busy");
        if (next.kind == WorldEditJob.Kind.PASTE && com.github.lunatrius.schematica.handler.ConfigurationHandler.pasteIgnoreBlockEntitiesEntirely) next.dropTiles();
        next.validateCommandFallback(); // Preflight before sending the first command.
        if (next.removesEntities()) {
            client.message(new ChatComponentTranslation("schematica.message.edit.entities_require_singleplayer"));
        }
        task = TaskRegistry.INSTANCE.start(next.player, next.dimension, next.taskKind(), TaskRegistry.Backend.COMMANDS,
            next.x + ", " + next.y + ", " + next.z);
        job = next; world = targetWorld; cursor = 0; sent = 0; delay = 0; pending = 0; waiting = 0;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public synchronized void onChat(ClientChatReceivedEvent event) {
        if (job == null || pending == 0) return;
        String key = reply(event.message);
        if ("commands.setblock.success".equals(key)) {
            pending--;
            job.blockCount++;
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
            if (key.startsWith("commands.setblock.") || key.startsWith("commands.generic.")
                || key.startsWith("commands.block.")) return key;
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
                client.message(new ChatComponentTranslation("schematica.message.edit.commands_unconfirmed", sent));
                cancel();
            }
            return;
        }
        if (delay-- > 0) return;
        try {
            // commandLimitPerTick commands every commandTaskInterval ticks
            int limit = sent == 0 ? 1 : com.github.lunatrius.schematica.handler.ConfigurationHandler.commandLimitPerTick, issued = 0;
            for (int scanned = 0; scanned < 65536 && cursor < job.volume && issued < limit; scanned++) {
                String command = client.command(job, cursor++, world);
                if (command != null) {
                    pending++; sent++; issued++;
                    client.send(command);
                }
            }
            delay = com.github.lunatrius.schematica.handler.ConfigurationHandler.commandTaskInterval - 1;
            task.update(TaskRegistry.Stage.COMMANDS, cursor, job.volume, sent, 0);
            if (cursor == job.volume && pending == 0) {
                client.message(job.kind == WorldEditJob.Kind.PASTE && sent != 0
                    ? new ChatComponentTranslation("litematica.message.schematic_pasted_using_setblock", sent)
                    : job.finishedMessage(true));
                end(true);
            }
        } catch (RuntimeException e) {
            Reference.logger.error("Command edit stopped after partial completion", e);
            client.message(job.finishedMessage(false));
            cancel();
        }
    }
}

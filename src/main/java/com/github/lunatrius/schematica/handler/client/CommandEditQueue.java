package com.github.lunatrius.schematica.handler.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import com.github.lunatrius.schematica.tool.WorldEditJob;
import com.github.lunatrius.schematica.task.TaskRegistry;
import com.github.lunatrius.schematica.util.MessageException;
import com.github.lunatrius.schematica.reference.Reference;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Vanilla-server fallback. Reports commands sent, since chat provides no reliable per-command acknowledgement. */
public final class CommandEditQueue {
    public static final CommandEditQueue INSTANCE = new CommandEditQueue();
    private WorldEditJob job;
    private World world;
    private TaskRegistry.Task task;
    private int cursor, sent, delay;

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
        next.validateCommandFallback(); // Preflight before sending the first command.
        if (next.removesEntities() && Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentTranslation("schematica.message.edit.entities_require_singleplayer"));
        }
        task = TaskRegistry.INSTANCE.start(next.player, next.dimension, next.taskKind(), TaskRegistry.Backend.COMMANDS,
            next.x + ", " + next.y + ", " + next.z);
        job = next; world = targetWorld; cursor = 0; sent = 0; delay = 0;
    }

    @SubscribeEvent
    public synchronized void onChat(ClientChatReceivedEvent event) {
        if (job != null && event.message instanceof ChatComponentTranslation) {
            String key = ((ChatComponentTranslation) event.message).getKey();
            if ("commands.generic.permission".equals(key) || "commands.generic.notFound".equals(key)
                || "commands.generic.syntax".equals(key)) cancel();
        }
    }

    @SubscribeEvent
    public synchronized void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || job == null) return;
        if (task.progress().cancelling) { cancel(); return; }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld != world || !job.player.equals(mc.thePlayer.getUniqueID())) { cancel(); return; }
        if (delay-- > 0) return;
        try {
            for (int scanned = 0; scanned < 2048 && cursor < job.volume; scanned++) {
                String command = job.command(cursor++, world);
                if (command != null) {
                    mc.thePlayer.sendChatMessage(command);
                    sent++; delay = 3;
                    break;
                }
            }
            task.update(TaskRegistry.Stage.COMMANDS, cursor, job.volume, sent, 0);
            if (cursor == job.volume) {
                mc.thePlayer.addChatMessage(job.kind == WorldEditJob.Kind.PASTE
                    ? new ChatComponentTranslation("litematica.message.schematic_pasted_using_setblock", sent)
                    : job.finishedMessage(true));
                end(true);
            }
        } catch (RuntimeException e) {
            Reference.logger.error("Command edit stopped after partial completion", e);
            mc.thePlayer.addChatMessage(job.finishedMessage(false));
            cancel();
        }
    }
}

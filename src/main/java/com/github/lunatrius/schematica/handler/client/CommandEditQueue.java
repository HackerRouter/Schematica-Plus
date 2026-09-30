package com.github.lunatrius.schematica.handler.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import com.github.lunatrius.schematica.tool.WorldEditJob;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Vanilla-server fallback. Reports commands sent, since chat provides no reliable per-command acknowledgement. */
public final class CommandEditQueue {
    public static final CommandEditQueue INSTANCE = new CommandEditQueue();
    private WorldEditJob job;
    private World world;
    private int cursor, sent, delay;

    public boolean cancel() {
        if (job == null) return false;
        job = null;
        return true;
    }

    public void submit(WorldEditJob next, World targetWorld) {
        next.validateCommandFallback(); // Preflight before sending the first command.
        job = next; world = targetWorld; cursor = 0; sent = 0; delay = 0;
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (job != null && event.message instanceof ChatComponentTranslation) {
            String key = ((ChatComponentTranslation) event.message).getKey();
            if ("commands.generic.permission".equals(key) || "commands.generic.notFound".equals(key)
                || "commands.generic.syntax".equals(key)) cancel();
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || job == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld != world) { cancel(); return; }
        if (delay-- > 0) return;
        for (int scanned = 0; scanned < 2048 && cursor < job.volume; scanned++) {
            String command = job.command(cursor++, world);
            if (command != null) {
                mc.thePlayer.sendChatMessage(command);
                sent++; delay = 3;
                break;
            }
        }
        if (cursor == job.volume) {
            mc.thePlayer.addChatMessage(new ChatComponentTranslation("schematica.message.edit.commands_sent", sent));
            cancel();
        }
    }
}

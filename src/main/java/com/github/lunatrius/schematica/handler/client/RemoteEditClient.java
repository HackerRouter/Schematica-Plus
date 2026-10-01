// SPDX-License-Identifier: LGPL-3.0-only
// Client side of the remote edit protocol, by HackerRouter, 2026.
package com.github.lunatrius.schematica.handler.client;

import java.util.Arrays;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import com.github.lunatrius.schematica.handler.RemoteEdits;
import com.github.lunatrius.schematica.network.PacketHandler;
import com.github.lunatrius.schematica.network.message.MessageEditCancel;
import com.github.lunatrius.schematica.network.message.MessageEditStatus;
import com.github.lunatrius.schematica.network.message.MessageEditUpload;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.task.TaskRegistry;
import com.github.lunatrius.schematica.tool.WorldEditJob;
import com.github.lunatrius.schematica.util.MessageException;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Uploads one edit at a time to a Plus server and mirrors its progress in the Task Manager. */
public final class RemoteEditClient {
    public static final RemoteEditClient INSTANCE = new RemoteEditClient();
    private static final int CHUNKS_PER_TICK = 8;

    private long id;
    private byte[] data;
    private int sent, count;
    private TaskRegistry.Task task;
    private World world;
    private Runnable onSuccess;
    private boolean accepted, cancelSent;

    private RemoteEditClient() {}

    public synchronized boolean busy() { return task != null; }

    /** Compresses the edit and starts uploading it; onSuccess runs on the client thread after the server finishes. */
    public synchronized void submit(WorldEditJob job, World targetWorld, Runnable onSuccess) {
        if (task != null) throw new MessageException("schematica.message.edit.busy");
        byte[] bytes;
        try {
            bytes = CompressedStreamTools.compress(job.write());
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Could not encode the edit", e);
        }
        if (bytes.length > RemoteEdits.MAX_UPLOAD_BYTES) throw new MessageException("schematica.message.edit.remote_too_large");
        id = System.nanoTime() ^ ((long) job.player.hashCode() << 32);
        data = bytes;
        sent = 0;
        count = Math.max(1, (bytes.length + MessageEditUpload.CHUNK_SIZE - 1) / MessageEditUpload.CHUNK_SIZE);
        world = targetWorld;
        this.onSuccess = onSuccess;
        accepted = cancelSent = false;
        task = TaskRegistry.INSTANCE.start(job.player, job.dimension, job.taskKind(), TaskRegistry.Backend.SERVER, job.x + ", " + job.y + ", " + job.z);
        task.update(TaskRegistry.Stage.UPLOAD, 0, bytes.length, 0, 0);
    }

    private void end() {
        if (task != null) task.finish();
        task = null;
        data = null;
        world = null;
        onSuccess = null;
    }

    private static void message(String key) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        ChatComponentTranslation text = new ChatComponentTranslation(key);
        text.getChatStyle().setColor(EnumChatFormatting.RED);
        mc.thePlayer.addChatMessage(text);
    }

    @SubscribeEvent
    public synchronized void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || task == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld != world) {
            message("schematica.message.edit.remote_lost");
            end();
            return;
        }
        if (task.progress().cancelling) {
            if (!cancelSent) PacketHandler.INSTANCE.sendToServer(new MessageEditCancel(id));
            cancelSent = true;
            if (!accepted) end();
            return;
        }
        for (int i = 0; i < CHUNKS_PER_TICK && sent < count; i++, sent++) {
            int from = sent * MessageEditUpload.CHUNK_SIZE;
            byte[] part = Arrays.copyOfRange(data, from, Math.min(data.length, from + MessageEditUpload.CHUNK_SIZE));
            PacketHandler.INSTANCE.sendToServer(new MessageEditUpload(id, task.dimension, sent, count, part));
        }
        if (!accepted) task.update(TaskRegistry.Stage.UPLOAD, Math.min(data.length, (long) sent * MessageEditUpload.CHUNK_SIZE), data.length, 0, 0);
    }

    /** Called on the client thread with a server status. */
    public synchronized void status(MessageEditStatus status) {
        if (task == null || status.id != id) return;
        switch (status.state) {
            case ACCEPTED: accepted = true; break;
            case PROGRESS:
                accepted = true;
                TaskRegistry.Stage[] stages = TaskRegistry.Stage.values();
                if (status.stage < 0 || status.stage >= stages.length) break;
                if (stages[status.stage] == TaskRegistry.Stage.WRITE) task.beginWrite();
                task.update(stages[status.stage], status.completed, status.total, status.affected, status.entities);
                break;
            case REJECTED:
                if (!status.reason.isEmpty()) message(status.reason);
                end();
                break;
            case FINISHED: {
                Runnable done = onSuccess;
                end();
                if (done != null) done.run();
                break;
            }
            default:
                Reference.logger.info("Remote edit {} did not finish", status.id);
                end();
        }
    }
}

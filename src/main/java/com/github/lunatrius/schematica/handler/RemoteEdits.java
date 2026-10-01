// SPDX-License-Identifier: LGPL-3.0-only
// Server side of the remote edit protocol, the Plus counterpart of Litematica's Servux paste, by HackerRouter, 2026.
package com.github.lunatrius.schematica.handler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;

import com.github.lunatrius.schematica.network.PacketHandler;
import com.github.lunatrius.schematica.network.message.MessageEditStatus;
import com.github.lunatrius.schematica.network.message.MessageEditUpload;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.task.TaskRegistry;
import com.github.lunatrius.schematica.tool.WorldEditJob;
import com.github.lunatrius.schematica.util.SchematicLimits;

/** Reassembles edits uploaded by Plus clients and runs them in the server's edit queue with full NBT. */
public final class RemoteEdits {
    public static final RemoteEdits INSTANCE = new RemoteEdits();
    public static final int MAX_UPLOAD_BYTES = 32 * 1024 * 1024;
    static final long IDLE_NANOS = 60_000_000_000L;
    private static final long PROGRESS_NANOS = 250_000_000L;

    private static final class Upload {
        final long id;
        final int dimension, count;
        final byte[][] parts;
        int received, bytes;
        long touched = System.nanoTime();

        Upload(long id, int dimension, int count) {
            this.id = id; this.dimension = dimension; this.count = count; this.parts = new byte[count][];
        }
    }

    private final Map<UUID, Upload> uploads = new ConcurrentHashMap<>();
    private final Map<UUID, Long> running = new ConcurrentHashMap<>();
    private final ExecutorService decoder = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Schematica Plus remote edit decoder");
        thread.setDaemon(true);
        return thread;
    });

    private RemoteEdits() {}

    private static void send(EntityPlayerMP player, MessageEditStatus status) {
        PacketHandler.INSTANCE.sendTo(status, player);
    }

    /** Network thread: stores one slice and, once all have arrived, decodes the edit off the server thread. */
    public void receive(EntityPlayerMP player, MessageEditUpload message) {
        if (player == null) return;
        UUID owner = player.getUniqueID();
        if (!ConfigurationHandler.remoteEditsEnabled) {
            if (message.index == 0) send(player, MessageEditStatus.rejected(message.id, "schematica.message.edit.remote_disabled"));
            return;
        }
        Upload upload = uploads.get(owner);
        if (message.index == 0) {
            if (message.count < 1 || (long) message.count * MessageEditUpload.CHUNK_SIZE > MAX_UPLOAD_BYTES + MessageEditUpload.CHUNK_SIZE) {
                send(player, MessageEditStatus.rejected(message.id, "schematica.message.edit.remote_too_large"));
                return;
            }
            upload = new Upload(message.id, message.dimension, message.count);
            uploads.put(owner, upload);
        }
        if (upload == null || upload.id != message.id || upload.count != message.count || message.index < 0 || message.index >= upload.count
            || upload.parts[message.index] != null || System.nanoTime() - upload.touched > IDLE_NANOS) {
            uploads.remove(owner);
            send(player, MessageEditStatus.rejected(message.id, "schematica.message.edit.remote_invalid"));
            return;
        }
        upload.parts[message.index] = message.data;
        upload.received++;
        upload.bytes += message.data.length;
        upload.touched = System.nanoTime();
        if (upload.bytes > MAX_UPLOAD_BYTES) {
            uploads.remove(owner);
            send(player, MessageEditStatus.rejected(message.id, "schematica.message.edit.remote_too_large"));
            return;
        }
        if (upload.received < upload.count) return;
        uploads.remove(owner);
        Upload complete = upload;
        decoder.execute(() -> decode(player, complete));
    }

    private void decode(EntityPlayerMP player, Upload upload) {
        WorldEditJob job;
        try {
            ByteArrayOutputStream joined = new ByteArrayOutputStream(upload.bytes);
            for (byte[] part : upload.parts) joined.write(part);
            job = WorldEditJob.read(read(joined.toByteArray()), player.getUniqueID(), upload.dimension);
        } catch (IOException | RuntimeException e) {
            Reference.logger.warn("Rejected an invalid remote edit from {}", player.getCommandSenderName(), e);
            send(player, MessageEditStatus.rejected(upload.id, "schematica.message.edit.remote_invalid"));
            return;
        }
        if (!WorldEditQueue.INSTANCE.enqueue(() -> start(player, upload.id, job))) {
            send(player, MessageEditStatus.rejected(upload.id, "schematica.message.edit.busy"));
        }
    }

    /** Gzip NBT with the schematic NBT size limit, so a small upload cannot expand without bound. */
    static NBTTagCompound read(byte[] compressed) throws IOException {
        try (InputStream input = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            return CompressedStreamTools.func_152456_a(new DataInputStream(input), new NBTSizeTracker(SchematicLimits.MAX_NBT_BYTES * 8));
        }
    }

    /** Server thread: permission checks as for MOVE, then the shared edit queue. */
    private void start(EntityPlayerMP player, long id, WorldEditJob job) {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || !server.getConfigurationManager().playerEntityList.contains(player) || player.dimension != job.dimension) {
            return;
        }
        if (!player.capabilities.isCreativeMode || !player.canCommandSenderUseCommand(2, "setblock")) {
            send(player, MessageEditStatus.rejected(id, "schematica.message.edit.permissions"));
            return;
        }
        long[] lastProgress = {0};
        job.completion = success -> {
            running.remove(player.getUniqueID(), id);
            send(player, new MessageEditStatus(id, success ? MessageEditStatus.State.FINISHED : MessageEditStatus.State.FAILED));
        };
        job.progress = progress -> {
            long now = System.nanoTime();
            if (now - lastProgress[0] < PROGRESS_NANOS) return;
            lastProgress[0] = now;
            send(player, status(id, progress));
        };
        if (!WorldEditQueue.INSTANCE.submit(server, job)) {
            send(player, MessageEditStatus.rejected(id, "schematica.message.edit.busy"));
            return;
        }
        running.put(player.getUniqueID(), id);
        send(player, new MessageEditStatus(id, MessageEditStatus.State.ACCEPTED));
    }

    static MessageEditStatus status(long id, TaskRegistry.Progress progress) {
        MessageEditStatus status = new MessageEditStatus(id, MessageEditStatus.State.PROGRESS);
        status.stage = progress.stage.ordinal();
        status.completed = progress.completed;
        status.total = progress.total;
        status.affected = progress.affected;
        status.entities = progress.entities;
        return status;
    }

    public void cancel(EntityPlayerMP player, long id) {
        if (player == null) return;
        Upload upload = uploads.get(player.getUniqueID());
        if (upload != null && upload.id == id) uploads.remove(player.getUniqueID());
        Long current = running.get(player.getUniqueID());
        if (current != null && current == id) WorldEditQueue.INSTANCE.cancel(player.getUniqueID());
    }

    public void forget(EntityPlayer player) {
        uploads.remove(player.getUniqueID());
        running.remove(player.getUniqueID());
    }
}

// SPDX-License-Identifier: LGPL-3.0-only
// Server side of the remote edit protocol, the Plus counterpart of Litematica's Servux paste, by HackerRouter, 2026.
package com.github.lunatrius.schematica.handler;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
import com.github.lunatrius.schematica.network.transfer.SliceAssembler;
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
        final RemoteEditOperations.Operation<WorldEditJob> operation;
        final int dimension;
        final SliceAssembler slices;
        long touched = System.nanoTime();

        Upload(RemoteEditOperations.Operation<WorldEditJob> operation, int dimension, SliceAssembler slices) {
            this.operation = operation; this.dimension = dimension; this.slices = slices;
        }
    }

    private static final class Received {
        final Upload complete;
        final String error;

        Received(Upload complete, String error) { this.complete = complete; this.error = error; }
    }

    private final Map<UUID, Upload> uploads = new HashMap<>();
    private final RemoteEditOperations<WorldEditJob> operations = new RemoteEditOperations<>(
        job -> WorldEditQueue.INSTANCE.cancel(job.player, job));
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
        if (!ConfigurationHandler.remoteEditsEnabled) {
            if (message.index == 0) send(player, MessageEditStatus.rejected(message.id, "schematica.message.edit.remote_disabled"));
            return;
        }
        if (message.index == 0 && !WorldEditQueue.mayEdit(player)) {
            send(player, MessageEditStatus.rejected(message.id, "schematica.message.edit.permissions"));
            return;
        }
        Received received;
        synchronized (uploads) { received = accept(player.getUniqueID(), player.dimension, message); }
        if (received.error != null) send(player, MessageEditStatus.rejected(message.id, received.error));
        if (received.complete != null) decoder.execute(() -> decode(player, received.complete));
    }

    private Received accept(UUID owner, int dimension, MessageEditUpload message) {
        if (message.dimension != dimension) return new Received(null, "schematica.message.edit.remote_invalid");
        Upload upload = uploads.get(owner);
        if (message.index == 0) {
            SliceAssembler slices = SliceAssembler.create(message.count, MessageEditUpload.CHUNK_SIZE, MAX_UPLOAD_BYTES);
            if (slices == null) return new Received(null, "schematica.message.edit.remote_too_large");
            if (upload != null && System.nanoTime() - upload.touched > IDLE_NANOS) {
                uploads.remove(owner);
                operations.finish(upload.operation);
            }
            RemoteEditOperations.Operation<WorldEditJob> operation = operations.begin(owner, message.id);
            if (operation == null) return new Received(null, "schematica.message.edit.busy");
            upload = new Upload(operation, message.dimension, slices);
            uploads.put(owner, upload);
        }
        if (upload == null || upload.operation.id != message.id) return new Received(null, "schematica.message.edit.remote_invalid");
        if (System.nanoTime() - upload.touched > IDLE_NANOS || !upload.slices.add(message.index, message.count, message.data)) {
            uploads.remove(owner);
            operations.finish(upload.operation);
            return new Received(null, upload.slices.bytes() > MAX_UPLOAD_BYTES ? "schematica.message.edit.remote_too_large" : "schematica.message.edit.remote_invalid");
        }
        upload.touched = System.nanoTime();
        if (!upload.slices.complete()) return new Received(null, null);
        uploads.remove(owner);
        operations.uploaded(upload.operation);
        return new Received(upload, null);
    }

    private void decode(EntityPlayerMP player, Upload upload) {
        if (!operations.active(upload.operation)) return;
        WorldEditJob job;
        try {
            job = WorldEditJob.read(read(upload.slices.join()), player.getUniqueID(), upload.dimension);
        } catch (IOException | RuntimeException e) {
            Reference.logger.warn("Rejected an invalid remote edit from {}", player.getCommandSenderName(), e);
            if (operations.finish(upload.operation)) send(player, MessageEditStatus.rejected(upload.operation.id, "schematica.message.edit.remote_invalid"));
            return;
        }
        if (!operations.active(upload.operation)) return;
        if (!WorldEditQueue.INSTANCE.enqueue(() -> start(player, upload.operation, job))) {
            if (operations.finish(upload.operation)) send(player, MessageEditStatus.rejected(upload.operation.id, "schematica.message.edit.busy"));
        }
    }

    /** Gzip NBT with the schematic NBT size limit, so a small upload cannot expand without bound. */
    static NBTTagCompound read(byte[] compressed) throws IOException {
        try (InputStream input = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            return CompressedStreamTools.func_152456_a(new DataInputStream(input), new NBTSizeTracker(SchematicLimits.MAX_NBT_BYTES * 8));
        }
    }

    /** Server thread: permission checks as for MOVE, then the shared edit queue. */
    private void start(EntityPlayerMP player, RemoteEditOperations.Operation<WorldEditJob> operation, WorldEditJob job) {
        if (!operations.active(operation)) return;
        long id = operation.id;
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || !server.getConfigurationManager().playerEntityList.contains(player) || player.dimension != job.dimension) {
            operations.finish(operation);
            return;
        }
        if (!WorldEditQueue.mayEdit(player)) {
            operations.finish(operation);
            send(player, MessageEditStatus.rejected(id, "schematica.message.edit.permissions"));
            return;
        }
        long[] lastProgress = {0};
        job.completion = outcome -> {
            operations.finish(operation);
            send(player, new MessageEditStatus(id, MessageEditStatus.State.of(outcome)));
        };
        job.progress = progress -> {
            long now = System.nanoTime();
            if (now - lastProgress[0] < PROGRESS_NANOS) return;
            lastProgress[0] = now;
            send(player, status(id, progress));
        };
        RemoteEditOperations.Start started;
        try {
            started = operations.start(operation, job, edit -> WorldEditQueue.INSTANCE.submit(server, edit));
        } catch (RuntimeException e) {
            Reference.logger.warn("Could not start the remote edit from {}", player.getCommandSenderName(), e);
            send(player, MessageEditStatus.rejected(id, "schematica.message.edit.remote_invalid"));
            return;
        }
        if (started == RemoteEditOperations.Start.BUSY) {
            send(player, MessageEditStatus.rejected(id, "schematica.message.edit.busy"));
            return;
        }
        if (started == RemoteEditOperations.Start.STARTED) send(player, new MessageEditStatus(id, MessageEditStatus.State.ACCEPTED));
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
        synchronized (uploads) {
            Upload upload = uploads.get(player.getUniqueID());
            if (upload != null && upload.operation.id == id) uploads.remove(player.getUniqueID());
            operations.cancel(player.getUniqueID(), id);
        }
    }

    public void forget(EntityPlayer player) {
        forget(player.getUniqueID());
    }

    void forget(UUID owner) {
        synchronized (uploads) {
            uploads.remove(owner);
            operations.forget(owner);
        }
    }

    public void clear() {
        synchronized (uploads) {
            uploads.clear();
            operations.clear();
        }
    }
}

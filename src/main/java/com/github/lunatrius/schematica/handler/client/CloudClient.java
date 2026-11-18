package com.github.lunatrius.schematica.handler.client;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.nbt.NBTTagCompound;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.client.cloud.CloudCache;
import com.github.lunatrius.schematica.client.util.InfoUtils;
import com.github.lunatrius.schematica.client.world.Placements;
import com.github.lunatrius.schematica.client.world.SchematicLibrary;
import com.github.lunatrius.schematica.client.world.SchematicSourceData;
import com.github.lunatrius.schematica.cloud.CloudCatalog;
import com.github.lunatrius.schematica.cloud.CloudFile;
import com.github.lunatrius.schematica.cloud.CloudPath;
import com.github.lunatrius.schematica.cloud.CloudStorage;
import com.github.lunatrius.schematica.network.PacketHandler;
import com.github.lunatrius.schematica.network.message.MessageCloudData;
import com.github.lunatrius.schematica.network.message.MessageCloudRequest;
import com.github.lunatrius.schematica.network.message.MessageCloudResult;
import com.github.lunatrius.schematica.network.message.MessageCloudUpload;
import com.github.lunatrius.schematica.network.transfer.NbtPayload;
import com.github.lunatrius.schematica.network.transfer.SliceAssembler;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.util.MessageType;
import com.github.lunatrius.schematica.util.SchematicLimits;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public final class CloudClient {
    public static final CloudClient INSTANCE = new CloudClient();
    static final int TIMEOUT_TICKS = 1200;
    static final int SLICES_PER_TICK = 8;

    private final Map<Integer, Pending> pending = new HashMap<>();
    private final Deque<MessageCloudUpload> uploading = new ArrayDeque<>();
    private final Deque<Job> jobs = new ArrayDeque<>();
    private int nextId;
    private CloudCatalog catalog;
    private int revision;
    private boolean requested;

    private CloudClient() {}

    private static final class Pending {
        final Consumer<byte[]> data;
        final Runnable success;
        Runnable failed;
        SliceAssembler slices;
        int ticks;

        Pending(Consumer<byte[]> data, Runnable success) { this.data = data; this.success = success; }
    }

    public boolean available() { return SchematicaPlus.proxy.supportsCloud; }

    public CloudCatalog catalog() { return catalog; }

    public int revision() { return revision; }

    public boolean loading() { return requested; }

    public boolean transferring() { return !pending.isEmpty() || !uploading.isEmpty() || !jobs.isEmpty(); }

    private int open(Consumer<byte[]> data, Runnable success) {
        int id = nextId++;
        pending.put(id, new Pending(data, success));
        return id;
    }

    private void send(MessageCloudRequest.Operation operation, CloudPath path, CloudPath target, Consumer<byte[]> data, Runnable success) {
        int id = open(data, success);
        PacketHandler.INSTANCE.sendToServer(new MessageCloudRequest(id, operation, path.toString(), target == null ? "" : target.toString()));
    }

    public void refresh() {
        if (!available() || requested) return;
        requested = true;
        send(MessageCloudRequest.Operation.CATALOG, CloudPath.root(CloudPath.PRIVATE), null, bytes -> {
            requested = false;
            if (bytes == null) return;
            try {
                NBTTagCompound tag = NbtPayload.read(bytes, SchematicLimits.MAX_NBT_BYTES);
                catalog = CloudCatalog.read(tag);
                revision++;
            } catch (IOException | RuntimeException e) {
                Reference.logger.warn("Could not read the list of server schematics", e);
            }
        }, null);
    }

    public void fetch(CloudFile file, Consumer<File> done) {
        File target = CloudCache.file(file.path);
        if (CloudCache.verified(file)) {
            done.accept(target);
            return;
        }
        send(MessageCloudRequest.Operation.DOWNLOAD, file.path, null, bytes -> {
            if (bytes == null) {
                done.accept(null);
                return;
            }
            File saved = null;
            try {
                if (!CloudStorage.hash(bytes).equals(file.hash)) throw new IOException("The downloaded file does not match");
                write(target, bytes);
                saved = target;
                refreshLoaded(target);
                revision++;
            } catch (IOException | RuntimeException e) {
                Reference.logger.warn("Could not store the downloaded schematic {}", file.path, e);
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "schematica.message.cloud.download_failed", file.path.name());
            }
            done.accept(saved);
        }, null);
    }

    private static void refreshLoaded(File target) {
        try {
            SchematicLibrary.Source<SchematicSourceData> source = Placements.LIBRARY.find(target);
            if (source == null || source.memory()) return;
            if (!source.data().unsaved()) {
                Placements.reloadSource(source);
                return;
            }
            String name = target.getName(), extension = name.substring(name.lastIndexOf('.'));
            File label = new File(target.getParentFile(), name.substring(0, name.length() - extension.length()) + " (edited)" + extension);
            Placements.detachSource(source, label);
            InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "schematica.message.cloud.kept_edited", name, label.getName());
        } catch (IOException | RuntimeException e) {
            Reference.logger.warn("Could not reload the downloaded schematic {}", target, e);
        }
    }

    static void write(File target, byte[] bytes) throws IOException {
        File directory = target.getParentFile();
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Could not create " + directory);
        File temporary = File.createTempFile(".schematica-download-", ".tmp", directory);
        try {
            Files.write(temporary.toPath(), bytes);
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary.toPath());
        }
    }

    public void upload(File local, CloudPath target) {
        upload(local, target, null);
    }

    /** Queues a file upload; failed runs on the client thread when reading, sending or storing it fails. */
    public void upload(File local, CloudPath target, Runnable failed) {
        jobs.add(new Job(local, null, target, failed));
    }

    /** Queues an upload of encoded contents that exist only in memory. */
    public void upload(byte[] contents, CloudPath target, Runnable failed) {
        jobs.add(new Job(null, contents.clone(), target, failed));
    }

    private static final class Job {
        final File local;
        final byte[] contents;
        final CloudPath target;
        final Runnable failed;

        Job(File local, byte[] contents, CloudPath target, Runnable failed) {
            this.local = local; this.contents = contents; this.target = target; this.failed = failed;
        }
    }

    private void start(Job job) {
        byte[] bytes;
        try {
            bytes = job.contents != null ? job.contents : Files.readAllBytes(job.local.toPath());
        } catch (IOException e) {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "schematica.message.cloud.upload_failed", job.local.getName());
            if (job.failed != null) job.failed.run();
            return;
        }
        int id = open(null, () -> {
            try {
                write(CloudCache.file(job.target), bytes);
            } catch (IOException e) {
                Reference.logger.warn("Could not keep a copy of the uploaded schematic", e);
            }
        });
        pending.get(id).failed = job.failed;
        int size = MessageCloudUpload.CHUNK_SIZE;
        int count = Math.max(1, (bytes.length + size - 1) / size);
        for (int index = 0; index < count; index++) {
            uploading.add(new MessageCloudUpload(id, index, count, job.target.toString(),
                Arrays.copyOfRange(bytes, index * size, Math.min(bytes.length, (index + 1) * size))));
        }
    }

    public void createDirectory(CloudPath path) { send(MessageCloudRequest.Operation.CREATE_DIRECTORY, path, null, null, null); }

    public void delete(CloudPath path) { send(MessageCloudRequest.Operation.DELETE, path, null, null, null); }

    public void publish(CloudPath file, CloudPath directory) { send(MessageCloudRequest.Operation.PUBLISH, file, directory, null, null); }

    public void received(MessageCloudData message) {
        Pending request = pending.get(message.id);
        if (request == null || request.data == null) return;
        if (message.count == 0) {
            finish(message.id, null);
            return;
        }
        if (request.slices == null) request.slices = SliceAssembler.create(message.count, MessageCloudData.CHUNK_SIZE, (int) SchematicLimits.MAX_NBT_BYTES);
        if (request.slices == null || !request.slices.add(message.index, message.count, message.data)) {
            finish(message.id, null);
            return;
        }
        request.ticks = 0;
        if (request.slices.complete()) finish(message.id, request.slices.join());
    }

    private void finish(int id, byte[] data) {
        Pending request = pending.remove(id);
        if (request != null && request.data != null) request.data.accept(data);
    }

    public void result(MessageCloudResult message) {
        if (message.id == MessageCloudResult.CHANGED) {
            if (catalog != null) refresh();
            return;
        }
        if (!message.key.isEmpty()) InfoUtils.showGuiOrInGameMessage(message.type, message.key, (Object[]) message.arguments);
        Pending request = pending.get(message.id);
        if (request == null || request.data != null) return;
        pending.remove(message.id);
        uploading.removeIf(slice -> slice.id == message.id);
        if (message.type == MessageType.SUCCESS) {
            if (request.success != null) request.success.run();
        } else if (request.failed != null) request.failed.run();
    }

    public void clear() {
        for (Iterator<Pending> iterator = pending.values().iterator(); iterator.hasNext();) {
            Pending request = iterator.next();
            iterator.remove();
            if (request.data != null) request.data.accept(null);
            else if (request.failed != null) request.failed.run();
        }
        uploading.clear();
        for (Job job : jobs) if (job.failed != null) job.failed.run();
        jobs.clear();
        catalog = null;
        requested = false;
        revision++;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (uploading.isEmpty() && !jobs.isEmpty()) start(jobs.poll());
        for (int i = 0; i < SLICES_PER_TICK && !uploading.isEmpty(); i++) PacketHandler.INSTANCE.sendToServer(uploading.poll());
        for (Iterator<Map.Entry<Integer, Pending>> iterator = pending.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<Integer, Pending> entry = iterator.next();
            boolean waiting = uploading.stream().anyMatch(slice -> slice.id == entry.getKey());
            if (waiting || ++entry.getValue().ticks < TIMEOUT_TICKS) continue;
            iterator.remove();
            if (entry.getValue().data != null) entry.getValue().data.accept(null);
            else {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "schematica.message.cloud.timed_out");
                if (entry.getValue().failed != null) entry.getValue().failed.run();
            }
        }
    }
}

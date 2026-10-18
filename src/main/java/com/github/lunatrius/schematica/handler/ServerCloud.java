package com.github.lunatrius.schematica.handler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.server.MinecraftServer;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.cloud.CloudPath;
import com.github.lunatrius.schematica.cloud.CloudStorage;
import com.github.lunatrius.schematica.network.PacketHandler;
import com.github.lunatrius.schematica.network.message.MessageCloudData;
import com.github.lunatrius.schematica.network.message.MessageCloudRequest;
import com.github.lunatrius.schematica.network.message.MessageCloudResult;
import com.github.lunatrius.schematica.network.message.MessageCloudUpload;
import com.github.lunatrius.schematica.network.transfer.SliceAssembler;
import com.github.lunatrius.schematica.reference.Names;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.util.FileUtils;
import com.github.lunatrius.schematica.util.MessageException;
import com.github.lunatrius.schematica.util.MessageType;
import com.github.lunatrius.schematica.util.SchematicLimits;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public final class ServerCloud {
    public static final ServerCloud INSTANCE = new ServerCloud();
    static final int SLICES_PER_TICK = 8;
    static final long IDLE_NANOS = 60_000_000_000L;
    static final int MAX_PENDING = 8, MAX_QUEUED_SLICES = 2048;

    private final Queue<Runnable> requests = new ConcurrentLinkedQueue<>();
    private final Map<UUID, AtomicInteger> pending = new ConcurrentHashMap<>();
    private final Map<UUID, Upload> uploads = new HashMap<>();
    private final Map<EntityPlayerMP, Deque<MessageCloudData>> outgoing = new LinkedHashMap<>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Schematica Plus schematic storage");
        thread.setDaemon(true);
        return thread;
    });
    private CloudStorage storage;

    private ServerCloud() {}

    private static final class Upload {
        final int id;
        final CloudPath path;
        final SliceAssembler slices;
        long touched = System.nanoTime();

        Upload(int id, CloudPath path, SliceAssembler slices) { this.id = id; this.path = path; this.slices = slices; }
    }

    synchronized CloudStorage storage() {
        File root = SchematicaPlus.proxy.cloudDirectory();
        if (storage == null || !storage.root().equals(root)) storage = new CloudStorage(root);
        return storage;
    }

    private static boolean level(EntityPlayerMP player, int level) {
        return level <= 0 || player.canCommandSenderUseCommand(level, Names.Command.NAME);
    }

    static boolean allowed(EntityPlayerMP player) {
        MinecraftServer server = MinecraftServer.getServer();
        return ConfigurationHandler.cloudEnabled && server != null && server.getConfigurationManager().playerEntityList.contains(player);
    }

    static boolean mayUpload(EntityPlayerMP player) { return level(player, ConfigurationHandler.cloudUploadPermissionLevel); }

    static boolean mayPublish(EntityPlayerMP player) { return mayUpload(player) && level(player, ConfigurationHandler.cloudPublicPermissionLevel); }

    static boolean mayManage(EntityPlayerMP player) { return level(player, ConfigurationHandler.cloudManagePermissionLevel); }

    static long quota() {
        return SchematicaPlus.proxy.dedicatedServer() ? ConfigurationHandler.playerQuotaKilobytes * 1024L : -1;
    }

    static long maxUpload() {
        return Math.min(SchematicLimits.MAX_NBT_BYTES, ConfigurationHandler.cloudMaxUploadKilobytes * 1024L);
    }

    private static void result(EntityPlayerMP player, int id, MessageType type, String key, Object... arguments) {
        String[] text = new String[Math.min(8, arguments.length)];
        for (int i = 0; i < text.length; i++) text[i] = String.valueOf(arguments[i]);
        PacketHandler.INSTANCE.sendTo(new MessageCloudResult(id, type, key, text), player);
    }

    private static void failed(EntityPlayerMP player, int id, Exception error, String fallback, Object... arguments) {
        if (error instanceof MessageException) {
            result(player, id, MessageType.ERROR, ((MessageException) error).key(), ((MessageException) error).arguments());
        } else {
            Reference.logger.warn("Schematic storage request of {} failed", player.getCommandSenderName(), error);
            result(player, id, MessageType.ERROR, fallback, arguments);
        }
    }

    private void changed(EntityPlayerMP player, CloudPath path) {
        MinecraftServer server = MinecraftServer.getServer();
        if (CloudPath.PUBLIC.equals(path.scope) && server != null) {
            for (Object other : server.getConfigurationManager().playerEntityList) {
                PacketHandler.INSTANCE.sendTo(MessageCloudResult.changed(), (EntityPlayerMP) other);
            }
        } else PacketHandler.INSTANCE.sendTo(MessageCloudResult.changed(), player);
    }

    public void request(EntityPlayerMP player, MessageCloudRequest message) {
        AtomicInteger count = pending.computeIfAbsent(player.getUniqueID(), id -> new AtomicInteger());
        if (count.incrementAndGet() > MAX_PENDING) {
            count.decrementAndGet();
            refuse(player, message.id, "schematica.message.cloud.busy");
            return;
        }
        requests.add(() -> handle(player, message, count::decrementAndGet));
    }

    private void work(Runnable done, Runnable task) {
        worker.execute(() -> {
            try {
                task.run();
            } finally {
                done.run();
            }
        });
    }

    private void handle(EntityPlayerMP player, MessageCloudRequest message, Runnable done) {
        if (!allowed(player)) {
            done.run();
            refuse(player, message.id, "schematica.message.cloud.disabled");
            return;
        }
        CloudPath path, target;
        try {
            path = CloudPath.parse(message.path);
            target = message.target.isEmpty() ? null : CloudPath.parse(message.target);
        } catch (IllegalArgumentException e) {
            done.run();
            refuse(player, message.id, "schematica.message.cloud.invalid_name", message.path);
            return;
        }
        UUID id = player.getUniqueID();
        String name = player.getCommandSenderName();
        boolean upload = mayUpload(player), publish = mayPublish(player), manage = mayManage(player);
        switch (message.operation) {
            case CATALOG:
                work(done, () -> {
                    try {
                        send(player, message.id, CompressedStreamTools.compress(storage().catalog(id, quota(), upload, publish, manage).write()));
                    } catch (IOException | RuntimeException e) {
                        Reference.logger.warn("Could not list the stored schematics", e);
                        refuse(player, message.id, "schematica.message.cloud.unavailable");
                    }
                });
                return;
            case DOWNLOAD:
                work(done, () -> {
                    try {
                        send(player, message.id, storage().read(path, id, SchematicLimits.MAX_NBT_BYTES));
                    } catch (IOException | RuntimeException e) {
                        refuse(player, message.id, e instanceof MessageException ? ((MessageException) e).key() : "schematica.message.cloud.download_failed",
                            e instanceof MessageException ? ((MessageException) e).arguments() : new Object[] {path.name()});
                    }
                });
                return;
            case CREATE_DIRECTORY:
                if (CloudPath.PUBLIC.equals(path.scope) && !publish) break;
                work(done, () -> {
                    try {
                        storage().createDirectory(path, id, name);
                        result(player, message.id, MessageType.SUCCESS, "schematica.message.cloud.created_directory", path.name());
                        changed(player, path);
                    } catch (IOException | RuntimeException e) {
                        failed(player, message.id, e, "schematica.message.cloud.request_failed");
                    }
                });
                return;
            case DELETE:
                work(done, () -> {
                    try {
                        storage().delete(path, id, manage);
                        result(player, message.id, MessageType.SUCCESS, "schematica.message.cloud.deleted", path.name());
                        changed(player, path);
                    } catch (IOException | RuntimeException e) {
                        failed(player, message.id, e, "schematica.message.cloud.request_failed");
                    }
                });
                return;
            case PUBLISH:
                if (!publish || target == null) break;
                work(done, () -> {
                    try {
                        CloudPath published = storage().publish(path, target, id, name);
                        result(player, message.id, MessageType.SUCCESS, "schematica.message.cloud.published", published.name());
                        changed(player, published);
                    } catch (IOException | RuntimeException e) {
                        failed(player, message.id, e, "schematica.message.cloud.request_failed");
                    }
                });
                return;
            default:
                break;
        }
        done.run();
        refuse(player, message.id, "schematica.message.cloud.no_permission");
    }

    private void refuse(EntityPlayerMP player, int id, String key, Object... arguments) {
        result(player, id, MessageType.ERROR, key, arguments);
        PacketHandler.INSTANCE.sendTo(new MessageCloudData(id, 0, 0, new byte[0]), player);
    }

    private void send(EntityPlayerMP player, int id, byte[] payload) {
        int size = MessageCloudData.CHUNK_SIZE;
        int count = Math.max(1, (payload.length + size - 1) / size);
        synchronized (outgoing) {
            Deque<MessageCloudData> queue = outgoing.computeIfAbsent(player, key -> new ArrayDeque<>());
            if (queue.size() + count > MAX_QUEUED_SLICES) {
                refuse(player, id, "schematica.message.cloud.busy");
                return;
            }
            for (int index = 0; index < count; index++) {
                queue.add(new MessageCloudData(id, index, count, Arrays.copyOfRange(payload, index * size, Math.min(payload.length, (index + 1) * size))));
            }
        }
    }

    public void upload(EntityPlayerMP player, MessageCloudUpload message) {
        Upload complete;
        try {
            synchronized (uploads) { complete = accept(player, message); }
        } catch (MessageException e) {
            result(player, message.id, MessageType.ERROR, e.key(), e.arguments());
            return;
        }
        if (complete != null) store(player, complete);
    }

    private Upload accept(EntityPlayerMP player, MessageCloudUpload message) {
        UUID owner = player.getUniqueID();
        Upload upload = uploads.get(owner);
        if (message.index == 0) {
            if (upload != null && upload.id != message.id && System.nanoTime() - upload.touched < IDLE_NANOS) {
                throw new MessageException("schematica.message.cloud.busy");
            }
            CloudPath path;
            try {
                path = CloudPath.parse(message.path);
            } catch (IllegalArgumentException e) {
                throw new MessageException("schematica.message.cloud.invalid_name", message.path);
            }
            if (!ConfigurationHandler.cloudEnabled || !mayUpload(player) || CloudPath.PUBLIC.equals(path.scope) && !mayPublish(player)) {
                throw new MessageException("schematica.message.cloud.no_permission");
            }
            SliceAssembler slices = SliceAssembler.create(message.count, MessageCloudUpload.CHUNK_SIZE, (int) maxUpload());
            if (slices == null) throw new MessageException("schematica.message.cloud.too_large", path.name(), FileUtils.humanReadableByteCount(maxUpload()));
            upload = new Upload(message.id, path, slices);
            uploads.put(owner, upload);
        }
        if (upload == null || upload.id != message.id) return null;
        if (!upload.slices.add(message.index, message.count, message.data)) {
            uploads.remove(owner);
            throw upload.slices.bytes() > maxUpload()
                ? new MessageException("schematica.message.cloud.too_large", upload.path.name(), FileUtils.humanReadableByteCount(maxUpload()))
                : new MessageException("schematica.message.cloud.upload_failed", upload.path.name());
        }
        upload.touched = System.nanoTime();
        if (!upload.slices.complete()) return null;
        uploads.remove(owner);
        return upload;
    }

    private void store(EntityPlayerMP player, Upload upload) {
        UUID id = player.getUniqueID();
        String name = player.getCommandSenderName();
        worker.execute(() -> {
            try {
                storage().write(upload.path, id, name, upload.slices.join(), quota());
                result(player, upload.id, MessageType.SUCCESS, "schematica.message.cloud.uploaded", upload.path.name());
                changed(player, upload.path);
            } catch (IOException | RuntimeException e) {
                failed(player, upload.id, e, "schematica.message.cloud.upload_failed", upload.path.name());
            }
        });
    }

    public void forget(EntityPlayer player) {
        pending.remove(player.getUniqueID());
        synchronized (uploads) { uploads.remove(player.getUniqueID()); }
        synchronized (outgoing) { outgoing.remove(player); }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (int i = 0; i < 16; i++) {
            Runnable request = requests.poll();
            if (request == null) break;
            request.run();
        }
        synchronized (outgoing) {
            for (Iterator<Map.Entry<EntityPlayerMP, Deque<MessageCloudData>>> entries = outgoing.entrySet().iterator(); entries.hasNext();) {
                Map.Entry<EntityPlayerMP, Deque<MessageCloudData>> entry = entries.next();
                for (int i = 0; i < SLICES_PER_TICK && !entry.getValue().isEmpty(); i++) PacketHandler.INSTANCE.sendTo(entry.getValue().poll(), entry.getKey());
                if (entry.getValue().isEmpty()) entries.remove();
            }
        }
        synchronized (uploads) {
            uploads.values().removeIf(upload -> System.nanoTime() - upload.touched > IDLE_NANOS);
        }
    }
}

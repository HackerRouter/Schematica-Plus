package com.github.lunatrius.schematica.handler;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.DimensionManager;

import com.github.lunatrius.schematica.cloud.SharedPlacement;
import com.github.lunatrius.schematica.network.PacketHandler;
import com.github.lunatrius.schematica.network.message.MessageCloudResult;
import com.github.lunatrius.schematica.network.message.MessageSharedRequest;
import com.github.lunatrius.schematica.network.message.MessageSharedUpdate;
import com.github.lunatrius.schematica.reference.Names;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.util.FileUtils;
import com.github.lunatrius.schematica.util.MessageType;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public final class SharedPlacements {
    public static final SharedPlacements INSTANCE = new SharedPlacements();
    static final int MAX_PLACEMENTS = 1024;

    private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();
    private final Map<UUID, SharedPlacement> placements = new LinkedHashMap<>();
    private final Map<UUID, Awaiting> awaiting = new HashMap<>();
    private final Map<UUID, UUID> modifiers = new HashMap<>();
    private File file;

    private SharedPlacements() {}

    private static final class Awaiting {
        final UUID player;
        final SharedPlacement placement;

        Awaiting(UUID player, SharedPlacement placement) { this.player = player; this.placement = placement; }
    }

    static File storage() {
        File save = DimensionManager.getCurrentSaveRootDirectory();
        return save == null ? null : new File(save, "schematica_plus/shared_placements.json");
    }

    private synchronized void load() {
        File current = storage();
        if (current == null || current.equals(file)) return;
        file = current;
        placements.clear();
        awaiting.clear();
        modifiers.clear();
        if (!file.isFile()) return;
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            for (JsonElement element : new JsonParser().parse(reader).getAsJsonObject().getAsJsonArray("placements")) {
                try {
                    SharedPlacement placement = SharedPlacement.fromJson(element.getAsJsonObject());
                    placements.put(placement.id, placement);
                } catch (RuntimeException e) {
                    Reference.logger.warn("Skipping an unreadable shared placement", e);
                }
            }
        } catch (IOException | RuntimeException e) {
            Reference.logger.error("Could not read the shared placements; they are kept until the file is fixed", e);
            file = null;
        }
    }

    private synchronized void save() {
        if (file == null) return;
        JsonArray list = new JsonArray();
        for (SharedPlacement placement : placements.values()) list.add(placement.toJson());
        JsonObject root = new JsonObject();
        root.add("placements", list);
        try {
            File directory = file.getParentFile();
            if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Could not create " + directory);
            FileUtils.writeUtf8Atomically(file, new GsonBuilder().setPrettyPrinting().create().toJson(root));
        } catch (IOException e) {
            Reference.logger.error("Could not save the shared placements", e);
        }
    }

    public synchronized boolean remove(UUID id) {
        load();
        if (placements.remove(id) == null) return false;
        modifiers.remove(id);
        save();
        broadcast(new MessageSharedUpdate(MessageSharedUpdate.Operation.REMOVE, id, null));
        return true;
    }

    public synchronized List<SharedPlacement> placements() {
        load();
        return new ArrayList<>(placements.values());
    }

    public synchronized boolean references(String fileName) {
        load();
        for (SharedPlacement placement : placements.values()) if (placement.file().name().equals(fileName)) return true;
        for (Awaiting entry : awaiting.values()) if (entry.placement.file().name().equals(fileName)) return true;
        return false;
    }

    public synchronized boolean awaiting(EntityPlayer player, String fileName) {
        for (Awaiting entry : awaiting.values()) {
            if (entry.player.equals(player.getUniqueID()) && entry.placement.file().name().equals(fileName)) return true;
        }
        return false;
    }

    private static boolean level(EntityPlayerMP player, int level) {
        return level <= 0 || player.canCommandSenderUseCommand(level, Names.Command.NAME);
    }

    static boolean mayShare(EntityPlayerMP player) {
        MinecraftServer server = MinecraftServer.getServer();
        return ConfigurationHandler.sharedPlacementsEnabled && server != null && server.getConfigurationManager().playerEntityList.contains(player)
            && level(player, ConfigurationHandler.sharedPlacementPermissionLevel);
    }

    private static void message(EntityPlayerMP player, MessageType type, String key, Object... arguments) {
        ServerCloud.result(player, MessageCloudResult.NOTICE, type, key, arguments);
    }

    private static void broadcast(MessageSharedUpdate update) {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) return;
        for (Object player : server.getConfigurationManager().playerEntityList) PacketHandler.INSTANCE.sendTo(update, (EntityPlayerMP) player);
    }

    public void request(EntityPlayerMP player, MessageSharedRequest message) {
        tasks.add(() -> handle(player, message));
    }

    public void joined(EntityPlayerMP player) {
        tasks.add(() -> {
            if (!ConfigurationHandler.sharedPlacementsEnabled) return;
            PacketHandler.INSTANCE.sendTo(new MessageSharedUpdate(MessageSharedUpdate.Operation.CLEAR, new UUID(0, 0), null), player);
            for (SharedPlacement placement : placements()) {
                PacketHandler.INSTANCE.sendTo(new MessageSharedUpdate(MessageSharedUpdate.Operation.ADD, placement.id, placement.write()), player);
            }
        });
    }

    private synchronized void handle(EntityPlayerMP player, MessageSharedRequest message) {
        load();
        if (!mayShare(player) && message.operation != MessageSharedRequest.Operation.REMOVE) {
            message(player, MessageType.ERROR, "schematica.message.cloud.no_permission");
            deny(player, message);
            return;
        }
        UUID id = player.getUniqueID();
        String name = player.getCommandSenderName();
        SharedPlacement current = placements.get(message.id);
        switch (message.operation) {
            case SHARE:
                share(player, message);
                return;
            case REMOVE:
                if (current == null) return;
                if (!current.ownerId.equals(id.toString()) && !level(player, ConfigurationHandler.sharedPlacementManagePermissionLevel)) {
                    message(player, MessageType.ERROR, "schematica.message.cloud.no_permission");
                    return;
                }
                remove(current.id);
                return;
            case MODIFY:
                UUID modifier = modifiers.get(message.id);
                if (current == null || modifier != null && !modifier.equals(id) && online(modifier)) {
                    deny(player, message);
                    return;
                }
                modifiers.put(current.id, id);
                PacketHandler.INSTANCE.sendTo(new MessageSharedUpdate(MessageSharedUpdate.Operation.ACCEPT, current.id, null), player);
                return;
            case FINISH:
                if (current == null || !id.equals(modifiers.get(current.id))) {
                    deny(player, message);
                    return;
                }
                modifiers.remove(current.id);
                try {
                    SharedPlacement change = SharedPlacement.read(message.placement);
                    if (!change.id.equals(current.id)) throw new IllegalArgumentException("Placement mismatch");
                    SharedPlacement next = current.modified(change, id, name);
                    placements.put(next.id, next);
                    save();
                    broadcast(new MessageSharedUpdate(MessageSharedUpdate.Operation.ADD, next.id, next.write()));
                } catch (IllegalArgumentException e) {
                    Reference.logger.warn("Rejected a modification of a shared placement from {}", name, e);
                    deny(player, message);
                }
                return;
            case CANCEL:
                if (current != null && id.equals(modifiers.get(current.id))) modifiers.remove(current.id);
                return;
            default:
        }
    }

    private static boolean online(UUID player) {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) return false;
        for (Object other : server.getConfigurationManager().playerEntityList) if (((EntityPlayerMP) other).getUniqueID().equals(player)) return true;
        return false;
    }

    private void deny(EntityPlayerMP player, MessageSharedRequest message) {
        if (message.operation == MessageSharedRequest.Operation.MODIFY || message.operation == MessageSharedRequest.Operation.FINISH) {
            PacketHandler.INSTANCE.sendTo(new MessageSharedUpdate(MessageSharedUpdate.Operation.DENY, message.id, null), player);
        }
    }

    private void share(EntityPlayerMP player, MessageSharedRequest message) {
        SharedPlacement placement;
        try {
            placement = SharedPlacement.read(message.placement).owned(player.getUniqueID(), player.getCommandSenderName());
        } catch (IllegalArgumentException e) {
            message(player, MessageType.ERROR, "syncmatica.error.invalid_file");
            return;
        }
        int owned = 0;
        for (SharedPlacement other : placements.values()) if (other.ownerId.equals(placement.ownerId)) owned++;
        int limit = ConfigurationHandler.sharedPlacementsPerPlayer;
        if (placements.containsKey(placement.id) || awaiting.containsKey(placement.id) || placements.size() >= MAX_PLACEMENTS || limit > 0 && owned >= limit) {
            message(player, MessageType.ERROR, "schematica.message.shared.limit", limit);
            return;
        }
        if (placement.size > ServerCloud.maxUpload()) {
            message(player, MessageType.ERROR, "schematica.message.cloud.too_large", placement.fileName, FileUtils.humanReadableByteCount(ServerCloud.maxUpload()));
            return;
        }
        if (ServerCloud.INSTANCE.storage().file(placement.file(), null).isFile()) {
            add(placement);
            return;
        }
        awaiting.put(placement.id, new Awaiting(player.getUniqueID(), placement));
        PacketHandler.INSTANCE.sendTo(new MessageSharedUpdate(MessageSharedUpdate.Operation.UPLOAD, placement.id, placement.write()), player);
    }

    private void add(SharedPlacement placement) {
        placements.put(placement.id, placement);
        save();
        broadcast(new MessageSharedUpdate(MessageSharedUpdate.Operation.ADD, placement.id, placement.write()));
    }

    public void uploaded(EntityPlayer player, String fileName) {
        UUID owner = player.getUniqueID();
        tasks.add(() -> {
            synchronized (this) {
                for (Iterator<Awaiting> entries = awaiting.values().iterator(); entries.hasNext();) {
                    Awaiting entry = entries.next();
                    if (!entry.player.equals(owner) || !entry.placement.file().name().equals(fileName)) continue;
                    entries.remove();
                    add(entry.placement);
                }
            }
        });
    }

    public synchronized void forget(EntityPlayer player) {
        awaiting.values().removeIf(entry -> entry.player.equals(player.getUniqueID()));
        modifiers.values().removeIf(player.getUniqueID()::equals);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (int i = 0; i < 64; i++) {
            Runnable task = tasks.poll();
            if (task == null) return;
            task.run();
        }
    }

    public synchronized void clear() {
        file = null;
        placements.clear();
        awaiting.clear();
        modifiers.clear();
        tasks.clear();
    }
}

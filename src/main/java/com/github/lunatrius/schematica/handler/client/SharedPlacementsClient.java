package com.github.lunatrius.schematica.handler.client;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.client.Minecraft;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.client.cloud.CloudCache;
import com.github.lunatrius.schematica.client.util.InfoUtils;
import com.github.lunatrius.schematica.client.world.Placements;
import com.github.lunatrius.schematica.client.world.SchematicLibrary;
import com.github.lunatrius.schematica.client.world.SchematicSourceData;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.client.world.SharedWorlds;
import com.github.lunatrius.schematica.cloud.CloudFile;
import com.github.lunatrius.schematica.cloud.CloudPath;
import com.github.lunatrius.schematica.cloud.SharedPlacement;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.network.PacketHandler;
import com.github.lunatrius.schematica.network.message.MessageSharedRequest;
import com.github.lunatrius.schematica.network.message.MessageSharedUpdate;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.util.MessageType;

public final class SharedPlacementsClient {
    public static final SharedPlacementsClient INSTANCE = new SharedPlacementsClient();

    private final Map<UUID, SharedPlacement> placements = new LinkedHashMap<>();
    private final Map<UUID, SchematicWorld> sharing = new HashMap<>();
    private final Set<UUID> downloading = new HashSet<>();
    private final Set<UUID> dismissed = new HashSet<>();
    private final Set<UUID> modifying = new HashSet<>();
    private final Queue<MessageSharedUpdate> received = new ConcurrentLinkedQueue<>();
    private int revision;

    private SharedPlacementsClient() {}

    public boolean available() { return SchematicaPlus.proxy.supportsSharedPlacements; }

    public int revision() { return revision; }

    public List<SharedPlacement> placements() { return new ArrayList<>(placements.values()); }

    public SharedPlacement get(UUID id) { return placements.get(id); }

    public boolean downloading(UUID id) { return downloading.contains(id); }

    public boolean modifying(UUID id) { return modifying.contains(id); }

    public boolean shared(SchematicWorld world) { return world.sharedId != null || sharing.containsValue(world); }

    public SchematicWorld loaded(UUID id) {
        for (SchematicWorld world : Placements.loaded) if (id.equals(world.sharedId)) return world;
        return null;
    }

    private static int dimension() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.theWorld == null ? Integer.MIN_VALUE : mc.theWorld.provider.dimensionId;
    }

    public static CloudFile file(SharedPlacement placement) {
        return new CloudFile(placement.file(), placement.size, 0, placement.hash, placement.ownerName);
    }

    public boolean cached(SharedPlacement placement) {
        return CloudCache.cached(file(placement));
    }

    public void receive(MessageSharedUpdate message) {
        received.add(message);
    }

    public void clearPending() {
        received.clear();
    }

    public void applyPending() {
        for (MessageSharedUpdate message; (message = received.poll()) != null;) update(message);
    }

    void update(MessageSharedUpdate message) {
        revision++;
        switch (message.operation) {
            case CLEAR:
                for (SharedPlacement placement : placements()) unload(placement.id, false);
                placements.clear();
                sharing.clear();
                modifying.clear();
                return;
            case ADD:
                SharedPlacement placement;
                try {
                    placement = SharedPlacement.read(message.placement);
                } catch (RuntimeException e) {
                    Reference.logger.warn("Ignoring an invalid shared placement", e);
                    return;
                }
                SharedPlacement previous = placements.put(placement.id, placement);
                SchematicWorld own = sharing.remove(placement.id);
                if (own != null && Placements.loaded.contains(own)) {
                    own.sharedId = placement.id;
                    own.setPlacementSettings(own.placementSettings().locked(true));
                    WorldHandler.INSTANCE.saveSession();
                    return;
                }
                SchematicWorld world = loaded(placement.id);
                if (world != null) {
                    if (matches(world, placement)) world.setPlacementSettings(world.placementSettings().locked(!modifying.contains(placement.id)));
                    else if (previous == null || previous.version != placement.version) reload(placement, world);
                } else if (ConfigurationHandler.sharedPlacementsAutoLoad && placement.dimension == dimension() && !dismissed.contains(placement.id)) {
                    load(placement);
                }
                return;
            case REMOVE:
                placements.remove(message.id);
                modifying.remove(message.id);
                unload(message.id, false);
                return;
            case UPLOAD:
                SchematicWorld source = sharing.get(message.id);
                if (source == null || !Placements.loaded.contains(source)) return;
                try {
                    SharedPlacement requested = SharedPlacement.read(message.placement);
                    CloudClient.INSTANCE.upload(Placements.LIBRARY.sourceOf(source).file(), requested.file());
                } catch (RuntimeException e) {
                    Reference.logger.warn("Could not send the file of a shared placement", e);
                }
                return;
            case ACCEPT:
                SchematicWorld accepted = loaded(message.id);
                if (accepted == null) {
                    PacketHandler.INSTANCE.sendToServer(new MessageSharedRequest(MessageSharedRequest.Operation.CANCEL, message.id, null));
                    return;
                }
                modifying.add(message.id);
                accepted.setPlacementSettings(accepted.placementSettings().locked(false));
                InfoUtils.showGuiOrInGameMessage(MessageType.SUCCESS, "syncmatica.success.modification_accepted");
                return;
            case DENY:
                modifying.remove(message.id);
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "syncmatica.error.modification_deny");
                SchematicWorld denied = loaded(message.id);
                SharedPlacement current = placements.get(message.id);
                if (denied != null && current != null) reload(current, denied);
                return;
            default:
        }
    }

    public void share(SchematicWorld world) {
        if (!available() || shared(world)) return;
        SchematicLibrary.Source<SchematicSourceData> source = Placements.LIBRARY.sourceOf(world);
        File file = source.file();
        if (source.memory() || !file.isFile() || !CloudPath.schematicName(file.getName())) {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "syncmatica.error.invalid_file");
            return;
        }
        UUID id = UUID.randomUUID();
        SharedPlacement placement;
        try {
            String name = world.name.isEmpty() ? file.getName() : world.name.length() > 128 ? world.name.substring(0, 128) : world.name;
            placement = SharedWorlds.capture(world, id, name, file.getName(), CloudCache.hash(file), file.length(), dimension());
            placement.validate();
        } catch (IOException | RuntimeException e) {
            Reference.logger.warn("Could not share {}", file, e);
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "syncmatica.error.create_from_schematic", file.getName());
            return;
        }
        sharing.put(id, world);
        PacketHandler.INSTANCE.sendToServer(new MessageSharedRequest(MessageSharedRequest.Operation.SHARE, id, placement.write()));
    }

    public void remove(UUID id) {
        PacketHandler.INSTANCE.sendToServer(new MessageSharedRequest(MessageSharedRequest.Operation.REMOVE, id, null));
    }

    public void load(SharedPlacement placement) {
        if (placement.dimension != dimension()) {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "syncmatica.error.player_dimension_mismatch");
            return;
        }
        if (loaded(placement.id) != null || !downloading.add(placement.id)) return;
        dismissed.remove(placement.id);
        CloudClient.INSTANCE.fetch(file(placement), local -> {
            if (local == null) {
                downloading.remove(placement.id);
                revision++;
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "syncmatica.error.failed_to_load", placement.fileName);
                return;
            }
            try {
                Placements.loadSourceAsync(local, source -> {
                    downloading.remove(placement.id);
                    revision++;
                    if (!placements.containsKey(placement.id) || loaded(placement.id) != null || placement.dimension != dimension()) return;
                    try {
                        SharedWorlds.create(source, placements.get(placement.id));
                    } catch (IOException | RuntimeException e) {
                        Reference.logger.warn("Could not place the shared placement {}", placement.name, e);
                        InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "syncmatica.error.failed_to_load", placement.fileName);
                    }
                }, error -> {
                    downloading.remove(placement.id);
                    revision++;
                    InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "syncmatica.error.failed_to_load", placement.fileName);
                });
            } catch (IOException e) {
                downloading.remove(placement.id);
                revision++;
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "syncmatica.error.failed_to_load", placement.fileName);
            }
        });
    }

    static boolean matches(SchematicWorld world, SharedPlacement placement) {
        SharedPlacement current = SharedWorlds.capture(world, placement);
        return Arrays.equals(current.origin, placement.origin) && current.transforms.equals(placement.transforms)
            && current.subregions.equals(placement.subregions);
    }

    private void reload(SharedPlacement placement, SchematicWorld world) {
        boolean selected = Placements.selected == world;
        Placements.remove(world);
        modifying.remove(placement.id);
        try {
            SchematicLibrary.Source<SchematicSourceData> source = Placements.loadSource(CloudCache.cached(file(placement))
                ? CloudCache.file(placement.file()) : Placements.LIBRARY.sourceOf(world).file());
            SchematicWorld next = SharedWorlds.create(source, placement);
            if (selected) Placements.select(next);
        } catch (IOException | RuntimeException e) {
            Reference.logger.warn("Could not update the shared placement {}", placement.name, e);
            load(placement);
        }
    }

    public void unload(UUID id, boolean dismiss) {
        if (dismiss) dismissed.add(id);
        SchematicWorld world = loaded(id);
        if (world != null) Placements.remove(world);
        modifying.remove(id);
        revision++;
    }

    public void requestModification(SchematicWorld world) {
        if (world.sharedId == null || modifying.contains(world.sharedId)) return;
        PacketHandler.INSTANCE.sendToServer(new MessageSharedRequest(MessageSharedRequest.Operation.MODIFY, world.sharedId, null));
    }

    public void finishModification(SchematicWorld world) {
        SharedPlacement placement = world.sharedId == null ? null : placements.get(world.sharedId);
        if (placement == null || !modifying.remove(world.sharedId)) return;
        world.setPlacementSettings(world.placementSettings().locked(true));
        PacketHandler.INSTANCE.sendToServer(new MessageSharedRequest(MessageSharedRequest.Operation.FINISH, placement.id,
            SharedWorlds.capture(world, placement).write()));
    }

    public void worldChanged() {
        downloading.clear();
        modifying.clear();
        revision++;
        if (!ConfigurationHandler.sharedPlacementsAutoLoad) return;
        for (SharedPlacement placement : placements()) {
            if (placement.dimension == dimension() && !dismissed.contains(placement.id)) load(placement);
        }
    }

    public void clear() {
        placements.clear();
        sharing.clear();
        downloading.clear();
        dismissed.clear();
        modifying.clear();
        revision++;
    }
}

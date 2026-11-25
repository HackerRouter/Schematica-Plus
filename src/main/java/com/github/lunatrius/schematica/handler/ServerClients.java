package com.github.lunatrius.schematica.handler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.reference.Reference;

public final class ServerClients {

    public static final ServerClients INSTANCE = new ServerClients();

    private final Map<UUID, String> compatibleVersions = new ConcurrentHashMap<>();

    public ServerClients() {}

    public boolean register(EntityPlayerMP player, String clientVersion) {
        if (player == null) return false;
        return register(player.getUniqueID(), player.getCommandSenderName(), Reference.VERSION, clientVersion);
    }

    public boolean register(UUID playerId, String playerName, String serverVersion, String clientVersion) {
        if (playerId == null) return false;
        if (SchematicaPlus.isClientCompatibleWithServer(serverVersion, clientVersion)) {
            compatibleVersions.put(playerId, clientVersion == null ? "" : clientVersion.trim());
            return true;
        }
        compatibleVersions.remove(playerId);
        Reference.logger.info("Treating client {} ({}) as client-only because server requires >= {}",
            playerName, clientVersion, serverVersion);
        return false;
    }

    public boolean isCompatible(Entity player) {
        return player instanceof EntityPlayer && isCompatible(player.getUniqueID());
    }

    public boolean isCompatible(UUID playerId) {
        return playerId != null && compatibleVersions.containsKey(playerId);
    }

    public String version(UUID playerId) {
        return playerId == null ? null : compatibleVersions.get(playerId);
    }

    public void forget(EntityPlayer player) {
        if (player != null) compatibleVersions.remove(player.getUniqueID());
    }

    public void clear() {
        compatibleVersions.clear();
    }
}

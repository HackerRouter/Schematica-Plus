package com.github.lunatrius.schematica.handler;

import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.command.CommandSchematic;
import com.github.lunatrius.schematica.network.PacketHandler;
import com.github.lunatrius.schematica.network.message.MessageSaveSelection;
import com.github.lunatrius.schematica.network.message.MessageSaveSelectionRequest;
import com.github.lunatrius.schematica.reference.Names;
import com.github.lunatrius.schematica.util.MessageType;
import com.github.lunatrius.schematica.util.PlayerMessage;
import com.github.lunatrius.schematica.world.storage.RegionSelection;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public final class SelectionSaves {
    public static final SelectionSaves INSTANCE = new SelectionSaves();

    private final Map<UUID, String> requested = new ConcurrentHashMap<>();
    private final Queue<Received> received = new ConcurrentLinkedQueue<>();

    private SelectionSaves() {}

    private static final class Received {
        final EntityPlayerMP player;
        final MessageSaveSelection message;

        Received(EntityPlayerMP player, MessageSaveSelection message) {
            this.player = player;
            this.message = message;
        }
    }

    public void request(EntityPlayerMP player, String name) {
        requested.put(player.getUniqueID(), name);
        PacketHandler.INSTANCE.sendTo(new MessageSaveSelectionRequest(name), player);
    }

    public void received(EntityPlayerMP player, MessageSaveSelection message) {
        if (requested.remove(player.getUniqueID(), message.name)) received.add(new Received(player, message));
    }

    public void forget(EntityPlayer player) {
        requested.remove(player.getUniqueID());
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = MinecraftServer.getServer();
        for (Received entry; (entry = received.poll()) != null;) {
            if (server == null || !server.getConfigurationManager().playerEntityList.contains(entry.player)) continue;
            RegionSelection selection = selection(entry.message);
            if (selection == null) SchematicaPlus.proxy.sendMessage(entry.player,
                new PlayerMessage(MessageType.ERROR, Names.Command.Save.Message.NO_AREA_SELECTED));
            else CommandSchematic.save(entry.player, entry.message.name, selection);
        }
    }

    static RegionSelection selection(MessageSaveSelection message) {
        if (message.regions.isEmpty()) return null;
        try {
            return new RegionSelection(message.regions, message.origin);
        } catch (IllegalArgumentException | ArithmeticException e) {
            return null;
        }
    }
}

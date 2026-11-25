package com.github.lunatrius.schematica.handler;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import com.github.lunatrius.schematica.network.PacketHandler;
import com.github.lunatrius.schematica.network.message.MessageCapabilities;
import com.github.lunatrius.schematica.reference.Reference;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class PlayerHandler {

    public static final PlayerHandler INSTANCE = new PlayerHandler();

    private PlayerHandler() {}

    private String sent;
    private final Queue<PendingHello> pendingHellos = new ConcurrentLinkedQueue<>();
    private final Queue<Runnable> serverTasks = new ConcurrentLinkedQueue<>();

    private static final class PendingHello {
        final EntityPlayerMP player;
        final String version;

        PendingHello(EntityPlayerMP player, String version) {
            this.player = player;
            this.version = version;
        }
    }

    public void onClientHello(EntityPlayerMP player, String clientVersion) {
        if (player == null) return;
        pendingHellos.add(new PendingHello(player, clientVersion));
    }

    public void runOnServerThread(Runnable task) {
        if (task == null) return;
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || !server.isServerRunning()) {
            task.run();
            return;
        }
        serverTasks.add(task);
    }

    public void clear() {
        pendingHellos.clear();
        serverTasks.clear();
        sent = null;
    }

    private static MessageCapabilities capabilities() {
        MessageCapabilities capabilities = new MessageCapabilities(
            ConfigurationHandler.printerEnabled,
            ConfigurationHandler.saveEnabled,
            ConfigurationHandler.loadEnabled);
        capabilities.printerMaxRange = ConfigurationHandler.printerEnabled ? PrinterRange.clamp(ConfigurationHandler.printerMaxRange) : 0;
        capabilities.printerMaxPlacementsPerTick = ConfigurationHandler.printerEnabled ? ConfigurationHandler.printerMaxPlacementsPerTick : 0;
        capabilities.printerMaxBreaksPerTick = ConfigurationHandler.printerEnabled ? ConfigurationHandler.printerMaxBreaksPerTick : 0;
        capabilities.supportsAccuratePlacement = ConfigurationHandler.accuratePlacementEnabled;
        capabilities.supportsNbtRequests = ConfigurationHandler.nbtRequestsEnabled;
        capabilities.supportsCloud = ConfigurationHandler.cloudEnabled;
        capabilities.supportsSharedPlacements = ConfigurationHandler.sharedPlacementsEnabled;
        capabilities.serverVersion = Reference.VERSION;
        return capabilities;
    }

    private static String key(MessageCapabilities c) {
        return c.isPrinterEnabled + "," + c.isSaveEnabled + "," + c.isLoadEnabled + "," + c.supportsAccuratePlacement + "," + c.supportsNbtRequests + "," + c.supportsCloud + "," + c.supportsSharedPlacements + "," + c.printerMaxRange + "," + c.printerMaxPlacementsPerTick + "," + c.printerMaxBreaksPerTick;
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
    }

    /** Server options changed at runtime (the single player config screen): tell the connected clients, so turning off printerEnabled stops their printers. */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (Runnable task; (task = serverTasks.poll()) != null;) {
            try {
                task.run();
            } catch (Exception ex) {
                Reference.logger.error("Failed to run scheduled server task", ex);
            }
        }
        MinecraftServer server = MinecraftServer.getServer();
        for (PendingHello hello; (hello = pendingHellos.poll()) != null;) {
            if (server == null || !server.getConfigurationManager().playerEntityList.contains(hello.player)) continue;
            if (!ServerClients.INSTANCE.register(hello.player, hello.version)) continue;
            try {
                MessageCapabilities capabilities = capabilities();
                if (sent == null) sent = key(capabilities);
                PacketHandler.INSTANCE.sendTo(capabilities, hello.player);
                SharedPlacements.INSTANCE.joined(hello.player);
            } catch (Exception ex) {
                Reference.logger.error("Failed to send capabilities!", ex);
            }
        }
        if (sent == null || server == null) return;
        MessageCapabilities capabilities = capabilities();
        String current = key(capabilities);
        if (current.equals(sent)) return;
        sent = current;
        try {
            for (Object candidate : server.getConfigurationManager().playerEntityList) {
                EntityPlayerMP player = (EntityPlayerMP) candidate;
                if (ServerClients.INSTANCE.isCompatible(player)) {
                    PacketHandler.INSTANCE.sendTo(capabilities, player);
                }
            }
        } catch (Exception ex) {
            Reference.logger.error("Failed to send capabilities!", ex);
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.player instanceof EntityPlayerMP) RemoteEdits.INSTANCE.forget(event.player);
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            DownloadHandler.INSTANCE.stop((EntityPlayerMP) event.player);
            AccuratePlacement.INSTANCE.forget(event.player);
            PrinterActions.INSTANCE.forget((EntityPlayerMP) event.player);
            RemoteEdits.INSTANCE.forget(event.player);
            NbtRequests.INSTANCE.forget(event.player);
            SelectionSaves.INSTANCE.forget(event.player);
            ServerCloud.INSTANCE.forget(event.player);
            SharedPlacements.INSTANCE.forget(event.player);
            ServerClients.INSTANCE.forget(event.player);
        }
    }
}

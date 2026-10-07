package com.github.lunatrius.schematica.handler;

import net.minecraft.entity.player.EntityPlayerMP;

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

    private static MessageCapabilities capabilities() {
        MessageCapabilities capabilities = new MessageCapabilities(
            ConfigurationHandler.printerEnabled,
            ConfigurationHandler.saveEnabled,
            ConfigurationHandler.loadEnabled);
        capabilities.supportsRemoteEdit = true;
        capabilities.supportsAccuratePlacement = ConfigurationHandler.accuratePlacementEnabled;
        return capabilities;
    }

    private static String key(MessageCapabilities c) {
        return c.isPrinterEnabled + "," + c.isSaveEnabled + "," + c.isLoadEnabled + "," + c.supportsRemoteEdit + "," + c.supportsAccuratePlacement;
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            try {
                MessageCapabilities capabilities = capabilities();
                if (sent == null) sent = key(capabilities);
                PacketHandler.INSTANCE.sendTo(capabilities, (EntityPlayerMP) event.player);
            } catch (Exception ex) {
                Reference.logger.error("Failed to send capabilities!", ex);
            }
        }
    }

    /** Server options changed at runtime (the single player config screen): tell the connected clients, so turning off printerEnabled stops their printers. */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || sent == null) return;
        MessageCapabilities capabilities = capabilities();
        String current = key(capabilities);
        if (current.equals(sent)) return;
        sent = current;
        try {
            PacketHandler.INSTANCE.sendToAll(capabilities);
        } catch (Exception ex) {
            Reference.logger.error("Failed to send capabilities!", ex);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            DownloadHandler.INSTANCE.transferMap.remove(event.player);
            AccuratePlacement.INSTANCE.forget(event.player);
            RemoteEdits.INSTANCE.forget(event.player);
        }
    }
}

package com.github.lunatrius.schematica.handler.client;

import net.minecraft.client.Minecraft;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.client.printer.SchematicPrinter;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Reference;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;

public class TickHandler {

    public static final TickHandler INSTANCE = new TickHandler();

    private final Minecraft minecraft = Minecraft.getMinecraft();

    private int ticks;

    private TickHandler() {}

    @SubscribeEvent
    public void onClientConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        Reference.logger.info("Scheduling client settings reset.");
        ClientProxy.isPendingReset = true;
        com.github.lunatrius.schematica.client.printer.LagMonitor.install(event.manager);
    }

    @SubscribeEvent
    public void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        com.github.lunatrius.schematica.handler.DownloadHandler.INSTANCE.beginDownload(null);
        CommandEditQueue.INSTANCE.cancel();
        com.github.lunatrius.schematica.network.message.MessageCapabilities.clearPending();
        Reference.logger.info("Scheduling client settings reset.");
        ClientProxy.isPendingReset = true;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) com.github.lunatrius.schematica.client.gui.material.MaterialLists.startTick();
        if (event.phase == TickEvent.Phase.END) {
            com.github.lunatrius.schematica.client.input.HotkeyHooks.tick();
            com.github.lunatrius.schematica.tool.ToolManager.tick();
            this.minecraft.mcProfiler.startSection("schematica");
            if (ClientProxy.isPendingReset) {
                SchematicaPlus.proxy.resetSettings();
                ClientProxy.isPendingReset = false;
            }
            com.github.lunatrius.schematica.network.message.MessageCapabilities.applyPending();
            WorldHandler.INSTANCE.updateWorld(this.minecraft);
            com.github.lunatrius.schematica.handler.QueueTickHandler.INSTANCE.clientTick(this.minecraft.theWorld, this.minecraft.thePlayer);
            com.github.lunatrius.schematica.client.world.GridPlacements.INSTANCE.tick(this.minecraft);
            com.github.lunatrius.schematica.client.verifier.VerificationManager.INSTANCE.tick(this.minecraft);
            com.github.lunatrius.schematica.client.gui.material.MaterialLists.tick();
            // layerModeFollowsPlayer: the single boundary of the layer range follows the camera
            if (ConfigurationHandler.layerModeFollowsPlayer && this.minecraft.renderViewEntity != null) {
                com.github.lunatrius.schematica.client.world.RenderLayerRange range = com.github.lunatrius.schematica.client.world.RenderLayerSettings.RANGE;
                if (range.mode() != com.github.lunatrius.schematica.client.world.RenderLayerRange.Mode.ALL
                    && range.mode() != com.github.lunatrius.schematica.client.world.RenderLayerRange.Mode.LAYER_RANGE) {
                    net.minecraft.entity.EntityLivingBase camera = this.minecraft.renderViewEntity;
                    double coordinate = range.axis() == com.github.lunatrius.schematica.client.world.RenderLayerRange.Axis.X ? camera.posX
                        : range.axis() == com.github.lunatrius.schematica.client.world.RenderLayerRange.Axis.Y ? camera.posY : camera.posZ;
                    int position = net.minecraft.util.MathHelper.floor_double(coordinate);
                    if (range.value(false) != position) range.setValue(false, position);
                }
            }
            com.github.lunatrius.schematica.client.projects.SchematicProjects.syncSelections();
            com.github.lunatrius.schematica.tool.RebuildJobs.tick(this.minecraft);
            com.github.lunatrius.schematica.client.printer.EasyPlace.tick(this.minecraft);
            if (this.minecraft.thePlayer != null) {
                this.minecraft.mcProfiler.startSection("printer");
                SchematicPrinter printer = SchematicPrinter.INSTANCE;
                if (ConfigurationHandler.printerAutoDisable && (this.minecraft.thePlayer.isDead || this.minecraft.thePlayer.getHealth() <= 0)) {
                    printer.stopWithMessage("schematica.message.printer.died");
                }
                printer.showPendingMessage();
                // placeDelay is the interval between passes in ticks (0 and 1: every tick)
                if (printer.isEnabled() && printer.isPrinting() && ++this.ticks >= Math.max(1, ConfigurationHandler.placeDelay)) {
                    this.ticks = 0;
                    printer.print();
                }
                this.minecraft.mcProfiler.endSection();
            }

            this.minecraft.mcProfiler.endSection();
        }
    }
}

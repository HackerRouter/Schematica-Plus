package com.github.lunatrius.schematica.handler.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.client.align.AutoAlignJob;
import com.github.lunatrius.schematica.client.container.ContainerLabels;
import com.github.lunatrius.schematica.client.gui.material.MaterialLists;
import com.github.lunatrius.schematica.client.input.HotkeyHooks;
import com.github.lunatrius.schematica.client.printer.DestroyAssist;
import com.github.lunatrius.schematica.client.printer.EasyPlace;
import com.github.lunatrius.schematica.client.printer.LagMonitor;
import com.github.lunatrius.schematica.client.printer.NextBlockHint;
import com.github.lunatrius.schematica.client.printer.SchematicPrinter;
import com.github.lunatrius.schematica.client.projects.SchematicProjects;
import com.github.lunatrius.schematica.client.verifier.VerificationManager;
import com.github.lunatrius.schematica.client.world.GridPlacements;
import com.github.lunatrius.schematica.client.world.RenderLayerRange;
import com.github.lunatrius.schematica.client.world.RenderLayerSettings;
import com.github.lunatrius.schematica.client.world.WorldFingerprints;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.handler.DownloadHandler;
import com.github.lunatrius.schematica.handler.QueueTickHandler;
import com.github.lunatrius.schematica.network.message.MessageCapabilities;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.tool.RebuildJobs;
import com.github.lunatrius.schematica.tool.ToolManager;

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
        LagMonitor.install(event.manager);
    }

    @SubscribeEvent
    public void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        DownloadHandler.INSTANCE.beginDownload(null);
        this.minecraft.func_152344_a(CommandEditQueue.INSTANCE::cancel);
        MessageCapabilities.clearPending();
        Reference.logger.info("Scheduling client settings reset.");
        ClientProxy.isPendingReset = true;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) MaterialLists.startTick();
        if (event.phase == TickEvent.Phase.END) {
            HotkeyHooks.tick();
            ToolManager.tick();
            this.minecraft.mcProfiler.startSection("schematica");
            if (ClientProxy.isPendingReset) {
                SchematicaPlus.proxy.resetSettings();
                ClientProxy.isPendingReset = false;
            }
            MessageCapabilities.applyPending();
            WorldHandler.INSTANCE.updateWorld(this.minecraft);
            QueueTickHandler.INSTANCE.clientTick(this.minecraft.theWorld, this.minecraft.thePlayer);
            GridPlacements.INSTANCE.tick(this.minecraft);
            VerificationManager.INSTANCE.tick(this.minecraft);
            AutoAlignJob.tick(this.minecraft);
            MaterialLists.tick();
            // layerModeFollowsPlayer: the single boundary of the layer range follows the camera
            if (ConfigurationHandler.layerModeFollowsPlayer && this.minecraft.renderViewEntity != null) {
                RenderLayerRange range = RenderLayerSettings.RANGE;
                if (range.mode() != RenderLayerRange.Mode.ALL
                    && range.mode() != RenderLayerRange.Mode.LAYER_RANGE) {
                    EntityLivingBase camera = this.minecraft.renderViewEntity;
                    double coordinate = range.axis() == RenderLayerRange.Axis.X ? camera.posX
                        : range.axis() == RenderLayerRange.Axis.Y ? camera.posY : camera.posZ;
                    int position = MathHelper.floor_double(coordinate);
                    if (range.value(false) != position) range.setValue(false, position);
                }
            }
            SchematicProjects.syncSelections();
            RebuildJobs.tick(this.minecraft);
            EasyPlace.tick(this.minecraft);
            DestroyAssist.tick(this.minecraft);
            NextBlockHint.tick(this.minecraft);
            ContainerLabels.tick(this.minecraft);
            WorldFingerprints.tick(this.minecraft);
            if (this.minecraft.thePlayer != null) {
                this.minecraft.mcProfiler.startSection("printer");
                SchematicPrinter printer = SchematicPrinter.INSTANCE;
                if (ConfigurationHandler.printerAutoDisable && (this.minecraft.thePlayer.isDead || this.minecraft.thePlayer.getHealth() <= 0)) {
                    printer.stopWithMessage("schematica.message.printer.died");
                }
                printer.showPendingMessage();
                printer.tickIdle();
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

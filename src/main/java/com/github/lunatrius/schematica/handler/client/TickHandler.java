package com.github.lunatrius.schematica.handler.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentTranslation;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.client.printer.SchematicPrinter;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Reference;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;

public class TickHandler {

    public static final TickHandler INSTANCE = new TickHandler();

    private final Minecraft minecraft = Minecraft.getMinecraft();

    private int ticks = -1;
    private int completionCheckCounter = 0;

    private TickHandler() {}

    @SubscribeEvent
    public void onClientConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        Reference.logger.info("Scheduling client settings reset.");
        ClientProxy.isPendingReset = true;
    }

    @SubscribeEvent
    public void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        com.github.lunatrius.schematica.handler.DownloadHandler.INSTANCE.beginDownload(null);
        CommandEditQueue.INSTANCE.cancel();
        Reference.logger.info("Scheduling client settings reset.");
        ClientProxy.isPendingReset = true;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            com.github.lunatrius.schematica.client.input.HotkeyHooks.tick();
            com.github.lunatrius.schematica.tool.ToolManager.tick();
            this.minecraft.mcProfiler.startSection("schematica");
            if (ClientProxy.isPendingReset) {
                SchematicaPlus.proxy.resetSettings();
                ClientProxy.isPendingReset = false;
            }
            WorldHandler.INSTANCE.updateWorld(this.minecraft);
            com.github.lunatrius.schematica.client.verifier.VerificationManager.INSTANCE.tick(this.minecraft);
            com.github.lunatrius.schematica.tool.RebuildJobs.tick(this.minecraft);
            SchematicWorld schematic = ClientProxy.schematic;
            if (this.minecraft.thePlayer != null && schematic != null && schematic.isRenderingEnabled()) {
                this.minecraft.mcProfiler.startSection("printer");
                SchematicPrinter printer = SchematicPrinter.INSTANCE;
                if (printer.isEnabled() && printer.isPrinting() && this.ticks-- < 0) {
                    this.ticks = ConfigurationHandler.placeDelay;

                    printer.print();

                    // Periodically check if printing is complete (every 40 ticks ~ 2 seconds)
                    this.completionCheckCounter++;
                    if (this.completionCheckCounter >= 40) {
                        this.completionCheckCounter = 0;
                        if (printer.isComplete()) {
                            printer.setPrinting(false);
                            Reference.logger.info("Printer finished — all blocks placed.");
                            if (this.minecraft.thePlayer != null) {
                                this.minecraft.thePlayer.addChatMessage(new ChatComponentTranslation("schematica.message.printer.finished"));
                            }
                        }
                    }
                }

                this.minecraft.mcProfiler.endSection();
            }

            this.minecraft.mcProfiler.endSection();
        }
    }
}

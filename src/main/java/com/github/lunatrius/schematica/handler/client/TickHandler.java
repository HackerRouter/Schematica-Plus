package com.github.lunatrius.schematica.handler.client;

import static com.github.lunatrius.schematica.client.util.WorldServerName.worldServerName;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.github.lunatrius.schematica.Schematica;
import com.github.lunatrius.schematica.client.printer.SchematicPrinter;
import com.github.lunatrius.schematica.client.renderer.RendererSchematicChunk;
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
        Reference.logger.info("Scheduling client settings reset.");
        ClientProxy.isPendingReset = true;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            this.minecraft.mcProfiler.startSection("schematica");
            SchematicWorld schematic = ClientProxy.schematic;
            if (this.minecraft.thePlayer != null && schematic != null && schematic.isRendering) {
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
                                this.minecraft.thePlayer.addChatMessage(new ChatComponentText(
                                    EnumChatFormatting.GREEN + "[Schematica] " +
                                    EnumChatFormatting.RESET + "Printing complete. Printer stopped."));
                            }
                        }
                    }
                }

                this.minecraft.mcProfiler.endStartSection("canUpdate");
                RendererSchematicChunk.setCanUpdate(true);

                this.minecraft.mcProfiler.endSection();
            }

            if (ClientProxy.isPendingReset) {
                Schematica.proxy.resetSettings();
                ClientProxy.isPendingReset = false;

                // resetSettings saved and cleared schematics, setting isPendingRestore.
                // If we're already in a world (connect reset fires after WorldEvent.Load),
                // restore immediately so schematics aren't lost.
                if (ClientProxy.isPendingRestore && this.minecraft.theWorld != null) {
                    String name = ClientProxy.lastWorldServerName;
                    // Try to get a fresh name if possible
                    try {
                        String fresh = worldServerName(this.minecraft);
                        if (fresh != null && !fresh.isEmpty()) {
                            name = fresh;
                            ClientProxy.lastWorldServerName = name;
                        }
                    } catch (Exception ignored) {}
                    if (name != null && !name.isEmpty()) {
                        ClientProxy.restoreLoadedSchematics(name);
                        ClientProxy.restoreAreaSelection(name);
                    }
                    ClientProxy.isPendingRestore = false;
                }
            }

            this.minecraft.mcProfiler.endSection();
        }
    }
}

package com.github.lunatrius.schematica.handler.client;

import static com.github.lunatrius.schematica.client.util.WorldServerName.worldServerName;

import net.minecraft.client.Minecraft;
import net.minecraft.world.IWorldAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;

import com.github.lunatrius.schematica.client.world.SchematicUpdater;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Reference;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class WorldHandler {

    @SubscribeEvent
    public void onLoad(final WorldEvent.Load event) {
        if (event.world.isRemote) {
            addWorldAccess(event.world, SchematicUpdater.INSTANCE);
            // Resolve and track the world/server name
            try {
                String name = worldServerName(Minecraft.getMinecraft());
                if (name != null && !name.isEmpty()) {
                    ClientProxy.lastWorldServerName = name;
                    // Restore schematics if they were cleared by resetSettings or if this is a fresh load
                    if (ClientProxy.isPendingRestore || ClientProxy.loadedSchematics.isEmpty()) {
                        ClientProxy.restoreLoadedSchematics(name);
                        ClientProxy.isPendingRestore = false;
                    }
                    // Restore area selection
                    ClientProxy.restoreAreaSelection(name);
                }
            } catch (Exception e) {
                Reference.logger.debug("Could not restore schematics on world load", e);
            }
        }
    }

    @SubscribeEvent
    public void onUnload(final WorldEvent.Unload event) {
        if (event.world.isRemote) {
            // Save loaded schematics before unloading
            try {
                // Try to get the current name; fall back to lastWorldServerName
                String name = null;
                try {
                    name = worldServerName(Minecraft.getMinecraft());
                } catch (Exception ignored) {}
                if (name == null || name.isEmpty()) {
                    name = ClientProxy.lastWorldServerName;
                }
                if (name != null && !name.isEmpty()) {
                    ClientProxy.saveLoadedSchematics(name);
                    ClientProxy.saveAreaSelection(name);
                    // Keep lastWorldServerName so resetSettings can also save if needed
                }
            } catch (Exception e) {
                Reference.logger.debug("Could not save schematics on world unload", e);
            }
            removeWorldAccess(event.world, SchematicUpdater.INSTANCE);
        }
    }

    public static void addWorldAccess(final World world, final IWorldAccess schematic) {
        if (world != null && schematic != null) {
            Reference.logger.debug("Adding world access to {}", world);
            world.addWorldAccess(schematic);
        }
    }

    public static void removeWorldAccess(final World world, final IWorldAccess schematic) {
        if (world != null && schematic != null) {
            Reference.logger.debug("Removing world access from {}", world);
            world.removeWorldAccess(schematic);
        }
    }
}

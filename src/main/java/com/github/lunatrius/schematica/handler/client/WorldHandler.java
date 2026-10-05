package com.github.lunatrius.schematica.handler.client;

import static com.github.lunatrius.schematica.client.util.WorldServerName.worldServerName;

import net.minecraft.client.Minecraft;
import net.minecraft.world.IWorldAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;

import com.github.lunatrius.schematica.client.projects.SchematicProjects;
import com.github.lunatrius.schematica.client.world.RenderLayerSettings;
import com.github.lunatrius.schematica.client.world.SchematicUpdater;
import com.github.lunatrius.schematica.client.util.WorldSession;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Reference;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class WorldHandler {

    public static final WorldHandler INSTANCE = new WorldHandler();
    private final WorldSession<World> session = new WorldSession<>(key -> {
        ClientProxy.saveLoadedSchematics(key);
        ClientProxy.saveAreaSelection(key);
        SchematicProjects.saveSession(key);
        RenderLayerSettings.save(key);
        com.github.lunatrius.schematica.tool.ToolManager.saveMode(key);
    }, ClientProxy::clearWorldState, key -> {
        ClientProxy.lastWorldServerName = key;
        ClientProxy.restoreLoadedSchematics(key);
        ClientProxy.restoreAreaSelection(key);
        SchematicProjects.restoreSession(key);
        RenderLayerSettings.restore(key);
        com.github.lunatrius.schematica.tool.ToolManager.restoreMode(key);
    });

    private WorldHandler() {}

    public void updateWorld(Minecraft minecraft) {
        try {
            World world = minecraft.theWorld;
            session.update(world, world == null ? null : worldServerName(minecraft, world));
        } catch (Exception e) {
            Reference.logger.error("Could not switch schematic world session", e);
        }
    }

    public void closeSession() {
        session.update(null, null);
    }

    public void saveSession() {
        session.save();
    }

    @SubscribeEvent
    public void onLoad(final WorldEvent.Load event) {
        if (event.world.isRemote) {
            addWorldAccess(event.world, SchematicUpdater.INSTANCE);
            com.github.lunatrius.schematica.world.schematic.ItemIdMaps.archiveCurrent();
        }
    }

    @SubscribeEvent
    public void onUnload(final WorldEvent.Unload event) {
        if (event.world.isRemote) {
            session.unload(event.world);
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

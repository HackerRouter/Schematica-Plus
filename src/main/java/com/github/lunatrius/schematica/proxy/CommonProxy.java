package com.github.lunatrius.schematica.proxy;

import java.io.File;
import java.io.IOException;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;
import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.command.CommandSchematicaList;
import com.github.lunatrius.schematica.command.CommandSchematicaRemove;
import com.github.lunatrius.schematica.command.CommandSchematicaSave;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.handler.DownloadHandler;
import com.github.lunatrius.schematica.handler.QueueTickHandler;
import com.github.lunatrius.schematica.nbt.ForgeMultipart;
import com.github.lunatrius.schematica.nbt.NBTConversionException;
import com.github.lunatrius.schematica.nbt.NBTHelper;
import com.github.lunatrius.schematica.network.PacketHandler;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.world.chunk.SchematicContainer;
import com.github.lunatrius.schematica.world.schematic.SchematicUtil;
import com.github.lunatrius.schematica.world.storage.Schematic;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public abstract class CommonProxy {

    public boolean isSaveEnabled = true;
    public boolean isLoadEnabled = true;
    public boolean GTNH = false;
    public volatile boolean supportsWorldMove;
    public void worldMoveFinished(long id, boolean success) {}

    public void preInit(FMLPreInitializationEvent event) {
        GTNH = Loader.isModLoaded("dreamcraft");
        Reference.logger = event.getModLog();
        ConfigurationHandler.init(event.getSuggestedConfigurationFile());
    }

    public void init(FMLInitializationEvent event) {
        PacketHandler.init();
        FMLCommonHandler.instance().bus().register(com.github.lunatrius.schematica.handler.WorldEditQueue.INSTANCE);

        FMLCommonHandler.instance()
            .bus()
            .register(QueueTickHandler.INSTANCE);
        FMLCommonHandler.instance()
            .bus()
            .register(DownloadHandler.INSTANCE);
    }

    public void postInit(FMLPostInitializationEvent event) {
        ForgeMultipart.init();
    }

    public void serverStarting(FMLServerStartingEvent event) {
        if (ConfigurationHandler.serversideSchematicsEnabled) {
            event.registerServerCommand(new CommandSchematicaSave());
            event.registerServerCommand(new CommandSchematicaList());
            event.registerServerCommand(new CommandSchematicaRemove());
        }
    }

    public void createFolders() {
        if (!ConfigurationHandler.schematicDirectory.exists()) {
            if (!ConfigurationHandler.schematicDirectory.mkdirs()) {
                Reference.logger.warn(
                    "Could not create schematic directory [{}]!",
                    ConfigurationHandler.schematicDirectory.getAbsolutePath());
            }
        }
    }

    public abstract File getDataDirectory();

    public File getDirectory(final String directory) {
        final File dataDirectory = getDataDirectory();
        final File subDirectory = new File(dataDirectory, directory);

        if (!subDirectory.exists()) {
            if (!subDirectory.mkdirs()) {
                Reference.logger.error("Could not create directory [{}]!", subDirectory.getAbsolutePath());
            }
        }

        try {
            return subDirectory.getCanonicalFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return subDirectory;
    }

    public void resetSettings() {
        supportsWorldMove = false;
        this.isSaveEnabled = true;
        this.isLoadEnabled = true;
    }

    public void unloadSchematic() {}

    /**
     * Attempts to resolve the server-side world corresponding to the given (possibly client-side) world.
     * Server-side worlds contain full tile entity NBT data (inventories, etc.) that client worlds lack.
     * Falls back to the original world if no server world is available (e.g. on a dedicated server or
     * if the integrated server is not running).
     */
    protected World getServerWorld(final World world) {
        if (!world.isRemote) return world;
        try {
            final MinecraftServer server = MinecraftServer.getServer();
            if (server != null && server.isServerRunning() && !server.isServerStopped()) {
                final WorldServer serverWorld = server.worldServerForDimension(world.provider.dimensionId);
                if (serverWorld != null) {
                    return serverWorld;
                }
            }
        } catch (Exception e) {
            Reference.logger.debug("Could not obtain server world, using client world for TE data", e);
        }
        return world;
    }

    public void copyChunkToSchematic(final ISchematic schematic, final World world, final int chunkX, final int chunkZ,
        final int minX, final int maxX, final int minY, final int maxY, final int minZ, final int maxZ) {
        // Try to use the server-side world for tile entity data, since client-side TEs
        // often lack full NBT (e.g. inventories are not synced to the client).
        final World serverWorld = getServerWorld(world);

        final int localMinX = minX < (chunkX << 4) ? 0 : (minX & 15);
        final int localMaxX = maxX > ((chunkX << 4) + 15) ? 15 : (maxX & 15);
        final int localMinZ = minZ < (chunkZ << 4) ? 0 : (minZ & 15);
        final int localMaxZ = maxZ > ((chunkZ << 4) + 15) ? 15 : (maxZ & 15);

        for (int chunkLocalX = localMinX; chunkLocalX <= localMaxX; chunkLocalX++) {
            for (int chunkLocalZ = localMinZ; chunkLocalZ <= localMaxZ; chunkLocalZ++) {
                for (int y = minY; y <= maxY; y++) {
                    final int x = chunkLocalX | (chunkX << 4);
                    final int z = chunkLocalZ | (chunkZ << 4);

                    final int localX = x - minX;
                    final int localY = y - minY;
                    final int localZ = z - minZ;

                    if (!schematic.containsBlock(localX, localY, localZ)) continue;

                    try {
                        final Block block = world.getBlock(x, y, z);
                        final int metadata = world.getBlockMetadata(x, y, z);
                        final boolean success = schematic.setBlock(localX, localY, localZ, block, metadata);

                        if (success && block.hasTileEntity(metadata)) {
                            // Prefer server-side TE for full NBT data (inventories, etc.)
                            TileEntity tileEntity = serverWorld.getTileEntity(x, y, z);
                            if (tileEntity == null) {
                                tileEntity = world.getTileEntity(x, y, z);
                            }
                            if (tileEntity != null) {
                                try {
                                    final TileEntity reloadedTileEntity = NBTHelper
                                        .reloadTileEntity(tileEntity, minX, minY, minZ);
                                    schematic.setTileEntity(localX, localY, localZ, reloadedTileEntity);
                                } catch (NBTConversionException nce) {
                                    Reference.logger
                                        .error("Error while trying to save tile entity '{}'!", tileEntity, nce);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Reference.logger.error("Something went wrong!", e);
                    }
                }
            }
        }

        final int minX1 = localMinX | (chunkX << 4);
        final int minZ1 = localMinZ | (chunkZ << 4);
        final int maxX1 = localMaxX | (chunkX << 4);
        final int maxZ1 = localMaxZ | (chunkZ << 4);
        final AxisAlignedBB bb = AxisAlignedBB.getBoundingBox(minX1, minY, minZ1, maxX1 + 1, maxY + 1, maxZ1 + 1);
        final List<Entity> entities = world.getEntitiesWithinAABB(Entity.class, bb);
        for (Entity entity : entities) {
            if (!schematic.containsBlock((int) Math.floor(entity.posX) - minX,
                (int) Math.floor(entity.posY) - minY, (int) Math.floor(entity.posZ) - minZ)) continue;
            try {
                final Entity reloadedEntity = NBTHelper.reloadEntity(entity, minX, minY, minZ);
                schematic.addEntity(reloadedEntity);
            } catch (NBTConversionException nce) {
                Reference.logger.error("Error while trying to save entity '{}'!", entity, nce);
            }
        }
    }

    public boolean saveSchematic(EntityPlayer player, File directory, String filename, World world, Vector3i from,
        Vector3i to) {
        return saveSchematic(player, directory, filename, world, from, to, java.util.Collections.emptyList(), com.github.lunatrius.schematica.api.SchematicOrigin.ZERO, null);
    }

    public boolean saveSchematic(EntityPlayer player, File directory, String filename, World world,
        com.github.lunatrius.schematica.world.storage.RegionSelection selection) {
        return saveSchematic(player, directory, filename, world,
            new Vector3i(selection.minX, selection.minY, selection.minZ), new Vector3i(selection.maxX, selection.maxY, selection.maxZ), selection.localRegions, selection.localOrigin, null);
    }

    /** Captures the selection like a save, but hands the encoded schematic to the client instead of writing a file. */
    public boolean captureSchematic(EntityPlayer player, String name, World world, com.github.lunatrius.schematica.world.storage.RegionSelection selection,
        java.util.function.Consumer<com.github.lunatrius.schematica.world.schematic.SchematicFileSnapshot> memory) {
        return saveSchematic(player, null, name, world,
            new Vector3i(selection.minX, selection.minY, selection.minZ), new Vector3i(selection.maxX, selection.maxY, selection.maxZ), selection.localRegions, selection.localOrigin,
            java.util.Objects.requireNonNull(memory));
    }

    private boolean saveSchematic(EntityPlayer player, File directory, String filename, World world, Vector3i from,
        Vector3i to, java.util.List<com.github.lunatrius.schematica.api.SchematicRegion> regions, com.github.lunatrius.schematica.api.SchematicOrigin origin,
        java.util.function.Consumer<com.github.lunatrius.schematica.world.schematic.SchematicFileSnapshot> memory) {
        synchronized (QueueTickHandler.INSTANCE) {
        try {
            if (!QueueTickHandler.INSTANCE.canQueue(player)) {
                throw new IllegalStateException("A save is already pending or the save queue is full");
            }
            String iconName = "";

            if (memory == null) try {
                String[] parts = filename.split(";");
                if (parts.length == 2) {
                    iconName = parts[0];
                    filename = parts[1];
                }
            } catch (Exception e) {
                Reference.logger.error("Failed to parse icon data!", e);
            }

            final int minX = Math.min(from.x, to.x);
            final int maxX = Math.max(from.x, to.x);
            final int minY = Math.min(from.y, to.y);
            final int maxY = Math.max(from.y, to.y);
            final int minZ = Math.min(from.z, to.z);
            final int maxZ = Math.max(from.z, to.z);

            com.github.lunatrius.schematica.util.SchematicLimits.worldBounds(minX, minY, minZ, maxX, maxY, maxZ);
            final int width = com.github.lunatrius.schematica.util.SchematicLimits.dimension(minX, maxX);
            final int height = com.github.lunatrius.schematica.util.SchematicLimits.dimension(minY, maxY);
            final int length = com.github.lunatrius.schematica.util.SchematicLimits.dimension(minZ, maxZ);
            final File file = memory != null ? new File(filename) : com.github.lunatrius.schematica.util.FileUtils.resolveSchematicFile(directory, filename);

            final Schematic schematic = new Schematic(SchematicUtil.getIconFromName(iconName), width, height, length);
            schematic.setRegions(regions);
            schematic.setOrigin(origin);
            final SchematicContainer container = new SchematicContainer(
                schematic,
                player,
                getServerWorld(world),
                file,
                minX,
                maxX,
                minY,
                maxY,
                minZ,
                maxZ);
            container.memory = memory;
            QueueTickHandler.INSTANCE.queueSchematic(container);

            return true;
        } catch (Exception e) {
            Reference.logger.error("Failed to save schematic!", e);
        }
        return false;
        }
    }

    public abstract boolean loadSchematic(EntityPlayer player, File directory, String filename);

    public abstract boolean isPlayerQuotaExceeded(EntityPlayer player);

    public abstract File getPlayerSchematicDirectory(EntityPlayer player, boolean privateDirectory);
}

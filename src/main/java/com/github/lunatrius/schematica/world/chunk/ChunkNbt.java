package com.github.lunatrius.schematica.world.chunk;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.Constants;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.nbt.NBTHelper;
import com.github.lunatrius.schematica.reference.Reference;

public final class ChunkNbt {
    private ChunkNbt() {}

    public static void merge(SchematicContainer container, int chunkX, int chunkZ, NBTTagCompound data) {
        ISchematic schematic = container.schematic;
        NBTTagList tiles = data.getTagList("TileEntities", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < tiles.tagCount(); i++) {
            NBTTagCompound tag = tiles.getCompoundTagAt(i);
            int x = tag.getInteger("x") - container.minX, y = tag.getInteger("y") - container.minY, z = tag.getInteger("z") - container.minZ;
            if (!inside(container, chunkX, chunkZ, x, y, z) || !schematic.containsBlock(x, y, z)) continue;
            Block block = schematic.getBlock(x, y, z);
            if (block == null || !block.hasTileEntity(schematic.getBlockMetadata(x, y, z))) continue;
            try {
                TileEntity tile = NBTHelper.readTileEntityFromCompound(tag);
                if (tile == null) continue;
                tile.setWorldObj(container.world);
                TileEntity copy = NBTHelper.reloadTileEntity(tile, container.minX, container.minY, container.minZ, true);
                if (copy != null) schematic.setTileEntity(x, y, z, copy);
            } catch (Exception e) {
                Reference.logger.warn("Could not use the server data of the block entity at {} {} {}", x, y, z, e);
            }
        }
        for (Entity entity : new ArrayList<>(schematic.getEntities())) {
            if (inside(container, chunkX, chunkZ, (int) Math.floor(entity.posX), (int) Math.floor(entity.posY), (int) Math.floor(entity.posZ))) {
                schematic.removeEntity(entity);
            }
        }
        NBTTagList entities = data.getTagList("Entities", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < entities.tagCount(); i++) {
            try {
                Entity entity = NBTHelper.readEntityFromCompound(entities.getCompoundTagAt(i), container.world);
                if (entity == null) continue;
                Entity copy = NBTHelper.reloadEntity(entity, container.minX, container.minY, container.minZ);
                if (copy == null) continue;
                int x = (int) Math.floor(copy.posX), y = (int) Math.floor(copy.posY), z = (int) Math.floor(copy.posZ);
                if (inside(container, chunkX, chunkZ, x, y, z) && schematic.containsBlock(x, y, z)) schematic.addEntity(copy);
            } catch (Exception e) {
                Reference.logger.warn("Could not use the server data of an entity", e);
            }
        }
    }

    private static boolean inside(SchematicContainer container, int chunkX, int chunkZ, int x, int y, int z) {
        int worldX = x + container.minX, worldZ = z + container.minZ;
        return worldX >> 4 == chunkX && worldZ >> 4 == chunkZ && y >= 0 && y <= container.maxY - container.minY
            && worldX >= container.minX && worldX <= container.maxX && worldZ >= container.minZ && worldZ <= container.maxZ;
    }
}

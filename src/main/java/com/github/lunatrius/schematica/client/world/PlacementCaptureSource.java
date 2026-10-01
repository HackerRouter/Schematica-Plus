// SPDX-License-Identifier: LGPL-3.0-only
// Litematica "Save from schematic world" over the loaded placements, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.world;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.lunatrius.schematica.nbt.NBTHelper;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.world.chunk.CaptureSource;

/** Blocks, block entities and entities of the enabled placements, the first placement holding a block winning. */
public final class PlacementCaptureSource implements CaptureSource {
    private final List<SchematicWorld> placements = new ArrayList<>();

    public PlacementCaptureSource(List<SchematicWorld> loaded) {
        for (SchematicWorld placement : loaded) if (placement.isEnabled()) placements.add(placement);
    }

    private SchematicWorld owner(int x, int y, int z) {
        for (SchematicWorld placement : placements) {
            int lx = x - placement.position.x, ly = y - placement.position.y, lz = z - placement.position.z;
            if (!placement.getSchematic().containsBlock(lx, ly, lz)) continue;
            Block block = placement.getBlock(lx, ly, lz);
            if (block != null && block != Blocks.air) return placement;
        }
        return null;
    }

    @Override
    public Block block(int x, int y, int z) {
        SchematicWorld placement = owner(x, y, z);
        return placement == null ? Blocks.air : placement.getBlock(x - placement.position.x, y - placement.position.y, z - placement.position.z);
    }

    @Override
    public int meta(int x, int y, int z) {
        SchematicWorld placement = owner(x, y, z);
        return placement == null ? 0 : placement.getBlockMetadata(x - placement.position.x, y - placement.position.y, z - placement.position.z);
    }

    @Override
    public TileEntity tile(int x, int y, int z, int offsetX, int offsetY, int offsetZ) throws Exception {
        SchematicWorld placement = owner(x, y, z);
        if (placement == null) return null;
        TileEntity tile = placement.getTileEntity(x - placement.position.x, y - placement.position.y, z - placement.position.z);
        return tile == null ? null : NBTHelper.reloadTileEntity(tile,
            offsetX - placement.position.x, offsetY - placement.position.y, offsetZ - placement.position.z);
    }

    @Override
    public List<Entity> entities(AxisAlignedBB box, int offsetX, int offsetY, int offsetZ) {
        List<Entity> copies = new ArrayList<>();
        for (SchematicWorld placement : placements) {
            for (Entity entity : placement.getSchematic().getEntities()) {
                double x = entity.posX + placement.position.x, y = entity.posY + placement.position.y, z = entity.posZ + placement.position.z;
                if (x < box.minX || x >= box.maxX || y < box.minY || y >= box.maxY || z < box.minZ || z >= box.maxZ) continue;
                try {
                    Entity copy = NBTHelper.reloadEntity(entity, offsetX - placement.position.x, offsetY - placement.position.y,
                        offsetZ - placement.position.z);
                    if (copy != null) copies.add(copy);
                } catch (Exception e) {
                    Reference.logger.error("Error while trying to save entity '{}'!", entity, e);
                }
            }
        }
        return copies;
    }

    @Override
    public boolean covers(int x, int y, int z, ForgeDirection side) {
        SchematicWorld placement = owner(x, y, z);
        if (placement == null) return false;
        int lx = x - placement.position.x, ly = y - placement.position.y, lz = z - placement.position.z;
        Block block = placement.getBlock(lx, ly, lz);
        return block.isOpaqueCube() && block.isSideSolid(placement, lx, ly, lz, side);
    }
}

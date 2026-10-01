// SPDX-License-Identifier: LGPL-3.0-only
// Block source of a normal save, by HackerRouter, 2026.
package com.github.lunatrius.schematica.world.chunk;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.lunatrius.schematica.nbt.NBTHelper;

/** The world being saved; block entities come from the server world when there is one (full inventories). */
public final class WorldCaptureSource implements CaptureSource {
    private final World world, serverWorld;

    public WorldCaptureSource(World world, World serverWorld) {
        this.world = world;
        this.serverWorld = serverWorld;
    }

    @Override public Block block(int x, int y, int z) { return world.getBlock(x, y, z); }

    @Override public int meta(int x, int y, int z) { return world.getBlockMetadata(x, y, z); }

    @Override
    public TileEntity tile(int x, int y, int z, int offsetX, int offsetY, int offsetZ) throws Exception {
        TileEntity tile = serverWorld.getTileEntity(x, y, z);
        if (tile == null) tile = world.getTileEntity(x, y, z);
        return tile == null ? null : NBTHelper.reloadTileEntity(tile, offsetX, offsetY, offsetZ);
    }

    @Override
    public List<Entity> entities(AxisAlignedBB box, int offsetX, int offsetY, int offsetZ) {
        List<Entity> copies = new ArrayList<>();
        for (Object entity : world.getEntitiesWithinAABB(Entity.class, box)) {
            try {
                Entity copy = NBTHelper.reloadEntity((Entity) entity, offsetX, offsetY, offsetZ);
                if (copy != null) copies.add(copy);
            } catch (Exception e) {
                com.github.lunatrius.schematica.reference.Reference.logger.error("Error while trying to save entity '{}'!", entity, e);
            }
        }
        return copies;
    }

    @Override
    public boolean covers(int x, int y, int z, ForgeDirection side) {
        if (y < 0 || y > 255) return false;
        Block block = world.getBlock(x, y, z);
        return block.isOpaqueCube() && block.isSideSolid(world, x, y, z, side);
    }
}

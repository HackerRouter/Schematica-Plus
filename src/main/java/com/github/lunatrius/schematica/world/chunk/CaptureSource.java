// SPDX-License-Identifier: LGPL-3.0-only
// Litematica SchematicSaveInfo sources and visibility rules, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.world.chunk;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockCarpet;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.block.BlockSnow;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.util.ForgeDirection;

/** Where a save reads blocks from: the world, or the schematic placements ("Save from schematic world"). */
public interface CaptureSource {
    Block block(int x, int y, int z);

    int meta(int x, int y, int z);

    /** A copy of the block entity at the position, moved by minus the offset, or null. */
    TileEntity tile(int x, int y, int z, int offsetX, int offsetY, int offsetZ) throws Exception;

    /** Copies of the entities inside the box, moved by minus the offset. */
    List<Entity> entities(AxisAlignedBB box, int offsetX, int offsetY, int offsetZ);

    /** Whether the block hides the given face of its neighbour: opaque with a solid side towards it. */
    boolean covers(int x, int y, int z, ForgeDirection side);

    /** LitematicaSchematic.isExposed: some neighbour does not fully cover the block. */
    static boolean exposed(CaptureSource source, int x, int y, int z) {
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            if (!source.covers(x + direction.offsetX, y + direction.offsetY, z + direction.offsetZ, direction.getOpposite())) return true;
        }
        return false;
    }

    static boolean gravity(Block block) { return block instanceof BlockFalling; }

    /** Repeaters, comparators, snow layers and carpets, which need a block below without falling. */
    static boolean needsSupport(Block block) {
        return block instanceof BlockRedstoneDiode || block instanceof BlockSnow || block instanceof BlockCarpet;
    }

    /** LitematicaSchematic.isSupport: the block holds up a visible block that needs support. */
    static boolean support(CaptureSource source, int x, int y, int z) {
        Block above = source.block(x, y + 1, z);
        if (needsSupport(above)) return true;
        return gravity(above) && (exposed(source, x, y + 1, z) || supportsExposed(source, x, y + 1, z));
    }

    static boolean supportsExposed(CaptureSource source, int x, int y, int z) {
        for (int up = y + 1; up < 256; up++) {
            Block block = source.block(x, up, z);
            if (needsSupport(block)) return true;
            if (!gravity(block)) return false;
            if (exposed(source, x, up, z)) return true;
        }
        return false;
    }
}

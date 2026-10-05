// Supports before the blocks that hang on them, after the behavior of Buildprint's "build prerequisites first", by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.printer;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;

/**
 * easyPlacePrerequisites: when the aimed schematic block cannot stand yet (a torch, rail or plant without the block
 * it needs, sand over nothing), Easy Place builds its missing support from the schematic instead, the block below
 * first. One step per click: the next click builds the aimed block or the next support.
 */
public final class Prerequisites {
    private static final ForgeDirection[] ORDER = {ForgeDirection.DOWN, ForgeDirection.NORTH, ForgeDirection.EAST,
        ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.UP};

    private Prerequisites() {}

    /** The direction to the support to build first, or null when the aimed block can be built (or nothing helps). */
    public static ForgeDirection support(World world, SchematicWorld schematic, int lx, int ly, int lz, int x, int y, int z) {
        if (!ConfigurationHandler.easyPlacePrerequisites) return null;
        Block block = schematic.getBlock(lx, ly, lz);
        if (block.isAir(schematic, lx, ly, lz) || !world.getBlock(x, y, z).isReplaceable(world, x, y, z)) return null;
        boolean falls = block instanceof BlockFalling && BlockFalling.func_149831_e(world, x, y - 1, z);
        if (!falls && block.canPlaceBlockAt(world, x, y, z)) return null;
        for (ForgeDirection side : ORDER) {
            if (falls && side != ForgeDirection.DOWN) break;
            int sx = lx + side.offsetX, sy = ly + side.offsetY, sz = lz + side.offsetZ;
            int wx = x + side.offsetX, wy = y + side.offsetY, wz = z + side.offsetZ;
            if (!schematic.getSchematic().containsBlock(sx, sy, sz) || !schematic.isBlockRendered(sx, sy, sz)) continue;
            Block needed = schematic.getBlock(sx, sy, sz);
            if (needed.isAir(schematic, sx, sy, sz) || FluidPrinter.isFluid(needed)) continue;
            if (!world.getBlock(wx, wy, wz).isReplaceable(world, wx, wy, wz)) continue;
            if (SchematicPrinter.INSTANCE.getSolidSides(world, wx, wy, wz).length == 0 && !ConfigurationHandler.placeInAir) continue;
            return side;
        }
        return null;
    }
}

// SPDX-License-Identifier: LGPL-3.0-only
// Litematica EasyPlaceUtils (the 1.12.2 "Post-Rewrite" Easy Place click positions), adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.printer;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.lunatrius.schematica.client.printer.registry.PlacementRegistry;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.tool.SchematicTargets;

/**
 * easyPlacePostRewrite: slabs get a click that makes the needed half without merging into a neighbouring slab, double
 * slabs are placed as two halves, and with easyPlaceClickAdjacent other blocks are placed against an existing block.
 * Directional blocks keep the placement registry's clicks; null means the regular click.
 */
final class PostRewrite {
    private PostRewrite() {}

    static EasyPlace.Click click(World world, int x, int y, int z, Block block, int meta, Block real, int realMeta, ItemStack stack, Vec3 hit) {
        if (block instanceof BlockSlab) return slab(world, x, y, z, block, meta, real, realMeta, stack, hit);
        if (!ConfigurationHandler.easyPlaceClickAdjacent || PlacementRegistry.INSTANCE.getPlacementData(block, stack) != null) return null;
        EasyPlace.Click adjacent = adjacent(world, x, y, z);
        return adjacent == null ? EasyPlace.Click.FAIL : adjacent;
    }

    private static boolean replaceable(World world, int x, int y, int z) {
        return world.getBlock(x, y, z).isReplaceable(world, x, y, z);
    }

    /** A single slab of the same kind as the schematic slab (the variant, ignoring the half and single/double). */
    private static boolean sameSingleSlab(Block block, int meta, Block other, int otherMeta, ItemStack stack) {
        return other instanceof BlockSlab && !other.isOpaqueCube()
            && net.minecraft.item.Item.getItemFromBlock(other) == stack.getItem() && (otherMeta & 7) == (meta & 7);
    }

    /** getAdjacentClickPosition: the real block behind the target, else any solid neighbour. */
    static EasyPlace.Click adjacent(World world, int x, int y, int z) {
        MovingObjectPosition vanilla = SchematicTargets.vanilla(SchematicTargets.validBlockRange(), false);
        if (vanilla != null && vanilla.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && vanilla.hitVec != null
            && !replaceable(world, vanilla.blockX, vanilla.blockY, vanilla.blockZ)) {
            ForgeDirection face = ForgeDirection.getOrientation(vanilla.sideHit);
            if (vanilla.blockX + face.offsetX == x && vanilla.blockY + face.offsetY == y && vanilla.blockZ + face.offsetZ == z) {
                return new EasyPlace.Click(vanilla.blockX, vanilla.blockY, vanilla.blockZ, vanilla.sideHit, vanilla.hitVec);
            }
        }
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            int sx = x + side.offsetX, sy = y + side.offsetY, sz = z + side.offsetZ;
            if (!replaceable(world, sx, sy, sz)) return new EasyPlace.Click(sx, sy, sz, side.getOpposite().ordinal(), faceCenter(sx, sy, sz, side));
        }
        return null;
    }

    /** getHitPositionForSidePosition: the middle of the neighbour's face toward the target. */
    private static Vec3 faceCenter(int sx, int sy, int sz, ForgeDirection fromTarget) {
        double hx = sx + 0.5 - fromTarget.offsetX * 0.5;
        double hy = sy + (fromTarget == ForgeDirection.DOWN ? 1.0 : fromTarget == ForgeDirection.UP ? 0.0 : 0.5);
        double hz = sz + 0.5 - fromTarget.offsetZ * 0.5;
        return Vec3.createVectorHelper(hx, hy, hz);
    }

    /** getClickPositionForSlab */
    private static EasyPlace.Click slab(World world, int x, int y, int z, Block block, int meta, Block real, int realMeta, ItemStack stack, Vec3 hit) {
        if (block.isOpaqueCube()) {
            if (sameSingleSlab(block, meta, real, realMeta, stack)) {
                boolean top = (realMeta & 8) != 0;
                return new EasyPlace.Click(x, y, z, top ? 0 : 1, Vec3.createVectorHelper(hit.xCoord, y + 0.5, hit.zCoord));
            }
            if (replaceable(world, x, y, z)) {
                EasyPlace.Click bottom = half(world, x, y, z, meta, false, stack, hit);
                if (bottom != null) return bottom;
                EasyPlace.Click top = half(world, x, y, z, meta, true, stack, hit);
                return top == null ? EasyPlace.Click.FAIL : top;
            }
            return EasyPlace.Click.FAIL;
        }
        if (!replaceable(world, x, y, z)) return EasyPlace.Click.FAIL;
        EasyPlace.Click click = half(world, x, y, z, meta, (meta & 8) != 0, stack, hit);
        return click == null ? EasyPlace.Click.FAIL : click;
    }

    /** getClickPositionForSlabHalf: clicking the target itself unless that would complete a slab above or below it. */
    private static EasyPlace.Click half(World world, int x, int y, int z, int meta, boolean top, ItemStack stack, Vec3 hit) {
        ForgeDirection clickSide = top ? ForgeDirection.DOWN : ForgeDirection.UP;
        if (!ConfigurationHandler.easyPlaceClickAdjacent) {
            Block target = world.getBlock(x, y, z);
            if (target.isReplaceable(world, x, y, z) && !target.getMaterial().isLiquid()) {
                int ox = x + clickSide.offsetX, oy = y + clickSide.offsetY, oz = z + clickSide.offsetZ;
                if (!sameSingleSlab(null, meta, world.getBlock(ox, oy, oz), world.getBlockMetadata(ox, oy, oz), stack)) {
                    return new EasyPlace.Click(x, y, z, clickSide.ordinal(), Vec3.createVectorHelper(hit.xCoord, y + 0.5, hit.zCoord));
                }
            } else if (target.getMaterial().isLiquid() && canClickNeighbour(world, x, y, z, meta, top, clickSide.getOpposite(), stack)) {
                ForgeDirection opposite = clickSide.getOpposite();
                int px = x + opposite.offsetX, py = y + opposite.offsetY, pz = z + opposite.offsetZ;
                return new EasyPlace.Click(px, py, pz, clickSide.ordinal(), Vec3.createVectorHelper(px + 0.5, py + 0.5, pz + 0.5));
            }
        }
        ForgeDirection opposite = clickSide.getOpposite();
        if (canClickNeighbour(world, x, y, z, meta, top, opposite, stack)) {
            int px = x + opposite.offsetX, py = y + opposite.offsetY, pz = z + opposite.offsetZ;
            return new EasyPlace.Click(px, py, pz, clickSide.ordinal(), faceCenter(px, py, pz, opposite));
        }
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            if (side.offsetY != 0 || !canClickNeighbour(world, x, y, z, meta, top, side, stack)) continue;
            int px = x + side.offsetX, py = y, pz = z + side.offsetZ;
            Vec3 face = faceCenter(px, py, pz, side);
            return new EasyPlace.Click(px, py, pz, side.getOpposite().ordinal(), Vec3.createVectorHelper(face.xCoord, py + (top ? 0.9 : 0.1), face.zCoord));
        }
        return null;
    }

    /** canClickOnAdjacentBlockToPlaceSingleSlabAt */
    private static boolean canClickNeighbour(World world, int x, int y, int z, int meta, boolean top, ForgeDirection side, ItemStack stack) {
        int px = x + side.offsetX, py = y + side.offsetY, pz = z + side.offsetZ;
        if (replaceable(world, px, py, pz)) return false;
        if (side.offsetY == 0) return true;
        Block neighbour = world.getBlock(px, py, pz);
        int neighbourMeta = world.getBlockMetadata(px, py, pz);
        return !sameSingleSlab(null, meta, neighbour, neighbourMeta, stack) || ((neighbourMeta & 8) != 0) != top;
    }
}

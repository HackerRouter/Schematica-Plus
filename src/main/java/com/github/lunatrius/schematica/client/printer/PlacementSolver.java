// Placement by simulation after the behavior of Buildprint's placement solver, by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.printer;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.lunatrius.schematica.client.world.CellState;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.util.BlockGroups;

/**
 * For blocks the placement registry does not know: every click the player could make (a face of a solid neighbor,
 * or the target itself when placing in the air, at a few heights, with the current or one of the six straight
 * rotations) is first simulated in a scratch cell that sees the real neighbors. The first click whose block gets
 * the schematic's metadata is used. When the clicks give different metadata and none is right, nothing is placed.
 */
public final class PlacementSolver {
    public static final class Solution {
        public final int x, y, z, side;
        public final Vec3 hit;
        /** The rotation to report before the click; null keeps the player's. */
        public final PrinterLook look;
        public final boolean neighbor;

        Solution(int x, int y, int z, int side, Vec3 hit, PrinterLook look, boolean neighbor) {
            this.x = x; this.y = y; this.z = z; this.side = side; this.hit = hit; this.look = look; this.neighbor = neighbor;
        }
    }

    /** No click gives the schematic state. */
    public static final Solution REFUSE = new Solution(0, 0, 0, 0, null, null, false);

    private static final float[] SIDE_HEIGHTS = {0.5f, 0.25f, 0.75f};

    private PlacementSolver() {}

    /** A solution, REFUSE, or null when the clicks do not decide the metadata (or cannot be simulated). */
    public static Solution solve(World world, EntityPlayer player, ItemStack stack, Block block, int meta, int x, int y, int z,
        ForgeDirection[] solidSides, boolean inAir) {
        if (!(stack.getItem() instanceof ItemBlock) || ((ItemBlock) stack.getItem()).field_150939_a != block) return null;
        ItemBlock item = (ItemBlock) stack.getItem();
        int mask = ~BlockGroups.survivalBits(block) & 15;
        float yaw = player.rotationYaw, pitch = player.rotationPitch;
        PrinterLook[] rotations = PrinterLook.candidates(yaw);
        PrinterLook[] looks = new PrinterLook[rotations.length + 1];
        System.arraycopy(rotations, 0, looks, 1, rotations.length);
        CellState.Scratch scratch = CellState.Scratch.around(world, x, y, z);
        Set<Integer> results = new HashSet<>();
        try {
            for (ForgeDirection direction : solidSides) {
                int sx = x + direction.offsetX, sy = y + direction.offsetY, sz = z + direction.offsetZ;
                int face = direction.getOpposite().ordinal();
                Solution found = probe(scratch, player, item, stack, block, meta, mask, looks, x, y, z, sx, sy, sz, face, true, results);
                if (found != null) return found;
            }
            if (inAir) {
                for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
                    Solution found = probe(scratch, player, item, stack, block, meta, mask, looks, x, y, z, x, y, z, direction.ordinal(), false, results);
                    if (found != null) return found;
                }
            }
        } catch (RuntimeException | LinkageError error) {
            Reference.logger.debug("Could not simulate placing {}", block, error);
            return null;
        } finally {
            player.rotationYaw = yaw;
            player.rotationPitch = pitch;
        }
        return results.size() > 1 ? REFUSE : null;
    }

    private static Solution probe(CellState.Scratch scratch, EntityPlayer player, ItemBlock item, ItemStack stack, Block block, int meta, int mask,
        PrinterLook[] looks, int x, int y, int z, int sx, int sy, int sz, int face, boolean neighbor, Set<Integer> results) {
        ForgeDirection side = ForgeDirection.getOrientation(face);
        float[] heights = side.offsetY == 0 ? SIDE_HEIGHTS : new float[] {side == ForgeDirection.UP ? 1f : 0f};
        float yaw = player.rotationYaw, pitch = player.rotationPitch;
        for (float height : heights) {
            float hx = side.offsetX == 0 ? 0.5f : side.offsetX > 0 ? 1f : 0f;
            float hz = side.offsetZ == 0 ? 0.5f : side.offsetZ > 0 ? 1f : 0f;
            for (PrinterLook look : looks) {
                player.rotationYaw = look == null ? yaw : look.yaw;
                player.rotationPitch = look == null ? pitch : look.pitch;
                CellState placed = place(scratch, player, item, stack, face, hx, height, hz, x, y, z);
                player.rotationYaw = yaw;
                player.rotationPitch = pitch;
                if (placed.block != block) continue;
                results.add(placed.meta & mask);
                if ((placed.meta & mask) == (meta & mask)) {
                    return new Solution(sx, sy, sz, face, Vec3.createVectorHelper(sx + hx, sy + height, sz + hz), look, neighbor);
                }
            }
        }
        return null;
    }

    /** ItemBlock.onItemUse after the position checks: onBlockPlaced, then placeBlockAt with the player moved next to the cell. */
    static CellState place(CellState.Scratch scratch, EntityPlayer player, ItemBlock item, ItemStack stack, int side, float hx, float hy, float hz,
        int x, int y, int z) {
        scratch.removeTileEntity(0, 0, 0);
        scratch.setBlock(0, 0, 0, Blocks.air, 0, 3);
        double px = player.posX, py = player.posY, pz = player.posZ;
        try {
            int meta = item.field_150939_a.onBlockPlaced(scratch, 0, 0, 0, side, hx, hy, hz, item.getMetadata(stack.getItemDamage()));
            player.posX = px - x; player.posY = py - y; player.posZ = pz - z;
            if (!item.placeBlockAt(stack.copy(), player, scratch, 0, 0, 0, side, hx, hy, hz, meta)) return CellState.of(item.field_150939_a, meta);
            Block placed = scratch.getBlock(0, 0, 0);
            return placed == Blocks.air ? CellState.of(item.field_150939_a, meta) : CellState.of(placed, scratch.getBlockMetadata(0, 0, 0));
        } finally {
            player.posX = px; player.posY = py; player.posZ = pz;
        }
    }
}

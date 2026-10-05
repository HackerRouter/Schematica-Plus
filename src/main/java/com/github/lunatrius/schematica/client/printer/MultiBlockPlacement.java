// Two-block items (doors, beds, double plants) for the printer and Easy Place, by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.printer;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.util.MathHelper;

/**
 * Doors, beds and double plants are placed by one item use that also sets their second block (the upper half, the
 * head). Only the first block is placed, by clicking the top of the block below it like a player does; the item
 * takes its facing from the player's yaw.
 */
public final class MultiBlockPlacement {
    public enum Kind { DOOR, BED, DOUBLE_PLANT }

    private MultiBlockPlacement() {}

    public static Kind kind(Block block) {
        if (block instanceof BlockDoor) return Kind.DOOR;
        if (block instanceof BlockBed) return Kind.BED;
        if (block instanceof BlockDoublePlant) return Kind.DOUBLE_PLANT;
        return null;
    }

    /** The upper half of a door or plant, or the head of a bed: set by placing the other block. */
    public static boolean secondary(int meta) { return (meta & 8) != 0; }

    /** The offset from the first block to the second one. */
    public static int[] secondOffset(Kind kind, int meta) {
        if (kind == Kind.BED) {
            int[] direction = BlockBed.field_149981_a[meta & 3];
            return new int[] {direction[0], 0, direction[1]};
        }
        return new int[] {0, 1, 0};
    }

    /** The facing the item gives for a player yaw (ItemDoor, ItemBed, BlockDoublePlant.onBlockPlacedBy). */
    public static int facing(Kind kind, float yaw) {
        switch (kind) {
            case DOOR: return MathHelper.floor_double((yaw + 180.0F) * 4.0F / 360.0F - 0.5D) & 3;
            case BED: return MathHelper.floor_double(yaw * 4.0F / 360.0F + 0.5D) & 3;
            default: return ((MathHelper.floor_double(yaw * 4.0F / 360.0F + 0.5D) & 3) + 2) % 4;
        }
    }

    /** The facing the schematic wants: of the first block, for plants of the upper half (sunflowers); -1 for any. */
    public static int wantedFacing(Kind kind, Block block, int meta, Block second, int secondMeta) {
        if (kind != Kind.DOUBLE_PLANT) return meta & 3;
        return second == block && secondary(secondMeta) ? secondMeta & 3 : -1;
    }

    /** One of the four horizontal yaws giving the facing, the one nearest the current yaw first. */
    public static PrinterLook look(Kind kind, float currentYaw, int facing) {
        for (PrinterLook candidate : PrinterLook.candidates(currentYaw)) {
            if (candidate.pitch == 0 && facing(kind, candidate.yaw) == facing) return candidate;
        }
        return null;
    }

    /**
     * The metadata bits that count when a placed block is compared with the schematic: a door's open bit and hinge
     * and a bed's occupied bit cannot be chosen by a placement unless the server applies them (accurate placement).
     */
    public static int stateMask(Kind kind, int meta, boolean accurate) {
        switch (kind) {
            case DOOR: return secondary(meta) ? (accurate ? 0x9 : 0x8) : (accurate ? 0xF : 0xB);
            case BED: return 0xB;
            default: return 0xF;
        }
    }
}

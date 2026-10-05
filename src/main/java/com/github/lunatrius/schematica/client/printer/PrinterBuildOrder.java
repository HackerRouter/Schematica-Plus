// Printer build order after the behavior of Buildprint's printer, by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.printer;

import java.util.Comparator;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBasePressurePlate;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockCarpet;
import net.minecraft.block.BlockCocoa;
import net.minecraft.block.BlockFlowerPot;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.BlockReed;
import net.minecraft.block.BlockSign;
import net.minecraft.block.BlockSkull;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.BlockTripWire;
import net.minecraft.block.BlockTripWireHook;
import net.minecraft.block.BlockVine;

/**
 * The "layers" build order: the lowest layer in reach first; in a layer full blocks, then other blocks, then
 * torches, rails, plants and other blocks that hang on a neighbor; the nearest first. Positions around the
 * player's body come last so that the printer does not wall the player in.
 */
public final class PrinterBuildOrder {
    public static final String[] MODES = {"layers", "iterator"};
    public static final int STRUCTURE = 0, OTHER = 1, ATTACHED = 2;

    private PrinterBuildOrder() {}

    /** A position the printer may work on. */
    static final class Candidate {
        final int x, y, z, tier;
        final Object placement;
        Candidate(int x, int y, int z, int tier, Object placement) { this.x = x; this.y = y; this.z = z; this.tier = tier; this.placement = placement; }
    }

    public static int tier(Block block) {
        if (attached(block)) return ATTACHED;
        return block.isNormalCube() ? STRUCTURE : OTHER;
    }

    static boolean attached(Block block) {
        return block instanceof BlockTorch || block instanceof BlockBush || block instanceof BlockVine || block instanceof BlockReed
            || block instanceof BlockCocoa || block instanceof BlockRedstoneWire || block instanceof BlockRedstoneDiode
            || block instanceof BlockButton || block instanceof BlockLever || block instanceof BlockBasePressurePlate
            || block instanceof BlockRailBase || block instanceof BlockTripWire || block instanceof BlockTripWireHook
            || block instanceof BlockCarpet || block instanceof BlockSnow || block instanceof BlockSign || block instanceof BlockSkull
            || block instanceof BlockLadder || block instanceof BlockFlowerPot;
    }

    /** Whether a block at the position would stand in the player's feet or head column or right beside it. */
    static boolean around(Candidate c, double x, double z, double feetY) {
        int feet = (int) Math.floor(feetY), head = (int) Math.floor(feetY + 1.7);
        return c.y >= feet && c.y <= head && Math.abs(c.x + 0.5 - x) < 1.5 && Math.abs(c.z + 0.5 - z) < 1.5;
    }

    static Comparator<Candidate> order(double eyeX, double eyeY, double eyeZ, double feetY, boolean downward) {
        return Comparator.<Candidate>comparingInt(c -> around(c, eyeX, eyeZ, feetY) ? 1 : 0)
            .thenComparingInt(c -> downward ? -c.y : c.y)
            .thenComparingInt(c -> c.tier)
            .thenComparingDouble(c -> {
                double dx = c.x + 0.5 - eyeX, dy = c.y + 0.5 - eyeY, dz = c.z + 0.5 - eyeZ;
                return dx * dx + dy * dy + dz * dz;
            })
            .thenComparingInt(c -> c.x)
            .thenComparingInt(c -> c.z);
    }
}

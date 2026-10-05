// SPDX-License-Identifier: LGPL-3.0-only
// MaLiLib BlockUtils.isInSameGroup / matchPropertiesOnly (replaceable block groups), adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.util;

import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAnvil;
import net.minecraft.block.BlockBasePressurePlate;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockCactus;
import net.minecraft.block.BlockCarpet;
import net.minecraft.block.BlockCocoa;
import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockDaylightDetector;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFarmland;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockFire;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockFlowerPot;
import net.minecraft.block.BlockFurnace;
import net.minecraft.block.BlockGlass;
import net.minecraft.block.BlockHardenedClay;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockNetherWart;
import net.minecraft.block.BlockOre;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockRailDetector;
import net.minecraft.block.BlockRailPowered;
import net.minecraft.block.BlockRedstoneComparator;
import net.minecraft.block.BlockRedstoneLight;
import net.minecraft.block.BlockRedstoneOre;
import net.minecraft.block.BlockRedstoneRepeater;
import net.minecraft.block.BlockRedstoneTorch;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.BlockReed;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.BlockSign;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStainedGlass;
import net.minecraft.block.BlockStainedGlassPane;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockStem;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.BlockTripWire;
import net.minecraft.block.BlockTripWireHook;
import net.minecraft.block.BlockWall;
import net.minecraft.block.BlockWood;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;

import cpw.mods.fml.common.registry.GameData;

/**
 * Different blocks (enableDifferentBlocks): two blocks of one replaceable group, such as two kinds of stairs, glass or
 * wool, with the same state. In 1.7.10 colors and wood types are metadata of one block, so the metadata bits that change
 * the dropped item (the variant) are left out of the state; within a group a different variant is a different block.
 */
public final class BlockGroups {
    public enum Result { SAME, DIFFERENT_BLOCK, WRONG_STATE, WRONG_BLOCK }

    /** enableDifferentBlocks */
    public static volatile boolean enabled;

    private static final Map<Block, String> GROUPS = new IdentityHashMap<>();
    private static final Map<Block, Integer> VARIANT_BITS = new IdentityHashMap<>();
    private static final String NONE = "";

    private BlockGroups() {}

    /** The replaceable group of a block, or null. */
    public static synchronized String group(Block block) {
        if (block == null) return null;
        String group = GROUPS.get(block);
        if (group == null) {
            group = findGroup(block);
            if (group == null) group = NONE;
            GROUPS.put(block, group);
        }
        return group.isEmpty() ? null : group;
    }

    private static String findGroup(Block block) {
        if (block instanceof BlockStairs) return "stairs";
        if (block instanceof BlockSlab) return block.isOpaqueCube() ? "double_slabs" : "slabs";
        if (block instanceof BlockWall) return "walls";
        if (block instanceof BlockFenceGate) return "fence_gates";
        if (block instanceof BlockFence) return "fences";
        if (block instanceof BlockDoor) return "doors";
        if (block instanceof BlockTrapDoor) return "trapdoors";
        if (block instanceof BlockButton) return "buttons";
        if (block instanceof BlockBasePressurePlate) return "pressure_plates";
        if (block instanceof BlockSign) return block == Blocks.wall_sign ? "wall_signs" : "standing_signs";
        if (block instanceof BlockLog) return "logs";
        if (block instanceof BlockLeaves) return "leaves";
        if (block instanceof BlockWood) return "planks";
        if (block instanceof BlockSapling) return "saplings";
        if (block instanceof BlockFlower) return "flowers";
        if (block instanceof BlockFlowerPot) return "flower_pots";
        if (block instanceof BlockAnvil) return "anvil";
        if (block instanceof BlockBed) return "beds";
        if (block instanceof BlockCarpet) return "wool_carpets";
        if (block instanceof BlockGlass || block instanceof BlockStainedGlass) return "glass";
        if (block instanceof BlockStainedGlassPane || block instanceof BlockPane && block.getMaterial() == Material.glass) return "glass_panes";
        if (block instanceof BlockHardenedClay) return "terracotta";
        if (block instanceof BlockColored) return block.getMaterial() == Material.cloth ? "wool" : "terracotta";
        if (block instanceof BlockOre || block instanceof BlockRedstoneOre) return "ores";
        return null;
    }

    /** The metadata bits that select the variant (the dropped item's damage), such as the color of wool. */
    public static synchronized int variantBits(Block block) {
        Integer bits = VARIANT_BITS.get(block);
        if (bits == null) {
            int mask = 0;
            try {
                for (int meta = 0; meta < 16; meta++) {
                    for (int bit = 1; bit < 16; bit <<= 1) {
                        if (block.damageDropped(meta) != block.damageDropped(meta ^ bit)) mask |= bit;
                    }
                }
            } catch (RuntimeException ignored) {
                mask = 0;
            }
            VARIANT_BITS.put(block, bits = mask);
        }
        return bits;
    }

    /** The metadata bits of a block that hold its growth stage (Litematica's "age" property). */
    public static int ageBits(Block block) {
        if (block instanceof BlockCrops || block instanceof BlockStem) return 0x7;
        if (block instanceof BlockNetherWart) return 0x3;
        if (block instanceof BlockCocoa) return 0xC;
        if (block instanceof BlockCactus || block instanceof BlockReed || block instanceof BlockFire) return 0xF;
        return 0;
    }

    private static final Class<?>[] SWITCHED = {BlockFurnace.class, BlockRedstoneLight.class, BlockRedstoneTorch.class,
        BlockRedstoneRepeater.class, BlockRedstoneComparator.class, BlockRedstoneOre.class};

    /**
     * The metadata bits a player cannot choose in survival or that follow from the surroundings: leaf decay bits,
     * sapling growth, the power of buttons, plates, detector and powered rails, daylight sensors, comparators and
     * redstone wire, extended pistons, triggered dispensers, tripwire and hooks, occupied beds, fluid levels,
     * farmland moisture and hoppers disabled by redstone. Growth stages count too.
     */
    public static int survivalBits(Block block) {
        int bits = ageBits(block);
        if (block instanceof BlockLeaves) bits |= 0xC;
        else if (block instanceof BlockSapling || block instanceof BlockButton || block instanceof BlockRailDetector || block instanceof BlockRailPowered
            || block instanceof BlockPistonBase || block instanceof BlockDispenser || block instanceof BlockHopper || block instanceof BlockRedstoneComparator) bits |= 0x8;
        else if (block instanceof BlockBasePressurePlate || block instanceof BlockDaylightDetector || block instanceof BlockLiquid
            || block instanceof BlockRedstoneWire) bits |= 0xF;
        else if (block instanceof BlockTripWire) bits |= 0xD;
        else if (block instanceof BlockTripWireHook) bits |= 0xC;
        else if (block instanceof BlockBed) bits |= 0x4;
        else if (block instanceof BlockFarmland) bits |= 0x7;
        return bits;
    }

    /** The lit and unlit (powered and unpowered) blocks of vanilla furnaces, lamps, torches, repeaters, comparators and redstone ore. */
    static boolean switchedPair(Block a, Block b) {
        if (a.getClass() != b.getClass()) return false;
        for (Class<?> type : SWITCHED) if (type == a.getClass()) return true;
        return false;
    }

    /** ignoreSurvivalStates: the same block, or its lit/powered twin, differing at most in its survival bits. */
    public static boolean sameInSurvival(Block expected, int expectedMeta, Block found, int foundMeta) {
        if (expected != found && !switchedPair(expected, found)) return false;
        int bits = survivalBits(expected);
        return (expectedMeta & ~bits) == (foundMeta & ~bits);
    }

    /** Whether the world block counts as the schematic block under ignoreCropAge and ignoreSurvivalStates. */
    public static boolean tolerated(Block expected, int expectedMeta, Block found, int foundMeta) {
        return com.github.lunatrius.schematica.handler.VisualSettings.ignoreSurvivalStates && sameInSurvival(expected, expectedMeta, found, foundMeta)
            || com.github.lunatrius.schematica.handler.VisualSettings.ignoreCropAge && sameIgnoringAge(expected, expectedMeta, found, foundMeta);
    }

    /** ignoreCropAge (BlockUtils.areStatesEqualIgnoringAge): the same block, differing at most in its age. */
    public static boolean sameIgnoringAge(Block expected, int expectedMeta, Block found, int foundMeta) {
        if (expected != found) return false;
        int age = ageBits(expected);
        return (expectedMeta & ~age) == (foundMeta & ~age);
    }

    /** Compares a schematic block with the block in the world; both are present and not air. */
    public static Result compare(Block expected, int expectedMeta, Block found, int foundMeta, boolean differentBlocks) {
        if (expected == found && expectedMeta == foundMeta) return Result.SAME;
        if (tolerated(expected, expectedMeta, found, foundMeta)) return Result.SAME;
        String group = differentBlocks ? group(expected) : null;
        boolean sameGroup = group != null && group.equals(group(found));
        if (expected != found && !sameGroup) return Result.WRONG_BLOCK;
        if (!sameGroup) return Result.WRONG_STATE;
        int expectedVariant = variantBits(expected), foundVariant = variantBits(found);
        if (com.github.lunatrius.schematica.handler.VisualSettings.ignoreSurvivalStates) {
            expectedVariant |= survivalBits(expected);
            foundVariant |= survivalBits(found);
        }
        boolean sameState = (expectedMeta & ~expectedVariant) == (foundMeta & ~foundVariant);
        return sameState ? Result.DIFFERENT_BLOCK : Result.WRONG_STATE;
    }

    public static Result compare(String expected, int expectedMeta, String found, int foundMeta, boolean differentBlocks) {
        if (!differentBlocks) {
            if (!expected.equals(found)) return Result.WRONG_BLOCK;
            if (expectedMeta == foundMeta) return Result.SAME;
            Block block = block(expected);
            return block != null && tolerated(block, expectedMeta, block, foundMeta) ? Result.SAME : Result.WRONG_STATE;
        }
        Block a = block(expected), b = block(found);
        if (a == null || b == null) return compare(expected, expectedMeta, found, foundMeta, false);
        return compare(a, expectedMeta, b, foundMeta, true);
    }

    private static Block block(String name) {
        try {
            if (!GameData.getBlockRegistry().containsKey(name)) return null;
            return GameData.getBlockRegistry().getObject(name);
        } catch (RuntimeException error) {
            return null;
        }
    }
}

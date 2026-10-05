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
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFire;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockFlowerPot;
import net.minecraft.block.BlockGlass;
import net.minecraft.block.BlockHardenedClay;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockNetherWart;
import net.minecraft.block.BlockOre;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockReed;
import net.minecraft.block.BlockRedstoneOre;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.BlockSign;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStainedGlass;
import net.minecraft.block.BlockStainedGlassPane;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockStem;
import net.minecraft.block.BlockTrapDoor;
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

    /** ignoreCropAge (BlockUtils.areStatesEqualIgnoringAge): the same block, differing at most in its age. */
    public static boolean sameIgnoringAge(Block expected, int expectedMeta, Block found, int foundMeta) {
        if (expected != found) return false;
        int age = ageBits(expected);
        return (expectedMeta & ~age) == (foundMeta & ~age);
    }

    /** Compares a schematic block with the block in the world; both are present and not air. */
    public static Result compare(Block expected, int expectedMeta, Block found, int foundMeta, boolean differentBlocks) {
        if (expected == found && expectedMeta == foundMeta) return Result.SAME;
        if (com.github.lunatrius.schematica.handler.VisualSettings.ignoreCropAge && sameIgnoringAge(expected, expectedMeta, found, foundMeta)) return Result.SAME;
        String group = differentBlocks ? group(expected) : null;
        boolean sameGroup = group != null && group.equals(group(found));
        if (expected != found && !sameGroup) return Result.WRONG_BLOCK;
        if (!sameGroup) return Result.WRONG_STATE;
        int expectedVariant = variantBits(expected), foundVariant = variantBits(found);
        boolean sameState = (expectedMeta & ~expectedVariant) == (foundMeta & ~foundVariant);
        return sameState ? Result.DIFFERENT_BLOCK : Result.WRONG_STATE;
    }

    public static Result compare(String expected, int expectedMeta, String found, int foundMeta, boolean differentBlocks) {
        if (!differentBlocks) {
            if (!expected.equals(found)) return Result.WRONG_BLOCK;
            if (expectedMeta == foundMeta) return Result.SAME;
            Block block = com.github.lunatrius.schematica.handler.VisualSettings.ignoreCropAge ? block(expected) : null;
            return block != null && sameIgnoringAge(block, expectedMeta, block, foundMeta) ? Result.SAME : Result.WRONG_STATE;
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

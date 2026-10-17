package com.github.lunatrius.schematica.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockCocoa;
import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockOldLeaf;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPressurePlate;
import net.minecraft.block.BlockRedstoneOre;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.material.Material;

import com.github.lunatrius.schematica.handler.VisualSettings;

import org.junit.Test;

import static com.github.lunatrius.schematica.util.BlockGroups.Result.*;
import static org.junit.Assert.*;

public class BlockGroupsTest {
    private static final Block WOOL = new BlockColored(Material.cloth);
    private static final Block CLAY = new BlockColored(Material.rock);
    private static final Block LOG = new BlockOldLog();
    private static final Block PLAIN = new Block(Material.rock) {};
    private static final Block OAK_STAIRS = new BlockStairs(PLAIN, 0) {};
    private static final Block STONE_STAIRS = new BlockStairs(PLAIN, 0) {};

    @Test public void colorsAndWoodTypesAreDifferentBlocksOnlyWithTheSameState() {
        assertEquals(DIFFERENT_BLOCK, BlockGroups.compare(WOOL, 0, WOOL, 1, true));
        assertEquals(WRONG_STATE, BlockGroups.compare(WOOL, 0, WOOL, 1, false));
        assertEquals(DIFFERENT_BLOCK, BlockGroups.compare(LOG, 0, LOG, 1, true));
        assertEquals(WRONG_STATE, BlockGroups.compare(LOG, 0, LOG, 4, true));
        assertEquals(WRONG_STATE, BlockGroups.compare(LOG, 0, LOG, 5, true));
        assertEquals(SAME, BlockGroups.compare(LOG, 5, LOG, 5, true));
    }

    @Test public void otherBlocksOfOneGroupMustKeepTheirState() {
        assertEquals(DIFFERENT_BLOCK, BlockGroups.compare(OAK_STAIRS, 2, STONE_STAIRS, 2, true));
        assertEquals(WRONG_STATE, BlockGroups.compare(OAK_STAIRS, 2, STONE_STAIRS, 3, true));
        assertEquals(WRONG_BLOCK, BlockGroups.compare(OAK_STAIRS, 2, STONE_STAIRS, 2, false));
        assertEquals(WRONG_BLOCK, BlockGroups.compare(OAK_STAIRS, 0, WOOL, 0, true));
        assertEquals(WRONG_BLOCK, BlockGroups.compare(WOOL, 0, CLAY, 0, true));
        assertEquals(WRONG_STATE, BlockGroups.compare(PLAIN, 1, PLAIN, 2, true));
        assertEquals(15, BlockGroups.variantBits(WOOL));
        assertEquals(3, BlockGroups.variantBits(LOG));
        assertEquals(0, BlockGroups.variantBits(OAK_STAIRS));
    }

    @Test public void ignoreCropAgeOnlySkipsTheGrowthBits() {
        Block wheat = new BlockCrops() {};
        Block cocoa = new BlockCocoa() {};
        boolean previous = VisualSettings.ignoreCropAge;
        boolean survival = VisualSettings.ignoreSurvivalStates;
        try {
            VisualSettings.ignoreSurvivalStates = false;
            VisualSettings.ignoreCropAge = false;
            assertEquals(WRONG_STATE, BlockGroups.compare(wheat, 7, wheat, 2, false));
            VisualSettings.ignoreCropAge = true;
            assertEquals(SAME, BlockGroups.compare(wheat, 7, wheat, 2, false));
            assertEquals(SAME, BlockGroups.compare(cocoa, 0x9, cocoa, 0x1, true));
            assertEquals(WRONG_STATE, BlockGroups.compare(cocoa, 0x9, cocoa, 0x2, true));
            assertEquals(WRONG_STATE, BlockGroups.compare(PLAIN, 1, PLAIN, 2, true));
        } finally {
            VisualSettings.ignoreCropAge = previous;
            VisualSettings.ignoreSurvivalStates = survival;
        }
    }

    @Test public void survivalStatesAndLitTwinsAreTheSameBlock() {
        Block leaves = new BlockOldLeaf();
        Block plate = new BlockPressurePlate("stone", Material.rock, BlockPressurePlate.Sensitivity.mobs) {};
        Block ore = new BlockRedstoneOre(false), litOre = new BlockRedstoneOre(true);
        Block wheat = new BlockCrops() {};
        boolean survival = VisualSettings.ignoreSurvivalStates;
        try {
            VisualSettings.ignoreSurvivalStates = true;
            assertEquals(SAME, BlockGroups.compare(leaves, 0x1, leaves, 0x5, true));
            assertEquals(SAME, BlockGroups.compare(leaves, 0x9, leaves, 0x5, true));
            assertEquals(DIFFERENT_BLOCK, BlockGroups.compare(leaves, 0x1, leaves, 0x6, true));
            assertEquals(WRONG_STATE, BlockGroups.compare(leaves, 0x1, leaves, 0x6, false));
            assertEquals(SAME, BlockGroups.compare(plate, 0, plate, 1, false));
            assertEquals(SAME, BlockGroups.compare(ore, 0, litOre, 0, false));
            assertEquals(SAME, BlockGroups.compare(wheat, 7, wheat, 2, false));
            assertEquals(WRONG_BLOCK, BlockGroups.compare(ore, 0, PLAIN, 0, false));
            assertEquals(WRONG_STATE, BlockGroups.compare(PLAIN, 1, PLAIN, 2, true));
            VisualSettings.ignoreSurvivalStates = false;
            assertEquals(WRONG_STATE, BlockGroups.compare(leaves, 0x1, leaves, 0x5, true));
            assertEquals(WRONG_BLOCK, BlockGroups.compare(ore, 0, litOre, 0, false));
        } finally {
            VisualSettings.ignoreSurvivalStates = survival;
        }
    }
}

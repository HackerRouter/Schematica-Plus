package com.github.lunatrius.schematica.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.material.Material;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.github.lunatrius.schematica.util.BlockGroups.Result.*;

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
}

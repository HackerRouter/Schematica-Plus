package com.github.lunatrius.schematica.world.schematic;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockHugeMushroom;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockRail;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.BlockVine;
import net.minecraft.block.material.Material;
import org.junit.Test;

import static org.junit.Assert.*;

public class BlockMetaTransformTest {
    private static final Block PLAIN = new Block(Material.rock) {};
    private static final Block STAIRS = new BlockStairs(PLAIN, 0) {};
    private static final Block LOG = new BlockOldLog();
    private static final Block PISTON = new BlockPistonBase(false) {};
    private static final Block TORCH = new BlockTorch() {};
    private static final Block LEVER = new BlockLever() {};
    private static final Block DOOR = new BlockDoor(Material.wood) {};
    private static final Block RAIL = new BlockRail() {};
    private static final Block VINE = new BlockVine() {};
    private static final Block MUSHROOM = new BlockHugeMushroom(Material.wood, 0) {};
    private static final Block BED = new BlockBed() {};
    private static final Block TRAPDOOR = new BlockTrapDoor(Material.wood) {};

    private static int apply(Block block, int meta, String ops) {
        for (char op : ops.toCharArray()) meta = BlockMetaTransform.transform(block, meta, op);
        return meta;
    }

    @Test public void knownStates() {
        assertEquals(2, apply(STAIRS, 0, "Y"));
        assertEquals(1, apply(STAIRS, 0, "x"));
        assertEquals(4, apply(STAIRS, 0, "y"));
        assertEquals(2, apply(STAIRS, 3, "X"));
        assertEquals(8, apply(LOG, 4 | 1, "Y") & 12);
        assertEquals(8, apply(LOG, 0, "X"));
        assertEquals(2, apply(PISTON, 1, "X"));
        assertEquals(8, apply(PISTON, 8 | 1, "y"));
        assertEquals(11, apply(PISTON, 8 | 3, "y"));
        assertEquals(3, apply(TORCH, 1, "Y"));
        assertEquals(2, apply(TORCH, 1, "x"));
        assertEquals(4, apply(TORCH, 5, "X"));
        assertEquals(6, apply(LEVER, 5, "Y"));
        assertEquals(8, apply(LEVER, 8 | 1, "Z"));
        assertEquals(2, apply(DOOR, 1, "Y"));
        assertEquals(9, apply(DOOR, 8, "x"));
        assertEquals(8, apply(DOOR, 8, "Y"));
        assertEquals(7, apply(RAIL, 6, "Y"));
        assertEquals(5, apply(RAIL, 2, "Y"));
        assertEquals(2, apply(VINE, 1, "Y"));
        assertEquals(3, apply(MUSHROOM, 1, "Y"));
        assertEquals(3, apply(MUSHROOM, 1, "x"));
        assertEquals(8 | 1, apply(BED, 8, "Y"));
        assertEquals(3, apply(BED, 1, "x"));
        assertEquals(8 | 1, apply(TRAPDOOR, 0, "zy"));
        assertEquals(-1, BlockMetaTransform.transform(PLAIN, 3, 'Y'));
        assertEquals(4, BlockMetaTransform.rotation16(0, 'Y'));
        assertEquals(12, BlockMetaTransform.rotation16(4, 'x'));
        assertEquals(8, BlockMetaTransform.rotation16(0, 'z'));
    }

    @Test public void fourRotationsAndTwoMirrorsAreIdentities() {
        Block[] blocks = {STAIRS, LOG, PISTON, TORCH, LEVER, DOOR, RAIL, VINE, MUSHROOM, BED, TRAPDOOR};
        for (Block block : blocks) {
            for (int meta = 0; meta < 16; meta++) {
                for (char op : "XYZ".toCharArray()) {
                    if (op == 'Y') assertEquals(block + " " + meta, meta, apply(block, meta, "YYYY"));
                }
                for (char op : "xyz".toCharArray()) {
                    assertEquals(block + " " + meta + " " + op, meta, apply(block, meta, "" + op + op));
                }
            }
        }
    }
}

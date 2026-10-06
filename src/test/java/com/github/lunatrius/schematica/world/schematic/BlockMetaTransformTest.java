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

    @Test public void closedSourceModBlocks() {
        // Thaumcraft essentia mirror (6 +) on the north side of its support turns east
        assertEquals(6 + 5, apply(new thaumcraft.common.blocks.BlockMirror(), 6 + 2, "Y"));
        // BiblioCraft armor stand top half facing east (0) turns south (1)
        assertEquals(4 + 1, apply(new jds.bibliocraft.blocks.BlockArmorStand(), 4, "Y"));
        // Witchery coffin head (8) toward south: mirrored across z it points north
        assertEquals(8 | 2, apply(new com.emoniph.witchery.blocks.BlockCoffin(), 8, "z"));
        assertEquals(4, apply(new com.emoniph.witchery.blocks.BlockKettle(), 5, "x"));
        assertEquals(0, apply(new com.emoniph.witchery.blocks.BlockKettle(), 0, "Y"));
        // Extra Utilities conveyor running north turns east; a liquid transfer node (6 +) facing down stays
        assertEquals(1, apply(new com.rwtema.extrautils.block.BlockConveyor(), 0, "Y"));
        assertEquals(6, apply(new com.rwtema.extrautils.tileentity.transfernodes.BlockTransferNode(), 6, "Y"));
        assertEquals(6 + 4, apply(new com.rwtema.extrautils.tileentity.transfernodes.BlockTransferNode(), 6 + 2, "YYY"));
        assertEquals(12, apply(new com.rwtema.extrautils.tileentity.transfernodes.BlockTransferNode(), 12, "Y"));
    }

    @Test public void privateSourceGtnhModBlocks() {
        // Catwalks: open north (8) and east (1) sides turn to east (1) and south (4)
        assertEquals(1 | 4, apply(new com.thecodewarrior.catwalks.block.BlockCatwalk(), 8 | 1, "Y"));
        assertEquals(8 | 2, apply(new com.thecodewarrior.catwalks.block.BlockCatwalk(), 8 | 1, "x"));
        assertEquals(2, apply(new com.thecodewarrior.catwalks.block.BlockSupportColumn(), 1, "Y"));
        assertEquals(1, apply(new com.thecodewarrior.catwalks.block.BlockSupportColumn(), 0, "X"));
        // caged ladders: left (2) and right (1) swap under a mirror, the side itself is in the block's name
        assertEquals(8 | 1, apply(new com.thecodewarrior.catwalks.block.BlockCagedLadder(), 8 | 2, "x"));
        assertEquals(8 | 2, apply(new com.thecodewarrior.catwalks.block.BlockCagedLadder(), 8 | 2, "Y"));
        assertEquals("catwalks:cagedLadder_east_lit_tape", BlockMetaTransform.ladderName("catwalks:cagedLadder_north_lit_tape", 'Y'));
        assertEquals("catwalks:cagedLadder_west_unlit", BlockMetaTransform.ladderName("catwalks:cagedLadder_east_unlit", 'x'));
        assertNull(BlockMetaTransform.ladderName("catwalks:catwalk_lit", 'Y'));
        assertEquals(5, apply(new com.pam.harvestcraft.BlockPamOven(), 2, "Y"));
        // Automagy: (yaw quarter + 2) & 3 is N, E, S, W; tallies 6 + side, maws any side
        assertEquals(1, apply(new tuhljin.automagy.blocks.BlockHourglass(), 0, "Y"));
        assertEquals(6 + 5, apply(new tuhljin.automagy.blocks.BlockTallyBase(), 6 + 2, "Y"));
        assertEquals(0, apply(new tuhljin.automagy.blocks.BlockMawHungry(), 1, "y"));
        // Galaxy Space machines use Galacticraft's E, W, N, S
        assertEquals(4 | 0, apply(new galaxyspace.core.block.machine.BlockMachine(), 4 | 2, "Y"));
    }

    @Test public void openSourceGtnhModBlocksFoundBySweep() {
        // Et Futurum: bee nest with honey (2-5 + 6), pink petals (direction in bits 2-3 over the count), chains (0 y, 1 x, 2 z)
        assertEquals(5 + 6, apply(new ganymedes01.etfuturum.blocks.BlockBeeHive(), 2 + 6, "Y"));
        assertEquals(1 << 2 | 3, apply(new ganymedes01.etfuturum.blocks.BlockPinkPetals(), 0 | 3, "Y"));
        assertEquals(2, apply(new ganymedes01.etfuturum.blocks.BlockChain(), 1, "Y"));
        assertEquals(0, apply(new ganymedes01.etfuturum.blocks.BlockChain(), 1, "Z"));
        // Tinkers' conveyors turn by eighths, drying racks swap floor axes
        assertEquals(8 | 3, apply(new tconstruct.world.blocks.ConveyorBase(), 8 | 1, "Y"));
        assertEquals(7, apply(new tconstruct.world.blocks.ConveyorBase(), 1, "x"));
        assertEquals(1, apply(new tconstruct.armor.blocks.DryingRack(), 0, "Y"));
        assertEquals(5, apply(new tconstruct.armor.blocks.DryingRack(), 2, "Y"));
        // Twilight Forest critter on the north side of a block (4) turns to the east side (1)
        assertEquals(1, apply(new twilightforest.block.BlockTFCritter(), 4, "Y"));
        assertEquals(6, apply(new twilightforest.block.BlockTFCritter(), 5, "y"));
        assertEquals(4, apply(new chylex.hee.block.BlockObsidianSpecial(), 3, "Y"));
        assertEquals(4, apply(new gmail.Lance5057.blocks.CrestMount(), 1, "Y"));
        assertEquals(2, apply(new pcl.openprinter.blocks.BlockShredder(), 1, "Y"));
        // Amun-Ra machines keep the sub-block (bits 0-1) under the N/S/W/E bits 2-3
        assertEquals(3 << 2 | 1, apply(new de.katzenpapst.amunra.block.BlockMachineMeta(), 0 << 2 | 1, "Y"));
        // arcane dropper facing up keeps up and flips its quarter-turn flag
        assertEquals(1 | 8, apply(new makeo.gadomancy.common.blocks.BlockArcaneDropper(), 1, "Y"));
        assertEquals(3, apply(new micdoodle8.mods.galacticraft.core.blocks.BlockDish(), 0, "Y"));
    }

    @Test public void openModsRotationModesAndStargates() {
        // FOUR_DIRECTIONS: local x east (0) turns south (local x +Z, index 3); bits above the mask stay
        Block openBlock = new openmods.block.OpenBlock();
        assertEquals(8 | 3, apply(openBlock, 8 | 0, "Y"));
        assertEquals(2, apply(openBlock, 0, "x"));
        assertEquals(0, apply(openBlock, 0, "YYYY"));
        // SGCraft facing index N, W, S, E
        assertEquals(3, apply(new gcewing.sg.blocks.SGBaseBlock(), 0, "Y"));
    }

    @Test public void serpentsAndBloodwood() {
        Block naga = new twilightforest.block.BlockTFNagastone(), snake = new team.chisel.block.BlockSnakestone();
        // head joined to the north (1) joins to the east (2); corners join below or above (4, 8) and a side N, S, W, E
        assertEquals(2, apply(naga, 1, "Y"));
        assertEquals(2, apply(snake, 1, "Y"));
        assertEquals(4 | 3, apply(naga, 4 | 0, "Y"));
        assertEquals(4 | 1, apply(snake, 4 | 0, "z"));
        // tilted: below + north becomes south + below; up + west would join two sides and keeps its metadata
        assertEquals(4 | 1, apply(naga, 4 | 0, "X"));
        assertEquals(8 | 2, apply(naga, 8 | 2, "X"));
        assertEquals(13, apply(naga, 12, "Y"));
        assertEquals(15, apply(naga, 15, "Y"));
        for (int meta = 0; meta < 16; meta++) assertEquals(meta, apply(snake, meta, "YYYY"));
        assertEquals(5, apply(new twilightforest.block.BlockTFNagastoneEtched(), 2, "Y"));
        // bloodwood quarters: north-west (0) turns north-east (1); lying logs swap axes
        Block bloodwood = new mods.natura.blocks.trees.LogTwoxTwo();
        assertEquals(1, apply(bloodwood, 0, "Y"));
        assertEquals(1, apply(bloodwood, 0, "x"));
        assertEquals(8, apply(bloodwood, 4, "Y"));
        assertEquals(10, apply(bloodwood, 0, "X"));
        assertEquals(15, apply(bloodwood, 15, "Y"));
        for (int meta = 0; meta < 12; meta++) for (String op : new String[] {"YYYY", "XXXX", "ZZZZ", "xx", "yy", "zz"}) assertEquals(meta, apply(bloodwood, meta, op));
    }
}

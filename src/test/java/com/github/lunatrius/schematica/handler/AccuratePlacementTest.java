package com.github.lunatrius.schematica.handler;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.material.Material;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class AccuratePlacementTest {
    private static final class Stairs extends BlockStairs { Stairs() { super(new Block(Material.wood) {}, 0); } }
    private static final class Lever extends BlockLever { Lever() { super(); } }
    private static final class Piston extends BlockPistonBase { Piston() { super(false); } }

    @Test public void onlyWhitelistedOrientationBitsMayChange() {
        Block stairs = new Stairs();
        assertTrue(AccuratePlacement.allowed(stairs, 0, 3));
        assertTrue(AccuratePlacement.allowed(stairs, 1, 6));
        assertFalse(AccuratePlacement.allowed(stairs, 0, 8));
        assertFalse(AccuratePlacement.allowed(stairs, 2, 2));
        assertFalse(AccuratePlacement.allowed(stairs, 0, 16));
    }

    @Test public void poweredStatesAndPlainBlocksStayAsPlaced() {
        Block lever = new Lever();
        assertTrue(AccuratePlacement.allowed(lever, 1, 5));
        assertFalse(AccuratePlacement.allowed(lever, 1, 9));
        Block piston = new Piston();
        assertTrue(AccuratePlacement.allowed(piston, 0, 4));
        assertFalse(AccuratePlacement.allowed(piston, 0, 12));
        assertFalse(AccuratePlacement.allowed(new Block(Material.rock) {}, 0, 1));
    }

    @Test public void positionKeysDoNotCollideAcrossAxes() {
        assertNotEquals(AccuratePlacement.key(1, 0, 0), AccuratePlacement.key(0, 1, 0));
        assertNotEquals(AccuratePlacement.key(0, 0, 1), AccuratePlacement.key(0, 1, 0));
        assertNotEquals(AccuratePlacement.key(-1, 64, 0), AccuratePlacement.key(0, 64, -1));
    }
}

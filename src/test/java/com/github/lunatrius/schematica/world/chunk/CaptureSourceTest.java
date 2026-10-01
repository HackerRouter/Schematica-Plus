package com.github.lunatrius.schematica.world.chunk;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.BlockCarpet;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CaptureSourceTest {
    private static final Block SOLID = new Block(Material.rock) {};
    private static final Block SAND = new BlockFalling() {};
    private static final Block CARPET = new BlockCarpet() {};

    /** Blocks by position; everything else is empty. */
    private static final class Grid implements CaptureSource {
        final Map<String, Block> blocks = new HashMap<>();
        void set(int x, int y, int z, Block block) { blocks.put(x + "," + y + "," + z, block); }
        @Override public Block block(int x, int y, int z) { return blocks.get(x + "," + y + "," + z); }
        @Override public int meta(int x, int y, int z) { return 0; }
        @Override public TileEntity tile(int x, int y, int z, int a, int b, int c) { return null; }
        @Override public List<Entity> entities(AxisAlignedBB box, int a, int b, int c) { return Collections.emptyList(); }
        @Override public boolean covers(int x, int y, int z, ForgeDirection side) { return block(x, y, z) == SOLID; }
    }

    private static Grid cube() {
        Grid grid = new Grid();
        for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) grid.set(x, y, z, SOLID);
        return grid;
    }

    @Test public void onlyFullyCoveredBlocksAreHidden() {
        Grid grid = cube();
        assertFalse(CaptureSource.exposed(grid, 0, 0, 0));
        assertTrue(CaptureSource.exposed(grid, 1, 0, 0));
        grid.set(0, 1, 0, null);
        assertTrue(CaptureSource.exposed(grid, 0, 0, 0));
    }

    @Test public void hiddenBlocksUnderSupportNeedersOrVisibleFallingBlocksAreSupport() {
        Grid grid = cube();
        assertFalse(CaptureSource.support(grid, 0, -1, 0));
        grid.set(0, 0, 0, CARPET);
        assertTrue(CaptureSource.support(grid, 0, -1, 0));
        grid = cube();
        grid.set(0, 1, 0, SAND);
        grid.set(0, 2, 0, null);
        assertTrue(CaptureSource.support(grid, 0, 0, 0));
        grid.set(0, 2, 0, SOLID);
        assertFalse(CaptureSource.support(grid, 0, 0, 0));
        grid.set(0, 2, 0, SAND);
        grid.set(0, 3, 0, null);
        assertTrue(CaptureSource.support(grid, 0, 0, 0));
    }
}

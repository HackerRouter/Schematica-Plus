package com.github.lunatrius.schematica.tool;

import java.lang.reflect.Method;
import java.util.BitSet;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.Chunk;
import org.junit.Test;
import static org.junit.Assert.*;

public class SilentBlockPlacementTest {
    @Test public void flushFailureDoesNotDiscardOtherCellsOrRepeatTheBatch() throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        java.lang.reflect.Field unsafe = unsafeClass.getDeclaredField("theUnsafe");
        unsafe.setAccessible(true);
        FlushWorld world = (FlushWorld) unsafeClass.getMethod("allocateInstance", Class.class).invoke(unsafe.get(null), FlushWorld.class);
        java.lang.reflect.Field provider = World.class.getDeclaredField("provider");
        provider.setAccessible(true);
        provider.set(world, new WorldProviderSurface());
        world.chunk = new Chunk(world, 0, 0);
        Block block = new CallbackBlock();
        SilentBlockPlacement placement = new SilentBlockPlacement();
        for (int x : new int[] {3, 4}) {
            placement.setBlock(world, x, 70, 5, block, 0);
            world.chunk.heightMap[(5 << 4) | x] = 71;
        }
        java.util.List<Integer> failures = new java.util.ArrayList<>();
        placement.flush(world, (x, y, z, error) -> failures.add(x));
        assertEquals(2, world.updates);
        assertTrue(failures.contains(3));
        placement.flush(world, (x, y, z, error) -> fail("Repeated flush"));
        assertEquals(2, world.updates);
    }

    private static final class FlushWorld extends WorldServer {
        Chunk chunk;
        int updates;
        private FlushWorld() { super(null, null, "", 0, null, null); }
        @Override public Chunk getChunkFromChunkCoords(int x, int z) { return chunk; }
        @Override public boolean func_147451_t(int x, int y, int z) { return true; }
        @Override public void markBlockForUpdate(int x, int y, int z) {
            updates++;
            if (x == 3) throw new IllegalStateException("Update failed");
        }
    }

    @Test public void writesSectionBoundariesWithoutCallingBlockCallbacks() throws Exception {
        Block block = new CallbackBlock();
        Method add = Block.blockRegistry.getClass().getDeclaredMethod("add", int.class, String.class, Object.class, BitSet.class);
        add.setAccessible(true);
        add.invoke(Block.blockRegistry, 4093, "test:silent_block", block, new BitSet());
        Chunk chunk = new Chunk(null, -1, 2);
        for (int y : new int[] {0, 15, 16, 255}) {
            SilentBlockPlacement.writeStorage(chunk, 3, y, 5, block, 7, true);
            assertSame(block, chunk.getBlock(3, y, 5));
            assertEquals(7, chunk.getBlockMetadata(3, y, 5));
            assertTrue(chunk.getBlockStorageArray()[y >> 4].getNeedsRandomTick());
            assertEquals(15, chunk.getBlockStorageArray()[y >> 4].getExtSkylightValue(4, y & 15, 5));
        }
        assertTrue(chunk.isModified);
        assertEquals(-999, chunk.precipitationHeightMap[5 << 4 | 3]);
        Chunk nether = new Chunk(null, 0, 0);
        SilentBlockPlacement.writeStorage(nether, 0, 64, 0, block, 2, false);
        assertNull(nether.getBlockStorageArray()[4].getSkylightArray());
    }

    private static final class CallbackBlock extends Block {
        private CallbackBlock() { super(Material.rock); setTickRandomly(true); }
        @Override public void onBlockAdded(World world, int x, int y, int z) { fail("Placement callback"); }
        @Override public void onBlockPreDestroy(World world, int x, int y, int z, int metadata) { fail("Removal callback"); }
        @Override public void breakBlock(World world, int x, int y, int z, Block block, int metadata) { fail("Break callback"); }
    }
}

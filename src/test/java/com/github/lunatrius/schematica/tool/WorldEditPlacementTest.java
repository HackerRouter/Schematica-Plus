// Server paste placement callbacks and tile restoration checks, by HackerRouter, 2026.
package com.github.lunatrius.schematica.tool;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.BitSet;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.material.Material;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.world.WorldServer;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class WorldEditPlacementTest {
    private static final Block AIR = new Block(Material.air) {
        @Override public boolean isOpaqueCube() { return false; }
    };
    private static final Block WALL = new Block(Material.rock) {};
    private static final Block CHEST = new BlockChest(0) {};

    @BeforeClass public static void register() throws Exception {
        Method add = Block.blockRegistry.getClass().getDeclaredMethod("add", int.class, String.class, Object.class, BitSet.class);
        add.setAccessible(true);
        add.invoke(Block.blockRegistry, 4073, "test:placement_chest", CHEST, new BitSet());
    }

    @Test public void chestFacingCallbackDoesNotDiscardSavedTileDataOrFinalUpdates() throws Exception {
        FakeWorld world = world(65535);
        WorldEditJob job = paste(3);
        assertFalse(job.step(world));
        assertEquals(5, world.metadataAfterAdded);
        assertEquals("Saved chest", ((TileEntityChest) world.tile).getInventoryName());
        assertEquals(3, world.metadata);
        assertEquals(1, job.blockCount);
        assertEquals(1, world.restorations);
        assertEquals(2, world.restoreFlags);
        assertEquals(4, world.tile.xCoord);
        assertEquals(70, world.tile.yCoord);
        assertEquals(6, world.tile.zCoord);
        assertEquals(0, world.neighborUpdates);
        boolean finished = false;
        for (int i = 0; i < 8 && !finished; i++) finished = job.step(world);
        assertTrue(finished);
        assertEquals(1, world.neighborUpdates);
        assertEquals(1, world.blockUpdates);
    }

    @Test public void callbackRestorationKeepsExtendedMetadataAndVanillaTruncation() throws Exception {
        for (int mask : new int[] {65535, 15}) {
            FakeWorld world = world(mask);
            WorldEditJob job = paste(316);
            job.step(world);
            assertEquals("Saved chest", ((TileEntityChest) world.tile).getInventoryName());
            assertEquals(316 & mask, world.metadata);
            assertEquals(316, world.restoreMetadata);
            assertEquals(1, job.blockCount);
        }
    }

    @Test public void acceptedLowBitsDoNotRequireAnotherMetadataWrite() throws Exception {
        FakeWorld world = world(15);
        world.skipAdded = true;
        WorldEditJob job = paste(316);
        job.step(world);
        assertEquals(12, world.metadata);
        assertEquals("Saved chest", ((TileEntityChest) world.tile).getInventoryName());
        assertEquals(0, world.restorations);
        assertEquals(1, job.blockCount);
    }

    @Test public void blockReplacedByAddedCallbackIsNotGivenChestMetadataOrTileData() throws Exception {
        FakeWorld world = world(65535);
        world.replaceOnAdded = true;
        WorldEditJob job = paste(3);
        job.step(world);
        assertSame(WALL, world.block);
        assertEquals(5, world.metadata);
        assertEquals(0, world.restorations);
        assertEquals(0, world.tileWrites);
        assertEquals(0, job.blockCount);
    }

    @Test public void blockReplacedDuringMetadataRestorationIsNotGivenChestTileData() throws Exception {
        FakeWorld world = world(65535);
        world.replaceOnRestore = true;
        WorldEditJob job = paste(3);
        job.step(world);
        assertEquals(1, world.restorations);
        assertSame(WALL, world.block);
        assertEquals(0, world.tileWrites);
        assertEquals(0, job.blockCount);
    }

    @Test public void rejectedMetadataRestorationDoesNotClaimOrApplyThePaste() throws Exception {
        FakeWorld world = world(65535);
        world.rejectRestore = true;
        WorldEditJob job = paste(3);
        job.step(world);
        assertEquals(1, world.restorations);
        assertEquals(5, world.metadata);
        assertEquals(0, world.tileWrites);
        assertEquals(0, job.blockCount);
    }

    private static WorldEditJob paste(int metadata) {
        WorldEditJob job = new WorldEditJob(UUID.randomUUID(), 0, WorldEditJob.Kind.PASTE, 4, 70, 6,
            1, 1, 1, null, 0, null, 0);
        NBTTagCompound tag = job.write();
        NBTTagList palette = new NBTTagList();
        palette.appendTag(new NBTTagString("test:placement_chest"));
        tag.setTag("palette", palette);
        tag.setByteArray("cells", new byte[] {0, 0});
        tag.setByteArray("meta", new byte[] {(byte) metadata});
        if (metadata > 255) tag.setByteArray("metaHigh", new byte[] {(byte) (metadata >> 8)});
        NBTTagCompound chest = new NBTTagCompound();
        chest.setString("id", "Chest");
        chest.setString("CustomName", "Saved chest");
        NBTTagCompound tile = new NBTTagCompound();
        tile.setInteger("index", 0);
        tile.setTag("data", chest);
        NBTTagList tiles = new NBTTagList();
        tiles.appendTag(tile);
        tag.setTag("tiles", tiles);
        return WorldEditJob.read(tag, job.player, 0);
    }

    private static FakeWorld world(int mask) throws Exception {
        // Skip WorldServer's constructor, which creates a live server and dimension.
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field field = unsafeClass.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        FakeWorld world = (FakeWorld) unsafeClass.getMethod("allocateInstance", Class.class).invoke(field.get(null), FakeWorld.class);
        world.block = AIR;
        world.mask = mask;
        return world;
    }

    private static final class FakeWorld extends WorldServer {
        Block block;
        TileEntity tile;
        int mask, metadata, metadataAfterAdded, restorations, restoreFlags, restoreMetadata;
        int tileWrites, neighborUpdates, blockUpdates;
        boolean adding, skipAdded, replaceOnAdded, replaceOnRestore, rejectRestore;

        private FakeWorld() { super(null, null, "", 0, null, null); }

        @Override public Block getBlock(int x, int y, int z) {
            if (x == 4 && y == 70 && z == 6) return block;
            return x == 3 && y == 70 && z == 6 ? WALL : AIR;
        }
        @Override public boolean isAirBlock(int x, int y, int z) { return getBlock(x, y, z) == AIR; }
        @Override public int getBlockMetadata(int x, int y, int z) { return metadata; }
        @Override public boolean setBlock(int x, int y, int z, Block value, int meta, int flags) {
            block = value;
            metadata = meta & mask;
            tile = value.createTileEntity(this, meta);
            adding = true;
            if (!skipAdded) value.onBlockAdded(this, x, y, z);
            adding = false;
            metadataAfterAdded = metadata;
            if (replaceOnAdded) block = WALL;
            return true;
        }
        @Override public boolean setBlockMetadataWithNotify(int x, int y, int z, int meta, int flags) {
            if (!adding) {
                restorations++;
                restoreMetadata = meta;
                restoreFlags = flags;
                if (rejectRestore) return false;
                if (replaceOnRestore) block = WALL;
            }
            metadata = meta & mask;
            return true;
        }
        @Override public void removeTileEntity(int x, int y, int z) { tile = null; }
        @Override public void setTileEntity(int x, int y, int z, TileEntity value) { tile = value; tileWrites++; }
        @Override public void markBlockForUpdate(int x, int y, int z) { blockUpdates++; }
        @Override public void notifyBlocksOfNeighborChange(int x, int y, int z, Block value) { neighborUpdates++; }
    }
}

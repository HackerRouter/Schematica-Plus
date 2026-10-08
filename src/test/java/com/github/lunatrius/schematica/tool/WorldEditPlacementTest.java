// Server paste placement callbacks and tile restoration checks, by HackerRouter, 2026.
package com.github.lunatrius.schematica.tool;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockDynamicLiquid;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockStaticLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import com.github.lunatrius.schematica.api.SchematicRegion;
import com.github.lunatrius.schematica.task.TaskRegistry;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class WorldEditPlacementTest {
    private static final Block AIR = new Block(Material.air) {
        @Override public boolean isOpaqueCube() { return false; }
    };
    private static final Block WALL = new Block(Material.rock) {};
    private static final Block CHEST = new BlockChest(0) {};
    private static final BlockDynamicLiquid FLOWING_WATER = new BlockDynamicLiquid(Material.water) {};
    private static final Block STILL_WATER = new BlockStaticLiquid(Material.water) {};
    private static final BlockDynamicLiquid FLOWING_LAVA = new BlockDynamicLiquid(Material.lava) {};
    private static final Block STILL_LAVA = new BlockStaticLiquid(Material.lava) {};
    private static final Block LEVER = new BlockLever() {};

    @BeforeClass public static void register() throws Exception {
        Method add = Block.blockRegistry.getClass().getDeclaredMethod("add", int.class, String.class, Object.class, BitSet.class);
        add.setAccessible(true);
        add.invoke(Block.blockRegistry, 4073, "test:placement_chest", CHEST, new BitSet());
        add.invoke(Block.blockRegistry, 4074, "test:placement_flowing_water", FLOWING_WATER, new BitSet());
        add.invoke(Block.blockRegistry, 4075, "test:placement_still_water", STILL_WATER, new BitSet());
        add.invoke(Block.blockRegistry, 4076, "test:placement_flowing_lava", FLOWING_LAVA, new BitSet());
        add.invoke(Block.blockRegistry, 4077, "test:placement_still_lava", STILL_LAVA, new BitSet());
        add.invoke(Block.blockRegistry, 4078, "test:placement_lever", LEVER, new BitSet());
    }

    @Test public void chestFacingCallbackDoesNotDiscardSavedTileDataOrFinalUpdates() throws Exception {
        FakeWorld world = world(65535);
        WorldEditJob job = paste(3);
        assertFalse(job.step(world));
        assertEquals(5, world.metadataAfterAdded);
        assertEquals("Saved chest", ((TileEntityChest) world.cell(4).tile).getInventoryName());
        assertEquals(3, world.cell(4).metadata);
        assertEquals(1, job.blockCount);
        assertEquals(1, world.restorations);
        assertEquals(2, world.restoreFlags);
        assertEquals(4, world.cell(4).tile.xCoord);
        assertEquals(70, world.cell(4).tile.yCoord);
        assertEquals(6, world.cell(4).tile.zCoord);
        assertEquals(0, world.neighborUpdates);
        finish(job, world);
        assertEquals(1, world.neighborUpdates);
        assertEquals(1, world.blockUpdates);
    }

    @Test public void callbackRestorationKeepsExtendedMetadataAndVanillaTruncation() throws Exception {
        for (int mask : new int[] {65535, 15}) {
            FakeWorld world = world(mask);
            WorldEditJob job = paste(316);
            job.step(world);
            assertEquals("Saved chest", ((TileEntityChest) world.cell(4).tile).getInventoryName());
            assertEquals(316 & mask, world.cell(4).metadata);
            assertEquals(316, world.restoreMetadata);
            assertEquals(1, job.blockCount);
        }
    }

    @Test public void acceptedLowBitsDoNotRequireAnotherMetadataWrite() throws Exception {
        FakeWorld world = world(15);
        world.skipAdded = true;
        WorldEditJob job = paste(316);
        job.step(world);
        assertEquals(12, world.cell(4).metadata);
        assertEquals("Saved chest", ((TileEntityChest) world.cell(4).tile).getInventoryName());
        assertEquals(0, world.restorations);
        assertEquals(1, job.blockCount);
    }

    @Test public void blockReplacedByAddedCallbackIsNotGivenChestMetadataOrTileData() throws Exception {
        FakeWorld world = world(65535);
        world.replaceOnAdded = true;
        WorldEditJob job = paste(3);
        assertThrows(com.github.lunatrius.schematica.util.MessageException.class, () -> job.step(world));
        assertSame(WALL, world.cell(4).block);
        assertEquals(5, world.cell(4).metadata);
        assertEquals(0, world.restorations);
        assertEquals(0, world.tileWrites);
        assertEquals(0, job.blockCount);
    }

    @Test public void blockReplacedDuringMetadataRestorationIsNotGivenChestTileData() throws Exception {
        FakeWorld world = world(65535);
        world.replaceOnRestore = true;
        WorldEditJob job = paste(3);
        assertThrows(com.github.lunatrius.schematica.util.MessageException.class, () -> job.step(world));
        assertEquals(1, world.restorations);
        assertSame(WALL, world.cell(4).block);
        assertEquals(0, world.tileWrites);
        assertEquals(0, job.blockCount);
    }

    @Test public void rejectedMetadataRestorationDoesNotClaimOrApplyThePaste() throws Exception {
        FakeWorld world = world(65535);
        world.rejectRestore = true;
        WorldEditJob job = paste(3);
        assertThrows(com.github.lunatrius.schematica.util.MessageException.class, () -> job.step(world));
        assertEquals(1, world.restorations);
        assertEquals(5, world.cell(4).metadata);
        assertEquals(0, world.tileWrites);
        assertEquals(0, job.blockCount);
    }

    @Test public void rejectedBlockWriteCannotFinishAsASuccessfulPaste() throws Exception {
        FakeWorld world = world(65535);
        world.rejectPlacement = true;
        WorldEditJob job = paste(3);
        com.github.lunatrius.schematica.util.MessageException error = assertThrows(
            com.github.lunatrius.schematica.util.MessageException.class, () -> finish(job, world));
        assertEquals("schematica.message.edit.block_failed", error.key());
        assertSame(AIR, world.cell(4).block);
        assertEquals(0, job.blockCount);
        assertEquals(0, world.tileWrites);
    }

    @Test public void identicalOrdinaryBlocksDoNotWriteOrReportChanges() throws Exception {
        FakeWorld world = world(65535);
        world.cell(4).block = WALL;
        WorldEditJob job = new WorldEditJob(UUID.randomUUID(), 0, WorldEditJob.Kind.FILL,
            4, 70, 6, 1, 1, 1, WALL, 0, null, 0);
        finish(job, world);
        assertEquals(0, job.blockCount);
        assertEquals(0, world.blockUpdates);
        assertEquals("schematica.message.edit.no_changes",
            ((net.minecraft.util.ChatComponentTranslation) job.finishedMessage(true)).getKey());
    }

    @Test public void matchingChestStateStillRestoresSavedTileData() throws Exception {
        FakeWorld world = world(65535);
        world.cell(4).block = CHEST;
        world.cell(4).metadata = 3;
        WorldEditJob job = paste(3);
        finish(job, world);
        assertEquals(1, job.blockCount);
        assertEquals("Saved chest", ((TileEntityChest) world.cell(4).tile).getInventoryName());
    }

    @Test public void partialPasteStopsAtTheRejectedCellWithoutCountingIt() throws Exception {
        FakeWorld world = world(65535);
        WorldEditJob job = paste(3, 3, false);
        job.step(world);
        world.rejectPlacement = true;
        assertThrows(com.github.lunatrius.schematica.util.MessageException.class, () -> finish(job, world));
        assertEquals(1, job.blockCount);
        assertEquals(1, world.tileWrites);
        assertSame(AIR, world.cell(5).block);
        assertSame(AIR, world.cell(6).block);
    }

    @Test public void silentPasteIntoAnEmptyChunkCannotClaimAWrittenBlock() throws Exception {
        FakeWorld world = world(15);
        world.chunk = new net.minecraft.world.chunk.EmptyChunk(world, 0, 0);
        WorldEditJob job = paste(3, 1, true);
        assertThrows(com.github.lunatrius.schematica.util.MessageException.class, () -> finish(job, world));
        assertEquals(0, job.blockCount);
        assertTrue(world.chunk.chunkTileEntityMap.isEmpty());
    }

    @Test public void commandPasteSkipsMatchingCellsButStillSendsDifferentMetadata() throws Exception {
        FakeWorld world = world(65535);
        world.cell(4).block = CHEST;
        world.cell(4).metadata = 316;
        WorldEditJob same = paste(316);
        assertEquals(null, same.command(0, world));
        world.cell(4).metadata = 12;
        assertTrue(same.command(0, world).contains(" 316 replace"));
    }

    @Test public void bothChestHalvesAreRestoredBeforeAnyFinalNeighborNotification() throws Exception {
        FakeWorld world = doubleChestWorld(65535);
        WorldEditJob job = paste(2, 2, false);
        job.step(world);
        TileEntity first = world.cell(4).tile;
        job.step(world);
        TileEntity second = world.cell(5).tile;
        assertEquals(3, world.cell(4).metadata);
        assertEquals(2, world.cell(5).metadata);
        world.onNotify = () -> assertPairMetadata(world, 2);
        finish(job, world);
        assertPairMetadata(world, 2);
        assertSame(first, world.cell(4).tile);
        assertSame(second, world.cell(5).tile);
        assertEquals("Saved chest 1", ((TileEntityChest) first).getInventoryName());
        assertEquals("Saved chest 2", ((TileEntityChest) second).getInventoryName());
        assertEquals(2, world.tileWrites);
        assertEquals(2, world.neighborUpdates);
        assertEquals(2, job.blockCount);
    }

    @Test public void cancellationAndRegionMasksOnlyFinalizeAlreadyPlacedCells() throws Exception {
        for (boolean cancel : new boolean[] {false, true}) {
            FakeWorld world = doubleChestWorld(65535);
            WorldEditJob job = paste(2, 3, false);
            if (!cancel) job.setRegions(Collections.singletonList(new SchematicRegion("Pair", 0, 0, 0, 1, 0, 0)));
            job.step(world);
            job.step(world);
            job.cancelled = cancel;
            world.onNotify = () -> assertPairMetadata(world, 2);
            finish(job, world);
            assertPairMetadata(world, 2);
            assertSame(AIR, world.cell(6).block);
            assertEquals(2, world.tileWrites);
            assertEquals(2, world.neighborUpdates);
            assertEquals(2, job.blockCount);
        }
    }

    @Test public void finalRestorationPreservesExtendedMetadataAndAcceptedLowBits() throws Exception {
        for (int mask : new int[] {65535, 15}) {
            FakeWorld world = doubleChestWorld(mask);
            WorldEditJob job = paste(316, 2, false);
            job.step(world);
            job.step(world);
            assertEquals(3, world.cell(4).metadata);
            world.onNotify = () -> assertPairMetadata(world, 316 & mask);
            finish(job, world);
            assertPairMetadata(world, 316 & mask);
            assertEquals(316, world.restoreMetadata);
            assertEquals(2, world.tileWrites);
            assertEquals(2, job.blockCount);
        }
    }

    @Test public void finalRestorationLeavesReplacedBlocksAloneAndContinuesUpdates() throws Exception {
        for (boolean replaceDuringRestore : new boolean[] {false, true}) {
            FakeWorld world = doubleChestWorld(65535);
            WorldEditJob job = paste(2, 2, false);
            job.step(world);
            job.step(world);
            int before = world.restorations;
            if (replaceDuringRestore) world.replaceOnRestore = true;
            else world.cell(4).block = WALL;
            finish(job, world);
            assertSame(WALL, world.cell(4).block);
            assertEquals(before + (replaceDuringRestore ? 1 : 0), world.restorations);
            assertEquals(2, world.tileWrites);
            assertEquals(2, world.neighborUpdates);
            assertEquals(2, job.blockCount);
        }
    }

    @Test public void rejectedFinalMetadataWriteDoesNotReportSuccess() throws Exception {
        FakeWorld world = doubleChestWorld(65535);
        WorldEditJob job = paste(2, 2, false);
        job.step(world);
        job.step(world);
        world.rejectRestore = true;
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> finish(job, world));
        assertTrue(failure.getMessage().contains("4, 70, 6"));
        assertEquals(3, world.cell(4).metadata);
        assertEquals(2, world.tileWrites);
        assertEquals(0, world.neighborUpdates);
        assertEquals(2, job.blockCount);
    }

    @Test public void flowingLiquidsCanSettleDuringPasteWithoutBlockingUpdatesOrEntities() throws Exception {
        BlockDynamicLiquid[] flowing = {FLOWING_WATER, FLOWING_LAVA};
        Block[] still = {STILL_WATER, STILL_LAVA};
        for (int liquid = 0; liquid < flowing.length; liquid++) {
            FakeWorld world = world(15);
            world.cell(4, 69, 6).block = WALL;
            world.cell(4, 70, 5).block = WALL;
            world.cell(4, 70, 7).block = WALL;
            String name = liquid == 0 ? "test:placement_flowing_water" : "test:placement_flowing_lava";
            WorldEditJob job = pasteBesideChest(name, 0);
            for (int i = 0; i < 8 && job.blockCount < 2; i++) job.step(world);
            assertEquals(2, job.blockCount);
            assertSame(flowing[liquid], world.cell(4).block);
            flowing[liquid].updateTick(world, 4, 70, 6, new Random(0));
            assertSame(still[liquid], world.cell(4).block);
            finish(job, world);
            assertSame(still[liquid], world.cell(4).block);
            assertEquals(2, world.neighborUpdates);
            assertEquals(2, job.blockCount);
            assertEquals("Saved chest 2", ((TileEntityChest) world.cell(5).tile).getInventoryName());
            TaskRegistry registry = new TaskRegistry();
            TaskRegistry.Task task = registry.start(job.player, 0, job.taskKind(), TaskRegistry.Backend.SERVER, "");
            job.publishProgress(task);
            assertEquals(TaskRegistry.Stage.ENTITIES, task.progress().stage);
            task.finish();
        }
    }

    @Test public void finalChestPassDoesNotUndoAnotherBlocksNormalStateChange() throws Exception {
        FakeWorld world = world(15);
        WorldEditJob job = pasteBesideChest("test:placement_lever", 1);
        for (int i = 0; i < 8 && job.blockCount < 2; i++) job.step(world);
        assertEquals(2, job.blockCount);
        assertTrue(LEVER.onBlockActivated(world, 4, 70, 6, null, 1, 0.5F, 0.5F, 0.5F));
        assertEquals(9, world.cell(4).metadata);
        int notifications = world.neighborUpdates;
        finish(job, world);
        assertSame(LEVER, world.cell(4).block);
        assertEquals(9, world.cell(4).metadata);
        assertEquals(notifications + 2, world.neighborUpdates);
        assertEquals(2, job.blockCount);
    }

    @Test public void silentPasteDoesNotAddMetadataWritesOrNeighborNotifications() throws Exception {
        FakeWorld world = world(15);
        world.chunk = new Chunk(world, 0, 0);
        WorldEditJob job = paste(2, 2, true);
        finish(job, world);
        assertEquals(2, world.getBlockMetadata(4, 70, 6));
        assertEquals(2, world.getBlockMetadata(5, 70, 6));
        assertEquals(2, world.chunk.chunkTileEntityMap.size());
        assertEquals(0, world.restorations);
        assertEquals(0, world.neighborUpdates);
        assertEquals(0, world.blockUpdates);
        assertEquals(2, job.blockCount);
    }

    @Test public void updateProgressAccountsForBothPassesWithoutGoingBackwards() throws Exception {
        FakeWorld world = doubleChestWorld(65535);
        WorldEditJob job = paste(2, 2, false);
        TaskRegistry registry = new TaskRegistry();
        TaskRegistry.Task task = registry.start(job.player, 0, job.taskKind(), TaskRegistry.Backend.SERVER, "");
        long completed = 0;
        boolean sawUpdates = false;
        while (!job.step(world)) {
            job.publishProgress(task);
            if (task.progress().stage == TaskRegistry.Stage.UPDATES) {
                sawUpdates = true;
                assertEquals(4, task.progress().total);
                assertTrue(task.progress().completed >= completed);
                completed = task.progress().completed;
            }
        }
        assertTrue(sawUpdates);
        assertEquals(4, completed);
        task.finish();
    }

    private static void assertPairMetadata(FakeWorld world, int metadata) {
        assertEquals(metadata, world.cell(4).metadata);
        assertEquals(metadata, world.cell(5).metadata);
    }

    private static void finish(WorldEditJob job, FakeWorld world) {
        for (int i = 0; i < 100; i++) if (job.step(world)) return;
        throw new AssertionError("Paste did not finish");
    }

    private static WorldEditJob paste(int metadata) {
        return paste(metadata, 1, false);
    }

    private static WorldEditJob pasteBesideChest(String name, int metadata) {
        WorldEditJob job = paste(3, 2, false);
        NBTTagCompound tag = job.write();
        tag.getTagList("palette", 8).appendTag(new NBTTagString(name));
        tag.setByteArray("cells", new byte[] {0, 1, 0, 0});
        tag.setByteArray("meta", new byte[] {(byte) metadata, 3});
        return WorldEditJob.read(tag, job.player, 0);
    }

    private static WorldEditJob paste(int metadata, int width, boolean silent) {
        WorldEditJob job = new WorldEditJob(UUID.randomUUID(), 0, WorldEditJob.Kind.PASTE, 4, 70, 6,
            width, 1, 1, null, 0, null, 0, silent, ReplaceBehavior.ALL);
        NBTTagCompound tag = job.write();
        NBTTagList palette = new NBTTagList();
        palette.appendTag(new NBTTagString("test:placement_chest"));
        tag.setTag("palette", palette);
        tag.setByteArray("cells", new byte[width * 2]);
        byte[] low = new byte[width], high = new byte[width];
        Arrays.fill(low, (byte) metadata);
        Arrays.fill(high, (byte) (metadata >> 8));
        tag.setByteArray("meta", low);
        if (metadata > 255) tag.setByteArray("metaHigh", high);
        NBTTagList tiles = new NBTTagList();
        for (int index = 0; index < width; index++) {
            NBTTagCompound chest = new NBTTagCompound();
            chest.setString("id", "Chest");
            chest.setString("CustomName", "Saved chest" + (width == 1 ? "" : " " + (index + 1)));
            NBTTagCompound tile = new NBTTagCompound();
            tile.setInteger("index", index);
            tile.setTag("data", chest);
            tiles.appendTag(tile);
        }
        tag.setTag("tiles", tiles);
        return WorldEditJob.read(tag, job.player, 0);
    }

    private static FakeWorld world(int mask) throws Exception {
        // Skip WorldServer's constructor, which creates a live server and dimension.
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field field = unsafeClass.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        FakeWorld world = (FakeWorld) unsafeClass.getMethod("allocateInstance", Class.class).invoke(field.get(null), FakeWorld.class);
        world.cells = new HashMap<>();
        world.cell(3).block = WALL;
        world.mask = mask;
        Field provider = World.class.getDeclaredField("provider");
        provider.setAccessible(true);
        provider.set(world, new WorldProviderSurface());
        return world;
    }

    private static FakeWorld doubleChestWorld(int mask) throws Exception {
        FakeWorld world = world(mask);
        world.cell(4, 70, 5).block = WALL;
        world.cell(5, 70, 5).block = WALL;
        return world;
    }

    private static final class Cell {
        Block block = AIR;
        TileEntity tile;
        int metadata;
    }

    private static final class FakeWorld extends WorldServer {
        Map<ChunkPosition, Cell> cells;
        Chunk chunk;
        Runnable onNotify;
        int mask, metadataAfterAdded, restorations, restoreFlags, restoreMetadata;
        int tileWrites, neighborUpdates, blockUpdates;
        boolean adding, skipAdded, replaceOnAdded, replaceOnRestore, rejectRestore, rejectPlacement;

        private FakeWorld() { super(null, null, "", 0, null, null); }

        Cell cell(int x) { return cell(x, 70, 6); }
        Cell cell(int x, int y, int z) { return cells.computeIfAbsent(new ChunkPosition(x, y, z), ignored -> new Cell()); }
        @Override public Block getBlock(int x, int y, int z) {
            return chunk == null ? cell(x, y, z).block : chunk.getBlock(x & 15, y, z & 15);
        }
        @Override public boolean isAirBlock(int x, int y, int z) { return getBlock(x, y, z) == AIR; }
        @Override public boolean blockExists(int x, int y, int z) { return y >= 0 && y < 256; }
        @Override public int getBlockMetadata(int x, int y, int z) {
            return chunk == null ? cell(x, y, z).metadata : chunk.getBlockMetadata(x & 15, y, z & 15);
        }
        @Override public Chunk getChunkFromChunkCoords(int x, int z) { return chunk; }
        @Override public void scheduleBlockUpdate(int x, int y, int z, Block block, int delay) {}
        @Override public void playSoundEffect(double x, double y, double z, String sound, float volume, float pitch) {}
        @Override public boolean setBlock(int x, int y, int z, Block value, int meta, int flags) {
            if (rejectPlacement) return false;
            Cell cell = cell(x, y, z);
            cell.block = value;
            cell.metadata = meta & mask;
            cell.tile = value.createTileEntity(this, meta);
            adding = true;
            if (!skipAdded) value.onBlockAdded(this, x, y, z);
            adding = false;
            metadataAfterAdded = cell.metadata;
            if (replaceOnAdded) cell.block = WALL;
            return true;
        }
        @Override public boolean setBlockMetadataWithNotify(int x, int y, int z, int meta, int flags) {
            if (!adding) {
                restorations++;
                restoreMetadata = meta;
                restoreFlags = flags;
                if (rejectRestore) return false;
                if (replaceOnRestore) cell(x, y, z).block = WALL;
            }
            cell(x, y, z).metadata = meta & mask;
            return true;
        }
        @Override public void removeTileEntity(int x, int y, int z) { cell(x, y, z).tile = null; }
        @Override public void setTileEntity(int x, int y, int z, TileEntity value) { cell(x, y, z).tile = value; tileWrites++; }
        @Override public void markBlockForUpdate(int x, int y, int z) { blockUpdates++; }
        @Override public void notifyBlocksOfNeighborChange(int x, int y, int z, Block value) {
            if (onNotify != null) onNotify.run();
            neighborUpdates++;
        }
    }
}

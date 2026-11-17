package com.github.lunatrius.schematica.tool;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collections;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import com.cardinalstar.cubicchunks.world.cube.ICubeProvider;
import com.github.lunatrius.schematica.compat.CubicChunks;
import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.api.SchematicOrigin;
import com.github.lunatrius.schematica.api.SchematicRegion;
import com.github.lunatrius.schematica.proxy.ServerProxy;
import com.github.lunatrius.schematica.world.chunk.SchematicContainer;
import com.github.lunatrius.schematica.world.schematic.SchematicAlpha;
import com.github.lunatrius.schematica.world.schematic.SchematicFileSnapshot;
import com.github.lunatrius.schematica.world.storage.Schematic;
import com.github.lunatrius.schematica.world.storage.RegionSelection;
import com.github.lunatrius.schematica.util.SchematicLimits;
import com.github.lunatrius.schematica.world.chunk.WorldCaptureSource;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class CubicBlockPlacementTest {
    private static final Block BLOCK = new Block(Material.rock) {
        @Override public void onBlockAdded(World world, int x, int y, int z) { fail("Placement callback"); }
        @Override public void breakBlock(World world, int x, int y, int z, Block block, int metadata) { fail("Break callback"); }
    };
    private static final Block AIR = new Block(Material.air) {
        @Override public boolean isOpaqueCube() { return false; }
    };

    @BeforeClass public static void register() throws Exception {
        Method add = Block.blockRegistry.getClass().getDeclaredMethod("add", int.class, String.class, Object.class, BitSet.class);
        add.setAccessible(true);
        add.invoke(Block.blockRegistry, 4068, "test:cubic_block", BLOCK, new BitSet());
        add.invoke(Block.blockRegistry, 4069, "test:cubic_air", AIR, new BitSet());
    }

    @Test public void writesAuthoritativeCubesAcrossSignedSectionBoundaries() throws Exception {
        for (boolean modern : new boolean[] {false, true}) {
            CubicWorld world = world(modern);
            SilentBlockPlacement writer = new SilentBlockPlacement();
            int[] heights = {-1000000, -17, -16, -1, 0, 15, 16, 63, 64, 255, 256, 4096, 1000000};
            for (int y : heights) {
                writer.setBlock(world, -13, y, 37, BLOCK, 7);
                assertSame(BLOCK, world.getBlock(-13, y, 37));
                assertEquals(7, world.getBlockMetadata(-13, y, 37));
                assertTrue(world.chunk.getCube(y >> 4).dirty);
            }
            writer.flush(world);
            assertEquals(heights.length, world.updates.size());
            assertEquals(world.updates, world.lightChecks);
            for (int y : heights) assertTrue(world.updates.contains(y));
            writer.flush(world);
            assertEquals(heights.length, world.updates.size());
            assertEquals(heights.length, world.lighting.heights);
            assertEquals(1000000, world.chunk.getTopYWithStaging(3, 5));
            assertFalse(world.lighting.ranges.isEmpty());
            if (modern) assertEquals(heights.length, ((ModernColumn) world.chunk).precipitation.changes);
            assertEquals(0, world.setBlockCalls);
        }
    }

    @Test public void pasteJobsReadBackBlocksAfterCrossingCubeBoundaries() throws Exception {
        for (boolean modern : new boolean[] {false, true}) {
            CubicWorld world = world(modern);
            for (int bottom : new int[] {-17, -1, 15, 63, 255, 4095, 999999}) {
                WorldEditJob job = new WorldEditJob(UUID.randomUUID(), 0, WorldEditJob.Kind.PASTE,
                    -13, bottom, 37, 1, 2, 1, null, 0, null, 0, true, ReplaceBehavior.ALL);
                NBTTagCompound tag = job.write();
                NBTTagList palette = new NBTTagList();
                palette.appendTag(new NBTTagString("test:cubic_block"));
                tag.setTag("palette", palette);
                tag.setByteArray("cells", new byte[4]);
                tag.setByteArray("meta", new byte[] {2, 3});
                job = WorldEditJob.read(tag, job.player, 0);
                boolean done = false;
                for (int i = 0; i < 20 && !done; i++) done = job.step(world);
                job.flushBlockChanges(world);
                assertTrue(done);
                assertNull(job.failure());
                assertEquals(0, job.failedBlockCount());
                assertEquals(2, job.blockCount);
                assertEquals(2, world.getBlockMetadata(-13, bottom, 37));
                assertEquals(3, world.getBlockMetadata(-13, bottom + 1, 37));
            }
            assertEquals(0, world.setBlockCalls);
        }
    }

    @Test public void removingRoofRelightsToTheRemainingSurface() throws Exception {
        for (boolean modern : new boolean[] {false, true}) {
            CubicWorld world = world(modern);
            SilentBlockPlacement writer = new SilentBlockPlacement();
            writer.setBlock(world, -13, 500, 37, BLOCK, 0);
            writer.setBlock(world, -13, 1000, 37, BLOCK, 0);
            writer.setBlock(world, -13, 1000, 37, AIR, 0);
            assertEquals(500, world.chunk.getTopYWithStaging(3, 5));
            assertArrayEquals(new int[] {1001, 501}, world.lighting.ranges.get(world.lighting.ranges.size() - 1));
        }
    }

    @Test public void legacyUntrackedCubeUsesStagingHeightmap() throws Exception {
        CubicWorld world = world(false);
        world.chunk.getCube(-2).tracked = false;
        SilentBlockPlacement.writeStorage(world.chunk, 3, -17, 5, BLOCK, 2, true);
        assertEquals(-17, world.chunk.stagingHeightMap.top());
        assertEquals(-2000000, world.chunk.opacity.top());
        assertEquals(0, world.lighting.heights);
        assertSame(BLOCK, world.getBlock(-13, -17, 37));
    }

    @Test public void loadedCubeChecksDoNotTreatMissingCubesAsAir() throws Exception {
        CubicWorld world = world(false);
        assertFalse(CubicChunks.loaded(world, -13, 500, 37));
        assertFalse(CubicChunks.emptySections(world.chunk, 30, 31));
        world.chunk.getCube(31);
        assertTrue(CubicChunks.loaded(world, -13, 500, 37));
        assertTrue(CubicChunks.emptySections(world.chunk, 31, 31));
        assertFalse(CubicChunks.emptySections(world.chunk, 30, 31));
        assertEquals(1, world.chunk.cubes.size());
        SilentBlockPlacement.writeStorage(world.chunk, 3, 500, 5, BLOCK, 0, true);
        assertFalse(CubicChunks.emptySections(world.chunk, 31, 31));
        assertEquals(1, CubicChunks.loadedStorage(world.chunk).size());
    }

    @Test public void capturesAndEncodesSelectionsAcrossZeroAndVanillaCeiling() throws Exception {
        CubicWorld world = world(false);
        for (int bottom : new int[] {-1000000, -1, 255, 1000000}) {
            SilentBlockPlacement.writeStorage(world.chunk, 3, bottom, 5, BLOCK, 2, true);
            SilentBlockPlacement.writeStorage(world.chunk, 3, bottom + 1, 5, BLOCK, 3, true);
            RegionSelection selection = new RegionSelection(Collections.singletonList(
                new SchematicRegion("cubes", -13, bottom, 37, -13, bottom + 1, 37)), new SchematicOrigin(-13, bottom + 1, 37));
            Schematic captured = new Schematic(null, 1, 2, 1);
            captured.setRegions(selection.localRegions);
            captured.setOrigin(selection.localOrigin);
            SchematicContainer container = new SchematicContainer(captured, null, world, new File("cubic.schemplus"),
                -13, -13, bottom, bottom + 1, 37, 37);
            new ServerProxy().copyChunkToSchematic(container, -1, 2);
            ISchematic restored = new SchematicAlpha().readFromNBT(SchematicFileSnapshot.capture(captured).readNBT());
            assertSame(BLOCK, restored.getBlock(0, 0, 0));
            assertSame(BLOCK, restored.getBlock(0, 1, 0));
            assertEquals(2, restored.getBlockMetadata(0, 0, 0));
            assertEquals(3, restored.getBlockMetadata(0, 1, 0));
            assertEquals(1, restored.getOrigin().y);
            assertEquals(1, restored.getRegions().size());
        }
    }

    @Test public void movingAcrossCubesAndCancellingRestoresBothRegions() throws Exception {
        for (int bottom : new int[] {-17, -1, 255, 999999}) for (boolean cancel : new boolean[] {false, true}) {
            CubicWorld world = world(false);
            SilentBlockPlacement writer = new SilentBlockPlacement();
            for (int offset = 0; offset < 2; offset++) writer.setBlock(world, -13, bottom + offset, 37, BLOCK, offset + 1);
            BitSet selected = new BitSet();
            selected.set(0, 2);
            BlockMoveTransaction<Integer> move = new BlockMoveTransaction<>(-13, bottom, 37, 1, 2, 1, 0, 4096, 0, selected, 0);
            BlockMoveTransaction.Access<Integer> access = new BlockMoveTransaction.Access<Integer>() {
                @Override public Integer read(int x, int y, int z) { return world.getBlockMetadata(x, y, z); }
                @Override public void write(int x, int y, int z, Integer value) { writer.setBlock(world, x, y, z, value == 0 ? AIR : BLOCK, value); }
                @Override public void changed(int x, int y, int z) { world.markBlockForUpdate(x, y, z); }
            };
            boolean done = false;
            for (int i = 0; i < 50 && !done; i++) done = move.step(access, cancel && move.mutated());
            writer.flush(world);
            assertTrue(done);
            assertNull(move.failure());
            assertEquals(!cancel, move.successful());
            for (int offset = 0; offset < 2; offset++) {
                assertEquals(cancel ? offset + 1 : 0, world.getBlockMetadata(-13, bottom + offset, 37));
                assertEquals(cancel ? 0 : offset + 1, world.getBlockMetadata(-13, bottom + 4096 + offset, 37));
            }
        }
    }

    @Test public void clientSaveRejectsUnloadedCubesBeforeCopyingAir() throws Exception {
        CubicWorld world = world(false);
        world.isRemote = true;
        SchematicContainer container = new SchematicContainer(new Schematic(null, 1, 2, 1), null, world, new File("cubic.schemplus"),
            -13, -13, 255, 256, 37, 37);
        world.chunk.getCube(15);
        assertThrows(IllegalStateException.class, () -> new ServerProxy().copyChunkToSchematic(container, -1, 2));
        assertEquals(1, world.chunk.cubes.size());
    }

    @Test public void boundsAndCaptureUseTheTargetWorldHeight() throws Exception {
        CubicWorld world = world(false);
        for (int y : new int[] {-1000000, -1, 256, 1000000}) {
            WorldEditJob job = new WorldEditJob(UUID.randomUUID(), 0, WorldEditJob.Kind.FILL, -13, y, 37, 1, 1, 1, BLOCK, 0, null, 0);
            job.validate(world);
            WorldEditJob restored = WorldEditJob.read(job.write(), job.player, 0);
            assertEquals(y, restored.y);
            assertThrows(IllegalArgumentException.class, () -> restored.validate(null));
            SilentBlockPlacement.writeStorage(world.chunk, 3, y, 5, BLOCK, 6, true);
            WorldCaptureSource capture = new WorldCaptureSource(world, world);
            assertSame(BLOCK, capture.block(-13, y, 37));
            assertEquals(6, capture.meta(-13, y, 37));
        }
        assertThrows(IllegalArgumentException.class, () -> SchematicLimits.worldBounds(world, 0, 2000001, 0, 0, 2000001, 0));
        assertThrows(IllegalArgumentException.class, () -> SchematicLimits.worldBounds(null, 0, 255, 0, 0, 256, 0));
        SchematicLimits.worldBounds(null, 0, 0, 0, 0, 255, 0);
    }

    public static CubicWorld world(boolean modern) throws Exception {
        Class<?> unsafe = Class.forName("sun.misc.Unsafe");
        Field instance = unsafe.getDeclaredField("theUnsafe");
        instance.setAccessible(true);
        CubicWorld world = (CubicWorld) unsafe.getMethod("allocateInstance", Class.class).invoke(instance.get(null), CubicWorld.class);
        Field provider = World.class.getDeclaredField("provider");
        provider.setAccessible(true);
        provider.set(world, new WorldProviderSurface());
        world.cubeProvider = (IChunkProvider) Proxy.newProxyInstance(ICubeProvider.class.getClassLoader(),
            new Class<?>[] {IChunkProvider.class, ICubeProvider.class}, (proxy, method, args) -> {
                if (method.getReturnType() == boolean.class) return true;
                if (method.getReturnType() == int.class) return 0;
                return null;
            });
        world.lighting = new Lighting();
        world.updates = new ArrayList<>();
        world.lightChecks = new ArrayList<>();
        world.chunk = modern ? new ModernColumn(world) : new Column(world);
        return world;
    }

    public static final class CubicWorld extends WorldServer {
        public Column chunk;
        IChunkProvider cubeProvider;
        public Lighting lighting;
        List<Integer> updates, lightChecks;
        int setBlockCalls;
        private CubicWorld() { super(null, null, "", 0, null, null); }
        public int getMinHeight() { return -2000000; }
        public int getMaxHeight() { return 2000001; }
        public Lighting getLightingManager() { return lighting; }
        @Override public List getEntitiesWithinAABB(Class type, AxisAlignedBB box) { return Collections.emptyList(); }
        @Override public IChunkProvider getChunkProvider() { return cubeProvider; }
        @Override public Chunk getChunkFromChunkCoords(int x, int z) { return chunk; }
        @Override public Block getBlock(int x, int y, int z) { return chunk.getBlock(x & 15, y, z & 15); }
        @Override public int getBlockMetadata(int x, int y, int z) { return chunk.getBlockMetadata(x & 15, y, z & 15); }
        @Override public boolean setBlock(int x, int y, int z, Block block, int meta, int flags) { setBlockCalls++; fail("World placement callback"); return false; }
        @Override public boolean func_147451_t(int x, int y, int z) { lightChecks.add(y); return true; }
        @Override public void markBlockForUpdate(int x, int y, int z) { updates.add(y); }
    }

    public static class Column extends Chunk {
        public final Map<Integer, Cube> cubes = new LinkedHashMap<>();
        public final Heights opacity = new Heights();
        public final Heights stagingHeightMap = new Heights();
        Column(World world) { super(world, -1, 2); isChunkLoaded = true; }
        public Cube getCube(int y) { return cubes.computeIfAbsent(y, value -> new Cube(this, value)); }
        public Cube getLoadedCube(int y) { return cubes.get(y); }
        public Collection<Cube> getLoadedCubes() { return cubes.values(); }
        public Object getOpacityIndex() { return opacity; }
        public int getTopYWithStaging(int x, int z) { return Math.max(opacity.top(), stagingHeightMap.top()); }
        @Override public Block getBlock(int x, int y, int z) {
            ExtendedBlockStorage storage = getCube(y >> 4).storage;
            Block block = storage == null ? null : storage.getBlockByExtId(x, y & 15, z);
            return block == null ? AIR : block;
        }
        @Override public int getBlockMetadata(int x, int y, int z) {
            ExtendedBlockStorage storage = getCube(y >> 4).storage;
            return storage == null ? 0 : storage.getExtBlockMetadata(x, y & 15, z);
        }
    }

    public static final class ModernColumn extends Column {
        public final ModernHeights modern = new ModernHeights();
        public final Precipitation precipitation = new Precipitation();
        ModernColumn(World world) { super(world); }
        @Override public Object getOpacityIndex() { return modern; }
        @Override public int getTopYWithStaging(int x, int z) { return modern.top(); }
        public Precipitation getPrecipitationIndex() { return precipitation; }
    }

    public static final class Cube {
        final Column column;
        final int y;
        ExtendedBlockStorage storage;
        boolean dirty, tracked = true;
        Cube(Column column, int y) { this.column = column; this.y = y; }
        public ExtendedBlockStorage getOrCreateStorage() {
            if (storage == null) {
                storage = new ExtendedBlockStorage(y << 4, true);
                if (y >= 0 && y < 16) column.getBlockStorageArray()[y] = storage;
            }
            return storage;
        }
        public ExtendedBlockStorage getStorage() { return storage; }
        public void markDirty() { dirty = true; }
        public boolean isInitialLightingDone() { return true; }
        public boolean isSurfaceTracked() { return tracked; }
        public boolean isCubeLoaded() { return true; }
        public boolean isEmpty() { return storage == null || storage.isEmpty(); }
    }

    public static class Heights {
        final TreeSet<Integer> blocks = new TreeSet<>();
        public void onOpacityChange(int x, int y, int z, int opacity) { if (opacity > 0) blocks.add(y); else blocks.remove(y); }
        int top() { return blocks.isEmpty() ? -2000000 : blocks.last(); }
    }
    public static final class ModernHeights extends Heights {
        public void onOpacityChange(int x, int y, int z, boolean solid) { super.onOpacityChange(x, y, z, solid ? 1 : 0); }
    }
    public static final class Precipitation {
        int changes;
        public void onBlockChange(int x, int y, int z, Block block) { changes++; }
    }
    public static final class Lighting {
        int heights;
        final List<int[]> ranges = new ArrayList<>();
        public void onHeightUpdate(int x, int y, int z) { heights++; }
        public void doOnBlockSetLightUpdates(Chunk chunk, int x, int from, int to, int z) { ranges.add(new int[] {from, to}); }
    }
}

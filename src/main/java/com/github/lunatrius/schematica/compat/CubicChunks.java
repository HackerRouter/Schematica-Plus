package com.github.lunatrius.schematica.compat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public final class CubicChunks {
    private static final Class<?>[] XYZ = {int.class, int.class, int.class};
    private static final Class<?>[] XZ = {int.class, int.class};
    private static final Class<?>[] Y = {int.class};
    private static final ClassValue<Boolean> MODERN_HEIGHTMAP = new ClassValue<Boolean>() {
        @Override protected Boolean computeValue(Class<?> type) {
            try { type.getMethod("getPrecipitationIndex"); return true; }
            catch (NoSuchMethodException error) { return false; }
        }
    };

    private CubicChunks() {}

    public static boolean enabled(World world) {
        return world != null && Reflect.is(world.getChunkProvider(), "com.cardinalstar.cubicchunks.world.cube.ICubeProvider");
    }

    public static int minHeight(World world) { return enabled(world) ? (Integer) call(world, "getMinHeight") : 0; }
    public static int maxHeight(World world) { return enabled(world) ? (Integer) call(world, "getMaxHeight") : 256; }
    public static boolean containsY(World world, long y) { return y >= minHeight(world) && y < maxHeight(world); }

    public static boolean loaded(World world, int x, int y, int z) {
        if (!containsY(world, y) || !world.getChunkProvider().chunkExists(x >> 4, z >> 4)) return false;
        if (!enabled(world)) return world.blockExists(x, y, z);
        Object cube = call(world.getChunkFromChunkCoords(x >> 4, z >> 4), "getLoadedCube", Y, y >> 4);
        return cube != null && (Boolean) call(cube, "isCubeLoaded");
    }

    public static List<ExtendedBlockStorage> loadedStorage(Chunk chunk) {
        if (!enabled(chunk.worldObj)) return Arrays.asList(chunk.getBlockStorageArray());
        List<ExtendedBlockStorage> result = new ArrayList<>();
        for (Object cube : (Iterable<?>) call(chunk, "getLoadedCubes")) {
            if ((Boolean) call(cube, "isCubeLoaded")) result.add((ExtendedBlockStorage) call(cube, "getStorage"));
        }
        return result;
    }

    public static boolean emptySections(Chunk chunk, int first, int last) {
        for (int section = first; section <= last; section++) {
            Object cube = call(chunk, "getLoadedCube", Y, section);
            if (cube == null || !(Boolean) call(cube, "isCubeLoaded") || !(Boolean) call(cube, "isEmpty")) return false;
        }
        return true;
    }

    public static void write(Chunk chunk, int x, int y, int z, Block block, int metadata) {
        Object cube = call(chunk, "getCube", Y, y >> 4);
        int oldTop = (Integer) call(chunk, "getTopYWithStaging", XZ, x, z);
        ExtendedBlockStorage storage = (ExtendedBlockStorage) call(cube, "getOrCreateStorage");
        storage.func_150818_a(x, y & 15, z, block);
        storage.setExtBlockMetadata(x, y & 15, z, metadata);
        call(cube, "markDirty");
        chunk.isModified = true;
        chunk.precipitationHeightMap[z << 4 | x] = -999;
        Object lighting = call(chunk.worldObj, "getLightingManager");
        boolean modern = MODERN_HEIGHTMAP.get(chunk.getClass());
        boolean tracked = modern || (Boolean) call(cube, "isSurfaceTracked");
        Object heightmap;
        try { heightmap = tracked ? call(chunk, "getOpacityIndex") : Reflect.get(chunk, "stagingHeightMap"); }
        catch (ReflectiveOperationException error) { throw failure(error); }
        if (modern) {
            call(heightmap, "onOpacityChange", new Class<?>[] {int.class, int.class, int.class, boolean.class}, x, y, z, block.getLightOpacity() > 0);
            call(call(chunk, "getPrecipitationIndex"), "onBlockChange", new Class<?>[] {int.class, int.class, int.class, Block.class}, x, y, z, block);
        } else {
            call(heightmap, "onOpacityChange", new Class<?>[] {int.class, int.class, int.class, int.class}, x, y, z, block.getLightOpacity());
        }
        if (tracked) call(lighting, "onHeightUpdate", XYZ, (chunk.xPosition << 4) + x, y, (chunk.zPosition << 4) + z);
        int newTop = (Integer) call(chunk, "getTopYWithStaging", XZ, x, z);
        if (oldTop != newTop && !chunk.worldObj.provider.hasNoSky && (Boolean) call(cube, "isInitialLightingDone")) {
            call(lighting, "doOnBlockSetLightUpdates", new Class<?>[] {Chunk.class, int.class, int.class, int.class, int.class},
                chunk, x, oldTop + 1, newTop + 1, z);
        }
    }

    private static Object call(Object target, String name) { return call(target, name, new Class<?>[0]); }

    private static Object call(Object target, String name, Class<?>[] types, Object... args) {
        try { return Reflect.call(target, name, types, args); }
        catch (ReflectiveOperationException error) { throw failure(error); }
    }

    private static IllegalStateException failure(ReflectiveOperationException error) {
        return new IllegalStateException("Could not access Cubic Chunks world data", error);
    }
}

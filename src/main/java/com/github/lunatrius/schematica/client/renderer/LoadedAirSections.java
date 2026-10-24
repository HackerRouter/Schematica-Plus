package com.github.lunatrius.schematica.client.renderer;

import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

final class LoadedAirSections {
    private LoadedAirSections() {}

    static boolean containsOnlyAir(World world, long minX, long minY, long minZ, long maxX, long maxY, long maxZ) {
        if (world == null || minX > maxX || minY > maxY || minZ > maxZ) return false;
        if (minX < Integer.MIN_VALUE || maxX > Integer.MAX_VALUE || minZ < Integer.MIN_VALUE || maxZ > Integer.MAX_VALUE) return false;
        int firstY = (int) Math.max(0, Math.min(256, minY)) >> 4, lastY = (int) Math.min(255, Math.max(-1, maxY)) >> 4;
        for (int x = (int) (minX >> 4); x <= (maxX >> 4); x++) for (int z = (int) (minZ >> 4); z <= (maxZ >> 4); z++) {
            if (!world.getChunkProvider().chunkExists(x, z)) return false;
            Chunk chunk = world.getChunkFromChunkCoords(x, z);
            if (chunk == null || !chunk.isChunkLoaded || chunk instanceof EmptyChunk) return false;
            ExtendedBlockStorage[] sections = chunk.getBlockStorageArray();
            for (int y = firstY; y <= lastY; y++) {
                if (y >= sections.length) return false;
                if (sections[y] != null && !sections[y].isEmpty()) return false;
            }
        }
        return true;
    }
}

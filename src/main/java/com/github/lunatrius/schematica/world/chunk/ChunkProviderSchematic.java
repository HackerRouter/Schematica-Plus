package com.github.lunatrius.schematica.world.chunk;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.IChunkProvider;
import com.github.lunatrius.schematica.client.world.SchematicWorld;

public class ChunkProviderSchematic implements IChunkProvider {

    private final SchematicWorld world;
    private final Map<Long, Chunk> chunks = new HashMap<>();

    public ChunkProviderSchematic(SchematicWorld world) {
        this.world = world;
    }

    @Override
    public boolean chunkExists(int x, int y) {
        return world.getSchematic() != null && x >= 0 && y >= 0
            && x <= (world.getWidth() - 1) / 16 && y <= (world.getLength() - 1) / 16;
    }

    @Override
    public Chunk provideChunk(int x, int y) {
        if (!chunkExists(x, y)) return new EmptyChunk(world, x, y);
        long key = ((long) x << 32) | (y & 0xffffffffL);
        return chunks.computeIfAbsent(key, ignored -> new SchematicChunk(world, x, y));
    }

    @Override
    public Chunk loadChunk(int x, int y) {
        return provideChunk(x, y);
    }

    @Override
    public void populate(IChunkProvider provider, int x, int y) {}

    @Override
    public boolean saveChunks(boolean saveExtra, IProgressUpdate progressUpdate) {
        return true;
    }

    @Override
    public boolean unloadQueuedChunks() {
        return false;
    }

    @Override
    public boolean canSave() {
        return false;
    }

    @Override
    public String makeString() {
        return "SchematicChunkCache";
    }

    @Override
    public List<BiomeGenBase.SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, int x, int y, int z) {
        return null;
    }

    @Override
    public ChunkPosition func_147416_a(World world, String name, int x, int y, int z) {
        return null;
    }

    @Override
    public int getLoadedChunkCount() {
        return chunks.size();
    }

    @Override
    public void recreateStructures(int x, int y) {}

    @Override
    public void saveExtraData() {}
}

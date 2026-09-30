package com.github.lunatrius.schematica.client.gui.material;

import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.world.chunk.Chunk;
import org.junit.Test;
import static org.junit.Assert.*;

public class MaterialScanTest {
    @Test public void distinguishesUnreceivedClientChunksFromLoadedAirChunks() {
        ChunkProviderClient provider = new ChunkProviderClient(null);
        assertTrue(provider.chunkExists(100, 100));
        assertFalse(MaterialScan.verifiedChunk(provider.provideChunk(100, 100), 64));
        Chunk loadedAir = new Chunk(null, 100, 100);
        loadedAir.isChunkLoaded = true;
        assertTrue(MaterialScan.verifiedChunk(loadedAir, 64));
        assertFalse(MaterialScan.verifiedChunk(loadedAir, -1));
        assertFalse(MaterialScan.verifiedChunk(loadedAir, 256));
        loadedAir.isChunkLoaded = false;
        assertFalse(MaterialScan.verifiedChunk(loadedAir, 64));
    }
}

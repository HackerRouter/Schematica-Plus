package com.github.lunatrius.schematica.world.chunk;

import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import org.junit.Test;
import static org.junit.Assert.*;

public class SchematicChunkTest {
    @Test public void isAvailableToModsThatRejectEmptyChunks() {
        Chunk chunk = new SchematicChunk(null, 2, 3);
        assertFalse(chunk instanceof EmptyChunk);
        assertFalse(chunk.isEmpty());
        assertTrue(chunk.isAtLocation(2, 3));
        assertEquals(15, chunk.getSavedLightValue(EnumSkyBlock.Sky, 0, 64, 0));
    }

    @Test public void remainsReadOnlyWithoutWorldUpdates() {
        Chunk chunk = new SchematicChunk(null, 0, 0);
        assertFalse(chunk.func_150807_a(0, 64, 0, null, 0));
        assertFalse(chunk.setBlockMetadata(0, 64, 0, 1));
        chunk.func_150812_a(0, 64, 0, null);
        chunk.removeTileEntity(0, 64, 0);
        chunk.addEntity(null);
        chunk.removeEntity(null);
        chunk.onChunkLoad();
        chunk.onChunkUnload();
        chunk.setChunkModified();
        assertFalse(chunk.needsSaving(true));
        assertTrue(chunk.chunkTileEntityMap.isEmpty());
    }
}

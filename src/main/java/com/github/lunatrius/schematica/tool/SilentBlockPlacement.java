package com.github.lunatrius.schematica.tool;

import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import cpw.mods.fml.relauncher.ReflectionHelper;

final class SilentBlockPlacement {
    private static final java.lang.reflect.Method RELIGHT = ReflectionHelper.findMethod(Chunk.class, null,
        new String[] {"relightBlock", "func_76615_h"}, int.class, int.class, int.class);
    private final Map<Chunk, BitSet> changed = new LinkedHashMap<>();

    void setBlock(WorldServer world, int x, int y, int z, Block block, int metadata) {
        Chunk chunk = world.getChunkFromChunkCoords(x >> 4, z >> 4);
        TileEntity previous = (TileEntity) chunk.chunkTileEntityMap.remove(new ChunkPosition(x & 15, y, z & 15));
        if (previous != null) {
            world.loadedTileEntityList.remove(previous);
            boolean restoring = world.restoringBlockSnapshots;
            try {
                world.restoringBlockSnapshots = true;
                previous.invalidate();
            } finally {
                world.restoringBlockSnapshots = restoring;
            }
        }
        writeStorage(chunk, x & 15, y, z & 15, block, metadata, !world.provider.hasNoSky);
        changed.computeIfAbsent(chunk, ignored -> new BitSet()).set((y << 8) | ((z & 15) << 4) | (x & 15));
    }

    static void writeStorage(Chunk chunk, int x, int y, int z, Block block, int metadata, boolean skylight) {
        ExtendedBlockStorage[] storage = chunk.getBlockStorageArray();
        if (storage[y >> 4] == null) {
            ExtendedBlockStorage section = new ExtendedBlockStorage(y & ~15, skylight);
            if (skylight) {
                for (int sx = 0; sx < 16; sx++) {
                    for (int sz = 0; sz < 16; sz++) {
                        for (int sy = 0; sy < 16; sy++) {
                            section.setExtSkylightValue(sx, sy, sz, (y & ~15) + sy >= chunk.getHeightValue(sx, sz) ? 15 : 0);
                        }
                    }
                }
            }
            storage[y >> 4] = section;
        }
        storage[y >> 4].func_150818_a(x, y & 15, z, block);
        storage[y >> 4].setExtBlockMetadata(x, y & 15, z, metadata);
        chunk.precipitationHeightMap[(z << 4) | x] = -999;
        chunk.setChunkModified();
    }

    void setTile(WorldServer world, TileEntity tile) {
        world.getChunkFromChunkCoords(tile.xCoord >> 4, tile.zCoord >> 4).addTileEntity(tile);
        com.github.lunatrius.schematica.nbt.ForgeMultipart.sendDescription(world, tile);
    }

    void flush(WorldServer world) {
        for (Map.Entry<Chunk, BitSet> entry : changed.entrySet()) {
            Chunk chunk = entry.getKey();
            BitSet cells = entry.getValue();
            int[] tops = new int[256];
            for (int cell = cells.nextSetBit(0); cell >= 0; cell = cells.nextSetBit(cell + 1)) {
                tops[cell & 255] = (cell >> 8) + 1;
            }
            for (int column = 0; column < tops.length; column++) {
                if (tops[column] == 0) continue;
                try { RELIGHT.invoke(chunk, column & 15, tops[column], column >> 4); }
                catch (ReflectiveOperationException e) { throw new IllegalStateException("Could not update pasted block lighting", e); }
            }
            for (int cell = cells.nextSetBit(0); cell >= 0; cell = cells.nextSetBit(cell + 1)) {
                int x = (chunk.xPosition << 4) + (cell & 15);
                int y = cell >> 8;
                int z = (chunk.zPosition << 4) + ((cell >> 4) & 15);
                world.func_147451_t(x, y, z);
                world.markBlockForUpdate(x, y, z);
            }
        }
        changed.clear();
    }
}

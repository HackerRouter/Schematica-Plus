package com.github.lunatrius.schematica.world.chunk;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.chunk.EmptyChunk;
import com.github.lunatrius.schematica.client.world.SchematicWorld;

final class SchematicChunk extends EmptyChunk {
    private final SchematicWorld schematic;

    SchematicChunk(SchematicWorld world, int x, int z) {
        super(world, x, z);
        this.schematic = world;
    }

    @Override public Block getBlock(int x, int y, int z) {
        return schematic.getBlock((xPosition << 4) + x, y, (zPosition << 4) + z);
    }

    @Override public int getBlockMetadata(int x, int y, int z) {
        return schematic.getBlockMetadata((xPosition << 4) + x, y, (zPosition << 4) + z);
    }

    @Override public TileEntity func_150806_e(int x, int y, int z) {
        return schematic.getTileEntity((xPosition << 4) + x, y, (zPosition << 4) + z);
    }

    @Override public boolean isEmpty() { return false; }

    @Override public boolean getAreLevelsEmpty(int minY, int maxY) {
        return maxY < 0 || minY >= schematic.getHeight();
    }

    @Override public int getSavedLightValue(EnumSkyBlock type, int x, int y, int z) { return 15; }

    @Override public int getBlockLightValue(int x, int y, int z, int amount) { return 15; }

    @Override public int getHeightValue(int x, int z) {
        for (int y = schematic.getHeight() - 1; y >= 0; y--) {
            if (!schematic.isAirBlock((xPosition << 4) + x, y, (zPosition << 4) + z)) return y + 1;
        }
        return 0;
    }

    @Override public boolean canBlockSeeTheSky(int x, int y, int z) { return y >= getHeightValue(x, z); }
}

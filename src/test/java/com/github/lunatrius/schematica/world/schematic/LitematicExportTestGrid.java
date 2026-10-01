package com.github.lunatrius.schematica.world.schematic;

import java.util.Collections;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import com.github.lunatrius.schematica.api.ISchematic;

/** A 2x1x1 schematic: the given block with metadata 5, then air. */
final class LitematicExportTestGrid implements ISchematic {
    private final Block block;

    LitematicExportTestGrid(Block block) { this.block = block; }

    @Override public Block getBlock(int x, int y, int z) { return x == 0 ? block : null; }
    @Override public boolean setBlock(int x, int y, int z, Block block) { return false; }
    @Override public boolean setBlock(int x, int y, int z, Block block, int metadata) { return false; }
    @Override public TileEntity getTileEntity(int x, int y, int z) { return null; }
    @Override public List<TileEntity> getTileEntities() { return Collections.emptyList(); }
    @Override public void setTileEntity(int x, int y, int z, TileEntity tileEntity) {}
    @Override public void removeTileEntity(int x, int y, int z) {}
    @Override public int getBlockMetadata(int x, int y, int z) { return x == 0 ? 5 : 0; }
    @Override public boolean setBlockMetadata(int x, int y, int z, int metadata) { return false; }
    @Override public List<Entity> getEntities() { return Collections.emptyList(); }
    @Override public void addEntity(Entity entity) {}
    @Override public void removeEntity(Entity entity) {}
    @Override public ItemStack getIcon() { return null; }
    @Override public void setIcon(ItemStack icon) {}
    @Override public int getWidth() { return 2; }
    @Override public int getLength() { return 1; }
    @Override public int getHeight() { return 1; }
}

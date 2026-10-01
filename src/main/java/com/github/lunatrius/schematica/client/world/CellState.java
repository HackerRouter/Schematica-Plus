package com.github.lunatrius.schematica.client.world;

import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.world.storage.Schematic;
import com.github.lunatrius.schematica.world.storage.SchematicCopies;

/** A block, metadata and optional tile entity template positioned at 0,0,0. */
public final class CellState {
    public static final CellState AIR = new CellState(Blocks.air, 0, null);

    public final Block block;
    public final int meta;
    public final TileEntity tile;

    public CellState(Block block, int meta, TileEntity tile) {
        this.block = block == null ? Blocks.air : block;
        this.meta = meta;
        this.tile = tile;
    }

    public static CellState of(Block block, int meta) {
        if (block == null || block == Blocks.air) return AIR;
        boolean tile;
        try { tile = block.hasTileEntity(meta); }
        catch (RuntimeException | LinkageError error) { tile = false; }
        if (!tile) return new CellState(block, meta, null);
        Scratch world = Scratch.create();
        world.setBlock(0, 0, 0, block, meta, 3);
        return world.state();
    }

    public static CellState read(ISchematic schematic, int x, int y, int z) {
        TileEntity tile = schematic.getTileEntity(x, y, z);
        return new CellState(schematic.getBlock(x, y, z), schematic.getBlockMetadata(x, y, z), tile == null ? null : SchematicCopies.tile(tile, x, y, z));
    }

    public boolean isAir() { return block == Blocks.air; }
    public boolean same(CellState other) { return other != null && block == other.block && meta == other.meta; }
    public boolean matches(ISchematic schematic, int x, int y, int z) {
        return schematic.getBlock(x, y, z) == block && schematic.getBlockMetadata(x, y, z) == meta;
    }

    public CellState transform(List<String> operations, Map<String, CellState> cache) {
        if (operations.isEmpty() || isAir()) return this;
        String key = tile == null ? Block.getIdFromBlock(block) + ":" + meta + ":" + String.join("", operations) : null;
        CellState cached = key == null ? null : cache.get(key);
        if (cached != null) return cached;
        Schematic cell = new Schematic(SchematicWorld.DEFAULT_ICON, 1, 1, 1);
        cell.setBlock(0, 0, 0, block, meta);
        if (tile != null) {
            TileEntity copy = SchematicCopies.tile(tile, 0, 0, 0);
            cell.setTileEntity(0, 0, 0, copy);
        }
        SchematicWorld world = new SchematicWorld(cell);
        PlacementState.applyTransforms(world, operations);
        TileEntity transformed = world.getTileEntity(0, 0, 0);
        CellState result = new CellState(world.getBlock(0, 0, 0), world.getBlockMetadata(0, 0, 0),
            transformed == null ? null : SchematicCopies.tile(transformed, 0, 0, 0));
        if (key != null) cache.put(key, result);
        return result;
    }

    /** Writes this state into a schematic, keeping an existing tile entity only when the block itself is unchanged. */
    public void write(ISchematic target, int x, int y, int z) {
        Block previous = target.getBlock(x, y, z);
        if (!target.setBlock(x, y, z, block, meta)) return;
        if (tile != null) {
            TileEntity copy = SchematicCopies.tile(tile, -x, -y, -z);
            target.setTileEntity(x, y, z, copy);
        } else if (previous != block || isAir()) target.removeTileEntity(x, y, z);
    }

    /** A detached one-block world in which placement code can run without touching a real world. */
    public static final class Scratch extends SchematicWorld {
        private Scratch(Schematic schematic) { super(schematic); }

        public static Scratch create() { return new Scratch(new Schematic(DEFAULT_ICON, 1, 1, 1)); }

        @Override
        public boolean setBlock(int x, int y, int z, Block block, int metadata, int flags) {
            boolean changed = super.setBlock(x, y, z, block, metadata, flags);
            if (changed && getTileEntity(x, y, z) == null) {
                try {
                    if (block.hasTileEntity(metadata)) {
                        TileEntity tile = block.createTileEntity(this, metadata);
                        if (tile != null) setTileEntity(x, y, z, tile);
                    }
                } catch (RuntimeException | LinkageError error) {
                    Reference.logger.warn("Could not create a preview tile entity for {}", block, error);
                }
            }
            return changed;
        }

        public CellState state() {
            TileEntity tile = getTileEntity(0, 0, 0);
            TileEntity copy = null;
            if (tile != null) {
                try { copy = SchematicCopies.tile(tile, 0, 0, 0); }
                catch (IllegalArgumentException error) { Reference.logger.warn("Could not capture a preview tile entity", error); }
            }
            return new CellState(getBlock(0, 0, 0), getBlockMetadata(0, 0, 0), copy);
        }
    }
}

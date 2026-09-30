package com.github.lunatrius.schematica.tool;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.nbt.ForgeMultipart;
import com.github.lunatrius.schematica.nbt.NBTHelper;
import com.github.lunatrius.schematica.util.SchematicLimits;
import cpw.mods.fml.common.registry.GameData;

/** A snapshot of an edit. Only the server tick queue advances its mutable cursor. */
public final class WorldEditJob {
    public enum Kind { PASTE, FILL, REPLACE }
    public final UUID player;
    public final int dimension, x, y, z, width, height, length, volume;
    public final Kind kind;
    public volatile boolean cancelled;
    private final Block replacement, target;
    private final int replacementMeta, targetMeta;
    private short[] blocks;
    private byte[] metadata;
    private final Map<Integer, NBTTagCompound> tiles = new HashMap<>();
    private final List<NBTTagCompound> entities = new ArrayList<>();
    private final BitSet placed = new BitSet();
    private int cursor, phase, entityCursor;
    public int blockCount, entityCount;

    public WorldEditJob(UUID player, int dimension, Kind kind, int x, int y, int z,
        int width, int height, int length, Block replacement, int replacementMeta, Block target, int targetMeta) {
        SchematicLimits.worldBounds(x, y, z, (long) x + width - 1, (long) y + height - 1, (long) z + length - 1);
        this.player = player; this.dimension = dimension; this.kind = kind;
        this.x = x; this.y = y; this.z = z;
        this.width = width; this.height = height; this.length = length;
        this.volume = SchematicLimits.volume(width, height, length);
        this.replacement = replacement; this.replacementMeta = replacementMeta;
        this.target = target; this.targetMeta = targetMeta;
    }

    public void capture(ISchematic source, boolean blockNBT, boolean includeEntities) {
        this.blocks = new short[volume];
        this.metadata = new byte[volume];
        for (int i = 0; i < volume; i++) {
            int sx = i % width, sz = i / width % length, sy = i / width / length;
            blocks[i] = (short) GameData.getBlockRegistry().getId(source.getBlock(sx, sy, sz));
            metadata[i] = (byte) source.getBlockMetadata(sx, sy, sz);
        }
        if (blockNBT) {
            for (TileEntity tile : source.getTileEntities()) {
                if (tile.xCoord < 0 || tile.xCoord >= width || tile.yCoord < 0 || tile.yCoord >= height
                    || tile.zCoord < 0 || tile.zCoord >= length) continue;
                NBTTagCompound tag = NBTHelper.writeTileEntityToCompound(tile);
                tiles.put(tile.xCoord + width * (tile.zCoord + length * tile.yCoord), tag);
            }
        }
        if (includeEntities) {
            for (Entity entity : source.getEntities()) {
                NBTTagCompound tag = NBTHelper.writeEntityToCompound(entity);
                if (tag != null) entities.add(tag);
            }
        }
    }

    public void validateCommandFallback() {
        if (!tiles.isEmpty() || !entities.isEmpty()) {
            throw new IllegalArgumentException("1.7.10 chat commands cannot safely transfer schematic NBT. "
                + "Use an integrated server, or disable block NBT and entities before pasting.");
        }
        java.util.Set<Block> checked = new java.util.HashSet<>();
        for (int i = 0; i < (kind == Kind.PASTE ? volume : 1); i++) {
            Block block = kind == Kind.PASTE ? GameData.getBlockRegistry().getObjectById(blocks[i] & 0xffff) : replacement;
            if (block != null && checked.add(block)) {
                String name = GameData.getBlockRegistry().getNameForObject(block);
                if (name == null || ("/setblock -30000000 255 -30000000 " + name + " 15 replace").length() > 100) {
                    throw new IllegalArgumentException("Block name exceeds the 1.7.10 command length limit");
                }
            }
        }
    }

    public String command(int index, net.minecraft.world.World world) {
        int wx = x + index % width, wz = z + index / width % length, wy = y + index / width / length;
        if (kind == Kind.REPLACE && (world.getBlock(wx, wy, wz) != target
            || world.getBlockMetadata(wx, wy, wz) != targetMeta)) return null;
        Block block = kind == Kind.PASTE ? GameData.getBlockRegistry().getObjectById(blocks[index] & 0xffff) : replacement;
        if (block == null || (kind == Kind.PASTE && block == Blocks.air)) return null;
        int meta = kind == Kind.PASTE ? metadata[index] & 15 : replacementMeta;
        return "/setblock " + wx + " " + wy + " " + wz + " "
            + GameData.getBlockRegistry().getNameForObject(block) + " " + meta + " replace";
    }

    /** Process one cell/entity, returning true only when all phases are finished. */
    public boolean step(WorldServer world) {
        if (cancelled && phase < 2) { phase = 2; cursor = 0; }
        if (phase < 3) {
            if (cursor == volume) { cursor = 0; phase++; return false; }
            int index = cursor++;
            int wx = x + index % width, wz = z + index / width % length, wy = y + index / width / length;
            Block block = kind == Kind.PASTE ? GameData.getBlockRegistry().getObjectById(blocks[index] & 0xffff) : replacement;
            int meta = kind == Kind.PASTE ? metadata[index] & 15 : replacementMeta;
            if (block == null || (kind == Kind.PASTE && block == Blocks.air)) return false;
            if (phase == 2) {
                if (!placed.get(index)) return false;
                world.markBlockForUpdate(wx, wy, wz);
                world.notifyBlocksOfNeighborChange(wx, wy, wz, block);
                return false;
            }
            // Structural blocks first, then decorations. Neighbour notifications wait until phase 2.
            boolean structural = block.getMaterial().isSolid();
            if ((phase == 0) != structural) return false;
            if (kind == Kind.REPLACE && (world.getBlock(wx, wy, wz) != target
                || world.getBlockMetadata(wx, wy, wz) != targetMeta)) return false;
            if (kind == Kind.PASTE) world.removeTileEntity(wx, wy, wz);
            boolean previous = world.restoringBlockSnapshots;
            try {
                world.restoringBlockSnapshots = true; // Suppress item drops from replaced inventories.
                world.setBlock(wx, wy, wz, block, meta, 2);
            } finally {
                world.restoringBlockSnapshots = previous;
            }
            if (world.getBlock(wx, wy, wz) == block && world.getBlockMetadata(wx, wy, wz) == meta) {
                if (kind == Kind.PASTE && block.hasTileEntity(meta)) {
                    NBTTagCompound tag = tiles.get(index);
                    TileEntity tile;
                    if (tag != null) {
                        tag = (NBTTagCompound) tag.copy();
                        tag.setInteger("x", wx); tag.setInteger("y", wy); tag.setInteger("z", wz);
                        tile = "savedMultipart".equals(tag.getString("id"))
                            ? ForgeMultipart.createFromNBT(tag, false) : TileEntity.createAndLoadEntity(tag);
                    } else tile = block.createTileEntity(world, meta);
                    if (tile == null) throw new IllegalStateException("Cannot create tile entity at " + wx + "," + wy + "," + wz);
                    world.setTileEntity(wx, wy, wz, tile);
                    tile.markDirty();
                }
                placed.set(index);
                blockCount++;
            }
            return false;
        }
        if (cancelled || entityCursor == entities.size()) return true;
        NBTTagCompound tag = (NBTTagCompound) entities.get(entityCursor++).copy();
        NBTTagList oldPos = tag.getTagList("Pos", 6);
        NBTTagList pos = new NBTTagList();
        pos.appendTag(new NBTTagDouble(oldPos.func_150309_d(0) + x));
        pos.appendTag(new NBTTagDouble(oldPos.func_150309_d(1) + y));
        pos.appendTag(new NBTTagDouble(oldPos.func_150309_d(2) + z));
        tag.setTag("Pos", pos);
        if (tag.hasKey("TileX")) {
            tag.setInteger("TileX", tag.getInteger("TileX") + x);
            tag.setInteger("TileY", tag.getInteger("TileY") + y);
            tag.setInteger("TileZ", tag.getInteger("TileZ") + z);
        }
        tag.removeTag("UUIDMost"); tag.removeTag("UUIDLeast");
        Entity entity = EntityList.createEntityFromNBT(tag, world);
        if (entity != null && world.spawnEntityInWorld(entity)) entityCount++;
        return false;
    }
}

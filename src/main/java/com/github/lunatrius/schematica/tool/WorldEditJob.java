package com.github.lunatrius.schematica.tool;

import com.github.lunatrius.schematica.util.MessageException;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
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
import com.github.lunatrius.schematica.task.TaskRegistry;
import cpw.mods.fml.common.registry.GameData;

/** A snapshot of an edit. Only the server tick queue advances its mutable cursor. */
public final class WorldEditJob extends WorldEditTask {
    public enum Kind { PASTE, FILL, REPLACE, DELETE_PLACEMENT }
    public final int width, height, length, volume;
    public final Kind kind;
    public final boolean pasteWithoutUpdates;
    public final ReplaceBehavior replace;
    /** For DELETE_PLACEMENT: which non-air world blocks inside the placement are removed. */
    public PlacementDeletionMode deletion = PlacementDeletionMode.ENTIRE_VOLUME;
    private List<com.github.lunatrius.schematica.api.SchematicRegion> entityRemoval = new ArrayList<>();
    private boolean entitiesRemoved;
    private final Block replacement, target;
    private final int replacementMeta, targetMeta;
    private short[] blocks;
    /** Block metadata, 16 bits for EndlessIDs extended metadata (GTNH 2.9 frames store the material there). */
    private short[] metadata;
    /** commandNameSetblock */
    public static volatile String setblockCommand = "setblock";
    private final Map<Integer, NBTTagCompound> tiles = new HashMap<>();
    private final List<NBTTagCompound> entities = new ArrayList<>();
    private final BitSet placed = new BitSet();
    private BitSet selected;
    private final SilentBlockPlacement silentPlacement;
    private int cursor, phase, entityCursor;
    private boolean metadataPassComplete;

    public WorldEditJob(UUID player, int dimension, Kind kind, int x, int y, int z,
        int width, int height, int length, Block replacement, int replacementMeta, Block target, int targetMeta) {
        this(player, dimension, kind, x, y, z, width, height, length, replacement, replacementMeta, target, targetMeta, false, ReplaceBehavior.ALL);
    }

    public WorldEditJob(UUID player, int dimension, Kind kind, int x, int y, int z,
        int width, int height, int length, Block replacement, int replacementMeta, Block target, int targetMeta,
        boolean pasteWithoutUpdates, ReplaceBehavior replace) {
        super(player, dimension, x, y, z);
        SchematicLimits.worldBounds(x, y, z, (long) x + width - 1, (long) y + height - 1, (long) z + length - 1);
        this.kind = kind;
        this.width = width; this.height = height; this.length = length;
        this.volume = SchematicLimits.volume(width, height, length);
        this.replacement = replacement; this.replacementMeta = replacementMeta;
        this.target = target; this.targetMeta = targetMeta;
        this.pasteWithoutUpdates = kind == Kind.PASTE && pasteWithoutUpdates;
        this.replace = kind == Kind.PASTE ? replace : ReplaceBehavior.ALL;
        this.silentPlacement = this.pasteWithoutUpdates ? new SilentBlockPlacement() : null;
        this.metadataPassComplete = kind != Kind.PASTE || this.pasteWithoutUpdates;
    }

    public void capture(ISchematic source, boolean blockNBT, boolean includeEntities) {
        capture(source, blockNBT, includeEntities, null, false);
    }

    /**
     * bounds: the local [minX, minY, minZ, maxX, maxY, maxZ) part to paste (pasteLayerBehavior rendered_only), or null for all;
     * ignoreInventories: containers are pasted empty (pasteIgnoreInventories).
     */
    public void capture(ISchematic source, boolean blockNBT, boolean includeEntities, int[] bounds, boolean ignoreInventories) {
        java.util.List<com.github.lunatrius.schematica.api.SchematicRegion> regions = source.getRegions();
        if (bounds != null) {
            if (regions.isEmpty()) regions = java.util.Collections.singletonList(new com.github.lunatrius.schematica.api.SchematicRegion(
                "all", 0, 0, 0, width - 1, height - 1, length - 1));
            java.util.List<com.github.lunatrius.schematica.api.SchematicRegion> clipped = new java.util.ArrayList<>();
            for (com.github.lunatrius.schematica.api.SchematicRegion region : regions) {
                int ax = Math.max(region.minX, bounds[0]), ay = Math.max(region.minY, bounds[1]), az = Math.max(region.minZ, bounds[2]);
                int bx = Math.min(region.maxX, bounds[3] - 1), by = Math.min(region.maxY, bounds[4] - 1), bz = Math.min(region.maxZ, bounds[5] - 1);
                if (ax <= bx && ay <= by && az <= bz) clipped.add(new com.github.lunatrius.schematica.api.SchematicRegion(region.name, ax, ay, az, bx, by, bz));
            }
            if (clipped.isEmpty()) throw new MessageException("schematica.message.paste.outside_layers");
            regions = clipped;
        }
        setRegions(regions);
        this.blocks = new short[volume];
        this.metadata = new short[volume];
        for (int i = 0; i < volume; i++) {
            int sx = i % width, sz = i / width % length, sy = i / width / length;
            blocks[i] = (short) GameData.getBlockRegistry().getId(source.getBlock(sx, sy, sz));
            metadata[i] = (short) source.getBlockMetadata(sx, sy, sz);
        }
        if (blockNBT) {
            for (TileEntity tile : source.getTileEntities()) {
                if (!source.containsBlock(tile.xCoord, tile.yCoord, tile.zCoord)) continue;
                if (tile.xCoord < 0 || tile.xCoord >= width || tile.yCoord < 0 || tile.yCoord >= height
                    || tile.zCoord < 0 || tile.zCoord >= length) continue;
                if (bounds != null && !inside(bounds, tile.xCoord, tile.yCoord, tile.zCoord)) continue;
                NBTTagCompound tag = NBTHelper.writeTileEntityToCompound(tile);
                if (ignoreInventories && tile instanceof net.minecraft.inventory.IInventory) tag = emptied(tile, tag);
                com.github.lunatrius.schematica.nbt.TileEntitySnapshots.removeVisualData(tag);
                tiles.put(tile.xCoord + width * (tile.zCoord + length * tile.yCoord), tag);
            }
        }
        if (includeEntities) {
            for (Entity entity : source.getEntities()) {
                if (bounds != null && !inside(bounds, (int) Math.floor(entity.posX), (int) Math.floor(entity.posY), (int) Math.floor(entity.posZ))) continue;
                NBTTagCompound tag = NBTHelper.writeEntityToCompound(entity);
                if (tag != null) entities.add(tag);
            }
        }
    }

    private static boolean inside(int[] bounds, int x, int y, int z) {
        return x >= bounds[0] && y >= bounds[1] && z >= bounds[2] && x < bounds[3] && y < bounds[4] && z < bounds[5];
    }

    /** The tile entity's data with its slots cleared, read back through a copy so mod containers clear their own items. */
    private static NBTTagCompound emptied(TileEntity tile, NBTTagCompound tag) {
        try {
            TileEntity copy = NBTHelper.readTileEntityFromCompound((NBTTagCompound) tag.copy());
            if (copy instanceof net.minecraft.inventory.IInventory) {
                net.minecraft.inventory.IInventory inventory = (net.minecraft.inventory.IInventory) copy;
                for (int i = 0; i < inventory.getSizeInventory(); i++) inventory.setInventorySlotContents(i, null);
                NBTTagCompound cleared = NBTHelper.writeTileEntityToCompound(copy);
                if (cleared != null) return cleared;
            }
        } catch (RuntimeException error) {
            com.github.lunatrius.schematica.reference.Reference.logger.debug("Could not empty a pasted container", error);
        }
        tag.removeTag("Items");
        return tag;
    }

    /** pasteIgnoreBlockEntitiesEntirely: command pasting leaves block entity data out instead of refusing. */
    public void dropTiles() { tiles.clear(); }

    public boolean hasCommandNbt() { return !tiles.isEmpty() || !entities.isEmpty(); }

    /** Only the queued snapshot is changed; the loaded schematic keeps its complete data. */
    public void dropCommandNbt() { tiles.clear(); entities.clear(); }

    public void validateCommandFallback() {
        validateCommandFallback(false);
    }

    public void validateCommandFallback(boolean blocksOnly) {
        if (pasteWithoutUpdates) {
            throw new MessageException("schematica.message.edit.updates_require_singleplayer");
        }
        if (!blocksOnly && hasCommandNbt()) {
            throw new MessageException("schematica.message.edit.nbt_requires_singleplayer");
        }
        java.util.Set<Block> checked = new java.util.HashSet<>();
        if (kind == Kind.DELETE_PLACEMENT) checked.add(replacement);
        for (int i = 0; i < (kind == Kind.PASTE ? volume : 1); i++) {
            Block block = kind == Kind.PASTE ? GameData.getBlockRegistry().getObjectById(blocks[i] & 0xffff) : replacement;
            if (block != null && checked.add(block)) {
                String name = GameData.getBlockRegistry().getNameForObject(block);
                if (name == null || blockCommand(-30000000, 255, -30000000, name, 15).length() > 100) {
                    throw new MessageException("schematica.message.edit.command_too_long");
                }
            }
        }
    }

    public String command(int index, net.minecraft.world.World world) {
        if (selected != null && !selected.get(index)) return null;
        int wx = x + index % width, wz = z + index / width % length, wy = y + index / width / length;
        if (kind == Kind.REPLACE && (world.getBlock(wx, wy, wz) != target
            || world.getBlockMetadata(wx, wy, wz) != targetMeta)) return null;
        if (kind == Kind.DELETE_PLACEMENT && !deletes(index, world, wx, wy, wz)) return null;
        Block block = kind == Kind.PASTE ? GameData.getBlockRegistry().getObjectById(blocks[index] & 0xffff) : replacement;
        if (block == null || !pastes(block, world.isAirBlock(wx, wy, wz))) return null;
        int meta = kind == Kind.PASTE ? metadata[index] & 0xffff : replacementMeta;
        if (world.blockExists(wx, wy, wz) && world.getBlock(wx, wy, wz) == block
            && world.getBlockMetadata(wx, wy, wz) == meta) return null;
        return blockCommand(wx, wy, wz, GameData.getBlockRegistry().getNameForObject(block), meta);
    }

    /** Encodes the whole edit for the remote edit protocol; blocks are stored by registry name. */
    public NBTTagCompound write() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("version", 1);
        tag.setString("kind", kind.name());
        tag.setIntArray("bounds", new int[] {x, y, z, width, height, length});
        tag.setBoolean("updates", pasteWithoutUpdates);
        tag.setString("replace", replace.value);
        tag.setString("deletion", deletion.value);
        if (replacement != null) { tag.setString("replacement", name(replacement)); tag.setInteger("replacementMeta", replacementMeta); }
        if (target != null) { tag.setString("target", name(target)); tag.setInteger("targetMeta", targetMeta); }
        if (selected != null) tag.setByteArray("mask", selected.toByteArray());
        if (blocks != null) {
            List<String> palette = new ArrayList<>();
            Map<Short, Integer> indices = new HashMap<>();
            byte[] cells = new byte[volume * 2];
            for (int i = 0; i < volume; i++) {
                Integer index = indices.get(blocks[i]);
                if (index == null) {
                    Block block = GameData.getBlockRegistry().getObjectById(blocks[i] & 0xffff);
                    index = palette.size();
                    palette.add(block == null ? "minecraft:air" : name(block));
                    indices.put(blocks[i], index);
                }
                cells[i * 2] = (byte) (index >> 8);
                cells[i * 2 + 1] = (byte) (int) index;
            }
            NBTTagList names = new NBTTagList();
            for (String entry : palette) names.appendTag(new net.minecraft.nbt.NBTTagString(entry));
            tag.setTag("palette", names);
            tag.setByteArray("cells", cells);
            byte[] low = new byte[volume], high = new byte[volume];
            boolean extended = false;
            for (int i = 0; i < volume; i++) {
                low[i] = (byte) metadata[i];
                high[i] = (byte) (metadata[i] >> 8);
                extended |= high[i] != 0;
            }
            tag.setByteArray("meta", low);
            if (extended) tag.setByteArray("metaHigh", high);
        }
        NBTTagList tileList = new NBTTagList();
        for (Map.Entry<Integer, NBTTagCompound> entry : tiles.entrySet()) {
            NBTTagCompound tile = new NBTTagCompound();
            tile.setInteger("index", entry.getKey());
            tile.setTag("data", entry.getValue());
            tileList.appendTag(tile);
        }
        tag.setTag("tiles", tileList);
        NBTTagList entityList = new NBTTagList();
        for (NBTTagCompound entity : entities) entityList.appendTag(entity);
        tag.setTag("entities", entityList);
        NBTTagList removal = new NBTTagList();
        for (com.github.lunatrius.schematica.api.SchematicRegion box : entityRemoval) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setIntArray("box", new int[] {box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ});
            removal.appendTag(entry);
        }
        tag.setTag("removeEntities", removal);
        return tag;
    }

    private static String name(Block block) {
        String name = GameData.getBlockRegistry().getNameForObject(block);
        if (name == null) throw new IllegalArgumentException("Unregistered block");
        return name;
    }

    private static Block block(String name) {
        if (!GameData.getBlockRegistry().containsKey(name)) throw new IllegalArgumentException("Unknown block " + name);
        return GameData.getBlockRegistry().getObject(name);
    }

    /** Decodes a remote edit, checking every size and reference against this server. */
    public static WorldEditJob read(NBTTagCompound tag, UUID player, int dimension) {
        if (tag.getInteger("version") != 1) throw new IllegalArgumentException("Unsupported edit version");
        Kind kind = Kind.valueOf(tag.getString("kind"));
        int[] bounds = tag.getIntArray("bounds");
        if (bounds.length != 6) throw new IllegalArgumentException("Invalid edit bounds");
        Block replacement = tag.hasKey("replacement", 8) ? block(tag.getString("replacement")) : null;
        Block target = tag.hasKey("target", 8) ? block(tag.getString("target")) : null;
        if ((kind == Kind.FILL || kind == Kind.REPLACE || kind == Kind.DELETE_PLACEMENT) && replacement == null) throw new IllegalArgumentException("Missing fill block");
        if (kind == Kind.REPLACE && target == null) throw new IllegalArgumentException("Missing target block");
        if (kind == Kind.DELETE_PLACEMENT && replacement != Blocks.air) throw new IllegalArgumentException("Deletion must place air");
        WorldEditJob job = new WorldEditJob(player, dimension, kind, bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5],
            replacement, tag.getInteger("replacementMeta") & 0xffff, target, tag.getInteger("targetMeta") & 0xffff,
            tag.getBoolean("updates"), ReplaceBehavior.parse(tag.getString("replace")));
        job.deletion = PlacementDeletionMode.parse(tag.getString("deletion"));
        if (tag.hasKey("mask", 7)) {
            BitSet mask = BitSet.valueOf(tag.getByteArray("mask"));
            if (mask.length() > job.volume) throw new IllegalArgumentException("Invalid region mask");
            job.selected = mask;
        }
        if (kind == Kind.PASTE || kind == Kind.DELETE_PLACEMENT) {
            NBTTagList names = tag.getTagList("palette", 8);
            byte[] cells = tag.getByteArray("cells"), meta = tag.getByteArray("meta"), metaHigh = tag.getByteArray("metaHigh");
            if (names.tagCount() == 0 || names.tagCount() > 65536 || cells.length != job.volume * 2 || meta.length != job.volume
                || metaHigh.length != 0 && metaHigh.length != job.volume) {
                throw new IllegalArgumentException("Invalid edit blocks");
            }
            short[] ids = new short[names.tagCount()];
            for (int i = 0; i < ids.length; i++) ids[i] = (short) GameData.getBlockRegistry().getId(block(names.getStringTagAt(i)));
            job.blocks = new short[job.volume];
            for (int i = 0; i < job.volume; i++) {
                int index = (cells[i * 2] & 0xff) << 8 | cells[i * 2 + 1] & 0xff;
                if (index >= ids.length) throw new IllegalArgumentException("Invalid palette index");
                job.blocks[i] = ids[index];
            }
            job.metadata = new short[job.volume];
            for (int i = 0; i < job.volume; i++) job.metadata[i] = (short) ((meta[i] & 0xff) | (metaHigh.length == 0 ? 0 : (metaHigh[i] & 0xff) << 8));
        }
        NBTTagList tileList = tag.getTagList("tiles", 10);
        if (kind != Kind.PASTE && tileList.tagCount() > 0) throw new IllegalArgumentException("Tile data outside a paste");
        for (int i = 0; i < tileList.tagCount(); i++) {
            NBTTagCompound tile = tileList.getCompoundTagAt(i);
            int index = tile.getInteger("index");
            if (index < 0 || index >= job.volume || !tile.hasKey("data", 10)) throw new IllegalArgumentException("Invalid tile entry");
            job.tiles.put(index, tile.getCompoundTag("data"));
        }
        NBTTagList entityList = tag.getTagList("entities", 10);
        if (kind != Kind.PASTE && entityList.tagCount() > 0) throw new IllegalArgumentException("Entities outside a paste");
        for (int i = 0; i < entityList.tagCount(); i++) job.entities.add(entityList.getCompoundTagAt(i));
        NBTTagList removal = tag.getTagList("removeEntities", 10);
        if (removal.tagCount() > 256) throw new IllegalArgumentException("Too many entity boxes");
        for (int i = 0; i < removal.tagCount(); i++) {
            int[] box = removal.getCompoundTagAt(i).getIntArray("box");
            if (box.length != 6 || box[0] < job.x || box[1] < job.y || box[2] < job.z
                || box[3] >= job.x + job.width || box[4] >= job.y + job.height || box[5] >= job.z + job.length
                || box[0] > box[3] || box[1] > box[4] || box[2] > box[5]) throw new IllegalArgumentException("Invalid entity box");
            job.entityRemoval.add(new com.github.lunatrius.schematica.api.SchematicRegion(Integer.toString(i), box[0], box[1], box[2], box[3], box[4], box[5]));
        }
        return job;
    }

    /** TaskDeleteBlocksByPlacement: only non-air world blocks the deletion mode selects. */
    private boolean deletes(int index, net.minecraft.world.World world, int wx, int wy, int wz) {
        Block existing = world.getBlock(wx, wy, wz);
        if (existing.isAir(world, wx, wy, wz)) return false;
        Block schematic = GameData.getBlockRegistry().getObjectById(blocks[index] & 0xffff);
        boolean schematicAir = schematic == null || schematic == Blocks.air;
        return deletion.deletes(schematicAir, existing == schematic && world.getBlockMetadata(wx, wy, wz) == (metadata[index] & 0xffff));
    }

    /** TaskFillArea.directRemoveEntities: non-player entities inside these world boxes are removed before deleting. */
    public void removeEntitiesIn(List<com.github.lunatrius.schematica.api.SchematicRegion> boxes) { entityRemoval = new ArrayList<>(boxes); }

    public boolean removesEntities() { return !entityRemoval.isEmpty(); }

    private void removeEntities(WorldServer world) {
        for (com.github.lunatrius.schematica.api.SchematicRegion box : entityRemoval) {
            net.minecraft.util.AxisAlignedBB bounds = net.minecraft.util.AxisAlignedBB.getBoundingBox(box.minX, box.minY, box.minZ,
                box.maxX + 1, box.maxY + 1, box.maxZ + 1);
            for (Object found : world.getEntitiesWithinAABBExcludingEntity(null, bounds)) {
                Entity entity = (Entity) found;
                if (!(entity instanceof net.minecraft.entity.player.EntityPlayer) && !entity.isDead) {
                    entity.setDead();
                    entityCount++;
                }
            }
        }
    }

    public interface CellFilter { boolean keep(int x, int y, int z); }

    /** Drops world positions the filter rejects, such as those outside the render layer range. */
    public void restrict(CellFilter filter) {
        BitSet next = selected == null ? new BitSet(volume) : (BitSet) selected.clone();
        if (selected == null) next.set(0, volume);
        for (int index = next.nextSetBit(0); index >= 0; index = next.nextSetBit(index + 1)) {
            if (!filter.keep(x + index % width, y + index / width / length, z + index / width % length)) next.clear(index);
        }
        selected = next;
    }

    /** Paste skips per the replace behavior, and never writes air over air; fills and deletes always apply. */
    private boolean pastes(Block block, boolean worldAir) {
        if (kind != Kind.PASTE) return true;
        boolean air = block == Blocks.air;
        return !(air && worldAir) && replace.places(worldAir, air);
    }

    String blockCommand(int x, int y, int z, String block, int metadata) {
        return "/" + setblockCommand + " " + x + " " + y + " " + z + " " + block + " " + metadata + (replace == ReplaceBehavior.NONE ? " keep" : " replace");
    }

    public void setRegions(List<com.github.lunatrius.schematica.api.SchematicRegion> regions) {
        selected = regions.isEmpty() ? null : com.github.lunatrius.schematica.world.storage.RegionMask.create(regions, width, height, length);
    }

    public void flushBlockChanges(WorldServer world) {
        if (silentPlacement != null) silentPlacement.flush(world);
    }

    @Override
    public net.minecraft.util.IChatComponent finishedMessage(boolean success) {
        if (success && blockCount == 0 && entityCount == 0) {
            return new net.minecraft.util.ChatComponentTranslation("schematica.message.edit.no_changes");
        }
        if (kind != Kind.DELETE_PLACEMENT) return super.finishedMessage(success);
        return new net.minecraft.util.ChatComponentText(success ? String.format("Deleted %d blocks", blockCount) : "Deletion task failed");
    }

    public TaskRegistry.Kind taskKind() {
        if (kind == Kind.PASTE) return TaskRegistry.Kind.PASTE;
        if (kind == Kind.REPLACE) return TaskRegistry.Kind.REPLACE;
        return replacement != null && replacement == Blocks.air ? TaskRegistry.Kind.DELETE : TaskRegistry.Kind.FILL;
    }

    public void publishProgress(TaskRegistry.Task task) {
        TaskRegistry.Stage stage = phase == 0 ? TaskRegistry.Stage.STRUCTURE : phase == 1
            ? TaskRegistry.Stage.DECORATIONS : phase == 2 ? TaskRegistry.Stage.UPDATES : TaskRegistry.Stage.ENTITIES;
        long completed = phase < 3 ? cursor : entityCursor, total = phase < 3 ? volume : entities.size();
        if (phase == 2 && kind == Kind.PASTE && !pasteWithoutUpdates) {
            total *= 2;
            if (metadataPassComplete) completed += volume;
        }
        task.update(stage, completed, total, blockCount, entityCount);
    }

    private boolean restoreMetadata(WorldServer world, int wx, int wy, int wz, Block block, int meta) {
        if (world.getBlock(wx, wy, wz) != block) return false;
        int stored = world.getBlockMetadata(wx, wy, wz);
        // Placement callbacks may change this block's facing when another block is placed later.
        if (silentPlacement == null && stored != meta && stored != (meta & 15)) {
            world.setBlockMetadataWithNotify(wx, wy, wz, meta, 2);
            stored = world.getBlockMetadata(wx, wy, wz);
        }
        // Without EndlessIDs the world keeps only the low 4 bits of extended metadata.
        return world.getBlock(wx, wy, wz) == block && (stored == meta || stored == (meta & 15));
    }

    /** Process one cell/entity, returning true only when all phases are finished. */
    public boolean step(WorldServer world) {
        if (cancelled && phase < 2) { phase = 2; cursor = 0; }
        if (!entitiesRemoved) {
            entitiesRemoved = true;
            if (!cancelled) removeEntities(world);
        }
        if (phase < 3) {
            if (cursor == volume) {
                cursor = 0;
                if (phase == 2 && !metadataPassComplete) metadataPassComplete = true;
                else phase++;
                return false;
            }
            int index = cursor++;
            if (selected != null && !selected.get(index)) return false;
            int wx = x + index % width, wz = z + index / width % length, wy = y + index / width / length;
            Block block = kind == Kind.PASTE ? GameData.getBlockRegistry().getObjectById(blocks[index] & 0xffff) : replacement;
            int meta = kind == Kind.PASTE ? metadata[index] & 0xffff : replacementMeta;
            if (block == null) return false;
            if (phase == 2) {
                if (!placed.get(index) || pasteWithoutUpdates) return false;
                // Later chest placement can turn the other half; preserve normal changes to other blocks.
                if (!metadataPassComplete) {
                    if (block instanceof BlockChest && world.getBlock(wx, wy, wz) == block
                        && !restoreMetadata(world, wx, wy, wz, block, meta) && world.getBlock(wx, wy, wz) == block) {
                        throw new IllegalStateException("Could not restore pasted chest metadata at " + wx + ", " + wy + ", " + wz);
                    }
                    return false;
                }
                world.markBlockForUpdate(wx, wy, wz);
                world.notifyBlocksOfNeighborChange(wx, wy, wz, block);
                return false;
            }
            // Structural blocks first, then decorations. Neighbour notifications wait until phase 2.
            boolean structural = block.getMaterial().isSolid();
            if ((phase == 0) != structural) return false;
            if (kind == Kind.REPLACE && (world.getBlock(wx, wy, wz) != target
                || world.getBlockMetadata(wx, wy, wz) != targetMeta)) return false;
            if (kind == Kind.DELETE_PLACEMENT && !deletes(index, world, wx, wy, wz)) return false;
            if (!pastes(block, world.isAirBlock(wx, wy, wz))) return false;
            if (!block.hasTileEntity(meta) && world.getBlock(wx, wy, wz) == block
                && world.getBlockMetadata(wx, wy, wz) == meta) return false;
            if (silentPlacement != null) {
                silentPlacement.setBlock(world, wx, wy, wz, block, meta);
            } else {
                if (kind == Kind.PASTE) world.removeTileEntity(wx, wy, wz);
                boolean previous = world.restoringBlockSnapshots;
                try {
                    world.restoringBlockSnapshots = true; // Suppress item drops from replaced inventories.
                    world.setBlock(wx, wy, wz, block, meta, 2);
                } finally {
                    world.restoringBlockSnapshots = previous;
                }
            }
            if (restoreMetadata(world, wx, wy, wz, block, meta)) {
                if (kind == Kind.PASTE && block.hasTileEntity(meta)) {
                    NBTTagCompound tag = tiles.get(index);
                    TileEntity tile;
                    if (tag != null) {
                        tag = com.github.lunatrius.schematica.compat.CoordinateLinks.paste((NBTTagCompound) tag.copy(), wx, wy, wz);
                        tag.setInteger("x", wx); tag.setInteger("y", wy); tag.setInteger("z", wz);
                        tile = "savedMultipart".equals(tag.getString("id"))
                            ? ForgeMultipart.createFromNBT(tag, false) : TileEntity.createAndLoadEntity(tag);
                    } else tile = block.createTileEntity(world, meta);
                    if (tile == null) throw new MessageException("schematica.message.edit.tile_failed", wx, wy, wz);
                    tile.xCoord = wx; tile.yCoord = wy; tile.zCoord = wz;
                    if (silentPlacement != null) silentPlacement.setTile(world, tile);
                    else {
                        world.setTileEntity(wx, wy, wz, tile);
                        tile.markDirty();
                        ForgeMultipart.sendDescription(world, tile);
                    }
                }
                placed.set(index);
                blockCount++;
            } else throw new MessageException("schematica.message.edit.block_failed", wx, wy, wz);
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

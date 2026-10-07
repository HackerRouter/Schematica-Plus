// SPDX-License-Identifier: LGPL-3.0-only
// LitematicaSchematic.toTag of 1.12.2 Litematica (maruohon, liteloader_1.12.2), adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.world.schematic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.api.SchematicOrigin;
import com.github.lunatrius.schematica.api.SchematicRegion;
import com.github.lunatrius.schematica.nbt.NBTHelper;
import com.github.lunatrius.schematica.nbt.TileEntitySnapshots;
import com.github.lunatrius.schematica.reference.Reference;

import cpw.mods.fml.common.registry.GameData;

/**
 * Writes a schematic as a version 4 .litematic of Minecraft 1.12.2 (data version 1343), the newest format a 1.7.10
 * world maps onto: 1.12.2 Litematica reads it directly and modern Litematica upgrades it with its 1.12 conversion
 * table. Blocks without a 1.12 state keep their registry name and store their metadata in "SchematicaPlusMeta".
 */
public final class LitematicExport {
    public static final int VERSION = 4;
    public static final int DATA_VERSION = 1343;
    public static final String META_KEY = "SchematicaPlusMeta";

    /** The root tag and the long arrays (BlockStates) that 1.7.10 NBT cannot hold. */
    public static final class Document {
        public final NBTTagCompound root;
        public final Map<NBTTagCompound, Map<String, long[]>> longArrays;

        public Document() { this(new NBTTagCompound(), new IdentityHashMap<>()); }

        public Document(NBTTagCompound root, Map<NBTTagCompound, Map<String, long[]>> longArrays) {
            this.root = root;
            this.longArrays = longArrays;
        }
    }

    private LitematicExport() {}

    public static Document encode(ISchematic schematic, World backupWorld, boolean includeNBT, boolean includeEntities,
        String name, String author, long time) {
        Document document = new Document();
        List<SchematicRegion> boxes = schematic.getRegions().isEmpty()
            ? Collections.singletonList(new SchematicRegion(name.isEmpty() ? "Unnamed" : name, 0, 0, 0,
                schematic.getWidth() - 1, schematic.getHeight() - 1, schematic.getLength() - 1))
            : schematic.getRegions();
        SchematicOrigin origin = schematic.getOrigin();
        Map<Entity, SchematicRegion> owners = new IdentityHashMap<>();
        if (includeEntities) {
            for (Entity entity : schematic.getEntities()) {
                int x = (int) Math.floor(entity.posX), y = (int) Math.floor(entity.posY), z = (int) Math.floor(entity.posZ);
                SchematicRegion owner = boxes.get(0);
                for (SchematicRegion box : boxes) if (box.contains(x, y, z)) { owner = box; break; }
                owners.put(entity, owner);
            }
        }
        NBTTagCompound regions = new NBTTagCompound();
        long volume = 0, blocks = 0;
        int minX = Integer.MAX_VALUE, minY = minX, minZ = minX, maxX = Integer.MIN_VALUE, maxY = maxX, maxZ = maxX;
        for (SchematicRegion box : boxes) {
            ISchematic part = schematic.getRegionSchematic(box.name);
            int width = box.maxX - box.minX + 1, height = box.maxY - box.minY + 1, length = box.maxZ - box.minZ + 1;
            NBTTagCompound region = new NBTTagCompound();
            blocks += writeBlocks(region, document, schematic, part, box, width, height, length);
            region.setTag("TileEntities", tiles(schematic, part, box, backupWorld, includeNBT));
            region.setTag("Entities", entities(schematic, part, box, owners, includeEntities));
            region.setTag("PendingBlockTicks", new NBTTagList());
            region.setTag("Position", vector(box.minX - origin.x, box.minY - origin.y, box.minZ - origin.z));
            region.setTag("Size", vector(width, height, length));
            regions.setTag(box.name, region);
            volume += (long) width * height * length;
            minX = Math.min(minX, box.minX); minY = Math.min(minY, box.minY); minZ = Math.min(minZ, box.minZ);
            maxX = Math.max(maxX, box.maxX); maxY = Math.max(maxY, box.maxY); maxZ = Math.max(maxZ, box.maxZ);
        }
        NBTTagCompound metadata = new NBTTagCompound();
        metadata.setString("Name", name);
        metadata.setString("Author", author);
        metadata.setString("Description", "");
        metadata.setInteger("RegionCount", boxes.size());
        metadata.setLong("TotalVolume", volume);
        metadata.setLong("TotalBlocks", blocks);
        metadata.setLong("TimeCreated", time);
        metadata.setLong("TimeModified", time);
        metadata.setTag("EnclosingSize", vector(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1));
        document.root.setInteger("Version", VERSION);
        document.root.setInteger("MinecraftDataVersion", DATA_VERSION);
        document.root.setTag("Metadata", metadata);
        document.root.setTag("Regions", regions);
        return document;
    }

    static NBTTagCompound vector(int x, int y, int z) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("x", x);
        tag.setInteger("y", y);
        tag.setInteger("z", z);
        return tag;
    }

    /** The palette entry for a block, or null for air. */
    static NBTTagCompound paletteEntry(Block block, int meta) {
        String name = block == null ? null : GameData.getBlockRegistry().getNameForObject(block);
        if (name == null || name.equals("minecraft:air")) return null;
        String state = meta <= 15 ? LegacyBlockStates.state(name, meta) : null;
        if (state != null) return LegacyBlockStates.tag(state);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Name", name);
        if (meta != 0) tag.setInteger(META_KEY, meta);
        return tag;
    }

    /** Fills BlockStatePalette and BlockStates (x + z * width + y * width * length, air at index 0); returns non-air count. */
    private static long writeBlocks(NBTTagCompound region, Document document, ISchematic schematic, ISchematic part,
        SchematicRegion box, int width, int height, int length) {
        NBTTagList palette = new NBTTagList();
        palette.appendTag(LegacyBlockStates.tag("minecraft:air"));
        Map<Long, Integer> indices = new HashMap<>();
        int[] values = new int[width * height * length];
        long count = 0;
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    int sx = box.minX + x, sy = box.minY + y, sz = box.minZ + z;
                    Block block;
                    int meta;
                    if (part != null) { block = part.getBlock(x, y, z); meta = part.getBlockMetadata(x, y, z); }
                    else if (schematic.containsBlock(sx, sy, sz)) { block = schematic.getBlock(sx, sy, sz); meta = schematic.getBlockMetadata(sx, sy, sz); }
                    else continue;
                    long key = (long) (block == null ? -1 : GameData.getBlockRegistry().getId(block)) << 16 | (meta & 0xffff);
                    Integer index = indices.get(key);
                    if (index == null) {
                        NBTTagCompound entry = paletteEntry(block, meta);
                        if (entry == null) index = 0;
                        else {
                            index = palette.tagCount();
                            palette.appendTag(entry);
                        }
                        indices.put(key, index);
                    }
                    if (index != 0) count++;
                    values[x + width * (z + length * y)] = index;
                }
            }
        }
        region.setTag("BlockStatePalette", palette);
        document.longArrays.computeIfAbsent(region, key -> new HashMap<>())
            .put("BlockStates", LitematicBitArray.pack(values, LitematicBitArray.getRequiredBits(palette.tagCount())));
        return count;
    }

    private static NBTTagList tiles(ISchematic schematic, ISchematic part, SchematicRegion box, World backupWorld, boolean includeNBT) {
        NBTTagList list = new NBTTagList();
        if (!includeNBT) return list;
        List<TileEntity> source = part != null ? part.getTileEntities() : schematic.getTileEntities();
        for (TileEntity tile : source) {
            int x = tile.xCoord, y = tile.yCoord, z = tile.zCoord;
            if (part == null) {
                if (!box.contains(x, y, z) || !schematic.containsBlock(x, y, z)) continue;
                x -= box.minX; y -= box.minY; z -= box.minZ;
            }
            try {
                if (!tile.hasWorldObj() && backupWorld != null) tile.setWorldObj(backupWorld);
                NBTTagCompound tag = NBTHelper.writeTileEntityToCompound(tile);
                TileEntitySnapshots.removeVisualData(tag);
                tag = LegacyNbt.tileTo112(tag);
                tag.setInteger("x", x);
                tag.setInteger("y", y);
                tag.setInteger("z", z);
                list.appendTag(tag);
            } catch (Exception e) {
                Reference.logger.error("Block entity {} at {} {} {} failed to save, skipping", tile.getClass().getName(), x, y, z, e);
            }
        }
        return list;
    }

    private static NBTTagList entities(ISchematic schematic, ISchematic part, SchematicRegion box, Map<Entity, SchematicRegion> owners,
        boolean includeEntities) {
        NBTTagList list = new NBTTagList();
        if (!includeEntities) return list;
        List<Entity> source = new ArrayList<>(part != null ? part.getEntities() : schematic.getEntities());
        for (Entity entity : source) {
            if (part == null && owners.get(entity) != box) continue;
            try {
                NBTTagCompound tag = NBTHelper.writeEntityToCompound(entity);
                if (tag == null) continue;
                if (part == null) offset(tag, -box.minX, -box.minY, -box.minZ);
                list.appendTag(LegacyNbt.entityTo112(tag));
            } catch (Throwable t) {
                Reference.logger.error("Entity {} failed to save, skipping", entity, t);
            }
        }
        return list;
    }

    /** Moves an entity tag (Pos and hanging TileX/Y/Z) and anything riding it. */
    static void offset(NBTTagCompound entity, int dx, int dy, int dz) {
        NBTTagList pos = entity.getTagList("Pos", NBT.TAG_DOUBLE);
        if (pos.tagCount() == 3) {
            NBTTagList moved = new NBTTagList();
            moved.appendTag(new NBTTagDouble(pos.func_150309_d(0) + dx));
            moved.appendTag(new NBTTagDouble(pos.func_150309_d(1) + dy));
            moved.appendTag(new NBTTagDouble(pos.func_150309_d(2) + dz));
            entity.setTag("Pos", moved);
        }
        if (entity.hasKey("TileX")) entity.setInteger("TileX", entity.getInteger("TileX") + dx);
        if (entity.hasKey("TileY")) entity.setInteger("TileY", entity.getInteger("TileY") + dy);
        if (entity.hasKey("TileZ")) entity.setInteger("TileZ", entity.getInteger("TileZ") + dz);
        if (entity.hasKey("Riding", NBT.TAG_COMPOUND)) offset(entity.getCompoundTag("Riding"), dx, dy, dz);
    }
}

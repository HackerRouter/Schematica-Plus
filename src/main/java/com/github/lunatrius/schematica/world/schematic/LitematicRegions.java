package com.github.lunatrius.schematica.world.schematic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants.NBT;

import com.github.lunatrius.schematica.api.SchematicOrigin;
import com.github.lunatrius.schematica.api.SchematicRegion;
import com.github.lunatrius.schematica.util.SchematicLimits;

final class LitematicRegions {
    final List<Region> regions;
    final SchematicOrigin minimum;
    final int width, height, length;

    LitematicRegions(NBTTagCompound root) {
        int version = root.getInteger("Version");
        if (version < 1 || version > 7) throw new IllegalArgumentException("Unsupported litematic version: " + version);
        NBTTagCompound data = root.getCompoundTag("Regions");
        List<String> names = new ArrayList<String>(data.func_150296_c());
        if (names.isEmpty() || names.size() > 256) throw new IllegalArgumentException("Invalid region count");
        Collections.sort(names);
        List<Region> entries = new ArrayList<>();
        Set<String> checkedNames = new HashSet<>();
        int minX = Integer.MAX_VALUE, minY = minX, minZ = minX;
        int maxX = Integer.MIN_VALUE, maxY = maxX, maxZ = maxX;
        long total = 0;
        for (String name : names) {
            Region region = new Region(name, data.getCompoundTag(name), version);
            if (!checkedNames.add(region.box.name)) throw new IllegalArgumentException("Duplicate region name");
            total += region.blocks.size();
            if (total > SchematicLimits.MAX_BLOCKS) throw new IllegalArgumentException("Combined region contents exceed allocation limit");
            entries.add(region);
            minX = Math.min(minX, region.box.minX); minY = Math.min(minY, region.box.minY); minZ = Math.min(minZ, region.box.minZ);
            maxX = Math.max(maxX, region.box.maxX); maxY = Math.max(maxY, region.box.maxY); maxZ = Math.max(maxZ, region.box.maxZ);
        }
        minimum = new SchematicOrigin(minX, minY, minZ);
        Math.negateExact(minX); Math.negateExact(minY); Math.negateExact(minZ);
        width = SchematicLimits.dimension(minX, maxX);
        height = SchematicLimits.dimension(minY, maxY);
        length = SchematicLimits.dimension(minZ, maxZ);
        SchematicLimits.volume(width, height, length);
        regions = Collections.unmodifiableList(entries);
    }

    static final class Region {
        final SchematicRegion box;
        final SchematicOrigin origin;
        final int width, height, length;
        final NBTTagList palette;
        private final NBTTagCompound data;
        private final int version;
        private final LitematicBitArray blocks;

        Region(String name, NBTTagCompound data, int version) {
            this.data = data;
            this.version = version;
            NBTTagCompound pos = data.getCompoundTag("Position"), size = data.getCompoundTag("Size");
            int x = coordinate(pos, "x"), y = coordinate(pos, "y"), z = coordinate(pos, "z");
            int sx = coordinate(size, "x"), sy = coordinate(size, "y"), sz = coordinate(size, "z");
            int volume = SchematicLimits.volume(Math.abs((long) sx), Math.abs((long) sy), Math.abs((long) sz));
            width = Math.abs(sx); height = Math.abs(sy); length = Math.abs(sz);
            box = new SchematicRegion(name, x, y, z, Math.addExact(x, sx - Integer.signum(sx)),
                Math.addExact(y, sy - Integer.signum(sy)), Math.addExact(z, sz - Integer.signum(sz)));
            origin = new SchematicOrigin(x - box.minX, y - box.minY, z - box.minZ);
            palette = data.getTagList("BlockStatePalette", NBT.TAG_COMPOUND);
            if (palette.tagCount() == 0) throw new IllegalArgumentException("Empty block palette in " + name);
            for (int i = 0; i < palette.tagCount(); i++) {
                if (palette.getCompoundTagAt(i).getString("Name").isEmpty()) throw new IllegalArgumentException("Missing block name in " + name);
            }
            blocks = new LitematicBitArray(LitematicBitArray.getRequiredBits(palette.tagCount()), volume,
                LitematicaNBTReader.getLongArray(data, "BlockStates"));
        }

        int paletteIndex(int x, int y, int z) {
            if (x < 0 || y < 0 || z < 0 || x >= width || y >= height || z >= length) throw new IndexOutOfBoundsException("Region block");
            int index = blocks.getAt(x + (long) width * (z + (long) length * y));
            if (index < 0 || index >= palette.tagCount()) throw new IllegalArgumentException("Invalid palette index in " + box.name);
            return index;
        }

        NBTTagList tileEntities() {
            NBTTagList input = data.getTagList("TileEntities", NBT.TAG_COMPOUND), result = new NBTTagList();
            for (int i = 0; i < input.tagCount(); i++) {
                NBTTagCompound entry = input.getCompoundTagAt(i);
                NBTTagCompound tile = (NBTTagCompound) (version == 1 ? entry.getCompoundTag("TileNBT") : entry).copy();
                if (version == 1) for (String axis : new String[] {"x", "y", "z"}) tile.setInteger(axis, coordinate(entry, axis));
                int x = coordinate(tile, "x"), y = coordinate(tile, "y"), z = coordinate(tile, "z");
                if (x < 0 || y < 0 || z < 0 || x >= width || y >= height || z >= length) {
                    throw new IllegalArgumentException("Tile entity outside region " + box.name);
                }
                result.appendTag(tile);
            }
            return result;
        }

        NBTTagList entities() {
            NBTTagList input = data.getTagList("Entities", NBT.TAG_COMPOUND), result = new NBTTagList();
            for (int i = 0; i < input.tagCount(); i++) {
                NBTTagCompound entry = input.getCompoundTagAt(i);
                NBTTagCompound entity = (NBTTagCompound) (version == 1 ? entry.getCompoundTag("EntityData") : entry).copy();
                if (version == 1) {
                    NBTTagList pos = new NBTTagList();
                    for (String axis : new String[] {"x", "y", "z"}) {
                        if (!entry.hasKey(axis, NBT.TAG_ANY_NUMERIC)) throw new IllegalArgumentException("Missing entity position");
                        pos.appendTag(new NBTTagDouble(entry.getDouble(axis)));
                    }
                    entity.setTag("Pos", pos);
                }
                offsetEntity(entity, origin, 0);
                result.appendTag(entity);
            }
            return result;
        }
    }

    private static int coordinate(NBTTagCompound tag, String axis) {
        if (!tag.hasKey(axis, NBT.TAG_INT)) throw new IllegalArgumentException("Missing coordinate: " + axis);
        return tag.getInteger(axis);
    }

    private static void offsetEntity(NBTTagCompound entity, SchematicOrigin origin, int depth) {
        if (depth > SchematicLimits.MAX_NBT_DEPTH) throw new IllegalArgumentException("Entity nesting too deep");
        NBTTagList input = entity.getTagList("Pos", NBT.TAG_DOUBLE), pos = new NBTTagList();
        if (input.tagCount() != 3) throw new IllegalArgumentException("Invalid entity position");
        int[] offset = origin.coordinates();
        for (int i = 0; i < 3; i++) {
            double value = input.func_150309_d(i) + offset[i];
            if (!Double.isFinite(value)) throw new IllegalArgumentException("Invalid entity position");
            pos.appendTag(new NBTTagDouble(value));
        }
        entity.setTag("Pos", pos);
        if (!entity.hasKey("TileX") && entity.hasKey("block_pos")) {
            int[] anchor;
            if (entity.hasKey("block_pos", NBT.TAG_INT_ARRAY)) {
                anchor = entity.getIntArray("block_pos");
            } else {
                NBTTagCompound block = entity.getCompoundTag("block_pos");
                anchor = new int[] {coordinate(block, "X"), coordinate(block, "Y"), coordinate(block, "Z")};
            }
            if (anchor.length != 3) throw new IllegalArgumentException("Invalid entity anchor");
            entity.setInteger("TileX", anchor[0]); entity.setInteger("TileY", anchor[1]); entity.setInteger("TileZ", anchor[2]);
            entity.removeTag("block_pos");
        }
        String[] axes = {"TileX", "TileY", "TileZ"};
        for (int i = 0; i < 3; i++) if (entity.hasKey(axes[i])) entity.setInteger(axes[i], Math.addExact(entity.getInteger(axes[i]), offset[i]));
        if (entity.hasKey("Riding", NBT.TAG_COMPOUND)) offsetEntity(entity.getCompoundTag("Riding"), origin, depth + 1);
        NBTTagList passengers = entity.getTagList("Passengers", NBT.TAG_COMPOUND);
        for (int i = 0; i < passengers.tagCount(); i++) offsetEntity(passengers.getCompoundTagAt(i), origin, depth + 1);
    }
}

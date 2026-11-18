// Sponge schematic (.schem) versions 1-3 after the Sponge schematic specification, for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.world.schematic;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.world.storage.SchematicFluids;
import com.github.lunatrius.schematica.api.SchematicOrigin;
import com.github.lunatrius.schematica.compat.BlockMapping;
import com.github.lunatrius.schematica.compat.BlockStateTranslator;
import com.github.lunatrius.schematica.compat.EntityTranslator;
import com.github.lunatrius.schematica.compat.TileEntityTranslator;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.util.SchematicLimits;
import com.github.lunatrius.schematica.world.storage.Schematic;

/**
 * Blocks are modern block state strings with varint palette indices (x + z * width + y * width * length);
 * version 3 nests everything in "Schematic" and block entity / entity data in "Data". The WorldEdit origin (where the
 * copy was made from) becomes the schematic origin.
 */
public final class SpongeSchematic extends SchematicFormat {
    static boolean isSponge(NBTTagCompound root) {
        NBTTagCompound tag = body(root);
        return tag.hasKey("Version", NBT.TAG_INT) && tag.hasKey("Width") && (tag.hasKey("Palette") || tag.hasKey("Blocks"));
    }

    private static NBTTagCompound body(NBTTagCompound root) {
        return root.hasKey("Schematic", NBT.TAG_COMPOUND) ? root.getCompoundTag("Schematic") : root;
    }

    @Override
    public ISchematic readFromNBT(NBTTagCompound root) {
        NBTTagCompound tag = body(root);
        int version = tag.getInteger("Version");
        if (version < 1 || version > 3) throw new IllegalArgumentException("Unsupported Sponge schematic version " + version);
        int width = tag.getShort("Width") & 0xFFFF, height = tag.getShort("Height") & 0xFFFF, length = tag.getShort("Length") & 0xFFFF;
        SchematicLimits.volume(width, height, length);
        NBTTagCompound blocks = version == 3 ? tag.getCompoundTag("Blocks") : tag;
        NBTTagCompound paletteTag = blocks.getCompoundTag("Palette");
        byte[] data = blocks.getByteArray(version == 3 ? "Data" : "BlockData");
        int volume = width * height * length;
        if (data.length < volume) throw new IllegalArgumentException("Truncated Sponge block data");
        BlockStateTranslator translator = BlockStateTranslator.instance();
        int max = 0;
        for (Object key : paletteTag.func_150296_c()) {
            int index = paletteTag.getInteger((String) key);
            if (!paletteTag.hasKey((String) key, NBT.TAG_INT) || index < 0 || index >= 1 << 20) {
                throw new IllegalArgumentException("Invalid Sponge palette index");
            }
            max = Math.max(max, index + 1);
        }
        String[] states = new String[max];
        BlockMapping[] palette = new BlockMapping[max];
        for (Object key : paletteTag.func_150296_c()) {
            int index = paletteTag.getInteger((String) key);
            if (states[index] != null) throw new IllegalArgumentException("Duplicate Sponge palette index");
            states[index] = (String) key;
            if (tag.getInteger("DataVersion") == LitematicExport.DATA_VERSION) {
                NBTTagCompound legacy = LegacyBlockStates.tag(states[index]);
                NBTTagCompound properties = legacy.getCompoundTag("Properties");
                if (properties.hasKey("schematica_meta")) {
                    legacy.setInteger(LitematicExport.META_KEY, Integer.parseInt(properties.getString("schematica_meta")));
                }
                palette[index] = SchematicLitematica.legacyMapping(legacy, translator);
            } else palette[index] = translator.translate((String) key);
        }

        Schematic schematic = new Schematic(new ItemStack(Blocks.grass), width, height, length);
        int[] offset = origin(tag, version);
        schematic.setOrigin(new SchematicOrigin(-offset[0], -offset[1], -offset[2]));
        int cursor = 0;
        for (int index = 0; index < volume; index++) {
            int value = 0, shift = 0;
            while (true) {
                if (cursor >= data.length) throw new IllegalArgumentException("Truncated Sponge block data");
                byte b = data[cursor++];
                if (shift == 28 && (b & 0xF8) != 0) throw new IllegalArgumentException("Invalid Sponge block data");
                value |= (b & 0x7F) << shift;
                if ((b & 0x80) == 0) break;
                shift += 7;
                if (shift > 28) throw new IllegalArgumentException("Invalid Sponge block data");
            }
            if (value < 0 || value >= palette.length || palette[value] == null) throw new IllegalArgumentException("Invalid Sponge palette entry");
            BlockMapping mapping = palette[value];
            if (mapping.block == Blocks.air) continue;
            int y = index / (width * length), rest = index % (width * length);
            schematic.setBlock(rest % width, y, rest / width, mapping.block, mapping.metadata);
        }
        if (cursor != data.length) throw new IllegalArgumentException("Trailing Sponge block data");

        NBTTagList tiles = blocks.getTagList("BlockEntities", NBT.TAG_COMPOUND);
        if (tiles.tagCount() == 0 && version < 3) tiles = tag.getTagList("TileEntities", NBT.TAG_COMPOUND);
        for (int i = 0; i < tiles.tagCount(); i++) readTile(schematic, tiles.getCompoundTagAt(i), version == 3, states, width, height, length, tag.getInteger("DataVersion"));
        NBTTagList entities = tag.getTagList("Entities", NBT.TAG_COMPOUND);
        int[] min = version == 3 ? new int[3] : ints(tag, "Offset");
        for (int i = 0; i < entities.tagCount(); i++) readEntity(schematic, entities.getCompoundTagAt(i), version == 3, min, tag.getInteger("DataVersion"));
        SchematicFluids.read(tag, schematic);
        return schematic;
    }

    /** The copy origin relative to the minimum corner, negated: WEOffset for versions 1-2, Offset for version 3. */
    private static int[] origin(NBTTagCompound tag, int version) {
        if (version == 3) return ints(tag, "Offset");
        NBTTagCompound metadata = tag.getCompoundTag("Metadata");
        if (!metadata.hasKey("WEOffsetX")) return new int[3];
        return new int[] {metadata.getInteger("WEOffsetX"), metadata.getInteger("WEOffsetY"), metadata.getInteger("WEOffsetZ")};
    }

    private static int[] ints(NBTTagCompound tag, String key) {
        int[] values = tag.getIntArray(key);
        return values.length == 3 ? values : new int[3];
    }

    private static NBTTagCompound data(NBTTagCompound entry, boolean nested) {
        NBTTagCompound data = nested ? (NBTTagCompound) entry.getCompoundTag("Data").copy() : (NBTTagCompound) entry.copy();
        data.removeTag("Pos");
        data.removeTag("Id");
        return data;
    }

    private static void readTile(Schematic schematic, NBTTagCompound entry, boolean nested, String[] states, int width, int height, int length, int dataVersion) {
        int[] pos = entry.getIntArray("Pos");
        if (pos.length != 3 || pos[0] < 0 || pos[1] < 0 || pos[2] < 0 || pos[0] >= width || pos[1] >= height || pos[2] >= length) return;
        NBTTagCompound tile = data(entry, nested);
        tile.setString("id", entry.hasKey("Id") ? entry.getString("Id") : tile.getString("id"));
        tile.setInteger("x", pos[0]);
        tile.setInteger("y", pos[1]);
        tile.setInteger("z", pos[2]);
        try {
            if (!TileEntityTranslator.instance().translate(tile, stateAt(schematic, states, pos), dataVersion)) {
                ImportReport.omittedData("block entity: " + tile.getString("id"));
                return;
            }
            SchematicImport.tile(schematic, tile);
        } catch (Exception | LinkageError e) {
            ImportReport.omittedData("block entity: " + tile.getString("id"));
            Reference.logger.debug("Skipped a Sponge block entity at {} {} {}", pos[0], pos[1], pos[2], e);
        }
    }

    /** The modern state string of the block, which some block entity translations need (skulls, signs). */
    private static String stateAt(Schematic schematic, String[] states, int[] pos) {
        Block block = schematic.getBlock(pos[0], pos[1], pos[2]);
        int meta = schematic.getBlockMetadata(pos[0], pos[1], pos[2]);
        BlockStateTranslator translator = BlockStateTranslator.instance();
        for (String state : states) {
            if (state == null) continue;
            BlockMapping mapping = translator.translate(state);
            if (mapping.block == block && mapping.metadata == meta) return state;
        }
        return null;
    }

    private static void readEntity(Schematic schematic, NBTTagCompound entry, boolean nested, int[] min, int dataVersion) {
        NBTTagList pos = entry.getTagList("Pos", NBT.TAG_DOUBLE);
        if (pos.tagCount() != 3) return;
        NBTTagCompound entity = data(entry, nested);
        entity.setString("id", entry.hasKey("Id") ? entry.getString("Id") : entity.getString("id"));
        NBTTagList relative = new NBTTagList();
        for (int i = 0; i < 3; i++) relative.appendTag(new NBTTagDouble(pos.func_150309_d(i) - min[i]));
        entity.setTag("Pos", relative);
        try {
            if (!EntityTranslator.instance().translate(entity, dataVersion)) {
                ImportReport.omittedData("entity: " + entity.getString("id"));
                return;
            }
            Entity created = SchematicLitematica.createEntity(entity);
            if (created != null) schematic.addEntity(created);
        } catch (Exception | LinkageError e) {
            ImportReport.omittedData("entity: " + entity.getString("id"));
            Reference.logger.debug("Skipped a Sponge entity", e);
        }
    }

    @Override
    public boolean writeToNBT(NBTTagCompound tagCompound, ISchematic schematic, World backupWorld) {
        encode(tagCompound, schematic, backupWorld, SchematicFormat.saveNBT, SchematicFormat.saveEntities);
        return true;
    }

    static void encode(NBTTagCompound tag, ISchematic schematic, World backupWorld, boolean includeNBT, boolean includeEntities) {
        int width = schematic.getWidth(), height = schematic.getHeight(), length = schematic.getLength();
        tag.setInteger("Version", 2);
        tag.setInteger("DataVersion", LitematicExport.DATA_VERSION);
        tag.setShort("Width", (short) width);
        tag.setShort("Height", (short) height);
        tag.setShort("Length", (short) length);
        tag.setIntArray("Offset", new int[3]);
        NBTTagCompound metadata = new NBTTagCompound();
        SchematicOrigin origin = schematic.getOrigin();
        metadata.setInteger("WEOffsetX", -origin.x);
        metadata.setInteger("WEOffsetY", -origin.y);
        metadata.setInteger("WEOffsetZ", -origin.z);
        tag.setTag("Metadata", metadata);
        NBTTagCompound palette = new NBTTagCompound();
        java.util.Map<Long, Integer> indices = new java.util.HashMap<>();
        java.io.ByteArrayOutputStream data = new java.io.ByteArrayOutputStream();
        for (int y = 0; y < height; y++) for (int z = 0; z < length; z++) for (int x = 0; x < width; x++) {
            Block block = schematic.containsBlock(x, y, z) ? schematic.getBlock(x, y, z) : null;
            int meta = block == null ? 0 : schematic.getBlockMetadata(x, y, z);
            long key = ((long) Block.getIdFromBlock(block) << 16) | (meta & 65535);
            Integer index = indices.get(key);
            if (index == null) {
                NBTTagCompound entry = LitematicExport.paletteEntry(block, meta);
                String state = entry == null ? "minecraft:air" : LegacyBlockStates.state(entry);
                if (entry != null && entry.hasKey(LitematicExport.META_KEY)) state += "[schematica_meta=" + meta + "]";
                index = palette.hasKey(state) ? palette.getInteger(state) : palette.func_150296_c().size();
                palette.setInteger(state, index);
                indices.put(key, index);
            }
            int value = index;
            while (value >= 128) {
                data.write((value & 127) | 128);
                value >>>= 7;
            }
            data.write(value);
        }
        tag.setTag("Palette", palette);
        tag.setInteger("PaletteMax", palette.func_150296_c().size());
        tag.setByteArray("BlockData", data.toByteArray());
        NBTTagList tiles = LitematicExport.tiles(schematic, schematic, null, backupWorld, includeNBT);
        for (int i = 0; i < tiles.tagCount(); i++) {
            NBTTagCompound tile = tiles.getCompoundTagAt(i);
            tile.setIntArray("Pos", new int[] {tile.getInteger("x"), tile.getInteger("y"), tile.getInteger("z")});
            for (String axis : new String[] {"x", "y", "z"}) tile.removeTag(axis);
            renameId(tile);
        }
        tag.setTag("BlockEntities", tiles);
        NBTTagList entities = LitematicExport.entities(schematic, schematic, null, null, includeEntities);
        for (int i = 0; i < entities.tagCount(); i++) renameId(entities.getCompoundTagAt(i));
        tag.setTag("Entities", entities);
        SchematicFluids.write(tag, schematic);
    }

    private static void renameId(NBTTagCompound data) {
        data.setString("Id", data.getString("id"));
        data.removeTag("id");
    }
}

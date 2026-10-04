// Sponge schematic (.schem) versions 1-3 after the Sponge schematic specification, for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.world.schematic;

import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.api.SchematicOrigin;
import com.github.lunatrius.schematica.compat.BlockMapping;
import com.github.lunatrius.schematica.compat.BlockStateTranslator;
import com.github.lunatrius.schematica.compat.EntityTranslator;
import com.github.lunatrius.schematica.compat.TileEntityTranslator;
import com.github.lunatrius.schematica.nbt.NBTHelper;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.util.SchematicLimits;
import com.github.lunatrius.schematica.world.storage.Schematic;

/**
 * Import only. Blocks are modern block state strings with varint palette indices (x + z * width + y * width * length);
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
        BlockStateTranslator translator = BlockStateTranslator.instance();
        int max = 0;
        for (Object key : paletteTag.func_150296_c()) max = Math.max(max, paletteTag.getInteger((String) key) + 1);
        if (max > 1 << 20) throw new IllegalArgumentException("Sponge palette too large");
        String[] states = new String[max];
        BlockMapping[] palette = new BlockMapping[max];
        for (Object key : paletteTag.func_150296_c()) {
            int index = paletteTag.getInteger((String) key);
            if (index < 0) throw new IllegalArgumentException("Invalid Sponge palette index");
            states[index] = (String) key;
            palette[index] = translator.translate((String) key);
        }

        Schematic schematic = new Schematic(new ItemStack(Blocks.grass), width, height, length);
        int[] offset = origin(tag, version);
        schematic.setOrigin(new SchematicOrigin(-offset[0], -offset[1], -offset[2]));
        int index = 0, volume = width * height * length;
        for (int i = 0; i < data.length && index < volume; index++) {
            int value = 0, shift = 0;
            while (true) {
                if (i >= data.length) throw new IllegalArgumentException("Truncated Sponge block data");
                byte b = data[i++];
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

        NBTTagList tiles = blocks.getTagList("BlockEntities", NBT.TAG_COMPOUND);
        if (tiles.tagCount() == 0 && version < 3) tiles = tag.getTagList("TileEntities", NBT.TAG_COMPOUND);
        for (int i = 0; i < tiles.tagCount(); i++) readTile(schematic, tiles.getCompoundTagAt(i), version == 3, states, width, height, length);
        NBTTagList entities = tag.getTagList("Entities", NBT.TAG_COMPOUND);
        int[] min = version == 3 ? new int[3] : ints(tag, "Offset");
        for (int i = 0; i < entities.tagCount(); i++) readEntity(schematic, entities.getCompoundTagAt(i), version == 3, min);
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

    private static void readTile(Schematic schematic, NBTTagCompound entry, boolean nested, String[] states, int width, int height, int length) {
        int[] pos = entry.getIntArray("Pos");
        if (pos.length != 3 || pos[0] < 0 || pos[1] < 0 || pos[2] < 0 || pos[0] >= width || pos[1] >= height || pos[2] >= length) return;
        NBTTagCompound tile = data(entry, nested);
        tile.setString("id", entry.hasKey("Id") ? entry.getString("Id") : tile.getString("id"));
        tile.setInteger("x", pos[0]);
        tile.setInteger("y", pos[1]);
        tile.setInteger("z", pos[2]);
        try {
            if (!TileEntityTranslator.instance().translate(tile, stateAt(schematic, states, pos))) return;
            TileEntity entity = NBTHelper.readTileEntityFromCompound(tile);
            if (entity != null) schematic.setTileEntity(pos[0], pos[1], pos[2], entity);
        } catch (Exception e) {
            Reference.logger.debug("Skipped a Sponge block entity at {} {} {}", pos[0], pos[1], pos[2], e);
        }
    }

    /** The modern state string of the block, which some block entity translations need (skulls, signs). */
    private static String stateAt(Schematic schematic, String[] states, int[] pos) {
        net.minecraft.block.Block block = schematic.getBlock(pos[0], pos[1], pos[2]);
        int meta = schematic.getBlockMetadata(pos[0], pos[1], pos[2]);
        BlockStateTranslator translator = BlockStateTranslator.instance();
        for (String state : states) {
            if (state == null) continue;
            BlockMapping mapping = translator.translate(state);
            if (mapping.block == block && mapping.metadata == meta) return state;
        }
        return null;
    }

    private static void readEntity(Schematic schematic, NBTTagCompound entry, boolean nested, int[] min) {
        NBTTagList pos = entry.getTagList("Pos", NBT.TAG_DOUBLE);
        if (pos.tagCount() != 3) return;
        NBTTagCompound entity = data(entry, nested);
        entity.setString("id", entry.hasKey("Id") ? entry.getString("Id") : entity.getString("id"));
        NBTTagList relative = new NBTTagList();
        for (int i = 0; i < 3; i++) relative.appendTag(new NBTTagDouble(pos.func_150309_d(i) - min[i]));
        entity.setTag("Pos", relative);
        try {
            if (!EntityTranslator.instance().translate(entity)) return;
            Entity created = SchematicLitematica.createEntity(entity);
            if (created != null) schematic.addEntity(created);
        } catch (Exception e) {
            Reference.logger.debug("Skipped a Sponge entity", e);
        }
    }

    @Override
    public boolean writeToNBT(NBTTagCompound tagCompound, ISchematic schematic, World backupWorld) {
        return false;
    }

}

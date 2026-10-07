// SPDX-License-Identifier: LGPL-3.0-only
// Litematica VanillaStructure / structure export, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.world.schematic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.compat.BlockMapping;
import com.github.lunatrius.schematica.compat.BlockStateTranslator;
import com.github.lunatrius.schematica.compat.EntityTranslator;
import com.github.lunatrius.schematica.compat.TileEntityTranslator;
import com.github.lunatrius.schematica.nbt.NBTHelper;
import com.github.lunatrius.schematica.nbt.TileEntitySnapshots;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.util.SchematicLimits;
import com.github.lunatrius.schematica.world.storage.Schematic;

/** Vanilla structure templates (.nbt): read from 1.12 or newer, written in the 1.12.2 layout (data version 1343). */
public final class VanillaStructure extends SchematicFormat {

    public static boolean isStructure(NBTTagCompound tag) {
        return tag.hasKey("size", NBT.TAG_LIST) && tag.hasKey("blocks", NBT.TAG_LIST)
            && (tag.hasKey("palette", NBT.TAG_LIST) || tag.hasKey("palettes", NBT.TAG_LIST));
    }

    private static int[] ints(NBTTagCompound tag, String key) {
        NBTTagList list = tag.getTagList(key, NBT.TAG_INT);
        if (list.tagCount() != 3) throw new IllegalArgumentException("Invalid structure " + key);
        int[] values = new int[3];
        NBTTagList copy = (NBTTagList) list.copy();
        for (int i = 2; i >= 0; i--) values[i] = ((NBTTagInt) copy.removeTag(i)).func_150287_d();
        return values;
    }

    @Override
    public ISchematic readFromNBT(NBTTagCompound tag) {
        if (!isStructure(tag)) throw new IllegalArgumentException("Not a structure template");
        int[] size = ints(tag, "size");
        SchematicLimits.volume(size[0], size[1], size[2]);
        int dataVersion = tag.getInteger("DataVersion");
        boolean legacy = dataVersion < 1631;
        NBTTagList paletteTag = tag.hasKey("palette", NBT.TAG_LIST) ? tag.getTagList("palette", NBT.TAG_COMPOUND) : firstPalette(tag);
        BlockStateTranslator translator = BlockStateTranslator.instance();
        BlockMapping[] palette = new BlockMapping[paletteTag.tagCount()];
        String[] states = new String[palette.length];
        for (int i = 0; i < palette.length; i++) {
            NBTTagCompound entry = paletteTag.getCompoundTagAt(i);
            states[i] = LegacyBlockStates.state(entry);
            palette[i] = legacy ? SchematicLitematica.legacyMapping(entry, translator) : translator.translate(states[i]);
        }
        Schematic schematic = new Schematic(new ItemStack(Blocks.grass), size[0], size[1], size[2]);
        NBTTagList blocks = tag.getTagList("blocks", NBT.TAG_COMPOUND);
        for (int i = 0; i < blocks.tagCount(); i++) {
            NBTTagCompound block = blocks.getCompoundTagAt(i);
            int[] pos = ints(block, "pos");
            int state = block.getInteger("state");
            if (state < 0 || state >= palette.length || pos[0] < 0 || pos[1] < 0 || pos[2] < 0
                || pos[0] >= size[0] || pos[1] >= size[1] || pos[2] >= size[2]) {
                throw new IllegalArgumentException("Invalid structure block");
            }
            BlockMapping mapping = palette[state];
            if (mapping.block == Blocks.air) continue;
            schematic.setBlock(pos[0], pos[1], pos[2], mapping.block, mapping.metadata);
            if (block.hasKey("nbt", NBT.TAG_COMPOUND)) {
                NBTTagCompound tile = (NBTTagCompound) block.getCompoundTag("nbt").copy();
                tile.setInteger("x", pos[0]);
                tile.setInteger("y", pos[1]);
                tile.setInteger("z", pos[2]);
                try {
                    if (legacy) tile = LegacyNbt.tileFrom112(tile);
                    else if (!TileEntityTranslator.instance().translate(tile, states[state])) continue;
                    TileEntity entity = NBTHelper.readTileEntityFromCompound(tile);
                    if (entity != null) schematic.setTileEntity(pos[0], pos[1], pos[2], entity);
                } catch (Exception e) {
                    Reference.logger.debug("Skipped a structure block entity at {} {} {}", pos[0], pos[1], pos[2], e);
                }
            }
        }
        NBTTagList entities = tag.getTagList("entities", NBT.TAG_COMPOUND);
        for (int i = 0; i < entities.tagCount(); i++) {
            NBTTagCompound info = entities.getCompoundTagAt(i);
            NBTTagCompound entity = (NBTTagCompound) info.getCompoundTag("nbt").copy();
            NBTTagList pos = info.getTagList("pos", NBT.TAG_DOUBLE);
            if (pos.tagCount() != 3) continue;
            entity.setTag("Pos", pos.copy());
            try {
                if (legacy) entity = LegacyNbt.entityFrom112(entity);
                else if (!EntityTranslator.instance().translate(entity)) continue;
                Entity created = SchematicLitematica.createEntity(entity);
                if (created != null) schematic.addEntity(created);
            } catch (Exception e) {
                Reference.logger.debug("Skipped a structure entity", e);
            }
        }
        return schematic;
    }

    /** Structures with random variants (shipwrecks) keep several palettes; the first one is used. */
    private static NBTTagList firstPalette(NBTTagCompound tag) {
        NBTTagList palettes = (NBTTagList) tag.getTagList("palettes", NBT.TAG_LIST).copy();
        if (palettes.tagCount() == 0) throw new IllegalArgumentException("Structure without a palette");
        return (NBTTagList) palettes.removeTag(0);
    }

    @Override
    public boolean writeToNBT(NBTTagCompound tagCompound, ISchematic schematic, World backupWorld) {
        return false;
    }

    /** The structure template of a schematic; positions outside its regions are left out, like structure voids. */
    public static NBTTagCompound encode(ISchematic schematic, World backupWorld, boolean includeEntities, String author) {
        int width = schematic.getWidth(), height = schematic.getHeight(), length = schematic.getLength();
        NBTTagList palette = new NBTTagList(), blocks = new NBTTagList();
        palette.appendTag(LegacyBlockStates.tag("minecraft:air"));
        Map<Long, Integer> indices = new HashMap<>();
        Map<Long, NBTTagCompound> tiles = new HashMap<>();
        for (TileEntity tile : schematic.getTileEntities()) {
            try {
                if (!tile.hasWorldObj() && backupWorld != null) tile.setWorldObj(backupWorld);
                NBTTagCompound tag = NBTHelper.writeTileEntityToCompound(tile);
                TileEntitySnapshots.removeVisualData(tag);
                tag = LegacyNbt.tileTo112(tag);
                tag.removeTag("x");
                tag.removeTag("y");
                tag.removeTag("z");
                tiles.put(key(tile.xCoord, tile.yCoord, tile.zCoord), tag);
            } catch (Exception e) {
                Reference.logger.error("Block entity {} failed to save, skipping", tile.getClass().getName(), e);
            }
        }
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    if (!schematic.containsBlock(x, y, z)) continue;
                    Block block = schematic.getBlock(x, y, z);
                    int meta = schematic.getBlockMetadata(x, y, z);
                    long blockKey = (long) (block == null ? -1 : net.minecraft.block.Block.getIdFromBlock(block)) << 16 | (meta & 0xffff);
                    Integer index = indices.get(blockKey);
                    if (index == null) {
                        NBTTagCompound entry = LitematicExport.paletteEntry(block, meta);
                        if (entry == null) index = 0;
                        else {
                            index = palette.tagCount();
                            palette.appendTag(entry);
                        }
                        indices.put(blockKey, index);
                    }
                    NBTTagCompound entry = new NBTTagCompound();
                    entry.setTag("pos", intList(x, y, z));
                    entry.setInteger("state", index);
                    NBTTagCompound tile = tiles.get(key(x, y, z));
                    if (tile != null && index != 0) entry.setTag("nbt", tile);
                    blocks.appendTag(entry);
                }
            }
        }
        NBTTagList entities = new NBTTagList();
        if (includeEntities) {
            List<Entity> source = schematic.getEntities();
            for (Entity entity : source) {
                try {
                    NBTTagCompound tag = NBTHelper.writeEntityToCompound(entity);
                    if (tag == null) continue;
                    NBTTagCompound info = new NBTTagCompound();
                    NBTTagList pos = new NBTTagList();
                    pos.appendTag(new NBTTagDouble(entity.posX));
                    pos.appendTag(new NBTTagDouble(entity.posY));
                    pos.appendTag(new NBTTagDouble(entity.posZ));
                    info.setTag("pos", pos);
                    info.setTag("blockPos", intList((int) Math.floor(entity.posX), (int) Math.floor(entity.posY), (int) Math.floor(entity.posZ)));
                    info.setTag("nbt", LegacyNbt.entityTo112(tag));
                    entities.appendTag(info);
                } catch (Throwable t) {
                    Reference.logger.error("Entity {} failed to save, skipping", entity, t);
                }
            }
        }
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("DataVersion", LitematicExport.DATA_VERSION);
        tag.setString("author", author);
        tag.setTag("size", intList(width, height, length));
        tag.setTag("palette", palette);
        tag.setTag("blocks", blocks);
        tag.setTag("entities", entities);
        return tag;
    }

    private static long key(int x, int y, int z) {
        return ((long) x & 0x1FFFFF) << 42 | ((long) y & 0x1FFFFF) << 21 | (long) z & 0x1FFFFF;
    }

    private static NBTTagList intList(int x, int y, int z) {
        NBTTagList list = new NBTTagList();
        list.appendTag(new NBTTagInt(x));
        list.appendTag(new NBTTagInt(y));
        list.appendTag(new NBTTagInt(z));
        return list;
    }
}

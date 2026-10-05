package com.github.lunatrius.schematica.world.schematic;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.junit.Test;

import static org.junit.Assert.*;

public class ItemIdMapsTest {
    private static ItemIdMaps.Archive ids(String name, Object... entries) {
        ItemIdMaps.Archive archive = new ItemIdMaps.Archive(name);
        for (int i = 0; i < entries.length; i += 2) archive.put((String) entries[i], (Integer) entries[i + 1]);
        return archive;
    }

    private static final ItemIdMaps.Archive OLD = ids("old",
        "\u0001minecraft:stone", 1, "\u0002minecraft:stone", 1, "\u0002minecraft:diamond", 264,
        "\u0001gregtech:gt.blockmachines", 1200, "\u0002gregtech:gt.metaitem.01", 7495, "\u0002removed:item", 7600);
    private static final ItemIdMaps.Archive NOW = ids("now",
        "\u0001minecraft:stone", 1, "\u0002minecraft:stone", 1, "\u0002minecraft:diamond", 264,
        "\u0001gregtech:gt.blockmachines", 1300, "\u0002gregtech:gt.metaitem.01", 8000, "\u0002harvestcraft:bakedpotato", 7495);

    private static NBTTagCompound stack(int id, int damage) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setShort("id", (short) id);
        tag.setByte("Count", (byte) 1);
        tag.setShort("Damage", (short) damage);
        return tag;
    }

    private static NBTTagCompound schematic(Object... blocks) {
        NBTTagCompound root = new NBTTagCompound();
        NBTTagCompound mapping = new NBTTagCompound();
        for (int i = 0; i < blocks.length; i += 2) mapping.setShort((String) blocks[i], ((Integer) blocks[i + 1]).shortValue());
        root.setTag("SchematicaMapping", mapping);
        NBTTagCompound tile = new NBTTagCompound();
        tile.setString("id", "BaseMetaTileEntity");
        NBTTagList inventory = new NBTTagList();
        inventory.appendTag(stack(7495, 32));
        inventory.appendTag(stack(264, 0));
        inventory.appendTag(stack(7600, 0));
        tile.setTag("Inventory", inventory);
        tile.setTag("mOutputItem0", stack(7495, 33));
        NBTTagList covers = new NBTTagList();
        NBTTagCompound cover = new NBTTagCompound();
        cover.setByte("s", (byte) 2);
        cover.setInteger("id", 7495 | 5 << 16);
        covers.appendTag(cover);
        tile.setTag("gt.covers", covers);
        NBTTagCompound drawer = new NBTTagCompound();
        drawer.setString("id", "StorageDrawers:tileDrawersStandard");
        NBTTagList slots = new NBTTagList();
        NBTTagCompound slot = new NBTTagCompound();
        slot.setShort("Item", (short) 7495);
        slot.setShort("Meta", (short) 1);
        slot.setInteger("Count", 300);
        slots.appendTag(slot);
        drawer.setTag("Slots", slots);
        NBTTagList tiles = new NBTTagList();
        tiles.appendTag(tile);
        tiles.appendTag(drawer);
        root.setTag("TileEntities", tiles);
        root.setTag("Icon", stack(7495, 32));
        return root;
    }

    private static NBTTagCompound tile(NBTTagCompound root, int index) { return root.getTagList("TileEntities", 10).getCompoundTagAt(index); }

    @Test public void namedIdsBecomeThisGamesAndUnknownNamesAreRemoved() {
        NBTTagCompound root = schematic("gregtech:gt.blockmachines", 1200);
        ItemIdMaps.writeMapping(root, OLD);
        assertEquals(7495, root.getCompoundTag(ItemIdMaps.ITEM_MAPPING).getInteger("gregtech:gt.metaitem.01"));
        ItemIdMaps.Result result = new ItemIdMaps.Result();
        ItemIdMaps.remap(root, NOW, Collections::emptyList, result);
        NBTTagList inventory = tile(root, 0).getTagList("Inventory", 10);
        assertEquals(2, inventory.tagCount());
        assertEquals(8000, inventory.getCompoundTagAt(0).getShort("id"));
        assertEquals(32, inventory.getCompoundTagAt(0).getShort("Damage"));
        assertEquals(264, inventory.getCompoundTagAt(1).getShort("id"));
        assertEquals(8000, tile(root, 0).getCompoundTag("mOutputItem0").getShort("id"));
        assertEquals(8000 | 5 << 16, tile(root, 0).getTagList("gt.covers", 10).getCompoundTagAt(0).getInteger("id"));
        assertEquals(8000, tile(root, 1).getTagList("Slots", 10).getCompoundTagAt(0).getShort("Item"));
        assertEquals(8000, root.getCompoundTag("Icon").getShort("id"));
        assertEquals(1, result.removed);
        assertEquals(Collections.singleton("removed:item"), result.unknownItems);
        assertEquals(8000, root.getCompoundTag(ItemIdMaps.ITEM_MAPPING).getInteger("gregtech:gt.metaitem.01"));

        ItemIdMaps.Result again = new ItemIdMaps.Result();
        ItemIdMaps.remap(root, NOW, Collections::emptyList, again);
        assertEquals(8000, inventory.getCompoundTagAt(0).getShort("id"));
        assertEquals(0, again.removed);
    }

    @Test public void unnamedIdsAreTranslatedWithTheIdMapWhoseBlockIdsMatch() {
        NBTTagCompound root = schematic("minecraft:stone", 1, "gregtech:gt.blockmachines", 1200);
        ItemIdMaps.Archive other = ids("other", "\u0001gregtech:gt.blockmachines", 1100, "\u0002gregtech:gt.metaitem.01", 7000);
        ItemIdMaps.Result result = new ItemIdMaps.Result();
        ItemIdMaps.remap(root, NOW, () -> Arrays.asList(other, OLD), result);
        assertEquals("old", result.translatedWith);
        assertEquals(8000, tile(root, 0).getTagList("Inventory", 10).getCompoundTagAt(0).getShort("id"));
        assertEquals(1, result.removed);
        assertEquals(8000, root.getCompoundTag(ItemIdMaps.ITEM_MAPPING).getInteger("gregtech:gt.metaitem.01"));
    }

    @Test public void unresolvableModdedIdsAreRemovedAndVanillaOnesKept() {
        NBTTagCompound root = schematic("gregtech:gt.blockmachines", 1200);
        ItemIdMaps.Result result = new ItemIdMaps.Result();
        ItemIdMaps.remap(root, NOW, Collections::emptyList, result);
        assertNull(result.translatedWith);
        assertTrue(result.unknownItems.isEmpty());
        NBTTagList inventory = tile(root, 0).getTagList("Inventory", 10);
        assertEquals(1, inventory.tagCount());
        assertEquals(264, inventory.getCompoundTagAt(0).getShort("id"));
        assertFalse(tile(root, 0).hasKey("mOutputItem0"));
        assertEquals(0, tile(root, 0).getTagList("gt.covers", 10).tagCount());
        assertFalse(tile(root, 1).getTagList("Slots", 10).getCompoundTagAt(0).hasKey("Item"));
        assertFalse(root.hasKey("Icon"));
        assertEquals(6, result.removed);
    }

    @Test public void matchingBlockIdsOrOnlyVanillaItemsKeepTheIds() {
        NBTTagCompound root = schematic("gregtech:gt.blockmachines", 1300);
        ItemIdMaps.Result result = new ItemIdMaps.Result();
        ItemIdMaps.remap(root, NOW, () -> { throw new AssertionError("no archive needed"); }, result);
        assertEquals(3, tile(root, 0).getTagList("Inventory", 10).tagCount());
        assertEquals(7495, tile(root, 0).getTagList("Inventory", 10).getCompoundTagAt(0).getShort("id"));
        assertEquals(0, result.removed);

        NBTTagCompound vanilla = new NBTTagCompound();
        NBTTagCompound chest = new NBTTagCompound();
        chest.setString("id", "Chest");
        NBTTagList items = new NBTTagList();
        items.appendTag(stack(264, 0));
        chest.setTag("Items", items);
        NBTTagList tiles = new NBTTagList();
        tiles.appendTag(chest);
        vanilla.setTag("TileEntities", tiles);
        ItemIdMaps.remap(vanilla, NOW, () -> { throw new AssertionError("no archive needed"); }, result);
        assertEquals(1, items.tagCount());
        assertFalse(vanilla.hasKey(ItemIdMaps.ITEM_MAPPING));
    }

    @Test public void archivesUseTheLevelDatLayoutAndAreWrittenOnce() throws Exception {
        File directory = Files.createTempDirectory("id_maps").toFile();
        Map<String, Integer> ids = new HashMap<>();
        ids.put("\u0001gregtech:gt.blockmachines", 1200);
        ids.put("\u0002gregtech:gt.metaitem.01", 7495);
        File file = ItemIdMaps.write(ids, directory);
        assertEquals(file, ItemIdMaps.write(new HashMap<>(ids), directory));
        assertEquals(1, directory.listFiles().length);
        ItemIdMaps.Archive archive;
        try (InputStream input = new FileInputStream(file)) {
            archive = ItemIdMaps.Archive.read(CompressedStreamTools.readCompressed(input), file.getName());
        }
        assertEquals(1200, archive.block("gregtech:gt.blockmachines"));
        assertEquals("gregtech:gt.metaitem.01", archive.itemName(7495));
        assertEquals(-1, archive.item("minecraft:stone"));
    }
}

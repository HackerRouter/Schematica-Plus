package com.github.lunatrius.schematica.world.schematic;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants.NBT;
import org.junit.Test;
import static org.junit.Assert.*;

public class ModernItemNbtTest {
    private static final Map<String, Integer> ITEMS = new HashMap<>();
    static {
        ITEMS.put("minecraft:wool", 35); ITEMS.put("minecraft:planks", 5); ITEMS.put("minecraft:log2", 162);
        ITEMS.put("minecraft:stained_hardened_clay", 159); ITEMS.put("minecraft:dye", 351);
        ITEMS.put("minecraft:diamond_sword", 276); ITEMS.put("minecraft:leather_chestplate", 299);
        ITEMS.put("minecraft:enchanted_book", 403); ITEMS.put("minecraft:written_book", 387);
        ITEMS.put("minecraft:potion", 373); ITEMS.put("minecraft:spawn_egg", 383);
        ITEMS.put("minecraft:netherbrick", 405); ITEMS.put("minecraft:nether_brick", 112);
        ITEMS.put("minecraft:stone", 1); ITEMS.put("minecraft:diamond", 264);
        ITEMS.put("minecraft:stone_slab", 44); ITEMS.put("minecraft:wooden_slab", 126); ITEMS.put("minecraft:anvil", 145);
        ITEMS.put("minecraft:repeater", 356); ITEMS.put("minecraft:comparator", 404); ITEMS.put("minecraft:redstone_torch", 76);
    }
    private static final ItemIdMaps.Ids IDS = new ItemIdMaps.Ids() {
        public int block(String name) { return -1; }
        public int item(String name) { return ITEMS.getOrDefault(name, -1); }
        public String itemName(int id) { return null; }
    };
    private static NBTTagCompound stack(String id, int count) {
        NBTTagCompound tag = new NBTTagCompound(); tag.setString("id", "minecraft:" + id); tag.setInteger("count", count);
        tag.setByte("Slot", (byte) 6); return tag;
    }
    private static NBTTagCompound components(NBTTagCompound item) {
        NBTTagCompound tag = new NBTTagCompound(); item.setTag("components", tag); return tag;
    }
    private static NBTTagCompound text(String value) {
        NBTTagCompound tag = new NBTTagCompound(); tag.setString("text", value); return tag;
    }
    private static void convert(NBTTagCompound tag, int version) { ModernItemNbt.convert(tag, version, IDS); }

    @Test public void preComponentWrittenBooksHaveReadableNativePages() {
        NBTTagCompound item = stack("written_book", 1), tag = new NBTTagCompound();
        NBTTagList pages = new NBTTagList(); pages.appendTag(new NBTTagString("{\"text\":\"Imported page\",\"extra\":[\" 中文\"]}"));
        tag.setTag("pages", pages); tag.setString("title", "Book"); tag.setString("author", "Builder"); item.setTag("tag", tag);
        convert(item, 3700);
        assertEquals("Imported page 中文", tag.getTagList("pages", NBT.TAG_STRING).getStringTagAt(0));
        assertEquals("Book", tag.getString("title")); assertEquals("Builder", tag.getString("author"));
    }

    @Test public void blockItemsUseItemFormsInsteadOfPlacedBlockIdsAndDamage() {
        String[] names = {"birch_slab", "quartz_slab", "chipped_anvil", "damaged_anvil", "repeater", "comparator", "redstone_torch", "acacia_wood"};
        int[] ids = {126, 44, 145, 145, 356, 404, 76, 162}, damage = {2, 7, 1, 2, 0, 0, 0, 12};
        for (int i = 0; i < names.length; i++) {
            NBTTagCompound item = stack(names[i], 12); convert(item, 5023);
            assertEquals(names[i], ids[i], item.getShort("id")); assertEquals(names[i], damage[i], item.getShort("Damage"));
            assertEquals(12, item.getByte("Count"));
        }
    }

    @Test public void flattenedVariantsKeepTheirNativeMetadataAndSlots() {
        String[] names = {"red_wool", "dark_oak_planks", "acacia_log", "blue_terracotta", "blue_dye", "bone_meal", "nether_brick", "nether_bricks"};
        int[] ids = {35, 5, 162, 159, 351, 351, 405, 112}, damage = {14, 5, 0, 11, 4, 15, 0, 0};
        for (int i = 0; i < names.length; i++) {
            NBTTagCompound item = stack(names[i], 37); convert(item, 3955);
            assertEquals(names[i], ids[i], item.getShort("id")); assertEquals(names[i], damage[i], item.getShort("Damage"));
            assertEquals(37, item.getByte("Count")); assertEquals(6, item.getByte("Slot")); assertFalse(item.hasKey("count"));
        }
    }

    @Test public void componentsKeepDamageNamesLoreAndOldEnchantments() {
        NBTTagCompound item = stack("diamond_sword", 1), c = components(item), enchantments = new NBTTagCompound(), levels = new NBTTagCompound();
        c.setInteger("minecraft:damage", 73); c.setString("minecraft:custom_name", "{\"text\":\"Named sword\"}");
        levels.setInteger("minecraft:sharpness", 4); levels.setInteger("minecraft:unbreaking", 3);
        enchantments.setTag("levels", levels); c.setTag("minecraft:enchantments", enchantments);
        NBTTagList lore = new NBTTagList(); lore.appendTag(new NBTTagString("{\"text\":\"Line one\"}")); c.setTag("minecraft:lore", lore);
        convert(item, 3837);
        assertEquals(73, item.getShort("Damage")); assertEquals("Named sword", item.getCompoundTag("tag").getCompoundTag("display").getString("Name"));
        assertEquals("Line one", item.getCompoundTag("tag").getCompoundTag("display").getTagList("Lore", NBT.TAG_STRING).getStringTagAt(0));
        NBTTagList ench = item.getCompoundTag("tag").getTagList("ench", NBT.TAG_COMPOUND);
        assertEquals(2, ench.tagCount());
        Map<Integer, Integer> actual = new HashMap<>();
        for (int i = 0; i < ench.tagCount(); i++) actual.put((int) ench.getCompoundTagAt(i).getShort("id"), (int) ench.getCompoundTagAt(i).getShort("lvl"));
        assertEquals(Integer.valueOf(4), actual.get(16)); assertEquals(Integer.valueOf(3), actual.get(34)); assertFalse(item.hasKey("components"));
    }

    @Test public void nbtTextAndSimplifiedEnchantmentsImportFrom1215() {
        NBTTagCompound item = stack("enchanted_book", 1), c = components(item), ench = new NBTTagCompound();
        c.setTag("minecraft:custom_name", text("附魔书")); ench.setInteger("minecraft:efficiency", 5); c.setTag("minecraft:stored_enchantments", ench);
        convert(item, 4325);
        assertEquals("附魔书", item.getCompoundTag("tag").getCompoundTag("display").getString("Name"));
        NBTTagCompound entry = item.getCompoundTag("tag").getTagList("StoredEnchantments", NBT.TAG_COMPOUND).getCompoundTagAt(0);
        assertEquals(32, entry.getShort("id")); assertEquals(5, entry.getShort("lvl"));
    }

    @Test public void oldNamedEnchantmentsAndDamageAreConvertedWithoutComponents() {
        NBTTagCompound item = stack("diamond_sword", 1), tag = new NBTTagCompound(), e = new NBTTagCompound();
        NBTTagList list = new NBTTagList(); e.setString("id", "minecraft:sharpness"); e.setShort("lvl", (short) 2); list.appendTag(e);
        tag.setTag("Enchantments", list); tag.setInteger("Damage", 91); item.setTag("tag", tag); convert(item, 3700);
        assertEquals(91, item.getShort("Damage")); assertFalse(tag.hasKey("Damage")); assertFalse(tag.hasKey("Enchantments"));
        assertEquals(16, tag.getTagList("ench", NBT.TAG_COMPOUND).getCompoundTagAt(0).getShort("id"));
    }

    @Test public void potionComponentsPreserveSplashDamageAndEffects() {
        NBTTagCompound item = stack("splash_potion", 1), c = components(item), potion = new NBTTagCompound(), effect = new NBTTagCompound();
        potion.setString("potion", "minecraft:strong_healing"); effect.setString("id", "minecraft:speed");
        effect.setInteger("amplifier", 2); effect.setInteger("duration", 600); NBTTagList list = new NBTTagList(); list.appendTag(effect);
        potion.setTag("custom_effects", list); c.setTag("minecraft:potion_contents", potion); convert(item, 4325);
        assertEquals(373, item.getShort("id")); assertEquals(16421, item.getShort("Damage"));
        NBTTagCompound restored = item.getCompoundTag("tag").getTagList("CustomPotionEffects", NBT.TAG_COMPOUND).getCompoundTagAt(0);
        assertEquals(1, restored.getByte("Id")); assertEquals(2, restored.getByte("Amplifier")); assertEquals(600, restored.getInteger("Duration"));
    }

    @Test public void filteredBookPagesBecomePlainLegacyPages() {
        NBTTagCompound item = stack("written_book", 1), c = components(item), book = new NBTTagCompound(), title = new NBTTagCompound();
        title.setString("raw", "Test book"); book.setTag("title", title); book.setString("author", "Builder");
        NBTTagCompound page = new NBTTagCompound(); page.setTag("raw", text("第一页")); NBTTagList pages = new NBTTagList(); pages.appendTag(page);
        book.setTag("pages", pages); c.setTag("minecraft:written_book_content", book); convert(item, 4325);
        NBTTagCompound data = item.getCompoundTag("tag"); assertEquals("Test book", data.getString("title")); assertEquals("Builder", data.getString("author"));
        assertEquals("第一页", data.getTagList("pages", NBT.TAG_STRING).getStringTagAt(0));
    }

    @Test public void bothDyedColorLayoutsAreSupported() {
        for (int version : new int[] {3837, 4325}) {
            NBTTagCompound item = stack("leather_chestplate", 1), c = components(item);
            if (version == 3837) { NBTTagCompound color = new NBTTagCompound(); color.setInteger("rgb", 0x123456); c.setTag("minecraft:dyed_color", color); }
            else c.setInteger("minecraft:dyed_color", 0x123456);
            c.setInteger("minecraft:repair_cost", 7); c.setTag("minecraft:unbreakable", new NBTTagCompound()); convert(item, version);
            assertEquals(0x123456, item.getCompoundTag("tag").getCompoundTag("display").getInteger("color"));
            assertEquals(7, item.getCompoundTag("tag").getInteger("RepairCost")); assertTrue(item.getCompoundTag("tag").getBoolean("Unbreakable"));
        }
    }

    @Test public void unsupportedItemsCountsAndComponentsAreReported() {
        ImportReport.begin();
        try {
            NBTTagCompound unknown = stack("netherite_ingot", 1); convert(unknown, 4325); assertEquals(0, unknown.getByte("Count"));
            NBTTagCompound tooMany = stack("diamond", 128); convert(tooMany, 4325); assertEquals(0, tooMany.getByte("Count"));
            NBTTagCompound item = stack("diamond_sword", 1), c = components(item), ench = new NBTTagCompound();
            ench.setInteger("minecraft:mending", 1); c.setTag("minecraft:enchantments", ench); c.setInteger("minecraft:custom_model_data", 12); convert(item, 4325);
            assertEquals(276, item.getShort("id")); assertEquals(0, item.getCompoundTag("tag").getTagList("ench", NBT.TAG_COMPOUND).tagCount());
        } finally { ImportReport.end(); }
        assertEquals(2, ImportReport.lastItems().removed);
        assertTrue(ImportReport.lastItems().unknownItems.contains("minecraft:netherite_ingot"));
        assertTrue(ImportReport.lastItems().omittedData.contains("minecraft:custom_model_data"));
        assertTrue(ImportReport.lastItems().omittedData.contains("enchantment: minecraft:mending"));
    }

    @Test public void customDataSurvivesAndExplicitComponentsTakePrecedence() {
        NBTTagCompound item = stack("diamond", 3), c = components(item), custom = new NBTTagCompound(), display = new NBTTagCompound();
        display.setString("Name", "Old"); custom.setTag("display", display); custom.setInteger("CustomValue", 42); c.setTag("minecraft:custom_data", custom);
        c.setString("minecraft:custom_name", "New"); convert(item, 4325);
        assertEquals("New", item.getCompoundTag("tag").getCompoundTag("display").getString("Name"));
        assertEquals(42, item.getCompoundTag("tag").getInteger("CustomValue")); assertEquals("Old", display.getString("Name"));
    }

    @Test public void numericStacksAreNotReinterpreted() {
        NBTTagCompound item = new NBTTagCompound(); item.setShort("id", (short) 35); item.setShort("Damage", (short) 14); item.setByte("Count", (byte) 2);
        NBTTagCompound original = (NBTTagCompound) item.copy(); convert(item, 4325); assertEquals(original, item);
    }
}

package com.github.lunatrius.schematica.compat;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants.NBT;
import org.junit.Test;
import static org.junit.Assert.*;

public class ModernEntityNbtTest {
    private static NBTTagCompound item(int id) {
        NBTTagCompound tag = new NBTTagCompound(); tag.setShort("id", (short) id); tag.setByte("Count", (byte) 1); return tag;
    }
    @Test public void equipmentCompoundPreservesSlotPositionsAndDropChances() {
        NBTTagCompound entity = new NBTTagCompound(), equipment = new NBTTagCompound(), chances = new NBTTagCompound(), name = new NBTTagCompound();
        entity.setString("id", "minecraft:zombie"); equipment.setTag("mainhand", item(276)); equipment.setTag("head", item(310));
        entity.setTag("equipment", equipment); chances.setFloat("head", 0.75F); entity.setTag("drop_chances", chances);
        name.setString("text", "Guard"); entity.setTag("CustomName", name);
        assertTrue(EntityTranslator.instance().translate(entity, 4325));
        assertEquals("Zombie", entity.getString("id")); assertEquals("Guard", entity.getString("CustomName"));
        NBTTagList items = entity.getTagList("Equipment", NBT.TAG_COMPOUND); assertEquals(5, items.tagCount());
        assertEquals(276, items.getCompoundTagAt(0).getShort("id")); assertEquals(310, items.getCompoundTagAt(4).getShort("id"));
        for (int i = 1; i < 4; i++) assertTrue(items.getCompoundTagAt(i).hasNoTags());
        assertEquals(0.75F, entity.getTagList("DropChances", NBT.TAG_FLOAT).func_150308_e(4), 0);
        assertEquals(0.085F, entity.getTagList("DropChances", NBT.TAG_FLOAT).func_150308_e(0), 0);
        assertFalse(entity.hasKey("equipment")); assertEquals(310, equipment.getCompoundTag("head").getShort("id"));
    }
    @Test public void preComponentHandAndArmorListsAlsoKeepEmptySlots() {
        NBTTagCompound entity = new NBTTagCompound(); entity.setString("id", "minecraft:skeleton");
        NBTTagList hand = new NBTTagList(), armor = new NBTTagList(); hand.appendTag(item(261)); hand.appendTag(new NBTTagCompound());
        armor.appendTag(new NBTTagCompound()); armor.appendTag(item(308)); entity.setTag("HandItems", hand); entity.setTag("ArmorItems", armor);
        assertTrue(EntityTranslator.instance().translate(entity, 3700));
        NBTTagList equipment = entity.getTagList("Equipment", NBT.TAG_COMPOUND);
        assertEquals(261, equipment.getCompoundTagAt(0).getShort("id")); assertTrue(equipment.getCompoundTagAt(1).hasNoTags());
        assertEquals(308, equipment.getCompoundTagAt(2).getShort("id")); assertTrue(equipment.getCompoundTagAt(4).hasNoTags());
    }
    @Test public void modernFurnaceNamesAndTimersAreReadableByVanilla1710() {
        NBTTagCompound tag = new NBTTagCompound(), name = new NBTTagCompound(); tag.setString("id", "minecraft:furnace");
        tag.setInteger("lit_time_remaining", 400); tag.setInteger("cooking_time_spent", 85); name.setString("text", "Named furnace"); tag.setTag("CustomName", name);
        assertTrue(TileEntityTranslator.instance().translate(tag, "minecraft:furnace[facing=east,lit=true]", 4325));
        assertEquals("Furnace", tag.getString("id")); assertEquals(400, tag.getShort("BurnTime")); assertEquals(85, tag.getShort("CookTime"));
        assertEquals("Named furnace", tag.getString("CustomName"));
    }
    @Test public void notePitchAndSkullTypeCanBeReconstructedWithoutTileNbt() {
        NBTTagCompound note = TileEntityTranslator.instance().createBlockStateTile(2, 3, 4, "minecraft:note_block[instrument=harp,note=19,powered=false]");
        assertEquals("Music", note.getString("id")); assertEquals(19, note.getByte("note")); assertEquals(2, note.getInteger("x"));
        NBTTagCompound skull = TileEntityTranslator.instance().createBlockStateTile(2, 3, 4, "minecraft:creeper_head[rotation=11,powered=false]");
        assertEquals("Skull", skull.getString("id")); assertEquals(4, skull.getByte("SkullType")); assertEquals(11, skull.getByte("Rot"));
        assertNull(TileEntityTranslator.instance().createBlockStateTile(0, 0, 0, "minecraft:stone"));
    }
}

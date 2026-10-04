package com.github.lunatrius.schematica.world.schematic;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants.NBT;
import org.junit.Test;
import static org.junit.Assert.*;

public class LegacyItemDataTest {
    private static NBTTagCompound stack(String id, int damage) {
        NBTTagCompound stack = new NBTTagCompound();
        stack.setString("id", id);
        stack.setByte("Count", (byte) 1);
        stack.setShort("Damage", (short) damage);
        return stack;
    }

    @Test public void potionsMoveTheirDamageIntoThePotionTagAndBack() {
        NBTTagCompound regeneration = stack("minecraft:potion", 8193);
        LegacyNbt.itemDataTo112(regeneration);
        assertEquals("minecraft:potion", regeneration.getString("id"));
        assertEquals("minecraft:regeneration", regeneration.getCompoundTag("tag").getString("Potion"));
        assertEquals(0, regeneration.getShort("Damage"));
        LegacyNbt.itemDataFrom112(regeneration);
        assertEquals(8193, regeneration.getShort("Damage"));
        assertFalse(regeneration.hasKey("tag"));

        NBTTagCompound splash = stack("minecraft:potion", 16421);
        LegacyNbt.itemDataTo112(splash);
        assertEquals("minecraft:splash_potion", splash.getString("id"));
        assertEquals("minecraft:strong_healing", splash.getCompoundTag("tag").getString("Potion"));
        LegacyNbt.itemDataFrom112(splash);
        assertEquals("minecraft:potion", splash.getString("id"));
        assertEquals(16421, splash.getShort("Damage"));

        assertEquals(16, LegacyNbt.potionDamage("minecraft:awkward", false));
        assertEquals(0, LegacyNbt.potionDamage("minecraft:water", false));
        assertEquals(8257, LegacyNbt.potionDamage("minecraft:long_regeneration", false));
    }

    @Test public void spawnEggsUseEntityTags() {
        NBTTagCompound egg = stack("minecraft:spawn_egg", 90);
        LegacyNbt.itemDataTo112(egg);
        assertEquals("minecraft:pig", egg.getCompoundTag("tag").getCompoundTag("EntityTag").getString("id"));
        assertEquals(0, egg.getShort("Damage"));
        LegacyNbt.itemDataFrom112(egg);
        assertEquals(90, egg.getShort("Damage"));
        assertFalse(egg.hasKey("tag"));
    }

    private static NBTTagCompound entity(String id, double x) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", id);
        NBTTagList pos = new NBTTagList();
        pos.appendTag(new NBTTagDouble(x));
        pos.appendTag(new NBTTagDouble(64));
        pos.appendTag(new NBTTagDouble(0));
        tag.setTag("Pos", pos);
        return tag;
    }

    @Test public void ridersBecomePassengersOfTheirMountsAndBack() {
        NBTTagCompound jockey = entity("Skeleton", 1), spider = entity("Spider", 2), boat = entity("Boat", 3);
        spider.setTag("Riding", boat);
        jockey.setTag("Riding", spider);
        NBTTagCompound root = LegacyNbt.entityTo112(jockey);
        assertEquals("minecraft:boat", root.getString("id"));
        NBTTagCompound spider112 = root.getTagList("Passengers", NBT.TAG_COMPOUND).getCompoundTagAt(0);
        assertEquals("minecraft:spider", spider112.getString("id"));
        assertEquals("minecraft:skeleton", spider112.getTagList("Passengers", NBT.TAG_COMPOUND).getCompoundTagAt(0).getString("id"));
        assertFalse(spider112.hasKey("Riding"));

        NBTTagCompound back = LegacyNbt.entityFrom112(root);
        assertEquals("Skeleton", back.getString("id"));
        assertEquals("Spider", back.getCompoundTag("Riding").getString("id"));
        assertEquals("Boat", back.getCompoundTag("Riding").getCompoundTag("Riding").getString("id"));
        assertEquals(3, back.getTagList("Pos", NBT.TAG_DOUBLE).func_150309_d(0), 0);
        assertFalse(back.hasKey("Passengers"));
    }
}

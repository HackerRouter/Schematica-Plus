package com.github.lunatrius.schematica.compat;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

import static org.junit.Assert.*;

public class LogisticsPipesNbtTest {
    @Test public void chassisModulesAndTurtleSidesTurn() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Orientation", 2);
        tag.setBoolean("turtleConnect_2", true);
        tag.setBoolean("turtleConnect_5", false);
        NBTTagCompound module = new NBTTagCompound();
        module.setInteger("sneakydirection", 1);
        NBTTagCompound slot = new NBTTagCompound();
        slot.setTag("module", module);
        tag.setTag("slot0", slot);
        NBTTagCompound none = new NBTTagCompound();
        none.setInteger("Orientation", 6);
        LogisticsPipesVisualAdapter.transform(tag, 'Y');
        LogisticsPipesVisualAdapter.transform(none, 'Y');
        assertEquals(5, tag.getInteger("Orientation"));
        assertTrue(tag.getBoolean("turtleConnect_5"));
        assertFalse(tag.getBoolean("turtleConnect_2"));
        assertEquals(1, module.getInteger("sneakydirection"));
        LogisticsPipesVisualAdapter.transform(tag, 'y');
        assertEquals(0, tag.getCompoundTag("slot0").getCompoundTag("module").getInteger("sneakydirection"));
        assertEquals(6, none.getInteger("Orientation"));
    }
}

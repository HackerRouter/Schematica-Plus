package com.github.lunatrius.schematica.compat;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.junit.Test;

import static org.junit.Assert.*;

public class BuildCraftPipeNbtTest {
    @Test public void sidesMoveAndGateDirectionsTurn() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagCompound plug = new NBTTagCompound();
        plug.setString("pluggableName", "gate");
        tag.setTag("pluggable[2]", plug);
        tag.setByte("redstoneInputSide[2]", (byte) 15);
        tag.setByte("redstoneInputSide[5]", (byte) 0);
        NBTTagCompound gate = new NBTTagCompound();
        gate.setInteger("direction", 2);
        tag.setTag("Gate[2]", gate);
        tag.setBoolean("powerSources[0]", true);
        BuildCraftPipeNbt.transform(tag, 'Y', false);
        // north (2) -> east (5), east -> south (3), down stays
        assertEquals("gate", tag.getCompoundTag("pluggable[5]").getString("pluggableName"));
        assertFalse(tag.hasKey("pluggable[2]"));
        assertEquals(15, tag.getByte("redstoneInputSide[5]"));
        assertEquals(0, tag.getByte("redstoneInputSide[3]"));
        assertFalse(tag.hasKey("redstoneInputSide[2]"));
        assertEquals(5, tag.getCompoundTag("Gate[5]").getInteger("direction"));
        assertTrue(tag.getBoolean("powerSources[0]"));
    }

    @Test public void diamondFilterRowsFollowTheirSides() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList items = new NBTTagList();
        NBTTagCompound slot = new NBTTagCompound();
        slot.setByte("Slot", (byte) (4 * 9 + 3));
        items.appendTag(slot);
        tag.setTag("Items", items);
        tag.setLong("usedFilters", 1L << 4 * 9 + 3);
        BuildCraftPipeNbt.transform(tag, 'x', true);
        // west (4) <-> east (5)
        assertEquals(5 * 9 + 3, items.getCompoundTagAt(0).getByte("Slot"));
        assertEquals(1L << 5 * 9 + 3, tag.getLong("usedFilters"));
        BuildCraftPipeNbt.transform(tag, 'x', false);
        assertEquals(5 * 9 + 3, items.getCompoundTagAt(0).getByte("Slot"));
    }
}

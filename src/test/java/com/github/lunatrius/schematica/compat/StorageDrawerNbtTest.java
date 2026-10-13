package com.github.lunatrius.schematica.compat;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.junit.Test;
import static org.junit.Assert.*;

public class StorageDrawerNbtTest {
    private static NBTTagCompound modern(int count) {
        NBTTagCompound tile = new NBTTagCompound(), slot = new NBTTagCompound(), stack = new NBTTagCompound(), data = new NBTTagCompound();
        tile.setString("id", "StorageDrawers:tileDrawersStandard");
        stack.setShort("id", (short) 4112);
        stack.setShort("Damage", (short) 7);
        stack.setByte("Count", (byte) 1);
        data.setString("CustomName", "Stored prototype");
        stack.setTag("tag", data);
        slot.setTag("ItemStack", stack);
        slot.setInteger("Count", count);
        NBTTagList slots = new NBTTagList(); slots.appendTag(slot); tile.setTag("Slots", slots);
        return tile;
    }

    private static NBTTagCompound slot(NBTTagCompound tile) { return tile.getTagList("Slots", 10).getCompoundTagAt(0); }

    @Test public void modernSlotsKeepFullCountsDamageAndItemNbtWithoutChangingTheSource() {
        NBTTagCompound source = modern(4096), before = (NBTTagCompound) source.copy();
        NBTTagCompound converted = StorageDrawerNbt.readable(source), slot = slot(converted);
        assertEquals(4112, slot.getShort("Item"));
        assertEquals(7, slot.getShort("Meta"));
        assertEquals(4096, slot.getInteger("Count"));
        assertEquals("Stored prototype", slot.getCompoundTag("Tags").getString("CustomName"));
        assertFalse(slot.hasKey("ItemStack"));
        assertEquals(before, source);
        assertSame(converted, StorageDrawerNbt.readable(converted));
    }

    @Test public void emptyLockedDrawersKeepTheirPrototype() {
        NBTTagCompound slot = slot(StorageDrawerNbt.readable(modern(0)));
        assertEquals(4112, slot.getShort("Item"));
        assertEquals(0, slot.getInteger("Count"));
    }

    @Test public void removedModernItemsDoNotResurrectStaleLegacyFields() {
        NBTTagCompound tile = modern(64), slot = slot(tile);
        slot.getCompoundTag("ItemStack").setByte("Count", (byte) 0);
        slot.setShort("Item", (short) 264);
        slot.setShort("Meta", (short) 0);
        slot.setTag("Tags", new NBTTagCompound());
        slot = slot(StorageDrawerNbt.readable(tile));
        assertEquals(0, slot.getInteger("Count"));
        assertFalse(slot.hasKey("Item"));
        assertFalse(slot.hasKey("Tags"));
    }

    @Test public void otherModsWithSlotFieldsKeepTheirOwnLayout() {
        NBTTagCompound tile = modern(64);
        tile.setString("id", "example:machine");
        assertSame(tile, StorageDrawerNbt.readable(tile));
    }
}

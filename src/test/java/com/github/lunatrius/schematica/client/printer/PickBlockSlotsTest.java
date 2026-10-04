package com.github.lunatrius.schematica.client.printer;

import java.util.Arrays;

import net.minecraft.item.Item;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import org.junit.Test;
import static org.junit.Assert.*;

public class PickBlockSlotsTest {
    @Test public void parsesOneBasedHotbarSlotsWithoutDuplicatesOrInvalidEntries() {
        assertEquals(Arrays.asList(0, 1, 2, 3, 4), PickBlockSlots.parse("1,2,3,4,5"));
        assertEquals(Arrays.asList(8, 0), PickBlockSlots.parse(" 9, 1,9, 0, 10, x,"));
        assertTrue(PickBlockSlots.parse("").isEmpty());
    }

    @Test public void swordsAndHoesCountAsTools() {
        assertTrue(PickBlockSlots.isTool(new ItemStack(new ItemSword(Item.ToolMaterial.IRON))));
        assertTrue(PickBlockSlots.isTool(new ItemStack(new ItemHoe(Item.ToolMaterial.IRON))));
        assertFalse(PickBlockSlots.isTool(new ItemStack(new Item())));
    }
}

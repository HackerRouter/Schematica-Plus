package com.github.lunatrius.schematica.client.printer;

import java.util.Arrays;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
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

    @Test public void inventoryIndicesMapToTheContainerSlotsThatShowThem() {
        InventoryBasic inventory = new InventoryBasic("player", false, 41), other = new InventoryBasic("crafting", false, 5);
        Container container = new Container() {
            {
                for (int i = 0; i < 5; i++) addSlotToContainer(new Slot(other, i, 0, 0));
                for (int i = 0; i < 4; i++) addSlotToContainer(new Slot(inventory, 37 + i, 0, 0));
                for (int i = 9; i < 36; i++) addSlotToContainer(new Slot(inventory, i, 0, 0));
                for (int i = 0; i < 9; i++) addSlotToContainer(new Slot(inventory, i, 0, 0));
                addSlotToContainer(new Slot(inventory, 36, 0, 0));
            }

            @Override public boolean canInteractWith(EntityPlayer player) { return true; }
        };
        assertEquals(36, PickBlockSlots.containerSlot(container, inventory, 0));
        assertEquals(9, PickBlockSlots.containerSlot(container, inventory, 9));
        assertEquals(45, PickBlockSlots.containerSlot(container, inventory, 36));
        assertEquals(5, PickBlockSlots.containerSlot(container, inventory, 37));
        assertEquals(-1, PickBlockSlots.containerSlot(container, inventory, 41));
    }
}

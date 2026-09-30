package com.github.lunatrius.schematica.client.gui.material;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;
import static org.junit.Assert.*;

public class MaterialItemKeyTest {
    @Test public void keepsNbtVariantsSeparateWithoutDependingOnStackSizeOrMutableTags() {
        ItemStack stack = new ItemStack(new Item(), 32, 5);
        stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setInteger("Variant", 1);
        MaterialItemKey key = new MaterialItemKey(stack);
        stack.stackSize = 1;
        assertEquals(key, new MaterialItemKey(stack));
        assertEquals(key.hashCode(), new MaterialItemKey(stack).hashCode());
        stack.getTagCompound().setInteger("Variant", 2);
        assertNotEquals(key, new MaterialItemKey(stack));
        ItemStack copy = key.stack();
        copy.getTagCompound().setInteger("Variant", 9);
        assertEquals(1, key.stack().getTagCompound().getInteger("Variant"));
    }
}

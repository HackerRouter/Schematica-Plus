package com.github.lunatrius.schematica.client.gui.material;

import java.util.Objects;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public final class MaterialItemKey {
    private final Item item;
    private final int damage;
    private final NBTTagCompound tag;
    private final int hash;

    public MaterialItemKey(ItemStack stack) {
        item = Objects.requireNonNull(stack.getItem());
        damage = stack.getItemDamage();
        tag = stack.hasTagCompound() ? (NBTTagCompound) stack.getTagCompound().copy() : null;
        hash = Objects.hash(item, damage, tag);
    }

    public ItemStack stack() {
        ItemStack stack = new ItemStack(item, 1, damage);
        if (tag != null) stack.setTagCompound((NBTTagCompound) tag.copy());
        return stack;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof MaterialItemKey)) return false;
        MaterialItemKey other = (MaterialItemKey) object;
        return item == other.item && damage == other.damage && Objects.equals(tag, other.tag);
    }

    @Override
    public int hashCode() { return hash; }
}

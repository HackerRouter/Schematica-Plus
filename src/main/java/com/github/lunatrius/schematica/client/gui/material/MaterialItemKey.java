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

    /** "modid:name@damage", with "#" and the NBT when there is one; null for unregistered items. */
    public String encode() {
        Object name = cpw.mods.fml.common.registry.GameData.getItemRegistry().getNameForObject(item);
        if (name == null) return null;
        return name + "@" + damage + (tag == null ? "" : "#" + tag);
    }

    public static MaterialItemKey decode(String value) {
        if (value == null) return null;
        try {
            int hash = value.indexOf('#'), at = value.lastIndexOf('@', hash < 0 ? value.length() : hash);
            if (at < 0) return null;
            Object item = cpw.mods.fml.common.registry.GameData.getItemRegistry().getObject(value.substring(0, at));
            if (!(item instanceof Item)) return null;
            ItemStack stack = new ItemStack((Item) item, 1, Integer.parseInt(value.substring(at + 1, hash < 0 ? value.length() : hash)));
            if (hash >= 0) {
                net.minecraft.nbt.NBTBase tag = net.minecraft.nbt.JsonToNBT.func_150315_a(value.substring(hash + 1));
                if (tag instanceof NBTTagCompound) stack.setTagCompound((NBTTagCompound) tag);
            }
            return new MaterialItemKey(stack);
        } catch (Exception error) {
            return null;
        }
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

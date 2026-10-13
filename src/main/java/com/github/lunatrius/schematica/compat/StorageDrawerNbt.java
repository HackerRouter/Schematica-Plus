package com.github.lunatrius.schematica.compat;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants.NBT;

/** The legacy drawer slot layout is understood by both StorageDrawers versions shipped in GTNH. */
public final class StorageDrawerNbt {
    private StorageDrawerNbt() {}

    public static NBTTagCompound readable(NBTTagCompound source) {
        if (!source.getString("id").startsWith("StorageDrawers:tileDrawers")) return source;
        NBTTagList slots = source.getTagList("Slots", NBT.TAG_COMPOUND);
        boolean modern = false;
        for (int i = 0; i < slots.tagCount(); i++) modern |= slots.getCompoundTagAt(i).hasKey("ItemStack", NBT.TAG_COMPOUND);
        if (!modern) return source;
        NBTTagCompound copy = (NBTTagCompound) source.copy();
        slots = copy.getTagList("Slots", NBT.TAG_COMPOUND);
        for (int i = 0; i < slots.tagCount(); i++) {
            NBTTagCompound slot = slots.getCompoundTagAt(i);
            if (!slot.hasKey("ItemStack", NBT.TAG_COMPOUND)) continue;
            NBTTagCompound stack = slot.getCompoundTag("ItemStack");
            slot.removeTag("Item");
            slot.removeTag("Meta");
            slot.removeTag("Tags");
            if (stack.hasKey("id", NBT.TAG_ANY_NUMERIC) && stack.getInteger("id") != 0 && stack.getByte("Count") != 0) {
                slot.setShort("Item", stack.getShort("id"));
                slot.setShort("Meta", stack.getShort("Damage"));
                if (stack.hasKey("tag", NBT.TAG_COMPOUND)) slot.setTag("Tags", stack.getCompoundTag("tag").copy());
            } else slot.setInteger("Count", 0);
            slot.removeTag("ItemStack");
        }
        return copy;
    }
}

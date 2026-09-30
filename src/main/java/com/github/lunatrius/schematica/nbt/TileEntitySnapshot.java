package com.github.lunatrius.schematica.nbt;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

final class TileEntitySnapshot {
    private NBTTagCompound original;
    private NBTTagCompound baseline;
    private NBTTagCompound visual;

    TileEntitySnapshot(NBTTagCompound tag) {
        original = (NBTTagCompound) tag.copy();
        visual = original.hasKey(TileUpdateData.KEY, 10) ? original.getCompoundTag(TileUpdateData.KEY) : null;
        original.removeTag(TileUpdateData.KEY);
    }

    NBTTagCompound visual() { return visual; }

    void visual(NBTTagCompound tag) { visual = tag; }

    boolean isInitialized() { return baseline != null; }

    void initialize(NBTTagCompound tag) { baseline = (NBTTagCompound) tag.copy(); }

    void rebase(NBTTagCompound transformed, NBTTagCompound preview) {
        original = (NBTTagCompound) transformed.copy();
        original.removeTag(TileUpdateData.KEY);
        initialize(preview);
    }

    NBTTagCompound write(NBTTagCompound current, int x, int y, int z) {
        NBTTagCompound tag = (NBTTagCompound) original.copy();
        if (baseline != null && current != null) applyChanges(tag, baseline, current);
        tag.setInteger("x", x);
        tag.setInteger("y", y);
        tag.setInteger("z", z);
        if (visual != null) tag.setTag(TileUpdateData.KEY, visual.copy());
        return tag;
    }

    private static void applyChanges(NBTTagCompound target, NBTTagCompound before, NBTTagCompound after) {
        Set<String> keys = new HashSet<>();
        for (Object key : before.func_150296_c()) keys.add((String) key);
        for (Object key : after.func_150296_c()) keys.add((String) key);
        for (String key : keys) {
            NBTBase oldValue = before.getTag(key), newValue = after.getTag(key);
            if (java.util.Objects.equals(oldValue, newValue)) continue;
            if (newValue == null) {
                target.removeTag(key);
            } else if (oldValue instanceof NBTTagCompound && newValue instanceof NBTTagCompound
                && target.hasKey(key, 10)) {
                applyChanges(target.getCompoundTag(key), (NBTTagCompound) oldValue, (NBTTagCompound) newValue);
            } else {
                target.setTag(key, newValue.copy());
            }
        }
    }
}

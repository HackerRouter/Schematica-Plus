package com.github.lunatrius.schematica.compat;

import java.util.Map;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import cpw.mods.fml.relauncher.ReflectionHelper;

public final class TileNBTCompat {
    private TileNBTCompat() {}

    public static NBTTagCompound write(TileEntity tile) {
        NBTTagCompound tag = new NBTTagCompound();
        if (Reflect.is(tile, "mcp.mobius.betterbarrels.common.blocks.TileEntityBarrel")) {
            try {
                writeBarrel(tile, tag);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Could not capture barrel without registering a world position", e);
            }
        } else {
            tile.writeToNBT(tag);
        }
        if (tag.getString("id").isEmpty()) {
            Map<?, ?> names = ReflectionHelper.getPrivateValue(TileEntity.class, null, "classToNameMap", "field_145853_j");
            Object name = names.get(tile.getClass());
            if (!(name instanceof String)) throw new IllegalArgumentException("Unregistered tile " + tile.getClass().getName());
            tag.setString("id", (String) name);
        }
        tag.setInteger("x", tile.xCoord);
        tag.setInteger("y", tile.yCoord);
        tag.setInteger("z", tile.zCoord);
        return tag;
    }

    private static void writeBarrel(Object tile, NBTTagCompound tag) throws ReflectiveOperationException {
        tag.setInteger("version", ((Number) Reflect.get(tile, "version")).intValue());
        tag.setInteger("orientation", ((Enum<?>) Reflect.get(tile, "orientation")).ordinal());
        tag.setInteger("rotation", ((Enum<?>) Reflect.get(tile, "rotation")).ordinal());
        tag.setIntArray("sideUpgrades", ((int[]) Reflect.get(tile, "sideUpgrades")).clone());
        tag.setIntArray("sideMeta", ((int[]) Reflect.get(tile, "sideMetadata")).clone());
        tag.setBoolean("ticking", (Boolean) Reflect.get(tile, "isTicking"));
        tag.setBoolean("linked", (Boolean) Reflect.get(tile, "isLinked"));
        tag.setByte("nticks", ((Number) Reflect.get(tile, "nTicks")).byteValue());
        tag.setInteger("bspaceid", ((Number) Reflect.get(tile, "id")).intValue());
        Reflect.call(Reflect.get(tile, "coreUpgrades"), "writeToNBT", new Class<?>[] { NBTTagCompound.class }, tag);
        tag.setTag("storage", ((NBTTagCompound) Reflect.call(Reflect.call(tile, "getStorage"), "writeTagCompound")).copy());
    }
}

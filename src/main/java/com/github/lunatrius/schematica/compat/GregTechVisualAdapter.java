package com.github.lunatrius.schematica.compat;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import com.github.lunatrius.schematica.api.ISchematicVisualAdapter;

final class GregTechVisualAdapter implements ISchematicVisualAdapter {
    @Override public String id() { return "gregtech:custom_data"; }
    @Override public String protocol() { return StreamVisualAdapter.version("gregtech"); }
    @Override public boolean replacesDescriptionPacket(TileEntity tile) { return false; }
    @Override public boolean supports(TileEntity tile) {
        return Reflect.is(tile, "gregtech.api.metatileentity.CommonBaseMetaTileEntity");
    }

    @Override public NBTTagCompound capture(TileEntity tile) throws Exception {
        Object meta = Reflect.call(tile, "getMetaTileEntity");
        if (meta == null) return null;
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Class", meta.getClass().getName());
        tag.setByte("Value", updateData(meta, tile.hasWorldObj() && tile.getWorldObj().isRemote));
        return tag;
    }

    static byte updateData(Object meta, boolean client) throws ReflectiveOperationException {
        byte value = (Byte) Reflect.call(meta, "getUpdateData");
        if (client && (Reflect.is(meta, "gregtech.common.tileentities.machines.multi.turbines.MTELargeTurbineBase")
            || Reflect.is(meta, "gregtech.common.tileentities.machines.multi.MTELargeTurbineLegacy")
            || Reflect.is(meta, "goodgenerator.blocks.tileEntity.base.MTELargeTurbineBaseLegacy"))) {
            value = (byte) (((Boolean) Reflect.get(meta, "mHasTurbine") ? 1 : 0)
                | ((Boolean) Reflect.get(meta, "mFormed") ? 2 : 0));
        } else if (client && Reflect.is(meta, "gregtech.common.tileentities.machines.multi.MTEAirFilterBase")) {
            value = (byte) ((Boolean) Reflect.get(meta, "mFormed") ? 1 : 0);
        }
        return value;
    }

    @Override public void restore(TileEntity tile, NBTTagCompound tag) throws Exception {
        Object meta = Reflect.call(tile, "getMetaTileEntity");
        if (meta != null && tag.getString("Class").equals(meta.getClass().getName()) && tag.hasKey("Value", 1)) {
            Reflect.call(meta, "onValueUpdate", new Class<?>[] {byte.class}, tag.getByte("Value"));
        }
    }
}

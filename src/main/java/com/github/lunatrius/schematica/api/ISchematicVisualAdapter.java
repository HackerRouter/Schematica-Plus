package com.github.lunatrius.schematica.api;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

public interface ISchematicVisualAdapter {
    String id();
    boolean supports(TileEntity tile);
    NBTTagCompound capture(TileEntity tile) throws Exception;
    void restore(TileEntity tile, NBTTagCompound data) throws Exception;
    default String protocol() { return "1"; }
    default boolean replacesDescriptionPacket(TileEntity tile) { return true; }
}

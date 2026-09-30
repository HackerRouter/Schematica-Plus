package com.github.lunatrius.schematica.compat;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import com.github.lunatrius.schematica.api.ISchematicVisualAdapter;

final class BuildCraftPipeAdapter implements ISchematicVisualAdapter {
    @Override public String id() { return "buildcraft:pipes"; }
    @Override public String protocol() { return StreamVisualAdapter.version("BuildCraft|Transport"); }
    @Override public boolean supports(TileEntity tile) {
        return Reflect.is(tile, "buildcraft.transport.TileGenericPipe");
    }

    @Override public NBTTagCompound capture(TileEntity tile) throws Exception {
        NBTTagCompound tag = new NBTTagCompound();
        for (byte id = 1; id <= 3; id++) {
            Object state = id == 3 ? Reflect.get(tile, "pipe")
                : Reflect.call(tile, "getStateInstance", new Class<?>[] { byte.class }, id);
            if (Reflect.is(state, "buildcraft.api.core.ISerializable")) {
                tag.setByteArray("State" + id, VisualStreams.writeBuffer(state, "writeData"));
            }
        }
        return tag;
    }

    @Override public void restore(TileEntity tile, NBTTagCompound tag) throws Exception {
        for (byte id = 1; id <= 3; id++) {
            if (!tag.hasKey("State" + id, 7)) continue;
            Object state = Reflect.call(tile, "getStateInstance", new Class<?>[] { byte.class }, id);
            VisualStreams.readBuffer(state, "readData", tag.getByteArray("State" + id));
            Reflect.call(tile, "afterStateUpdated", new Class<?>[] { byte.class }, id);
        }
    }
}

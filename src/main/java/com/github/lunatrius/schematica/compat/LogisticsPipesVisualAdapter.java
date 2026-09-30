package com.github.lunatrius.schematica.compat;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import com.github.lunatrius.schematica.api.ISchematicVisualAdapter;

final class LogisticsPipesVisualAdapter implements ISchematicVisualAdapter {
    private static final String[] STATES = {"renderState", "bcPlugableState", "pipe"};
    @Override public String id() { return "logisticspipes:pipe_state"; }
    @Override public String protocol() { return VisualAdapters.environmentProtocol(); }
    @Override public boolean supports(TileEntity tile) { return Reflect.is(tile, "logisticspipes.pipes.basic.LogisticsTileGenericPipe"); }

    @Override public NBTTagCompound capture(TileEntity tile) throws Exception {
        NBTTagCompound tag = new NBTTagCompound();
        for (String name : STATES) {
            Object state = Reflect.get(tile, name);
            if (state != null) tag.setByteArray(name,
                VisualStreams.writeBuffer(state, "writeData", "logisticspipes.network.LPDataOutputStream"));
        }
        return tag;
    }

    @Override public void restore(TileEntity tile, NBTTagCompound tag) throws Exception {
        for (String name : STATES) {
            Object state = Reflect.get(tile, name);
            if (state != null && tag.hasKey(name, 7)) VisualStreams.readBuffer(state, "readData",
                "logisticspipes.network.LPDataInputStream", tag.getByteArray(name));
        }
        Reflect.call(tile, "afterStateUpdated");
    }
}

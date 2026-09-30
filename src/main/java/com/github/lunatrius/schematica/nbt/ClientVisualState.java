package com.github.lunatrius.schematica.nbt;

import java.lang.reflect.Field;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import com.github.lunatrius.schematica.reference.Reference;

final class ClientVisualState {
    private static final ClassValue<Field> PIPE_CONNECTIONS = new ClassValue<Field>() {
        @Override protected Field computeValue(Class<?> type) {
            for (Class<?> parent = type; parent != null; parent = parent.getSuperclass()) {
                if (parent.getName().equals("gregtech.api.metatileentity.BaseMetaPipeEntity")) {
                    try {
                        return parent.getField("mConnections");
                    } catch (NoSuchFieldException e) {
                        Reference.logger.debug("GregTech pipe connection field is unavailable", e);
                    }
                }
            }
            return null;
        }
    };

    private ClientVisualState() {}

    static void capture(TileEntity tile, NBTTagCompound tag) {
        if (!tile.hasWorldObj() || !tile.getWorldObj().isRemote) return;
        capturePipeConnections(tile, tag);
    }

    static void capturePipeConnections(Object tile, NBTTagCompound tag) {
        Field connections = PIPE_CONNECTIONS.get(tile.getClass());
        if (connections == null) return;
        try {
            tag.setByte("mConnections", connections.getByte(tile));
        } catch (IllegalAccessException e) {
            Reference.logger.debug("Could not capture GregTech pipe connections", e);
        }
    }
}

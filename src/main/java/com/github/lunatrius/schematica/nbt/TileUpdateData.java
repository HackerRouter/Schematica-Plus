package com.github.lunatrius.schematica.nbt;

import java.io.IOException;
import java.lang.reflect.Field;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import cpw.mods.fml.relauncher.ReflectionHelper;

final class TileUpdateData {
    static final String KEY = "SchematicaVisualState";
    private static final Field PACKET_TYPE = ReflectionHelper.findField(S35PacketUpdateTileEntity.class, "field_148859_d");
    private static final Field PACKET_DATA = ReflectionHelper.findField(S35PacketUpdateTileEntity.class, "field_148860_e");

    private TileUpdateData() {}

    static NBTTagCompound combine(String tileClass, NBTTagCompound packet, NBTTagCompound adapters) {
        if (packet == null && adapters.hasNoTags()) return null;
        NBTTagCompound data = new NBTTagCompound();
        data.setInteger("Version", 2);
        data.setString("Class", tileClass);
        if (packet != null) data.setTag("Packet", packet);
        data.setTag("Adapters", adapters);
        return data;
    }

    static NBTTagCompound adapters(NBTTagCompound data, String tileClass) {
        return data != null && data.getInteger("Version") == 2 && data.getString("Class").equals(tileClass)
            ? data.getCompoundTag("Adapters") : new NBTTagCompound();
    }

    /**
     * Reads the update packet's fields rather than its bytes: modpacks change how the packet is serialized (GTNH 2.9
     * writes its NBT in another form than vanilla reads back). The payload is still bounded by its serialized size.
     */
    static NBTTagCompound capture(String tileClass, Packet packet) throws IOException {
        if (!(packet instanceof S35PacketUpdateTileEntity)) return null;
        int type;
        NBTTagCompound payload;
        try {
            type = PACKET_TYPE.getInt(packet);
            payload = (NBTTagCompound) PACKET_DATA.get(packet);
        } catch (IllegalAccessException e) {
            throw new IOException(e);
        }
        if (payload == null) return null;
        ByteBuf bound = Unpooled.buffer(256, Short.MAX_VALUE);
        try {
            CompressedStreamTools.write(payload, new ByteBufOutputStream(bound));
        } finally {
            bound.release();
        }
        NBTTagCompound data = new NBTTagCompound();
        data.setInteger("Version", 1);
        data.setString("Class", tileClass);
        data.setInteger("Type", type & 255);
        data.setTag("Data", payload.copy());
        return data;
    }

    static S35PacketUpdateTileEntity packet(NBTTagCompound data, String tileClass, int x, int y, int z) {
        return packet(data, tileClass, "1", x, y, z);
    }

    static S35PacketUpdateTileEntity packet(NBTTagCompound data, String tileClass, String protocol, int x, int y, int z) {
        if (data != null && data.getInteger("Version") == 2 && data.getString("Class").equals(tileClass)) {
            data = data.getCompoundTag("Packet");
        }
        if (data == null || data.getInteger("Version") != 1 || !data.getString("Class").equals(tileClass)
            || !data.hasKey("Type", 3) || !data.hasKey("Data", 10)) return null;
        if (data.hasKey("Protocol", 8) && !data.getString("Protocol").equals(protocol)) return null;
        int type = data.getInteger("Type");
        if (type < 0 || type > 255) return null;
        NBTTagCompound payload = (NBTTagCompound) data.getCompoundTag("Data").copy();
        if (payload.hasKey("x", 99)) payload.setInteger("x", x);
        if (payload.hasKey("y", 99)) payload.setInteger("y", y);
        if (payload.hasKey("z", 99)) payload.setInteger("z", z);
        return new S35PacketUpdateTileEntity(x, y, z, type, payload);
    }
}

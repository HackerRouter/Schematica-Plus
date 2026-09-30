package com.github.lunatrius.schematica.nbt;

import java.io.IOException;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;

final class TileUpdateData {
    static final String KEY = "SchematicaVisualState";

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

    static NBTTagCompound capture(String tileClass, Packet packet) throws IOException {
        if (!(packet instanceof S35PacketUpdateTileEntity)) return null;
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer(256, Short.MAX_VALUE + 13));
        try {
            packet.writePacketData(buffer);
            buffer.skipBytes(10);
            int type = buffer.readUnsignedByte();
            NBTTagCompound payload = buffer.readNBTTagCompoundFromBuffer();
            if (payload == null || buffer.isReadable()) return null;
            NBTTagCompound data = new NBTTagCompound();
            data.setInteger("Version", 1);
            data.setString("Class", tileClass);
            data.setInteger("Type", type);
            data.setTag("Data", payload);
            return data;
        } finally {
            buffer.release();
        }
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

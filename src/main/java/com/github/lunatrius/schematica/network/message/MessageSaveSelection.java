package com.github.lunatrius.schematica.network.message;

import java.util.ArrayList;
import java.util.List;

import com.github.lunatrius.schematica.api.SchematicOrigin;
import com.github.lunatrius.schematica.api.SchematicRegion;
import com.github.lunatrius.schematica.handler.SelectionSaves;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public final class MessageSaveSelection implements IMessage, IMessageHandler<MessageSaveSelection, IMessage> {
    public String name;
    public SchematicOrigin origin;
    public List<SchematicRegion> regions = new ArrayList<>();

    public MessageSaveSelection() {}

    public MessageSaveSelection(String name, List<SchematicRegion> regions, SchematicOrigin origin) {
        this.name = name; this.regions = new ArrayList<>(regions); this.origin = origin;
    }

    @Override public void fromBytes(ByteBuf buffer) {
        name = ByteBufUtils.readUTF8String(buffer);
        origin = new SchematicOrigin(buffer.readInt(), buffer.readInt(), buffer.readInt());
        int count = buffer.readUnsignedShort();
        if (count > 256) throw new IllegalArgumentException("Invalid selection");
        regions = new ArrayList<>();
        for (int i = 0; i < count; i++) regions.add(new SchematicRegion(ByteBufUtils.readUTF8String(buffer),
            buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt()));
        if (buffer.isReadable()) throw new IllegalArgumentException("Invalid selection");
    }

    @Override public void toBytes(ByteBuf buffer) {
        ByteBufUtils.writeUTF8String(buffer, name);
        buffer.writeInt(origin.x).writeInt(origin.y).writeInt(origin.z).writeShort(regions.size());
        for (SchematicRegion region : regions) {
            ByteBufUtils.writeUTF8String(buffer, region.name);
            buffer.writeInt(region.minX).writeInt(region.minY).writeInt(region.minZ);
            buffer.writeInt(region.maxX).writeInt(region.maxY).writeInt(region.maxZ);
        }
    }

    @Override public IMessage onMessage(MessageSaveSelection message, MessageContext context) {
        SelectionSaves.INSTANCE.received(context.getServerHandler().playerEntity, message);
        return null;
    }
}

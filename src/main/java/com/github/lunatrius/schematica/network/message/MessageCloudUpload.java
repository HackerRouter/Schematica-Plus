package com.github.lunatrius.schematica.network.message;

import com.github.lunatrius.schematica.handler.ServerCloud;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public final class MessageCloudUpload implements IMessage, IMessageHandler<MessageCloudUpload, IMessage> {
    public static final int CHUNK_SIZE = 30000;
    public int id, index, count;
    public String path;
    public byte[] data;

    public MessageCloudUpload() {}

    public MessageCloudUpload(int id, int index, int count, String path, byte[] data) {
        this.id = id; this.index = index; this.count = count; this.path = path; this.data = data;
    }

    @Override public void fromBytes(ByteBuf buffer) {
        id = buffer.readInt();
        index = buffer.readInt();
        count = buffer.readInt();
        path = ByteBufUtils.readUTF8String(buffer);
        int length = buffer.readInt();
        if (length < 0 || length > CHUNK_SIZE || length != buffer.readableBytes() || index < 0 || index >= count) {
            throw new IllegalArgumentException("Invalid upload slice");
        }
        data = new byte[length];
        buffer.readBytes(data);
    }

    @Override public void toBytes(ByteBuf buffer) {
        buffer.writeInt(id).writeInt(index).writeInt(count);
        ByteBufUtils.writeUTF8String(buffer, path);
        buffer.writeInt(data.length).writeBytes(data);
    }

    @Override public IMessage onMessage(MessageCloudUpload message, MessageContext context) {
        ServerCloud.INSTANCE.upload(context.getServerHandler().playerEntity, message);
        return null;
    }
}

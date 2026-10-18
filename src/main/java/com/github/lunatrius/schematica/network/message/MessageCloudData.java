package com.github.lunatrius.schematica.network.message;

import net.minecraft.client.Minecraft;

import com.github.lunatrius.schematica.handler.client.CloudClient;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public final class MessageCloudData implements IMessage, IMessageHandler<MessageCloudData, IMessage> {
    public static final int CHUNK_SIZE = 30000;
    public int id, index, count;
    public byte[] data;

    public MessageCloudData() {}

    public MessageCloudData(int id, int index, int count, byte[] data) {
        this.id = id; this.index = index; this.count = count; this.data = data;
    }

    @Override public void fromBytes(ByteBuf buffer) {
        id = buffer.readInt();
        index = buffer.readInt();
        count = buffer.readInt();
        int length = buffer.readInt();
        if (length < 0 || length > CHUNK_SIZE || length != buffer.readableBytes() || count < 0 || count > 0 && (index < 0 || index >= count)) {
            throw new IllegalArgumentException("Invalid cloud slice");
        }
        data = new byte[length];
        buffer.readBytes(data);
    }

    @Override public void toBytes(ByteBuf buffer) {
        buffer.writeInt(id).writeInt(index).writeInt(count).writeInt(data.length).writeBytes(data);
    }

    @Override public IMessage onMessage(MessageCloudData message, MessageContext context) {
        Minecraft.getMinecraft().func_152344_a(() -> CloudClient.INSTANCE.received(message));
        return null;
    }
}

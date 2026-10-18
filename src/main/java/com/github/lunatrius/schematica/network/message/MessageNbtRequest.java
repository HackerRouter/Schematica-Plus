package com.github.lunatrius.schematica.network.message;

import com.github.lunatrius.schematica.handler.NbtRequests;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public final class MessageNbtRequest implements IMessage, IMessageHandler<MessageNbtRequest, IMessage> {
    public int id, chunkX, chunkZ, minY, maxY;

    public MessageNbtRequest() {}

    public MessageNbtRequest(int id, int chunkX, int chunkZ, int minY, int maxY) {
        this.id = id; this.chunkX = chunkX; this.chunkZ = chunkZ; this.minY = minY; this.maxY = maxY;
    }

    @Override public void fromBytes(ByteBuf buffer) {
        id = buffer.readInt(); chunkX = buffer.readInt(); chunkZ = buffer.readInt(); minY = buffer.readInt(); maxY = buffer.readInt();
        if (minY < 0 || maxY > 255 || minY > maxY || buffer.isReadable()) throw new IllegalArgumentException("Invalid data request");
    }

    @Override public void toBytes(ByteBuf buffer) {
        buffer.writeInt(id).writeInt(chunkX).writeInt(chunkZ).writeInt(minY).writeInt(maxY);
    }

    @Override public IMessage onMessage(MessageNbtRequest message, MessageContext context) {
        NbtRequests.INSTANCE.enqueue(context.getServerHandler().playerEntity, message);
        return null;
    }
}

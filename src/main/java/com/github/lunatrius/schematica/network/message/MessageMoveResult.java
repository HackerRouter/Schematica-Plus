package com.github.lunatrius.schematica.network.message;

import com.github.lunatrius.schematica.SchematicaPlus;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public final class MessageMoveResult implements IMessage, IMessageHandler<MessageMoveResult, IMessage> {
    public long id;
    public boolean success;
    public MessageMoveResult() {}
    public MessageMoveResult(long id, boolean success) { this.id = id; this.success = success; }
    @Override public void fromBytes(ByteBuf buffer) { id = buffer.readLong(); success = buffer.readBoolean(); }
    @Override public void toBytes(ByteBuf buffer) { buffer.writeLong(id); buffer.writeBoolean(success); }
    @Override public IMessage onMessage(MessageMoveResult message, MessageContext context) {
        SchematicaPlus.proxy.worldMoveFinished(message.id, message.success);
        return null;
    }
}

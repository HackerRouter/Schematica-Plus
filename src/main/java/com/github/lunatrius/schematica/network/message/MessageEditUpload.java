// SPDX-License-Identifier: LGPL-3.0-only
// Remote edit upload, the Plus counterpart of Litematica's Servux paste transfer, by HackerRouter, 2026.
package com.github.lunatrius.schematica.network.message;

import com.github.lunatrius.schematica.handler.RemoteEdits;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** One slice of a compressed edit, sent from client to server. */
public final class MessageEditUpload implements IMessage, IMessageHandler<MessageEditUpload, IMessage> {
    public static final int CHUNK_SIZE = 30000;
    public long id;
    public int index, count, dimension;
    public byte[] data;

    public MessageEditUpload() {}

    public MessageEditUpload(long id, int dimension, int index, int count, byte[] data) {
        this.id = id; this.dimension = dimension; this.index = index; this.count = count; this.data = data;
    }

    @Override public void fromBytes(ByteBuf buffer) {
        id = buffer.readLong(); dimension = buffer.readInt(); index = buffer.readInt(); count = buffer.readInt();
        int length = buffer.readInt();
        if (length < 0 || length > CHUNK_SIZE || length != buffer.readableBytes()) throw new IllegalArgumentException("Invalid edit chunk");
        data = new byte[length];
        buffer.readBytes(data);
    }

    @Override public void toBytes(ByteBuf buffer) {
        buffer.writeLong(id); buffer.writeInt(dimension); buffer.writeInt(index); buffer.writeInt(count);
        buffer.writeInt(data.length);
        buffer.writeBytes(data);
    }

    @Override public IMessage onMessage(MessageEditUpload message, MessageContext context) {
        RemoteEdits.INSTANCE.receive(context.getServerHandler().playerEntity, message);
        return null;
    }
}

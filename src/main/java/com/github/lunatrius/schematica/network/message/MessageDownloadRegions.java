// SPDX-License-Identifier: LGPL-3.0-only
// Independent region contents of a download, by HackerRouter, 2026.
package com.github.lunatrius.schematica.network.message;

import com.github.lunatrius.schematica.handler.DownloadHandler;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** Server to client, after the block chunks: one slice of the compressed region payload of a multi-region schematic. */
public final class MessageDownloadRegions implements IMessage, IMessageHandler<MessageDownloadRegions, IMessage> {
    public static final int CHUNK_SIZE = 30000;
    public int index, count;
    public byte[] data;

    public MessageDownloadRegions() {}

    public MessageDownloadRegions(int index, int count, byte[] data) {
        this.index = index; this.count = count; this.data = data;
    }

    @Override public void fromBytes(ByteBuf buffer) {
        index = buffer.readInt();
        count = buffer.readInt();
        int length = buffer.readInt();
        if (length < 0 || length > CHUNK_SIZE || length != buffer.readableBytes() || index < 0 || index >= count) {
            throw new IllegalArgumentException("Invalid region slice");
        }
        data = new byte[length];
        buffer.readBytes(data);
    }

    @Override public void toBytes(ByteBuf buffer) {
        buffer.writeInt(index).writeInt(count).writeInt(data.length);
        buffer.writeBytes(data);
    }

    @Override public IMessage onMessage(MessageDownloadRegions message, MessageContext context) {
        DownloadHandler.INSTANCE.receivedRegions(message);
        return null;
    }
}

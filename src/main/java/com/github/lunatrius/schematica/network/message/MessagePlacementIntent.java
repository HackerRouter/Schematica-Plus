// SPDX-License-Identifier: LGPL-3.0-only
// Accurate placement intent, the Plus counterpart of Litematica's easyPlaceProtocol v3, by HackerRouter, 2026.
package com.github.lunatrius.schematica.network.message;

import java.nio.charset.StandardCharsets;

import com.github.lunatrius.schematica.handler.AccuratePlacement;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** Client to server, just before a placement click: the block and metadata the next placement at x, y, z should end with. */
public final class MessagePlacementIntent implements IMessage, IMessageHandler<MessagePlacementIntent, IMessage> {
    public int x, y, z, metadata;
    public String block = "";

    public MessagePlacementIntent() {}

    public MessagePlacementIntent(int x, int y, int z, String block, int metadata) {
        this.x = x; this.y = y; this.z = z; this.block = block; this.metadata = metadata;
    }

    @Override public void fromBytes(ByteBuf buffer) {
        x = buffer.readInt(); y = buffer.readInt(); z = buffer.readInt(); metadata = buffer.readUnsignedByte();
        int length = buffer.readUnsignedShort();
        if (length > 256 || length != buffer.readableBytes() || metadata > 15) throw new IllegalArgumentException("Invalid placement intent");
        byte[] name = new byte[length];
        buffer.readBytes(name);
        block = new String(name, StandardCharsets.UTF_8);
    }

    @Override public void toBytes(ByteBuf buffer) {
        buffer.writeInt(x); buffer.writeInt(y); buffer.writeInt(z); buffer.writeByte(metadata);
        byte[] name = block.getBytes(StandardCharsets.UTF_8);
        buffer.writeShort(name.length);
        buffer.writeBytes(name);
    }

    @Override public IMessage onMessage(MessagePlacementIntent message, MessageContext context) {
        AccuratePlacement.INSTANCE.intent(context.getServerHandler().playerEntity, message);
        return null;
    }
}

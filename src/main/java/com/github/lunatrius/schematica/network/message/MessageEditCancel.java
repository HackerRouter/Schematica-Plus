// SPDX-License-Identifier: LGPL-3.0-only
// Remote edit cancellation, by HackerRouter, 2026.
package com.github.lunatrius.schematica.network.message;

import com.github.lunatrius.schematica.handler.RemoteEdits;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public final class MessageEditCancel implements IMessage, IMessageHandler<MessageEditCancel, IMessage> {
    public long id;

    public MessageEditCancel() {}
    public MessageEditCancel(long id) { this.id = id; }

    @Override public void fromBytes(ByteBuf buffer) { id = buffer.readLong(); }
    @Override public void toBytes(ByteBuf buffer) { buffer.writeLong(id); }

    @Override public IMessage onMessage(MessageEditCancel message, MessageContext context) {
        RemoteEdits.INSTANCE.cancel(context.getServerHandler().playerEntity, message.id);
        return null;
    }
}

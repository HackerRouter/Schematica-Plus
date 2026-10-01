// SPDX-License-Identifier: LGPL-3.0-only
// Remote edit progress and result, by HackerRouter, 2026.
package com.github.lunatrius.schematica.network.message;

import java.nio.charset.StandardCharsets;

import com.github.lunatrius.schematica.SchematicaPlus;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** Server to client: an upload was accepted, refused, progressed or finished. */
public final class MessageEditStatus implements IMessage, IMessageHandler<MessageEditStatus, IMessage> {
    public enum State { ACCEPTED, REJECTED, PROGRESS, FINISHED, FAILED }

    public long id;
    public State state = State.REJECTED;
    public int stage;
    public long completed, total, affected, entities;
    public String reason = "";

    public MessageEditStatus() {}

    public MessageEditStatus(long id, State state) { this.id = id; this.state = state; }

    public static MessageEditStatus rejected(long id, String reason) {
        MessageEditStatus status = new MessageEditStatus(id, State.REJECTED);
        status.reason = reason;
        return status;
    }

    @Override public void fromBytes(ByteBuf buffer) {
        id = buffer.readLong();
        int ordinal = buffer.readUnsignedByte();
        if (ordinal >= State.values().length) throw new IllegalArgumentException("Invalid edit state");
        state = State.values()[ordinal];
        stage = buffer.readUnsignedByte();
        completed = buffer.readLong(); total = buffer.readLong(); affected = buffer.readLong(); entities = buffer.readLong();
        int length = buffer.readUnsignedShort();
        if (length > 256 || length > buffer.readableBytes()) throw new IllegalArgumentException("Invalid edit reason");
        byte[] text = new byte[length];
        buffer.readBytes(text);
        reason = new String(text, StandardCharsets.UTF_8);
    }

    @Override public void toBytes(ByteBuf buffer) {
        buffer.writeLong(id);
        buffer.writeByte(state.ordinal());
        buffer.writeByte(stage);
        buffer.writeLong(completed); buffer.writeLong(total); buffer.writeLong(affected); buffer.writeLong(entities);
        byte[] text = reason.getBytes(StandardCharsets.UTF_8);
        buffer.writeShort(text.length);
        buffer.writeBytes(text);
    }

    @Override public IMessage onMessage(MessageEditStatus message, MessageContext context) {
        SchematicaPlus.proxy.remoteEditStatus(message);
        return null;
    }
}

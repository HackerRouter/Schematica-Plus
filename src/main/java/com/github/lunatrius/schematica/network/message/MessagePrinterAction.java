// SPDX-License-Identifier: LGPL-3.0-only
package com.github.lunatrius.schematica.network.message;

import com.github.lunatrius.schematica.handler.PrinterActions;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** A printer click uses the server's held item, never client-supplied item data. */
public final class MessagePrinterAction implements IMessage, IMessageHandler<MessagePrinterAction, IMessage> {
    public static final int PLACE = 3;
    public int action, x, y, z, side, slot;
    public float hitX, hitY, hitZ, yaw, pitch;
    public boolean sneaking;

    public MessagePrinterAction() {}

    public boolean valid() {
        return action >= 0 && action <= PLACE && slot >= 0 && slot < 9 && side >= (action == 1 ? -1 : 0) && side <= 5
            && y >= 0 && y <= 255 && Math.abs((long) x) < 30_000_000 && Math.abs((long) z) < 30_000_000
            && Float.isFinite(yaw) && Float.isFinite(pitch) && pitch >= -90 && pitch <= 90
            && fraction(hitX) && fraction(hitY) && fraction(hitZ);
    }

    private static boolean fraction(float value) { return Float.isFinite(value) && value >= 0 && value <= 1; }

    @Override public void fromBytes(ByteBuf buffer) {
        action = buffer.readUnsignedByte(); x = buffer.readInt(); y = buffer.readInt(); z = buffer.readInt();
        side = buffer.readByte(); slot = buffer.readUnsignedByte();
        hitX = buffer.readFloat(); hitY = buffer.readFloat(); hitZ = buffer.readFloat();
        yaw = buffer.readFloat(); pitch = buffer.readFloat(); sneaking = buffer.readBoolean();
        if (buffer.isReadable() || !valid()) throw new IllegalArgumentException("Invalid printer action");
    }

    @Override public void toBytes(ByteBuf buffer) {
        buffer.writeByte(action); buffer.writeInt(x); buffer.writeInt(y); buffer.writeInt(z);
        buffer.writeByte(side); buffer.writeByte(slot);
        buffer.writeFloat(hitX); buffer.writeFloat(hitY); buffer.writeFloat(hitZ);
        buffer.writeFloat(yaw); buffer.writeFloat(pitch); buffer.writeBoolean(sneaking);
    }

    @Override public IMessage onMessage(MessagePrinterAction message, MessageContext context) {
        PrinterActions.INSTANCE.enqueue(context.getServerHandler().playerEntity, message);
        return null;
    }
}

package com.github.lunatrius.schematica.network.message;

import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;

import com.github.lunatrius.schematica.handler.SharedPlacements;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public final class MessageSharedRequest implements IMessage, IMessageHandler<MessageSharedRequest, IMessage> {
    public enum Operation { SHARE, REMOVE, MODIFY, FINISH, CANCEL }

    public Operation operation;
    public UUID id;
    public NBTTagCompound placement;

    public MessageSharedRequest() {}

    public MessageSharedRequest(Operation operation, UUID id, NBTTagCompound placement) {
        this.operation = operation; this.id = id; this.placement = placement == null ? new NBTTagCompound() : placement;
    }

    @Override public void fromBytes(ByteBuf buffer) {
        int ordinal = buffer.readUnsignedByte();
        if (ordinal >= Operation.values().length) throw new IllegalArgumentException("Invalid shared placement request");
        operation = Operation.values()[ordinal];
        id = new UUID(buffer.readLong(), buffer.readLong());
        placement = ByteBufUtils.readTag(buffer);
        if (placement == null) placement = new NBTTagCompound();
    }

    @Override public void toBytes(ByteBuf buffer) {
        buffer.writeByte(operation.ordinal()).writeLong(id.getMostSignificantBits()).writeLong(id.getLeastSignificantBits());
        ByteBufUtils.writeTag(buffer, placement);
    }

    @Override public IMessage onMessage(MessageSharedRequest message, MessageContext context) {
        SharedPlacements.INSTANCE.request(context.getServerHandler().playerEntity, message);
        return null;
    }
}

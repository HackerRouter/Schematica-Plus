package com.github.lunatrius.schematica.network.message;

import com.github.lunatrius.schematica.handler.ServerCloud;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public final class MessageCloudRequest implements IMessage, IMessageHandler<MessageCloudRequest, IMessage> {
    public enum Operation { CATALOG, DOWNLOAD, CREATE_DIRECTORY, DELETE, PUBLISH }

    public int id;
    public Operation operation;
    public String path, target;

    public MessageCloudRequest() {}

    public MessageCloudRequest(int id, Operation operation, String path, String target) {
        this.id = id; this.operation = operation; this.path = path; this.target = target;
    }

    @Override public void fromBytes(ByteBuf buffer) {
        id = buffer.readInt();
        int ordinal = buffer.readUnsignedByte();
        if (ordinal >= Operation.values().length) throw new IllegalArgumentException("Invalid cloud request");
        operation = Operation.values()[ordinal];
        path = ByteBufUtils.readUTF8String(buffer);
        target = ByteBufUtils.readUTF8String(buffer);
        if (buffer.isReadable()) throw new IllegalArgumentException("Invalid cloud request");
    }

    @Override public void toBytes(ByteBuf buffer) {
        buffer.writeInt(id).writeByte(operation.ordinal());
        ByteBufUtils.writeUTF8String(buffer, path);
        ByteBufUtils.writeUTF8String(buffer, target);
    }

    @Override public IMessage onMessage(MessageCloudRequest message, MessageContext context) {
        ServerCloud.INSTANCE.request(context.getServerHandler().playerEntity, message);
        return null;
    }
}

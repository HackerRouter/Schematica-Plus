package com.github.lunatrius.schematica.network.message;

import net.minecraft.client.Minecraft;

import com.github.lunatrius.schematica.handler.client.CloudClient;
import com.github.lunatrius.schematica.util.MessageType;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public final class MessageCloudResult implements IMessage, IMessageHandler<MessageCloudResult, IMessage> {
    public static final int CHANGED = -1, NOTICE = -2;
    public int id;
    public MessageType type;
    public String key;
    public String[] arguments;

    public MessageCloudResult() {}

    public MessageCloudResult(int id, MessageType type, String key, String... arguments) {
        this.id = id; this.type = type; this.key = key; this.arguments = arguments;
    }

    public static MessageCloudResult changed() { return new MessageCloudResult(CHANGED, MessageType.INFO, ""); }

    @Override public void fromBytes(ByteBuf buffer) {
        id = buffer.readInt();
        int ordinal = buffer.readUnsignedByte();
        if (ordinal >= MessageType.values().length) throw new IllegalArgumentException("Invalid cloud result");
        type = MessageType.values()[ordinal];
        key = ByteBufUtils.readUTF8String(buffer);
        int count = buffer.readUnsignedByte();
        if (count > 8) throw new IllegalArgumentException("Invalid cloud result");
        arguments = new String[count];
        for (int i = 0; i < count; i++) arguments[i] = ByteBufUtils.readUTF8String(buffer);
    }

    @Override public void toBytes(ByteBuf buffer) {
        buffer.writeInt(id).writeByte(type.ordinal());
        ByteBufUtils.writeUTF8String(buffer, key);
        buffer.writeByte(arguments.length);
        for (String argument : arguments) ByteBufUtils.writeUTF8String(buffer, argument);
    }

    @Override public IMessage onMessage(MessageCloudResult message, MessageContext context) {
        Minecraft.getMinecraft().func_152344_a(() -> CloudClient.INSTANCE.result(message));
        return null;
    }
}

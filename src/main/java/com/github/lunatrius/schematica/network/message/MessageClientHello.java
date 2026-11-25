package com.github.lunatrius.schematica.network.message;

import com.github.lunatrius.schematica.handler.PlayerHandler;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public final class MessageClientHello implements IMessage, IMessageHandler<MessageClientHello, IMessage> {

    public String version;

    public MessageClientHello() {}

    public MessageClientHello(String version) {
        this.version = version;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        version = ByteBufUtils.readUTF8String(buf);
        if (version.length() > 128) version = version.substring(0, 128);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, version == null ? "" : version);
    }

    @Override
    public IMessage onMessage(MessageClientHello message, MessageContext ctx) {
        PlayerHandler.INSTANCE.onClientHello(ctx.getServerHandler().playerEntity, message.version);
        return null;
    }
}

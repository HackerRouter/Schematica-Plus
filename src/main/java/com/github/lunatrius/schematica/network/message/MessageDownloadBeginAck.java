package com.github.lunatrius.schematica.network.message;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.StatCollector;

import com.github.lunatrius.schematica.handler.DownloadHandler;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class MessageDownloadBeginAck implements IMessage, IMessageHandler<MessageDownloadBeginAck, IMessage> {

    public boolean supportsGeometry = true;
    public boolean supportsRegions = true;

    @Override
    public void fromBytes(ByteBuf buf) {
        this.supportsGeometry = buf.isReadable() && buf.readBoolean();
        this.supportsRegions = buf.isReadable() && buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(this.supportsGeometry);
        buf.writeBoolean(this.supportsRegions);
    }

    @Override
    public IMessage onMessage(MessageDownloadBeginAck message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        if (!DownloadHandler.INSTANCE.acceptBegin(player, message.supportsGeometry, message.supportsRegions)) {
            String key = "schematica.message.download.update_client";
            player.addChatMessage(new ChatComponentText(StatCollector.canTranslate(key) ? StatCollector.translateToLocal(key)
                : "Schematica Plus: update the client to download schematics with subregions or custom origins."));
        }

        return null;
    }
}

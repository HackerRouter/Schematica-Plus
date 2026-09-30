package com.github.lunatrius.schematica.network.message;

import java.io.File;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentTranslation;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.handler.DownloadHandler;
import com.github.lunatrius.schematica.reference.Names;
import com.github.lunatrius.schematica.world.schematic.SchematicFormat;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class MessageDownloadEnd implements IMessage, IMessageHandler<MessageDownloadEnd, IMessage> {

    public String name;

    public MessageDownloadEnd() {}

    public MessageDownloadEnd(String name) {
        this.name = name;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.name = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.name);
    }

    @Override
    public IMessage onMessage(MessageDownloadEnd message, MessageContext ctx) {
        File directory = SchematicaPlus.proxy.getPlayerSchematicDirectory(null, true);
        File saved = DownloadHandler.INSTANCE.isDownloadComplete() ? SchematicFormat
            .saveToFile(directory, message.name, DownloadHandler.INSTANCE.schematic, null, true, true) : null;

        if (Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(
                new ChatComponentTranslation(saved != null ? Names.Command.Download.Message.DOWNLOAD_SUCCEEDED
                    : Names.Command.Download.Message.DOWNLOAD_FAILED, saved != null ? saved.getName() : message.name));
        }

        DownloadHandler.INSTANCE.beginDownload(null);

        return null;
    }
}

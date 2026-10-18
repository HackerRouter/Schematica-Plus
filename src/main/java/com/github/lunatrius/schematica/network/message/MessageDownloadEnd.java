package com.github.lunatrius.schematica.network.message;

import java.io.File;
import java.io.IOException;

import net.minecraft.client.Minecraft;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.client.util.InfoUtils;
import com.github.lunatrius.schematica.handler.DownloadHandler;
import com.github.lunatrius.schematica.reference.Names;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.util.FileUtils;
import com.github.lunatrius.schematica.util.MessageType;
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
        Minecraft.getMinecraft().func_152344_a(() -> finish(message));
        return null;
    }

    private static void finish(MessageDownloadEnd message) {
        File directory = SchematicaPlus.proxy.getPlayerSchematicDirectory(null, true);
        File saved = null;
        try {
            ISchematic downloaded = DownloadHandler.INSTANCE.completedSchematic();
            if (downloaded != null) saved = SchematicFormat.saveToFile(
                FileUtils.resolveSchematicFile(directory, message.name), downloaded, null, true, true,
                Minecraft.getMinecraft().thePlayer != null ? Minecraft.getMinecraft().thePlayer.getCommandSenderName() : "");
        } catch (IOException | RuntimeException e) {
            Reference.logger.warn("Could not restore the downloaded regions", e);
        }

        if (saved != null) InfoUtils.sendOpenFileChatMessage(Minecraft.getMinecraft().thePlayer, Names.Command.Download.Message.DOWNLOAD_SUCCEEDED, saved);
        else InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, Names.Command.Download.Message.DOWNLOAD_FAILED, message.name);

        DownloadHandler.INSTANCE.beginDownload(null);
    }
}

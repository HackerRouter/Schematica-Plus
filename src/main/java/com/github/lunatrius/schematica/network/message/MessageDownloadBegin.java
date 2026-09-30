package com.github.lunatrius.schematica.network.message;

import net.minecraft.item.ItemStack;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.handler.DownloadHandler;
import com.github.lunatrius.schematica.network.transfer.DownloadGeometry;
import com.github.lunatrius.schematica.world.storage.Schematic;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class MessageDownloadBegin implements IMessage, IMessageHandler<MessageDownloadBegin, IMessage> {

    public ItemStack icon;
    public int width;
    public int height;
    public int length;
    public DownloadGeometry geometry = DownloadGeometry.LEGACY;
    private boolean invalid;

    public MessageDownloadBegin() {}

    public MessageDownloadBegin(ISchematic schematic) {
        this.icon = schematic.getIcon();
        this.width = schematic.getWidth();
        this.height = schematic.getHeight();
        this.length = schematic.getLength();
        this.geometry = DownloadGeometry.of(schematic);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.invalid = false;
        try {
            this.icon = ByteBufUtils.readItemStack(buf);
            this.width = buf.readShort();
            this.height = buf.readShort();
            this.length = buf.readShort();
            this.geometry = DownloadGeometry.read(buf);
            this.geometry.validate(this.width, this.height, this.length);
        } catch (RuntimeException e) {
            this.invalid = true;
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeItemStack(buf, this.icon);
        buf.writeShort(this.width);
        buf.writeShort(this.height);
        buf.writeShort(this.length);
        this.geometry.write(buf);
    }

    @Override
    public IMessage onMessage(MessageDownloadBegin message, MessageContext ctx) {
        DownloadHandler.INSTANCE.beginDownload(null);
        try {
            if (message.invalid) throw new IllegalArgumentException("Malformed download header");
            message.geometry.validate(message.width, message.height, message.length);
            Schematic schematic = new Schematic(message.icon, message.width, message.height, message.length);
            schematic.setRegions(message.geometry.regions);
            schematic.setOrigin(message.geometry.origin);
            DownloadHandler.INSTANCE.beginDownload(schematic);
        } catch (IllegalArgumentException e) {
            com.github.lunatrius.schematica.reference.Reference.logger.warn("Rejected download geometry", e);
            return null;
        }

        return new MessageDownloadBeginAck();
    }
}

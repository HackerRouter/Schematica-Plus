package com.github.lunatrius.schematica.network.message;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.client.printer.SchematicPrinter;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Reference;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class MessageCapabilities implements IMessage, IMessageHandler<MessageCapabilities, IMessage> {

    public boolean isPrinterEnabled;
    public boolean isSaveEnabled;
    public boolean isLoadEnabled;
    public boolean supportsWorldMove = true;
    public boolean supportsRemoteEdit, supportsAccuratePlacement;

    public MessageCapabilities() {
        this(false, false, false);
    }

    public MessageCapabilities(boolean isPrinterEnabled, boolean isSaveEnabled, boolean isLoadEnabled) {
        this.isPrinterEnabled = isPrinterEnabled;
        this.isSaveEnabled = isSaveEnabled;
        this.isLoadEnabled = isLoadEnabled;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.isPrinterEnabled = buf.readBoolean();
        this.isSaveEnabled = buf.readBoolean();
        this.isLoadEnabled = buf.readBoolean();
        supportsWorldMove = buf.isReadable() && buf.readBoolean();
        supportsRemoteEdit = buf.isReadable() && buf.readBoolean();
        supportsAccuratePlacement = buf.isReadable() && buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(this.isPrinterEnabled);
        buf.writeBoolean(this.isSaveEnabled);
        buf.writeBoolean(this.isLoadEnabled);
        buf.writeBoolean(supportsWorldMove);
        buf.writeBoolean(supportsRemoteEdit);
        buf.writeBoolean(supportsAccuratePlacement);
    }

    @Override
    public IMessage onMessage(MessageCapabilities message, MessageContext ctx) {
        if (ClientProxy.isPendingReset) {
            SchematicaPlus.proxy.resetSettings();
            ClientProxy.isPendingReset = false;
        }

        SchematicPrinter.INSTANCE.setEnabled(message.isPrinterEnabled);
        SchematicaPlus.proxy.isSaveEnabled = message.isSaveEnabled;
        SchematicaPlus.proxy.isLoadEnabled = message.isLoadEnabled;
        SchematicaPlus.proxy.supportsWorldMove = message.supportsWorldMove;
        SchematicaPlus.proxy.supportsRemoteEdit = message.supportsRemoteEdit;
        SchematicaPlus.proxy.supportsAccuratePlacement = message.supportsAccuratePlacement;

        Reference.logger.info(
            "Server capabilities{printer={}, save={}, load={}, remoteEdit={}, accuratePlacement={}}",
            message.isPrinterEnabled,
            message.isSaveEnabled,
            message.isLoadEnabled,
            message.supportsRemoteEdit,
            message.supportsAccuratePlacement);

        return null;
    }
}

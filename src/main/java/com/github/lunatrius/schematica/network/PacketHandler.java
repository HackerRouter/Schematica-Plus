package com.github.lunatrius.schematica.network;

import com.github.lunatrius.schematica.network.message.MessageCapabilities;
import com.github.lunatrius.schematica.network.message.MessageDownloadBegin;
import com.github.lunatrius.schematica.network.message.MessageDownloadBeginAck;
import com.github.lunatrius.schematica.network.message.MessageDownloadChunk;
import com.github.lunatrius.schematica.network.message.MessageDownloadChunkAck;
import com.github.lunatrius.schematica.network.message.MessageDownloadEnd;
import com.github.lunatrius.schematica.network.message.MessageDownloadRegions;
import com.github.lunatrius.schematica.network.message.MessageEditCancel;
import com.github.lunatrius.schematica.network.message.MessageEditStatus;
import com.github.lunatrius.schematica.network.message.MessageEditUpload;
import com.github.lunatrius.schematica.network.message.MessageMoveRequest;
import com.github.lunatrius.schematica.network.message.MessageMoveResult;
import com.github.lunatrius.schematica.network.message.MessagePlacementIntent;
import com.github.lunatrius.schematica.network.message.MessagePrinterAction;
import com.github.lunatrius.schematica.reference.Reference;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public class PacketHandler {

    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE
        .newSimpleChannel(Reference.MODID.toLowerCase());

    public static void init() {
        INSTANCE.registerMessage(MessagePrinterAction.class,
            MessagePrinterAction.class, 13, Side.SERVER);
        INSTANCE.registerMessage(MessageCapabilities.class, MessageCapabilities.class, 0, Side.CLIENT);

        INSTANCE.registerMessage(MessageDownloadBegin.class, MessageDownloadBegin.class, 1, Side.CLIENT);
        INSTANCE.registerMessage(MessageDownloadBeginAck.class, MessageDownloadBeginAck.class, 2, Side.SERVER);
        INSTANCE.registerMessage(MessageDownloadChunk.class, MessageDownloadChunk.class, 3, Side.CLIENT);
        INSTANCE.registerMessage(MessageDownloadChunkAck.class, MessageDownloadChunkAck.class, 4, Side.SERVER);
        INSTANCE.registerMessage(MessageDownloadEnd.class, MessageDownloadEnd.class, 5, Side.CLIENT);
        INSTANCE.registerMessage(MessageMoveRequest.class,
            MessageMoveRequest.class, 6, Side.SERVER);
        INSTANCE.registerMessage(MessageMoveResult.class,
            MessageMoveResult.class, 7, Side.CLIENT);
        INSTANCE.registerMessage(MessageEditUpload.class,
            MessageEditUpload.class, 8, Side.SERVER);
        INSTANCE.registerMessage(MessageEditCancel.class,
            MessageEditCancel.class, 9, Side.SERVER);
        INSTANCE.registerMessage(MessageEditStatus.class,
            MessageEditStatus.class, 10, Side.CLIENT);
        INSTANCE.registerMessage(MessagePlacementIntent.class,
            MessagePlacementIntent.class, 11, Side.SERVER);
        INSTANCE.registerMessage(MessageDownloadRegions.class,
            MessageDownloadRegions.class, 12, Side.CLIENT);
    }
}

package com.github.lunatrius.schematica.network.message;

import java.util.Collections;

import net.minecraft.client.Minecraft;

import com.github.lunatrius.schematica.api.SchematicOrigin;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Area;
import com.github.lunatrius.schematica.client.selection.AreaSelections;
import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;
import com.github.lunatrius.schematica.network.PacketHandler;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public final class MessageSaveSelectionRequest implements IMessage, IMessageHandler<MessageSaveSelectionRequest, IMessage> {
    public String name;

    public MessageSaveSelectionRequest() {}

    public MessageSaveSelectionRequest(String name) {
        this.name = name;
    }

    @Override public void fromBytes(ByteBuf buffer) {
        name = ByteBufUtils.readUTF8String(buffer);
    }

    @Override public void toBytes(ByteBuf buffer) {
        ByteBufUtils.writeUTF8String(buffer, name);
    }

    @Override public IMessage onMessage(MessageSaveSelectionRequest message, MessageContext context) {
        Minecraft.getMinecraft().func_152344_a(() -> PacketHandler.INSTANCE.sendToServer(reply(message.name)));
        return null;
    }

    private static MessageSaveSelection reply(String name) {
        AreaSelections.capture();
        Area area = AreaSelections.library().selected();
        if (area == null || area.boxes().isEmpty()) return new MessageSaveSelection(name, Collections.emptyList(), SchematicOrigin.ZERO);
        Vector3i origin = area.origin();
        return new MessageSaveSelection(name, area.regions(), new SchematicOrigin(origin.x, origin.y, origin.z));
    }
}

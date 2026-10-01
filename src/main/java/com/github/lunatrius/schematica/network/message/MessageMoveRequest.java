package com.github.lunatrius.schematica.network.message;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import com.github.lunatrius.schematica.api.SchematicRegion;
import com.github.lunatrius.schematica.handler.WorldEditQueue;
import com.github.lunatrius.schematica.network.PacketHandler;
import com.github.lunatrius.schematica.tool.WorldMoveJob;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public final class MessageMoveRequest implements IMessage, IMessageHandler<MessageMoveRequest, IMessage> {
    public long id;
    public int dimension, dx, dy, dz;
    public List<SchematicRegion> regions = new ArrayList<>();

    public MessageMoveRequest() {}
    public MessageMoveRequest(long id, int dimension, List<SchematicRegion> regions, int dx, int dy, int dz) {
        this.id = id; this.dimension = dimension; this.regions = new ArrayList<>(regions); this.dx = dx; this.dy = dy; this.dz = dz;
    }
    @Override public void fromBytes(ByteBuf buffer) {
        id = buffer.readLong(); dimension = buffer.readInt(); dx = buffer.readInt(); dy = buffer.readInt(); dz = buffer.readInt();
        int count = buffer.readUnsignedShort();
        if (count < 1 || count > 256 || buffer.readableBytes() != count * 24) throw new IllegalArgumentException("Invalid move request");
        regions = new ArrayList<>();
        for (int i = 0; i < count; i++) regions.add(new SchematicRegion(Integer.toString(i), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt()));
    }
    @Override public void toBytes(ByteBuf buffer) {
        buffer.writeLong(id); buffer.writeInt(dimension); buffer.writeInt(dx); buffer.writeInt(dy); buffer.writeInt(dz); buffer.writeShort(regions.size());
        for (SchematicRegion region : regions) {
            buffer.writeInt(region.minX); buffer.writeInt(region.minY); buffer.writeInt(region.minZ);
            buffer.writeInt(region.maxX); buffer.writeInt(region.maxY); buffer.writeInt(region.maxZ);
        }
    }
    @Override public IMessage onMessage(MessageMoveRequest message, MessageContext context) {
        EntityPlayerMP player = context.getServerHandler().playerEntity;
        boolean queued = WorldEditQueue.INSTANCE.enqueue(() -> {
            MinecraftServer server = MinecraftServer.getServer();
            boolean submitted = false;
            try {
                if (server != null && server.getConfigurationManager().playerEntityList.contains(player) && player.dimension == message.dimension
                    && player.capabilities.isCreativeMode && player.canCommandSenderUseCommand(2, "setblock")) {
                    WorldMoveJob job = new WorldMoveJob(player.getUniqueID(), message.dimension, message.regions, message.dx, message.dy, message.dz);
                    job.completion = success -> PacketHandler.INSTANCE.sendTo(new MessageMoveResult(message.id, success), player);
                    submitted = WorldEditQueue.INSTANCE.submit(server, job);
                }
            } catch (IllegalArgumentException | ArithmeticException error) {
                com.github.lunatrius.schematica.reference.Reference.logger.warn("Rejected invalid move request", error);
            }
            if (!submitted) PacketHandler.INSTANCE.sendTo(new MessageMoveResult(message.id, false), player);
        });
        return queued ? null : new MessageMoveResult(message.id, false);
    }
}

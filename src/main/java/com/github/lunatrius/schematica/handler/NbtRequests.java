// SPDX-License-Identifier: LGPL-3.0-only
// Servux LitematicsDataProvider.onBulkEntityRequest, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.handler;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import com.github.lunatrius.schematica.command.CommandSchematicaPlus;
import com.github.lunatrius.schematica.nbt.NBTHelper;
import com.github.lunatrius.schematica.network.PacketHandler;
import com.github.lunatrius.schematica.network.message.MessageNbtData;
import com.github.lunatrius.schematica.network.message.MessageNbtRequest;
import com.github.lunatrius.schematica.reference.Reference;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public final class NbtRequests {
    public static final NbtRequests INSTANCE = new NbtRequests();
    static final int MAX_PENDING = 64;
    static final int PER_TICK = 8;
    public static final int MAX_BYTES = 16 * 1024 * 1024;

    private final Queue<Request> requests = new ConcurrentLinkedQueue<>();
    private final Map<UUID, AtomicInteger> pending = new ConcurrentHashMap<>();

    private NbtRequests() {}

    private static final class Request {
        final EntityPlayerMP player;
        final MessageNbtRequest message;

        Request(EntityPlayerMP player, MessageNbtRequest message) {
            this.player = player;
            this.message = message;
        }
    }

    public void enqueue(EntityPlayerMP player, MessageNbtRequest message) {
        AtomicInteger count = pending.computeIfAbsent(player.getUniqueID(), id -> new AtomicInteger());
        if (count.incrementAndGet() > MAX_PENDING) {
            count.decrementAndGet();
            deny(player, message.id);
            return;
        }
        requests.add(new Request(player, message));
    }

    public void forget(EntityPlayer player) {
        pending.remove(player.getUniqueID());
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (int i = 0; i < PER_TICK; i++) {
            Request request = requests.poll();
            if (request == null) return;
            AtomicInteger count = pending.get(request.player.getUniqueID());
            if (count != null) count.decrementAndGet();
            try {
                answer(request.player, request.message);
            } catch (Exception e) {
                Reference.logger.warn("Could not answer the data request of {}", request.player.getCommandSenderName(), e);
                deny(request.player, request.message.id);
            }
        }
    }

    static boolean allowed(EntityPlayerMP player) {
        MinecraftServer server = MinecraftServer.getServer();
        if (!ConfigurationHandler.nbtRequestsEnabled || server == null || !server.getConfigurationManager().playerEntityList.contains(player)) return false;
        int level = ConfigurationHandler.nbtRequestPermissionLevel;
        return level <= 0 || player.canCommandSenderUseCommand(level, CommandSchematicaPlus.NAME);
    }

    private static void answer(EntityPlayerMP player, MessageNbtRequest message) throws IOException {
        World world = player.worldObj;
        if (!allowed(player) || !world.getChunkProvider().chunkExists(message.chunkX, message.chunkZ)) {
            deny(player, message.id);
            return;
        }
        byte[] payload = CompressedStreamTools.compress(collect(world, message));
        if (payload.length > MAX_BYTES) {
            deny(player, message.id);
            return;
        }
        int size = MessageNbtData.CHUNK_SIZE;
        int count = Math.max(1, (payload.length + size - 1) / size);
        for (int index = 0; index < count; index++) {
            byte[] slice = Arrays.copyOfRange(payload, index * size, Math.min(payload.length, (index + 1) * size));
            PacketHandler.INSTANCE.sendTo(new MessageNbtData(message.id, index, count, slice), player);
        }
    }

    static NBTTagCompound collect(World world, MessageNbtRequest message) {
        Chunk chunk = world.getChunkFromChunkCoords(message.chunkX, message.chunkZ);
        NBTTagList tiles = new NBTTagList();
        for (Object value : chunk.chunkTileEntityMap.values()) {
            TileEntity tile = (TileEntity) value;
            if (tile.isInvalid() || tile.yCoord < message.minY || tile.yCoord > message.maxY) continue;
            tiles.appendTag(NBTHelper.writeTileEntityToCompound(tile));
        }
        NBTTagList entities = new NBTTagList();
        int x = message.chunkX << 4, z = message.chunkZ << 4;
        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(x, message.minY, z, x + 16, message.maxY + 1, z + 16);
        for (Object value : world.getEntitiesWithinAABB(Entity.class, box)) {
            if (value instanceof EntityPlayer) continue;
            NBTTagCompound tag = NBTHelper.writeEntityToCompound((Entity) value);
            if (tag != null) entities.appendTag(tag);
        }
        NBTTagCompound data = new NBTTagCompound();
        data.setTag("TileEntities", tiles);
        data.setTag("Entities", entities);
        return data;
    }

    private static void deny(EntityPlayerMP player, int id) {
        PacketHandler.INSTANCE.sendTo(new MessageNbtData(id, 0, 0, new byte[0]), player);
    }
}

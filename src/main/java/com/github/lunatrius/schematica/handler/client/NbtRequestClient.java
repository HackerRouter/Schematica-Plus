package com.github.lunatrius.schematica.handler.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.nbt.NBTTagCompound;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.handler.NbtRequests;
import com.github.lunatrius.schematica.network.PacketHandler;
import com.github.lunatrius.schematica.network.message.MessageNbtData;
import com.github.lunatrius.schematica.network.message.MessageNbtRequest;
import com.github.lunatrius.schematica.network.transfer.NbtPayload;
import com.github.lunatrius.schematica.network.transfer.SliceAssembler;
import com.github.lunatrius.schematica.reference.Reference;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public final class NbtRequestClient {
    public static final NbtRequestClient INSTANCE = new NbtRequestClient();
    static final int TIMEOUT_TICKS = 600;

    private final Map<Integer, Pending> pending = new HashMap<>();
    private int nextId;

    private NbtRequestClient() {}

    private static final class Pending {
        final Consumer<NBTTagCompound> done;
        SliceAssembler slices;
        int ticks;

        Pending(Consumer<NBTTagCompound> done) {
            this.done = done;
        }
    }

    public boolean available() {
        return SchematicaPlus.proxy.supportsNbtRequests;
    }

    public void request(int chunkX, int chunkZ, int minY, int maxY, Consumer<NBTTagCompound> done) {
        PacketHandler.INSTANCE.sendToServer(new MessageNbtRequest(open(done), chunkX, chunkZ, minY, maxY));
    }

    int open(Consumer<NBTTagCompound> done) {
        int id = nextId++;
        pending.put(id, new Pending(done));
        return id;
    }

    public void received(MessageNbtData message) {
        Pending request = pending.get(message.id);
        if (request == null) return;
        if (message.count == 0) {
            finish(message.id, null);
            return;
        }
        if (request.slices == null) request.slices = SliceAssembler.create(message.count, MessageNbtData.CHUNK_SIZE, NbtRequests.MAX_BYTES);
        if (request.slices == null || !request.slices.add(message.index, message.count, message.data)) {
            finish(message.id, null);
            return;
        }
        request.ticks = 0;
        if (!request.slices.complete()) return;
        NBTTagCompound data = null;
        try {
            data = NbtPayload.read(request.slices.join(), (long) NbtRequests.MAX_BYTES * 8);
        } catch (IOException | RuntimeException e) {
            Reference.logger.warn("Could not read the requested block data", e);
        }
        finish(message.id, data);
    }

    private void finish(int id, NBTTagCompound data) {
        Pending request = pending.remove(id);
        if (request != null) request.done.accept(data);
    }

    public void clear() {
        for (Iterator<Pending> iterator = pending.values().iterator(); iterator.hasNext();) {
            Pending request = iterator.next();
            iterator.remove();
            request.done.accept(null);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pending.isEmpty()) return;
        for (Iterator<Pending> iterator = pending.values().iterator(); iterator.hasNext();) {
            Pending request = iterator.next();
            if (++request.ticks < TIMEOUT_TICKS) continue;
            iterator.remove();
            request.done.accept(null);
        }
    }
}

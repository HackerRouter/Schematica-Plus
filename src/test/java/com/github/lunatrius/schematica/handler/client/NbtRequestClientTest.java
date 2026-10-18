package com.github.lunatrius.schematica.handler.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.github.lunatrius.schematica.network.message.MessageNbtData;
import com.github.lunatrius.schematica.network.message.MessageNbtRequest;

import cpw.mods.fml.common.gameevent.TickEvent;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

public class NbtRequestClientTest {
    private final List<NBTTagCompound> results = new ArrayList<>();

    @After public void clear() { NbtRequestClient.INSTANCE.clear(); }

    @Test public void slicedAnswersAreJoinedAndDenialsFallBack() throws Exception {
        NBTTagCompound data = new NBTTagCompound();
        NBTTagList tiles = new NBTTagList();
        NBTTagCompound tile = new NBTTagCompound();
        byte[] noise = new byte[50000];
        new Random(42).nextBytes(noise);
        tile.setByteArray("Data", noise);
        tiles.appendTag(tile);
        data.setTag("TileEntities", tiles);
        byte[] payload = CompressedStreamTools.compress(data);
        int size = MessageNbtData.CHUNK_SIZE, count = (payload.length + size - 1) / size;
        assertTrue(count > 1);
        int id = NbtRequestClient.INSTANCE.open(results::add);
        for (int index = count - 1; index >= 0; index--) {
            NbtRequestClient.INSTANCE.received(new MessageNbtData(id, index, count,
                Arrays.copyOfRange(payload, index * size, Math.min(payload.length, (index + 1) * size))));
        }
        assertEquals(data, results.get(0));
        int denied = NbtRequestClient.INSTANCE.open(results::add);
        NbtRequestClient.INSTANCE.received(new MessageNbtData(denied, 0, 0, new byte[0]));
        assertNull(results.get(1));
        NbtRequestClient.INSTANCE.received(new MessageNbtData(denied, 0, 1, new byte[0]));
        assertEquals(2, results.size());
    }

    @Test public void unansweredRequestsTimeOutAndDisconnectsReleaseThem() {
        NbtRequestClient.INSTANCE.open(results::add);
        for (int i = 0; i < NbtRequestClient.TIMEOUT_TICKS; i++) {
            NbtRequestClient.INSTANCE.onClientTick(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
        }
        assertEquals(1, results.size());
        assertNull(results.get(0));
        NbtRequestClient.INSTANCE.open(results::add);
        NbtRequestClient.INSTANCE.clear();
        assertEquals(2, results.size());
    }

    @Test public void requestsRoundTripAndRejectInvalidHeights() {
        ByteBuf buffer = Unpooled.buffer();
        new MessageNbtRequest(7, -3, 12, 10, 64).toBytes(buffer);
        MessageNbtRequest read = new MessageNbtRequest();
        read.fromBytes(buffer);
        assertEquals(7, read.id);
        assertEquals(-3, read.chunkX);
        assertEquals(64, read.maxY);
        ByteBuf invalid = Unpooled.buffer();
        new MessageNbtRequest(1, 0, 0, 70, 64).toBytes(invalid);
        assertThrows(IllegalArgumentException.class, () -> new MessageNbtRequest().fromBytes(invalid));
        ByteBuf slice = Unpooled.buffer();
        new MessageNbtData(1, 2, 2, new byte[0]).toBytes(slice);
        assertThrows(IllegalArgumentException.class, () -> new MessageNbtData().fromBytes(slice));
    }
}

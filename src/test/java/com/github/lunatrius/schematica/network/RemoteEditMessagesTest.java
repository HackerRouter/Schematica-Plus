package com.github.lunatrius.schematica.network;

import org.junit.Test;

import com.github.lunatrius.schematica.network.message.MessageEditStatus;
import com.github.lunatrius.schematica.network.message.MessageEditUpload;
import com.github.lunatrius.schematica.network.message.MessagePlacementIntent;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class RemoteEditMessagesTest {
    @Test public void uploadSlicesRoundTripAndRejectOversizedData() {
        ByteBuf buffer = Unpooled.buffer();
        new MessageEditUpload(42L, -1, 2, 3, new byte[] {1, 2, 3}).toBytes(buffer);
        MessageEditUpload read = new MessageEditUpload();
        read.fromBytes(buffer);
        assertEquals(42L, read.id);
        assertEquals(-1, read.dimension);
        assertEquals(2, read.index);
        assertEquals(3, read.count);
        assertArrayEquals(new byte[] {1, 2, 3}, read.data);

        ByteBuf large = Unpooled.buffer();
        new MessageEditUpload(1L, 0, 0, 1, new byte[MessageEditUpload.CHUNK_SIZE + 1]).toBytes(large);
        assertThrows(IllegalArgumentException.class, () -> new MessageEditUpload().fromBytes(large));
    }

    @Test public void statusCarriesProgressAndReason() {
        MessageEditStatus status = new MessageEditStatus(7L, MessageEditStatus.State.PROGRESS);
        status.stage = 3; status.completed = 10; status.total = 20; status.affected = 5; status.entities = 1;
        ByteBuf buffer = Unpooled.buffer();
        status.toBytes(buffer);
        MessageEditStatus read = new MessageEditStatus();
        read.fromBytes(buffer);
        assertEquals(MessageEditStatus.State.PROGRESS, read.state);
        assertEquals(3, read.stage);
        assertEquals(20, read.total);

        ByteBuf rejected = Unpooled.buffer();
        MessageEditStatus.rejected(8L, "schematica.message.edit.busy").toBytes(rejected);
        read = new MessageEditStatus();
        read.fromBytes(rejected);
        assertEquals(MessageEditStatus.State.REJECTED, read.state);
        assertEquals("schematica.message.edit.busy", read.reason);
    }

    @Test public void placementIntentRoundTripsAndChecksMetadata() {
        ByteBuf buffer = Unpooled.buffer();
        new MessagePlacementIntent(-3, 64, 9, "minecraft:stone_stairs", 6).toBytes(buffer);
        MessagePlacementIntent read = new MessagePlacementIntent();
        read.fromBytes(buffer);
        assertEquals(-3, read.x); assertEquals(64, read.y); assertEquals(9, read.z);
        assertEquals("minecraft:stone_stairs", read.block);
        assertEquals(6, read.metadata);

        ByteBuf invalid = Unpooled.buffer();
        new MessagePlacementIntent(0, 0, 0, "minecraft:stone", 16).toBytes(invalid);
        assertThrows(IllegalArgumentException.class, () -> new MessagePlacementIntent().fromBytes(invalid));
    }
}

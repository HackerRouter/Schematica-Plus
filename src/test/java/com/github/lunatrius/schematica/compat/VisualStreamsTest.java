package com.github.lunatrius.schematica.compat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import io.netty.buffer.ByteBuf;
import org.junit.Test;
import static org.junit.Assert.*;

public class VisualStreamsTest {
    public static class Multipart {
        int connections = 0x531;
        ByteBuf lastBuffer;
        public void writeDesc(codechicken.lib.data.MCDataOutput stream) {
            lastBuffer = ((codechicken.lib.packet.PacketCustom) stream).buffer;
            stream.writeInt(connections);
        }
        public void readDesc(codechicken.lib.data.MCDataInput stream) {
            lastBuffer = ((codechicken.lib.packet.PacketCustom) stream).buffer;
            connections = stream.readInt();
        }
    }

    @Test public void roundTripsMultipartDescriptionsAndReleasesCodecBuffers() throws Exception {
        Multipart source = new Multipart();
        byte[] data = VisualStreams.writeMultipart(source);
        assertEquals(4, data.length);
        assertEquals(0, source.lastBuffer.refCnt());
        Multipart preview = new Multipart();
        preview.connections = 0;
        VisualStreams.readMultipart(preview, data);
        assertEquals(source.connections, preview.connections);
        assertEquals(0, preview.lastBuffer.refCnt());
        assertThrows(IOException.class, () -> VisualStreams.readMultipart(preview, new byte[8]));
        assertEquals(0, preview.lastBuffer.refCnt());
    }

    public static class StreamTile {
        int facing;
        boolean active;
        public void write(DataOutputStream stream) throws IOException {
            stream.writeInt(facing);
            stream.writeBoolean(active);
        }
        public void read(DataInputStream stream) throws IOException {
            facing = stream.readInt();
            active = stream.readBoolean();
        }
    }

    public static class BufferTile {
        ByteBuf lastBuffer;
        int connections;
        public void write(ByteBuf buffer) {
            lastBuffer = buffer;
            buffer.writeInt(connections);
        }
        public void read(ByteBuf buffer) {
            lastBuffer = buffer;
            connections = buffer.readInt();
        }
        public void fail(ByteBuf buffer) {
            lastBuffer = buffer;
            throw new IllegalStateException("broken mod codec");
        }
        public void oversized(DataOutputStream stream) throws IOException {
            stream.write(new byte[VisualStreams.MAX_BYTES + 1]);
        }
    }

    @Test public void roundTripsDataStreamsWithoutAWorldOrNetworkHandler() throws Exception {
        StreamTile source = new StreamTile();
        source.facing = 5;
        source.active = true;
        byte[] data = VisualStreams.writeStream(source, "write", DataOutputStream.class.getName());
        StreamTile target = new StreamTile();
        VisualStreams.readStream(target, "read", DataInputStream.class.getName(), data);
        assertEquals(5, target.facing);
        assertTrue(target.active);
    }

    @Test public void roundTripsBuffersAndReleasesThemOnFailure() throws Exception {
        BufferTile tile = new BufferTile();
        tile.connections = 0x31;
        byte[] bytes = VisualStreams.writeBuffer(tile, "write");
        assertEquals(0, tile.lastBuffer.refCnt());
        tile.connections = 0;
        VisualStreams.readBuffer(tile, "read", bytes);
        assertEquals(0x31, tile.connections);
        assertEquals(0, tile.lastBuffer.refCnt());
        assertThrows(ReflectiveOperationException.class, () -> VisualStreams.writeBuffer(tile, "fail"));
        assertEquals(0, tile.lastBuffer.refCnt());
    }

    @Test public void rejectsOversizedAndMismatchedPayloads() {
        BufferTile tile = new BufferTile();
        assertThrows(ReflectiveOperationException.class,
            () -> VisualStreams.writeStream(tile, "oversized", DataOutputStream.class.getName()));
        assertThrows(IllegalArgumentException.class,
            () -> VisualStreams.readBuffer(tile, "read", new byte[VisualStreams.MAX_BYTES + 1]));
        assertThrows(IOException.class, () -> VisualStreams.readBuffer(tile, "read", new byte[8]));
        assertEquals(0, tile.lastBuffer.refCnt());
    }
}

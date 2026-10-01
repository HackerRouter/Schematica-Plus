package com.github.lunatrius.schematica.tool;

import java.util.Arrays;

import org.junit.Test;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import com.github.lunatrius.schematica.api.SchematicRegion;
import com.github.lunatrius.schematica.network.message.MessageMoveRequest;
import com.github.lunatrius.schematica.network.message.MessageMoveResult;

import static org.junit.Assert.*;

public class MovePacketTest {
    @Test public void boundedRegionsAndCompletionIdentitySurviveWireRoundtrip() {
        MessageMoveRequest request = new MessageMoveRequest(123L, -1, Arrays.asList(new SchematicRegion("A", -10, 60, 7, -5, 62, 8), new SchematicRegion("B", 3, 70, 3, 4, 71, 4)), 20, -3, -8);
        ByteBuf buffer = Unpooled.buffer();
        try {
            request.toBytes(buffer); MessageMoveRequest read = new MessageMoveRequest(); read.fromBytes(buffer);
            assertEquals(123L, read.id); assertEquals(-1, read.dimension); assertEquals(-8, read.dz); assertEquals(2, read.regions.size());
            assertEquals(-10, read.regions.get(0).minX); assertEquals(71, read.regions.get(1).maxY);
            buffer.clear(); new MessageMoveResult(123L, false).toBytes(buffer); MessageMoveResult result = new MessageMoveResult(); result.fromBytes(buffer);
            assertEquals(123L, result.id); assertFalse(result.success);
        } finally { buffer.release(); }
    }
    @Test public void oversizedTruncatedAndTrailingDataAreRejected() {
        for (int count : new int[] {0, 1, 257}) {
            ByteBuf buffer = Unpooled.buffer();
            try {
                buffer.writeLong(0); for (int i = 0; i < 4; i++) buffer.writeInt(0); buffer.writeShort(count);
                assertThrows(IllegalArgumentException.class, () -> new MessageMoveRequest().fromBytes(buffer));
            } finally { buffer.release(); }
        }
    }
}

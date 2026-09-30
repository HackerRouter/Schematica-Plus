package com.github.lunatrius.schematica.network;

import java.lang.reflect.Proxy;
import org.junit.Test;
import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.handler.DownloadHandler;
import com.github.lunatrius.schematica.network.transfer.SchematicTransfer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TransferTest {
    private ISchematic dimensions(int width, int height, int length) {
        return (ISchematic) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {ISchematic.class},
            (proxy, method, args) -> {
                switch (method.getName()) {
                    case "getWidth": return width;
                    case "getHeight": return height;
                    case "getLength": return length;
                    default: throw new UnsupportedOperationException(method.getName());
                }
            });
    }

    @Test public void retransmissionKeepsRetryBudgetUntilAnAcknowledgement() {
        SchematicTransfer transfer = new SchematicTransfer(dimensions(32, 1, 1), "test");
        transfer.setState(SchematicTransfer.State.BEGIN);
        transfer.retries = 3;
        transfer.setState(SchematicTransfer.State.BEGIN);
        assertEquals(3, transfer.retries);
        transfer.setState(SchematicTransfer.State.CHUNK_WAIT);
        assertEquals(0, transfer.retries);
        transfer.setState(SchematicTransfer.State.CHUNK);
        transfer.retries = 4;
        transfer.setState(SchematicTransfer.State.CHUNK);
        assertEquals(4, transfer.retries);
        transfer.confirmChunk(0, 0, 0);
        assertEquals(16, transfer.baseX);
        assertEquals(0, transfer.retries);
        transfer.confirmChunk(16, 0, 0); // Not sent yet; ignore the unexpected acknowledgement.
        assertEquals(16, transfer.baseX);
        assertEquals(SchematicTransfer.State.CHUNK_WAIT, transfer.state);
    }

    @Test public void completionRequiresAllUniqueChunksAndRejectsInvalidCoordinates() {
        DownloadHandler handler = DownloadHandler.INSTANCE;
        try {
            handler.beginDownload(dimensions(17, 1, 1));
            assertFalse(handler.isDownloadComplete());
            assertFalse(handler.validChunk(-16, 0, 0));
            assertFalse(handler.validChunk(1, 0, 0));
            assertFalse(handler.validChunk(32, 0, 0));
            assertTrue(handler.validChunk(16, 0, 0));
            assertFalse(handler.hasReceivedChunk(0, 0, 0));
            handler.receivedChunk(0, 0, 0);
            assertTrue(handler.hasReceivedChunk(0, 0, 0));
            assertFalse(handler.hasReceivedChunk(16, 0, 0));
            handler.receivedChunk(0, 0, 0);
            assertFalse(handler.isDownloadComplete());
            handler.receivedChunk(16, 0, 0);
            assertTrue(handler.isDownloadComplete());
        } finally { handler.beginDownload(null); }
    }
}

// Download completion and independent region transfer checks, by HackerRouter, 2026.
package com.github.lunatrius.schematica.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.api.SchematicRegion;
import com.github.lunatrius.schematica.network.message.MessageDownloadEnd;
import com.github.lunatrius.schematica.network.message.MessageDownloadRegions;
import com.github.lunatrius.schematica.network.transfer.SchematicTransfer;
import com.github.lunatrius.schematica.world.storage.MultiRegionSchematic;
import com.github.lunatrius.schematica.world.storage.Schematic;

import cpw.mods.fml.common.network.simpleimpl.IMessage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class DownloadCompletionTest {
    private SchematicTransfer ready(ISchematic schematic) {
        SchematicTransfer transfer = new SchematicTransfer(schematic, "download.schemplus");
        assertTrue(transfer.acceptRegionSupport(true));
        transfer.setState(SchematicTransfer.State.CHUNK);
        assertFalse(transfer.confirmChunk(0, 0, 0));
        assertEquals(SchematicTransfer.State.END_WAIT, transfer.state);
        return transfer;
    }

    @Test public void flatSchematicFinishesWithoutRegionPayload() throws Exception {
        for (boolean regionBoxes : new boolean[] {false, true}) {
            Schematic flat = new Schematic(null, 1, 1, 1);
            if (regionBoxes) flat.setRegions(Collections.singletonList(new SchematicRegion("A", 0, 0, 0, 0, 0, 0)));
            SchematicTransfer transfer = ready(flat);
            List<IMessage> packets = new ArrayList<>();
            assertFalse(DownloadHandler.sendEnd(transfer, packets::add));
            assertEquals(1, packets.size());
            assertEquals("download.schemplus", ((MessageDownloadEnd) packets.get(0)).name);
            assertEquals(SchematicTransfer.State.END, transfer.state);
        }
    }

    @Test public void legacyClientsFinishFlatDownloadsWithoutRegionSupport() throws Exception {
        SchematicTransfer transfer = ready(new Schematic(null, 1, 1, 1));
        assertTrue(transfer.acceptRegionSupport(false));
        List<IMessage> packets = new ArrayList<>();
        assertFalse(DownloadHandler.sendEnd(transfer, packets::add));
        assertEquals(1, packets.size());
        assertTrue(packets.get(0) instanceof MessageDownloadEnd);
        assertEquals(SchematicTransfer.State.END, transfer.state);
    }

    private SchematicTransfer cachedRegions(int bytes) {
        SchematicRegion region = new SchematicRegion("A", 0, 0, 0, 0, 0, 0);
        MultiRegionSchematic source = new MultiRegionSchematic(null, 1, 1, 1);
        source.setRegions(Collections.singletonList(region));
        source.addRegion(region, new Schematic(null, 1, 1, 1));
        SchematicTransfer transfer = ready(source);
        transfer.regionPayload = new byte[bytes];
        return transfer;
    }

    @Test public void regionSlicesFinishBeforeTheEndMessage() throws Exception {
        SchematicTransfer transfer = cachedRegions(9 * MessageDownloadRegions.CHUNK_SIZE + 7);
        List<IMessage> packets = new ArrayList<>();
        assertTrue(DownloadHandler.sendEnd(transfer, packets::add));
        assertEquals(8, packets.size());
        assertEquals(SchematicTransfer.State.END_WAIT, transfer.state);
        assertFalse(DownloadHandler.sendEnd(transfer, packets::add));
        assertEquals(11, packets.size());
        for (int i = 0; i < 10; i++) {
            MessageDownloadRegions part = (MessageDownloadRegions) packets.get(i);
            assertEquals(i, part.index);
            assertEquals(10, part.count);
            assertEquals(i == 9 ? 7 : MessageDownloadRegions.CHUNK_SIZE, part.data.length);
        }
        assertTrue(packets.get(10) instanceof MessageDownloadEnd);
        assertEquals(SchematicTransfer.State.END, transfer.state);
    }

    @Test public void exactlyEightRegionSlicesFinishInOneTick() throws Exception {
        SchematicTransfer transfer = cachedRegions(8 * MessageDownloadRegions.CHUNK_SIZE);
        List<IMessage> packets = new ArrayList<>();
        assertFalse(DownloadHandler.sendEnd(transfer, packets::add));
        assertEquals(9, packets.size());
        assertTrue(packets.get(8) instanceof MessageDownloadEnd);
        assertEquals(SchematicTransfer.State.END, transfer.state);
    }

    @Test public void failedRegionEncodingDoesNotSendASuccessfulEnd() {
        Schematic broken = new Schematic(null, 1, 1, 1) {
            @Override public List<SchematicRegion> getRegions() {
                return Collections.singletonList(new SchematicRegion("A", 0, 0, 0, 0, 0, 0));
            }
            @Override public ISchematic getRegionSchematic(String name) {
                throw new IllegalStateException("Unreadable region");
            }
        };
        SchematicTransfer transfer = ready(broken);
        List<IMessage> packets = new ArrayList<>();
        assertThrows(IllegalStateException.class, () -> DownloadHandler.sendEnd(transfer, packets::add));
        assertTrue(packets.isEmpty());
        assertEquals(SchematicTransfer.State.END_WAIT, transfer.state);
    }
}

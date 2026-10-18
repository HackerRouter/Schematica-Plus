package com.github.lunatrius.schematica.handler;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import com.github.lunatrius.schematica.api.SchematicOrigin;
import com.github.lunatrius.schematica.api.SchematicRegion;
import com.github.lunatrius.schematica.network.message.MessageSaveSelection;
import com.github.lunatrius.schematica.world.storage.RegionSelection;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import static org.junit.Assert.*;

public class SelectionSavesTest {
    @Test public void selectionRoundTripsAndKeepsTheOrigin() {
        MessageSaveSelection message = new MessageSaveSelection("house", Arrays.asList(
            new SchematicRegion("base", 10, 64, 10, 14, 66, 12), new SchematicRegion("tower", 12, 67, 11, 12, 75, 11)),
            new SchematicOrigin(11, 64, 10));
        ByteBuf buffer = Unpooled.buffer();
        message.toBytes(buffer);
        MessageSaveSelection read = new MessageSaveSelection();
        read.fromBytes(buffer);
        assertEquals("house", read.name);
        assertEquals(2, read.regions.size());
        assertEquals("tower", read.regions.get(1).name);
        RegionSelection selection = SelectionSaves.selection(read);
        assertNotNull(selection);
        assertEquals(10, selection.minX);
        assertEquals(75, selection.maxY);
        assertEquals(1, selection.localOrigin.x);
        assertEquals(0, selection.localOrigin.y);
        assertEquals(1, selection.localRegions.get(1).maxZ);
    }

    @Test public void emptyOrInvalidSelectionsAreRejected() {
        assertNull(SelectionSaves.selection(new MessageSaveSelection("empty", Collections.emptyList(), SchematicOrigin.ZERO)));
        assertNull(SelectionSaves.selection(new MessageSaveSelection("deep", Collections.singletonList(
            new SchematicRegion("box", 0, -5, 0, 1, 1, 1)), SchematicOrigin.ZERO)));
        ByteBuf buffer = Unpooled.buffer();
        new MessageSaveSelection("trailing", Collections.emptyList(), SchematicOrigin.ZERO).toBytes(buffer);
        buffer.writeByte(1);
        assertThrows(IllegalArgumentException.class, () -> new MessageSaveSelection().fromBytes(buffer));
    }
}

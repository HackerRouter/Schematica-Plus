package com.github.lunatrius.schematica.compat;

import appeng.parts.networking.PartCable;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;
import static org.junit.Assert.*;

public class AE2VisualAdapterTest {
    @Test public void preservesClientCableConnectionsWithoutCallingNetworkWriters() throws Exception {
        PartCable source = new PartCable();
        Object network = source.network;
        NBTTagCompound tag = AE2VisualAdapter.captureObject(source);
        PartCable preview = new PartCable();
        preview.powered = false;
        preview.connections.clear();
        preview.channelsOnSide[2] = 0;
        AE2VisualAdapter.restoreObject(preview, tag);
        assertTrue(preview.powered);
        assertEquals(source.connections, preview.connections);
        assertArrayEquals(source.channelsOnSide, preview.channelsOnSide);
        assertEquals(2, source.connections.size());
        assertSame(network, source.network);
        assertNotSame(network, preview.network);
    }
}

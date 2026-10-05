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

    @Test public void devicesAndCableBusPartsTurnWithTheSchematic() {
        net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
        tag.setString("orientation_forward", "NORTH");
        tag.setString("orientation_up", "UP");
        net.minecraft.nbt.NBTTagCompound north = new net.minecraft.nbt.NBTTagCompound();
        north.setString("id", "north part");
        tag.setTag("def:2", north);
        tag.setTag("extra:2", new net.minecraft.nbt.NBTTagCompound());
        tag.setTag("def:6", new net.minecraft.nbt.NBTTagCompound());
        net.minecraft.nbt.NBTTagCompound top = new net.minecraft.nbt.NBTTagCompound();
        top.setByte("spin", (byte) 0);
        tag.setTag("extra:1", top);
        tag.setTag("facade:4", new net.minecraft.nbt.NBTTagCompound());
        AE2VisualAdapter.transformTag(tag, 'Y');
        org.junit.Assert.assertEquals("EAST", tag.getString("orientation_forward"));
        org.junit.Assert.assertEquals("UP", tag.getString("orientation_up"));
        org.junit.Assert.assertEquals("north part", tag.getCompoundTag("def:5").getString("id"));
        org.junit.Assert.assertFalse(tag.hasKey("def:2"));
        org.junit.Assert.assertTrue(tag.hasKey("def:6"));
        org.junit.Assert.assertTrue(tag.hasKey("facade:2"));
        org.junit.Assert.assertEquals(1, tag.getCompoundTag("extra:1").getByte("spin"));
        AE2VisualAdapter.transformTag(tag, 'X');
        org.junit.Assert.assertEquals(0, tag.getCompoundTag("extra:2").getByte("spin"));
    }
}

package com.github.lunatrius.schematica.compat;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.parts.networking.PartCable;
import appeng.parts.reporting.AbstractPartReporting;
import org.junit.Test;

import static org.junit.Assert.*;

public class AE2VisualAdapterTest {
    @Test public void rotatedPreviewRemovesOldPartsAndPreservesPoweredState() throws Exception {
        CableHost host = new CableHost();
        AbstractPartReporting north = host.add(ForgeDirection.NORTH, 20, 0);
        host.add(ForgeDirection.UP, 21, 1);
        new AE2VisualAdapter().transformPreview(host, 'Y');
        assertNull(host.getPart(ForgeDirection.NORTH));
        AbstractPartReporting east = (AbstractPartReporting) host.getPart(ForgeDirection.EAST);
        assertNotSame(north, east);
        assertEquals(20, east.clientFlags);
        assertEquals(0.75f, east.opacity, 0);
        assertEquals(2, ((AbstractPartReporting) host.getPart(ForgeDirection.UP)).spin);
        assertEquals(EnumSet.of(ForgeDirection.EAST, ForgeDirection.WEST), host.cable.connections);
        assertArrayEquals(new int[] {0, 0, 0, 0, 8, 8}, host.cable.channelsOnSide);
        assertTrue(host.cable.powered);
        NBTTagCompound exported = new NBTTagCompound();
        host.writeToNBT(exported);
        assertFalse(exported.hasKey("def:2"));
        assertTrue(exported.hasKey("def:5"));
    }

    @Test public void mirroredPartsExchangeTheirOwnVisualStateWithoutAccumulatingGhosts() throws Exception {
        CableHost host = new CableHost();
        host.add(ForgeDirection.NORTH, 20, 0);
        host.add(ForgeDirection.SOUTH, 4, 0);
        AE2VisualAdapter adapter = new AE2VisualAdapter();
        adapter.transformPreview(host, 'z');
        assertEquals(4, ((AbstractPartReporting) host.getPart(ForgeDirection.NORTH)).clientFlags);
        assertEquals(20, ((AbstractPartReporting) host.getPart(ForgeDirection.SOUTH)).clientFlags);
        adapter.transformPreview(host, 'z');
        for (int i = 0; i < 4; i++) adapter.transformPreview(host, 'Y');
        assertEquals(2, host.parts.size());
        assertEquals(20, ((AbstractPartReporting) host.getPart(ForgeDirection.NORTH)).clientFlags);
        assertEquals(4, ((AbstractPartReporting) host.getPart(ForgeDirection.SOUTH)).clientFlags);
    }

    public static class CableHost extends TileEntity implements appeng.api.parts.IPartHost {
        final Map<ForgeDirection, AbstractPartReporting> parts = new EnumMap<>(ForgeDirection.class);
        final PartCable cable = new PartCable();

        AbstractPartReporting add(ForgeDirection side, int flags, int spin) {
            AbstractPartReporting part = new AbstractPartReporting();
            part.clientFlags = (byte) flags;
            part.spin = (byte) spin;
            part.opacity = 0.75f;
            parts.put(side, part);
            return part;
        }

        @Override public Object getPart(ForgeDirection side) { return side == ForgeDirection.UNKNOWN ? cable : parts.get(side); }
        @Override public void removePart(ForgeDirection side, boolean suppressUpdate) {
            assertTrue(suppressUpdate);
            parts.remove(side);
        }

        @Override public void writeToNBT(NBTTagCompound tag) {
            for (Map.Entry<ForgeDirection, AbstractPartReporting> entry : parts.entrySet()) {
                tag.setTag("def:" + entry.getKey().ordinal(), new NBTTagCompound());
                NBTTagCompound extra = new NBTTagCompound();
                extra.setByte("spin", entry.getValue().spin);
                tag.setTag("extra:" + entry.getKey().ordinal(), extra);
            }
        }

        @Override public void readFromNBT(NBTTagCompound tag) {
            // AE2 leaves existing parts untouched when a side has no saved definition.
            for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
                if (!tag.hasKey("def:" + side.ordinal())) continue;
                AbstractPartReporting part = parts.computeIfAbsent(side, ignored -> new AbstractPartReporting());
                part.spin = tag.getCompoundTag("extra:" + side.ordinal()).getByte("spin");
            }
        }
    }

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
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("orientation_forward", "NORTH");
        tag.setString("orientation_up", "UP");
        NBTTagCompound north = new NBTTagCompound();
        north.setString("id", "north part");
        tag.setTag("def:2", north);
        tag.setTag("extra:2", new NBTTagCompound());
        tag.setTag("def:6", new NBTTagCompound());
        NBTTagCompound top = new NBTTagCompound();
        top.setByte("spin", (byte) 0);
        tag.setTag("extra:1", top);
        tag.setTag("facade:4", new NBTTagCompound());
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

package com.github.lunatrius.schematica.nbt;

import java.util.Random;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import org.junit.Test;
import static org.junit.Assert.*;

public class TileUpdateDataTest {
    @Test public void keepsAdaptersSeparateAndReadsLegacyPackets() throws Exception {
        NBTTagCompound legacy = TileUpdateData.capture("test.Tile",
            new S35PacketUpdateTileEntity(0, 0, 0, 7, new NBTTagCompound()));
        NBTTagCompound adapters = new NBTTagCompound();
        NBTTagCompound adapter = new NBTTagCompound();
        adapter.setByteArray("Data", new byte[] { 1, 2, 3 });
        adapters.setTag("example:tile", adapter);
        NBTTagCompound combined = TileUpdateData.combine("test.Tile", legacy, adapters);
        assertEquals(7, TileUpdateData.packet(combined, "test.Tile", 4, 5, 6).func_148853_f());
        assertEquals(7, TileUpdateData.packet(legacy, "test.Tile", 4, 5, 6).func_148853_f());
        assertEquals(adapters, TileUpdateData.adapters(combined, "test.Tile"));
        assertTrue(TileUpdateData.adapters(combined, "wrong.Tile").hasNoTags());
        assertNull(TileUpdateData.packet(TileUpdateData.combine("test.Tile", null, adapters), "test.Tile", 0, 0, 0));
    }

    @Test public void roundTripsUpdateTypeAndRebasesOnlyRootCoordinates() throws Exception {
        NBTTagCompound payload = new NBTTagCompound();
        payload.setInteger("x", 1200);
        payload.setInteger("y", 70);
        payload.setInteger("z", -90);
        payload.setByteArray("X", new byte[] { 1, -1, 80 });
        NBTTagCompound linked = new NBTTagCompound();
        linked.setInteger("x", 999);
        payload.setTag("LinkedMachine", linked);
        NBTTagCompound data = TileUpdateData.capture("test.Tile",
            new S35PacketUpdateTileEntity(1200, 70, -90, 255, payload));
        S35PacketUpdateTileEntity packet = TileUpdateData.packet(data, "test.Tile", 2, 3, 4);
        assertEquals(255, packet.func_148853_f());
        assertEquals(2, packet.func_148856_c());
        assertEquals(3, packet.func_148855_d());
        assertEquals(4, packet.func_148854_e());
        assertEquals(2, packet.func_148857_g().getInteger("x"));
        assertEquals(999, packet.func_148857_g().getCompoundTag("LinkedMachine").getInteger("x"));
        assertArrayEquals(new byte[] { 1, -1, 80 }, packet.func_148857_g().getByteArray("X"));
        assertEquals(1200, payload.getInteger("x"));
        packet.func_148857_g().setByteArray("X", new byte[0]);
        assertEquals(3, data.getCompoundTag("Data").getByteArray("X").length);
    }

    @Test public void rejectsMismatchedClassesAndUnknownVersions() throws Exception {
        NBTTagCompound data = TileUpdateData.capture("test.Tile",
            new S35PacketUpdateTileEntity(0, 0, 0, 1, new NBTTagCompound()));
        assertNull(TileUpdateData.packet(data, "test.OtherTile", 0, 0, 0));
        data.setInteger("Version", 2);
        assertNull(TileUpdateData.packet(data, "test.Tile", 0, 0, 0));
        data.setInteger("Version", 1);
        data.setInteger("Type", 256);
        assertNull(TileUpdateData.packet(data, "test.Tile", 0, 0, 0));
        assertNull(TileUpdateData.capture("test.Tile", null));
    }

    @Test public void boundsOversizedUpdatePackets() throws Exception {
        byte[] bytes = new byte[40000];
        new Random(42).nextBytes(bytes);
        NBTTagCompound payload = new NBTTagCompound();
        payload.setByteArray("data", bytes);
        try {
            TileUpdateData.capture("test.Tile", new S35PacketUpdateTileEntity(0, 0, 0, 1, payload));
            fail("Oversized packets must not be retained");
        } catch (IndexOutOfBoundsException expected) {
            assertTrue(expected.getMessage().contains("maxCapacity"));
        }
    }
}

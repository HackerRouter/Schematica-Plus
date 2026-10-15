package com.github.lunatrius.schematica.handler;

import com.github.lunatrius.schematica.network.message.MessagePrinterAction;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import static org.junit.Assert.*;

public class PrinterRangeTest {
    @Test public void serverRequestsExposeTheNativeInteractionCoordinates() {
        MessagePrinterAction m = new MessagePrinterAction();
        m.action = 2; m.x = 32; m.y = 127; m.z = -80; m.side = 5;
        m.hitX = .25f; m.hitY = 1; m.hitZ = .75f;
        net.minecraft.network.play.client.C08PacketPlayerBlockPlacement place = PrinterActions.placement(m, null);
        assertEquals(32, place.func_149576_c()); assertEquals(127, place.func_149571_d());
        assertEquals(-80, place.func_149570_e()); assertEquals(5, place.func_149568_f());
        assertEquals(.25f, place.func_149573_h(), 0); assertEquals(1, place.func_149569_i(), 0);
        assertEquals(.75f, place.func_149575_j(), 0); assertNull(place.func_149574_g());
        net.minecraft.network.play.client.C07PacketPlayerDigging dig = PrinterActions.digging(m);
        assertEquals(2, dig.func_149506_g()); assertEquals(32, dig.func_149505_c());
        assertEquals(127, dig.func_149503_d()); assertEquals(-80, dig.func_149502_e()); assertEquals(5, dig.func_149501_f());
        net.minecraft.item.ItemStack held = new net.minecraft.item.ItemStack(new net.minecraft.item.Item(), 4, 0);
        place = PrinterActions.placement(m, held);
        held.stackSize--;
        assertEquals(4, place.func_149574_g().stackSize);
        assertNotSame(held, place.func_149574_g());
    }

    @Test public void clientHonorsServerLimitAndFallsBackWithoutServerSupport() {
        assertEquals(24, PrinterRange.effective(128, 4.5, 24), 0);
        assertEquals(128, PrinterRange.effective(128, 4.5, 128), 0);
        assertEquals(32, PrinterRange.effective(128, 4.5, 32), 0);
        assertEquals(4.5, PrinterRange.effective(32, 4.5, 0), 0);
        assertEquals(5, PrinterRange.effective(0, 5, 32), 0);
        assertEquals(3, PrinterRange.effective(0, 5, 3), 0);
        assertEquals(128, PrinterRange.effective(1000, 5, 1000), 0);
        assertEquals(0, PrinterRange.clamp(Double.NaN), 0);
        assertEquals(0, PrinterRange.clamp(Double.POSITIVE_INFINITY), 0);
    }

    @Test public void distanceUsesEyesAndRejectsUnrepresentableWorldCoordinates() {
        assertTrue(PrinterRange.contains(.5, 65.5, .5, 128, 65, 0, 128));
        assertFalse(PrinterRange.contains(.5, 65.5, .5, 129, 65, 0, 128));
        assertTrue(PrinterRange.contains(.5, 65.5, .5, 32, 65, 0, 32));
        assertFalse(PrinterRange.contains(.5, 65.5, .5, 32, 65, 1, 32));
        assertFalse(PrinterRange.contains(.5, 65.5, .5, 33, 65, 0, 32));
        assertFalse(PrinterRange.contains(.5, 65.5, .5, 0, -1, 0, 100));
        assertFalse(PrinterRange.contains(Integer.MIN_VALUE, 65, 0, Integer.MIN_VALUE, 65, 0, 32));
    }

    @Test public void printerClickRoundTripRetainsNativeClickAndPlayerState() {
        MessagePrinterAction m = new MessagePrinterAction();
        m.action = MessagePrinterAction.PLACE; m.x = -123; m.y = 65; m.z = 42; m.side = 4; m.slot = 8;
        m.hitX = .25f; m.hitY = 1; m.hitZ = .75f; m.yaw = -135; m.pitch = -30; m.sneaking = true;
        ByteBuf wire = Unpooled.buffer();
        try {
            m.toBytes(wire);
            MessagePrinterAction decoded = new MessagePrinterAction();
            decoded.fromBytes(wire);
            assertEquals(m.x, decoded.x); assertEquals(m.slot, decoded.slot); assertEquals(m.side, decoded.side);
            assertEquals(m.hitX, decoded.hitX, 0); assertEquals(m.hitY, decoded.hitY, 0); assertEquals(m.hitZ, decoded.hitZ, 0);
            assertEquals(m.yaw, decoded.yaw, 0); assertEquals(m.pitch, decoded.pitch, 0); assertTrue(decoded.sneaking);
            assertEquals(0, wire.readableBytes());
        } finally { wire.release(); }
    }

    @Test public void invalidClickCannotAddressAirUseInventoryOrNonFiniteCoordinates() {
        MessagePrinterAction m = new MessagePrinterAction();
        assertTrue(m.valid());
        m.side = 255; assertFalse(m.valid()); m.side = 0;
        m.slot = 9; assertFalse(m.valid()); m.slot = 0;
        m.action = 4; assertFalse(m.valid()); m.action = 3;
        m.hitX = Float.NaN; assertFalse(m.valid()); m.hitX = 0;
        m.hitY = 1.1f; assertFalse(m.valid()); m.hitY = 0;
        m.yaw = Float.POSITIVE_INFINITY; assertFalse(m.valid()); m.yaw = 0;
        m.y = 256; assertFalse(m.valid());
    }
}

package com.github.lunatrius.schematica.handler;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;

import com.github.lunatrius.schematica.network.message.MessagePrinterAction;
import com.github.lunatrius.schematica.network.message.MessagePlacementIntent;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import static org.junit.Assert.*;

public class PrinterRangeTest {
    @Test public void serverRequestsExposeTheNativeInteractionCoordinates() {
        MessagePrinterAction m = new MessagePrinterAction();
        m.action = 2; m.x = 32; m.y = 127; m.z = -80; m.side = 5;
        m.hitX = .25f; m.hitY = 1; m.hitZ = .75f;
        C08PacketPlayerBlockPlacement place = PrinterActions.placement(m, null);
        assertEquals(32, place.func_149576_c()); assertEquals(127, place.func_149571_d());
        assertEquals(-80, place.func_149570_e()); assertEquals(5, place.func_149568_f());
        assertEquals(.25f, place.func_149573_h(), 0); assertEquals(1, place.func_149569_i(), 0);
        assertEquals(.75f, place.func_149575_j(), 0); assertNull(place.func_149574_g());
        C07PacketPlayerDigging dig = PrinterActions.digging(m);
        assertEquals(2, dig.func_149506_g()); assertEquals(32, dig.func_149505_c());
        assertEquals(127, dig.func_149503_d()); assertEquals(-80, dig.func_149502_e()); assertEquals(5, dig.func_149501_f());
        ItemStack held = new ItemStack(new Item(), 4, 0);
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
        m.targetX = -123; m.targetY = 66; m.targetZ = 42;
        m.clickedBlock = 4; m.clickedMeta = 2; m.targetBlock = 0;
        m.intents.add(new MessagePlacementIntent(-123, 66, 42, "minecraft:oak_stairs", 5));
        ByteBuf wire = Unpooled.buffer();
        try {
            m.toBytes(wire);
            MessagePrinterAction decoded = new MessagePrinterAction();
            decoded.fromBytes(wire);
            assertEquals(m.x, decoded.x); assertEquals(m.slot, decoded.slot); assertEquals(m.side, decoded.side);
            assertEquals(m.hitX, decoded.hitX, 0); assertEquals(m.hitY, decoded.hitY, 0); assertEquals(m.hitZ, decoded.hitZ, 0);
            assertEquals(m.yaw, decoded.yaw, 0); assertEquals(m.pitch, decoded.pitch, 0); assertTrue(decoded.sneaking);
            assertEquals(66, decoded.targetY); assertEquals(4, decoded.clickedBlock); assertEquals(2, decoded.clickedMeta);
            assertEquals(1, decoded.intents.size()); assertEquals("minecraft:oak_stairs", decoded.intents.get(0).block);
            assertEquals(5, decoded.intents.get(0).metadata);
            assertEquals(0, wire.readableBytes());
        } finally { wire.release(); }
    }

    @Test public void invalidClickCannotAddressAirUseInventoryOrNonFiniteCoordinates() {
        MessagePrinterAction m = new MessagePrinterAction();
        assertTrue(m.valid());
        m.side = 255; assertFalse(m.valid()); m.side = 0;
        m.slot = 9; assertFalse(m.valid()); m.slot = 0;
        m.action = 5; assertFalse(m.valid()); m.action = 3;
        m.hitX = Float.NaN; assertFalse(m.valid()); m.hitX = 0;
        m.hitY = 1.1f; assertFalse(m.valid()); m.hitY = 0;
        m.yaw = Float.POSITIVE_INFINITY; assertFalse(m.valid()); m.yaw = 0;
        m.y = 256; assertFalse(m.valid());
    }

    @Test public void movedStacksAreFoundWithoutSubstitutingDifferentBlockVariantsOrNbt() {
        Item item = new Item();
        ItemStack wanted = new ItemStack(item, 1, 2);
        wanted.setTagCompound(new NBTTagCompound());
        wanted.getTagCompound().setString("variant", "first");
        ItemStack[] inventory = new ItemStack[36];
        inventory[0] = new ItemStack(item, 64, 3);
        inventory[1] = wanted.copy();
        inventory[1].getTagCompound().setString("variant", "second");
        inventory[20] = wanted.copy();
        inventory[20].stackSize = 13;
        assertEquals(20, PrinterActions.findItem(inventory, 0, wanted));
        inventory[20] = null;
        assertEquals(-1, PrinterActions.findItem(inventory, 0, wanted));
    }

    @Test public void toolWearDoesNotInvalidateTheRemainingQueuedBreaks() {
        Item tool = new Item().setMaxDamage(250);
        ItemStack expected = new ItemStack(tool, 1, 10);
        ItemStack actual = new ItemStack(tool, 1, 25);
        assertTrue(PrinterActions.matchesItem(actual, expected));
        actual.stackSize = 0;
        assertFalse(PrinterActions.matchesItem(actual, expected));
    }

    @Test public void targetsAndIntentsCannotReachOutsideTheRequestedInteraction() {
        MessagePrinterAction m = new MessagePrinterAction();
        m.action = MessagePrinterAction.BREAK;
        assertTrue(m.valid());
        m.targetX = 2;
        assertFalse(m.valid());
        m.targetX = 0;
        m.intents.add(new MessagePlacementIntent(0, 3, 0, "minecraft:stone", 0));
        assertFalse(m.valid());
    }
}

// AE2 material counting regressions, by HackerRouter, 2026.
package com.github.lunatrius.schematica.compat;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.parts.PartItemStack;
import org.junit.Test;

import static org.junit.Assert.*;

public class AE2ItemsTest {
    private static final Item ITEM = new Item();

    public static class Host extends TileEntity implements appeng.api.parts.IPartHost {
        final Map<ForgeDirection, NativePart> parts = new EnumMap<>(ForgeDirection.class);
        final Facades facades = new Facades();
        @Override public Object getPart(ForgeDirection side) { return parts.get(side); }
        @Override public void removePart(ForgeDirection side, boolean suppressUpdate) { parts.remove(side); }
        public Facades getFacadeContainer() { return facades; }
        Host add(ForgeDirection side, int meta) { parts.put(side, new NativePart(meta)); return this; }
    }

    public static class NativePart {
        final ItemStack stack;
        boolean powered;
        NativePart(int meta) { stack = new ItemStack(ITEM, 8, meta); }
        public ItemStack getItemStack(PartItemStack context) {
            assertEquals(PartItemStack.Pick, context);
            return stack;
        }
    }

    public static class Panel extends NativePart {
        final int spin;
        Panel(int spin) { super(1); this.spin = spin; }
        public int getSpin() { return spin; }
    }

    public static class Facade {
        final ItemStack stack = new ItemStack(ITEM, 4, 10);
        Facade(String material) {
            stack.setTagCompound(new NBTTagCompound());
            stack.getTagCompound().setString("material", material);
        }
        public ItemStack getItemStack() { return stack; }
    }

    public static class Facades {
        final Map<ForgeDirection, Facade> faces = new EnumMap<>(ForgeDirection.class);
        public Facade getFacade(ForgeDirection side) {
            assertNotEquals(ForgeDirection.UNKNOWN, side);
            return faces.get(side);
        }
    }

    @Test public void countsCableSidePartsAndFacadesWithoutChangingNativeStacks() throws Exception {
        Host host = new Host().add(ForgeDirection.UNKNOWN, 0).add(ForgeDirection.NORTH, 1).add(ForgeDirection.UP, 2);
        Facade facade = new Facade("stone");
        host.facades.faces.put(ForgeDirection.WEST, facade);
        ItemStack[] items = AE2Items.items(host);
        assertEquals(4, items.length);
        for (ItemStack stack : items) assertEquals(1, stack.stackSize);
        ItemStack copiedFacade = Arrays.stream(items).filter(s -> s.getItemDamage() == 10).findFirst().get();
        assertEquals("stone", copiedFacade.getTagCompound().getString("material"));
        copiedFacade.getTagCompound().setString("material", "glass");
        assertEquals("stone", facade.stack.getTagCompound().getString("material"));
        assertEquals(4, facade.stack.stackSize);
        assertEquals(8, host.parts.get(ForgeDirection.NORTH).stack.stackSize);
    }

    @Test public void matchingCableDoesNotSatisfyMissingTerminalsOrFacades() throws Exception {
        Host expected = new Host().add(ForgeDirection.UNKNOWN, 0).add(ForgeDirection.NORTH, 1);
        expected.facades.faces.put(ForgeDirection.EAST, new Facade("stone"));
        Host actual = new Host().add(ForgeDirection.UNKNOWN, 0);
        assertEquals(2, AE2Items.missing(expected, actual, false).size());
        actual.add(ForgeDirection.NORTH, 1);
        actual.parts.get(ForgeDirection.NORTH).powered = true;
        assertEquals(1, AE2Items.missing(expected, actual, false).size());
        actual.facades.faces.put(ForgeDirection.EAST, new Facade("stone"));
        assertTrue(AE2Items.missing(expected, actual, false).isEmpty());
    }

    @Test public void ignoreStateStillRequiresEachPartAndTheCorrectFacadeMaterial() throws Exception {
        Host expected = new Host().add(ForgeDirection.NORTH, 1).add(ForgeDirection.SOUTH, 1);
        Host actual = new Host().add(ForgeDirection.EAST, 1);
        assertEquals(2, AE2Items.missing(expected, actual, false).size());
        assertEquals(1, AE2Items.missing(expected, actual, true).size());
        expected.facades.faces.put(ForgeDirection.UP, new Facade("stone"));
        actual.facades.faces.put(ForgeDirection.DOWN, new Facade("glass"));
        assertEquals(2, AE2Items.missing(expected, actual, true).size());
    }

    @Test public void panelSpinIsComparedOnlyWhenStateMatters() throws Exception {
        Host expected = new Host(), actual = new Host();
        expected.parts.put(ForgeDirection.UP, new Panel(1));
        actual.parts.put(ForgeDirection.UP, new Panel(2));
        assertEquals(1, AE2Items.missing(expected, actual, false).size());
        assertTrue(AE2Items.missing(expected, actual, true).isEmpty());
    }

    @Test public void unknownPartsCannotSilentlyProducePartialCounts() throws Exception {
        Host host = new Host().add(ForgeDirection.UNKNOWN, 0);
        host.parts.put(ForgeDirection.NORTH, new NativePart(1) {
            @Override public ItemStack getItemStack(PartItemStack context) { return null; }
        });
        try { AE2Items.items(host); fail("Expected unknown part"); }
        catch (ReflectiveOperationException expected) { assertEquals("AE2 part has no build item", expected.getMessage()); }
        assertFalse(AE2Items.supports(new TileEntity()));
        assertEquals(0, AE2Items.items(null).length);
    }
}

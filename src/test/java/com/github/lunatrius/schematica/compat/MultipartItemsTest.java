// Multipart item and orientation regression coverage, by HackerRouter, 2026.
package com.github.lunatrius.schematica.compat;

import java.util.Arrays;
import java.util.Collections;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import codechicken.multipart.TileMultipart;
import org.junit.Test;
import static org.junit.Assert.*;

public class MultipartItemsTest {
    private static final Item ITEM = new Item();

    public static class NativePart {
        final ItemStack item;
        final NBTTagCompound state = new NBTTagCompound();
        NativePart(int meta, int orient) {
            item = new ItemStack(ITEM, 8, meta);
            state.setByte("orient", (byte) orient);
            state.setByte("shape", (byte) 0);
        }
        public String getType() { return "test_gate"; }
        public ItemStack pickItem(MovingObjectPosition hit) { return item; }
        public void save(NBTTagCompound tag) {
            for (Object key : state.func_150296_c()) tag.setTag((String) key, state.getTag((String) key).copy());
        }
    }

    @Test public void countsEveryPartOnceAndPreservesNativeItemTags() throws Exception {
        NativePart first = new NativePart(0, 0), second = new NativePart(1, 4);
        first.item.setTagCompound(new NBTTagCompound());
        first.item.getTagCompound().setString("material", "stone");
        ItemStack[] items = MultipartItems.items(new TileMultipart(first, second));
        assertEquals(2, items.length);
        assertEquals(1, items[0].stackSize);
        assertEquals(1, items[1].getItemDamage());
        assertEquals("stone", items[0].getTagCompound().getString("material"));
        assertNotSame(first.item, items[0]);
        assertEquals(8, first.item.stackSize);
    }

    @Test public void unknownPartDoesNotSilentlyProduceAPartialMaterialList() throws Exception {
        NativePart unknown = new NativePart(0, 0) {
            @Override public ItemStack pickItem(MovingObjectPosition hit) { return null; }
        };
        assertNull(MultipartItems.items(new TileMultipart(new NativePart(0, 0), unknown)));
    }

    @Test public void gateFacingAndConfigurationMustMatchButRuntimeStateMayDiffer() throws Exception {
        NativePart a = new NativePart(0, 0), b = new NativePart(0, 0);
        a.state.setInteger("connMap", 123);
        b.state.setByte("state", (byte) 15);
        assertTrue(MultipartItems.describe(a).matches(MultipartItems.describe(b)));
        b.state.setByte("orient", (byte) 1);
        assertFalse(MultipartItems.describe(a).matches(MultipartItems.describe(b)));
        b.state.setByte("orient", (byte) 0);
        b.state.setByte("shape", (byte) 1);
        assertFalse(MultipartItems.describe(a).matches(MultipartItems.describe(b)));
        assertFalse(MultipartItems.describe(a).matches(MultipartItems.describe(new NativePart(1, 0))));
    }

    @Test public void oneExistingPartCannotSatisfyTwoRequestedParts() throws Exception {
        MultipartItems.Part a = MultipartItems.describe(new NativePart(0, 0));
        assertEquals(1, MultipartItems.missing(Arrays.asList(a, a), Collections.singletonList(a)).size());
        assertTrue(MultipartItems.missing(Collections.singletonList(a), Arrays.asList(a, a)).isEmpty());
    }

    @Test public void materialIgnoreStateStillRequiresEveryItem() throws Exception {
        MultipartItems.Part a = MultipartItems.describe(new NativePart(0, 0));
        MultipartItems.Part rotated = MultipartItems.describe(new NativePart(0, 1));
        MultipartItems.Part different = MultipartItems.describe(new NativePart(1, 0));
        assertTrue(MultipartItems.missing(Collections.singletonList(a), Collections.singletonList(rotated), true).isEmpty());
        assertEquals(1, MultipartItems.missing(Collections.singletonList(a), Collections.singletonList(rotated), false).size());
        assertEquals(1, MultipartItems.missing(Collections.singletonList(a), Collections.singletonList(different), true).size());
    }

    @Test public void wireAttachmentFacesAndItemNbtRemainDistinct() throws Exception {
        NativePart a = new NativePart(0, 0), b = new NativePart(0, 0);
        a.state.setByte("side", (byte) 0);
        b.state.setByte("side", (byte) 2);
        assertFalse(MultipartItems.describe(a).matches(MultipartItems.describe(b)));
        b.state.setByte("side", (byte) 0);
        b.item.setTagCompound(new NBTTagCompound());
        b.item.getTagCompound().setString("material", "glass");
        assertFalse(MultipartItems.describe(a).matches(MultipartItems.describe(b)));
    }
}

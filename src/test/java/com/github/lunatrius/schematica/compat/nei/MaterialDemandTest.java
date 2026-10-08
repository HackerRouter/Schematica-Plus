// Material demand persistence regression coverage, by HackerRouter, 2026.
package com.github.lunatrius.schematica.compat.nei;

import java.util.Arrays;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import java.lang.reflect.Field;
import net.minecraft.util.RegistryNamespaced;
import net.minecraft.util.RegistrySimple;
import net.minecraft.util.ObjectIntIdentityMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class MaterialDemandTest {
    private static final Item ITEM = new Item();

    private static Field registryField;
    private static Object previousRegistry;
    private static Map names;

    @BeforeClass public static void registerItem() throws Exception {
        registryField = RegistryNamespaced.class.getDeclaredField("underlyingIntegerMap");
        registryField.setAccessible(true);
        previousRegistry = registryField.get(Item.itemRegistry);
        ObjectIntIdentityMap ids = new ObjectIntIdentityMap();
        ids.func_148746_a(ITEM, 32000);
        registryField.set(Item.itemRegistry, ids);
        Field nameField = RegistrySimple.class.getDeclaredField("registryObjects");
        nameField.setAccessible(true);
        names = (Map) nameField.get(Item.itemRegistry);
        names.put("schematica_test:material_demand", ITEM);
    }

    @AfterClass public static void restoreRegistry() throws Exception {
        registryField.set(Item.itemRegistry, previousRegistry);
        names.remove("schematica_test:material_demand");
    }

    @Test public void preservesLargeCountsMetadataAndNativeTags() {
        ItemStack stack = new ItemStack(ITEM, Integer.MAX_VALUE, 12);
        stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setInteger("Shape", 91);
        List<ItemStack> restored = MaterialDemand.decode(MaterialDemand.encode(Arrays.asList(stack)));
        assertEquals(1, restored.size());
        assertEquals(Integer.MAX_VALUE, restored.get(0).stackSize);
        assertEquals(12, restored.get(0).getItemDamage());
        assertEquals(91, restored.get(0).getTagCompound().getInteger("Shape"));
        assertSame(ITEM, restored.get(0).getItem());
    }

    @Test public void snapshotAndRestoredStacksDoNotMutateInputs() {
        ItemStack stack = new ItemStack(ITEM, 257, 0);
        stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setString("material", "stone");
        NBTTagList saved = MaterialDemand.encode(Arrays.asList(stack));
        stack.stackSize = 1;
        stack.getTagCompound().setString("material", "wood");
        ItemStack restored = MaterialDemand.decode(saved).get(0);
        assertEquals(257, restored.stackSize);
        assertEquals("stone", restored.getTagCompound().getString("material"));
        restored.getTagCompound().setString("material", "glass");
        assertEquals("stone", MaterialDemand.decode(saved).get(0).getTagCompound().getString("material"));
    }

    @Test public void omitsInvalidOrEmptyDemands() {
        NBTTagList saved = MaterialDemand.encode(Arrays.asList(null, new ItemStack(ITEM, 0), new ItemStack(ITEM, -1)));
        assertEquals(0, saved.tagCount());
        saved.appendTag(new NBTTagCompound());
        NBTTagCompound row = new ItemStack(ITEM).writeToNBT(new NBTTagCompound());
        row.setInteger("Amount", -1);
        saved.appendTag(row);
        assertTrue(MaterialDemand.decode(saved).isEmpty());
    }

    @Test public void registryNameSurvivesChangedNumericItemIds() {
        NBTTagList saved = MaterialDemand.encode(Arrays.asList(new ItemStack(ITEM, 17)));
        saved.getCompoundTagAt(0).setShort("id", (short) 123);
        ItemStack restored = MaterialDemand.decode(saved).get(0);
        assertSame(ITEM, restored.getItem());
        assertEquals(17, restored.stackSize);
    }

    @Test public void missingNamedItemCannotBecomeAnotherItemOrAPartialPlan() {
        NBTTagList saved = MaterialDemand.encode(Arrays.asList(new ItemStack(ITEM, 17), new ItemStack(ITEM, 3)));
        saved.getCompoundTagAt(1).setString("Name", "schematica_test:missing_item");
        assertTrue(MaterialDemand.decode(saved).isEmpty());
    }
}

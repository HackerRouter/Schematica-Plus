package com.github.lunatrius.schematica.client.container;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;

import com.github.lunatrius.schematica.client.container.ContainerComparison.Source;
import com.github.lunatrius.schematica.client.container.ContainerComparison.Status;
import com.github.lunatrius.schematica.client.container.ContainerComparison.Target;

import static org.junit.Assert.*;

public class ContainerComparisonTest {
    private static final Item STONE = new Item().setHasSubtypes(true), DIRT = new Item();

    @Test public void classifiesSlots() {
        assertEquals(Status.MATCH, ContainerComparison.compare(null, null));
        assertEquals(Status.MISSING, ContainerComparison.compare(new ItemStack(STONE, 3), null));
        assertEquals(Status.EXTRA, ContainerComparison.compare(null, new ItemStack(DIRT)));
        assertEquals(Status.WRONG_ITEM, ContainerComparison.compare(new ItemStack(STONE, 1, 0), new ItemStack(DIRT)));
        assertEquals(Status.WRONG_ITEM, ContainerComparison.compare(new ItemStack(STONE, 1, 0), new ItemStack(STONE, 1, 1)));
        assertEquals(Status.MISMATCH, ContainerComparison.compare(new ItemStack(STONE, 3), new ItemStack(STONE, 2)));
        ItemStack named = new ItemStack(STONE, 3);
        named.setTagCompound(new NBTTagCompound());
        named.getTagCompound().setString("x", "y");
        assertEquals(Status.MISMATCH, ContainerComparison.compare(named, new ItemStack(STONE, 3)));
        assertEquals(Status.MATCH, ContainerComparison.compare(new ItemStack(STONE, 3), new ItemStack(STONE, 3)));
    }

    @Test public void autofillMovesWholeStacksOrSplitsAndPutsTheRestBack() {
        List<Target> targets = Arrays.asList(
            new Target(0, new ItemStack(STONE, 64), null),
            new Target(1, new ItemStack(STONE, 3), new ItemStack(STONE, 1)),
            new Target(2, new ItemStack(DIRT, 5), new ItemStack(STONE, 1)),
            new Target(3, null, null));
        List<Source> sources = Arrays.asList(new Source(30, new ItemStack(STONE, 64)), new Source(31, new ItemStack(STONE, 10)));
        List<int[]> clicks = ContainerComparison.autofill(targets, sources);
        // slot 0: the whole first stack; slot 1: two single items from the second stack, the rest goes back
        int[][] wanted = {{30, 0}, {0, 0}, {31, 0}, {1, 1}, {1, 1}, {31, 0}};
        assertEquals(wanted.length, clicks.size());
        for (int i = 0; i < wanted.length; i++) assertArrayEquals(wanted[i], clicks.get(i));
    }

    @Test public void autofillSkipsOtherDamageAndNbt() {
        ItemStack tagged = new ItemStack(STONE, 4);
        tagged.setTagCompound(new NBTTagCompound());
        List<int[]> clicks = ContainerComparison.autofill(Collections.singletonList(new Target(0, new ItemStack(STONE, 4, 0), null)),
            Arrays.asList(new Source(20, new ItemStack(STONE, 4, 1)), new Source(21, tagged)));
        assertTrue(clicks.isEmpty());
    }

    @Test public void missingSumsPerItemAndSubtractsTheKnownContents() {
        List<ItemStack> expected = Arrays.asList(new ItemStack(STONE, 64, 1), null, new ItemStack(DIRT, 10), new ItemStack(STONE, 6, 1), new ItemStack(STONE, 3, 2));
        List<ItemStack> unknown = ContainerComparison.missing(expected, null);
        assertEquals(3, unknown.size());
        assertEquals(70, unknown.get(0).stackSize);
        assertEquals(1, unknown.get(0).getItemDamage());
        assertEquals(10, unknown.get(1).stackSize);
        assertEquals(64, expected.get(0).stackSize);
        List<ItemStack> known = ContainerComparison.missing(expected, Arrays.asList(new ItemStack(STONE, 64, 1), new ItemStack(DIRT, 12), new ItemStack(STONE, 1, 1), null));
        assertEquals(2, known.size());
        assertEquals(5, known.get(0).stackSize);
        assertEquals(2, known.get(1).getItemDamage());
        assertTrue(ContainerComparison.missing(expected, Arrays.asList(new ItemStack(STONE, 70, 1), new ItemStack(DIRT, 10), new ItemStack(STONE, 3, 2))).isEmpty());
    }
}

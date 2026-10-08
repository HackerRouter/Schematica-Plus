// Persistent material demand snapshots, by HackerRouter, 2026.
package com.github.lunatrius.schematica.compat.nei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;

final class MaterialDemand {
    private static final String KEY = "SchematicaMaterials";

    private MaterialDemand() {}

    static ItemStack marker(List<ItemStack> stacks) {
        ItemStack book = new ItemStack(Items.written_book);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag(KEY, encode(stacks));
        String title = UiTranslations.format("schematica.nei.demand");
        tag.setString("title", title);
        tag.setString("author", "Schematica Plus");
        NBTTagList pages = new NBTTagList();
        pages.appendTag(new NBTTagString(UiTranslations.format("schematica.nei.demand_info")));
        tag.setTag("pages", pages);
        book.setTagCompound(tag);
        book.setStackDisplayName(title);
        return book;
    }

    static List<ItemStack> read(ItemStack book) {
        return book != null && book.getItem() == Items.written_book && book.hasTagCompound()
            ? decode(book.getTagCompound().getTagList(KEY, 10)) : new ArrayList<>();
    }

    static NBTTagList encode(List<ItemStack> stacks) {
        NBTTagList list = new NBTTagList();
        for (ItemStack stack : stacks) {
            if (stack == null || stack.getItem() == null || stack.stackSize <= 0) continue;
            ItemStack copy = stack.copy();
            copy.stackSize = 1;
            NBTTagCompound row = copy.writeToNBT(new NBTTagCompound());
            String name = Item.itemRegistry.getNameForObject(stack.getItem());
            if (name == null) throw new IllegalArgumentException("Unregistered material item");
            row.setString("Name", name);
            row.setInteger("Amount", stack.stackSize);
            list.appendTag(row);
        }
        return list;
    }

    static List<ItemStack> decode(NBTTagList list) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound row = list.getCompoundTagAt(i);
            NBTTagCompound data = (NBTTagCompound) row.copy();
            if (row.hasKey("Name")) {
                Object item = Item.itemRegistry.getObject(row.getString("Name"));
                if (!(item instanceof Item)) return new ArrayList<>();
                data.setShort("id", (short) Item.getIdFromItem((Item) item));
            }
            ItemStack stack = ItemStack.loadItemStackFromNBT(data);
            if (stack == null || stack.getItem() == null || row.getInteger("Amount") <= 0) continue;
            stack.stackSize = row.getInteger("Amount");
            stacks.add(stack);
        }
        return stacks;
    }
}

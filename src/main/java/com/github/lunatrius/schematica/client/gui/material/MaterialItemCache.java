// SPDX-License-Identifier: LGPL-3.0-only
// Litematica MaterialListItemCache (from Stormatica by CubicMetre), adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.material;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * materialListContainerScan: the items of the containers opened last, most recent first, for the CACHE_ORDER sort.
 * Items are told apart by item and damage, not NBT. Shulker boxes and bundles do not exist in 1.7.10.
 */
public final class MaterialItemCache {
    private static final LinkedHashSet<MaterialItemKey> ITEMS = new LinkedHashSet<>();

    private MaterialItemCache() {}

    static MaterialItemKey plain(ItemStack stack) {
        ItemStack copy = new ItemStack(stack.getItem(), 1, stack.getItemDamage());
        return new MaterialItemKey(copy);
    }

    public static synchronized void scan(List<?> slots) {
        List<MaterialItemKey> found = new ArrayList<>();
        for (Object object : slots) {
            ItemStack stack = object instanceof Slot ? ((Slot) object).getStack() : null;
            if (stack == null || stack.getItem() == null) continue;
            MaterialItemKey key = plain(stack);
            if (!found.contains(key)) found.add(key);
        }
        for (MaterialItemKey key : found) {
            ITEMS.remove(key);
            ITEMS.add(key);
        }
    }

    /** 0 for the item seen last, growing with age; Integer.MAX_VALUE when not seen. */
    public static synchronized int priority(MaterialItemKey key) {
        MaterialItemKey plain = plain(key.stack());
        int index = 0, found = -1;
        for (MaterialItemKey item : ITEMS) {
            if (item.equals(plain)) found = index;
            index++;
        }
        return found < 0 ? Integer.MAX_VALUE : ITEMS.size() - 1 - found;
    }

    public static synchronized void clear() { ITEMS.clear(); }
}

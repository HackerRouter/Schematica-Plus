// Native AE2 building materials and placement state, by HackerRouter, 2026.
package com.github.lunatrius.schematica.compat;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

/** Building materials from native AE2 cable hosts, without their inventories or upgrades. */
public final class AE2Items {
    private AE2Items() {}

    public static boolean supports(TileEntity tile) { return Reflect.is(tile, "appeng.api.parts.IPartHost"); }

    /** AE2's client facade stream omits the registry names present on crafted items. */
    public static NBTTagCompound materialTag(ItemStack stack) {
        NBTTagCompound tag = stack.hasTagCompound() ? (NBTTagCompound) stack.getTagCompound().copy() : null;
        if (tag == null || !Reflect.is(stack.getItem(), "appeng.items.parts.ItemFacade")) return tag;
        try {
            ItemStack texture = (ItemStack) Reflect.call(stack.getItem(), "getTextureItem", new Class<?>[] {ItemStack.class}, stack);
            if (texture == null) return tag;
            ItemStack canonical = (ItemStack) Reflect.call(stack.getItem(), "createFacadeForItem",
                new Class<?>[] {ItemStack.class, boolean.class}, texture, false);
            if (canonical != null && canonical.hasTagCompound()) {
                for (String key : new String[] {"x", "modid", "itemname"}) {
                    if (canonical.getTagCompound().hasKey(key)) tag.setTag(key, canonical.getTagCompound().getTag(key).copy());
                }
            }
        } catch (ReflectiveOperationException ignored) {}
        return tag;
    }

    private static final class Entry {
        final ItemStack stack;
        final ForgeDirection side;
        final int spin;
        final boolean facade;

        Entry(ItemStack stack, ForgeDirection side, int spin, boolean facade) throws ReflectiveOperationException {
            if (stack == null || stack.getItem() == null) throw new ReflectiveOperationException("AE2 part has no build item");
            this.stack = stack.copy();
            this.stack.stackSize = 1;
            this.stack.setTagCompound(materialTag(stack));
            this.side = side;
            this.spin = spin;
            this.facade = facade;
        }

        boolean matches(Entry other, boolean ignoreState) {
            return facade == other.facade && MultipartItems.sameItem(stack, other.stack)
                && (ignoreState || side == other.side && spin == other.spin);
        }
    }

    private static List<Entry> entries(TileEntity tile) throws ReflectiveOperationException {
        List<Entry> result = new ArrayList<>();
        if (!supports(tile)) return result;
        Class<?> context = Class.forName("appeng.api.parts.PartItemStack");
        Object pick = context.getField("Pick").get(null);
        Object facades = Reflect.call(tile, "getFacadeContainer");
        for (ForgeDirection side : ForgeDirection.values()) {
            Object part = Reflect.call(tile, "getPart", new Class<?>[] {ForgeDirection.class}, side);
            if (part != null) {
                ItemStack stack = (ItemStack) Reflect.call(part, "getItemStack", new Class<?>[] {context}, pick);
                int spin = 0;
                try { spin = ((Number) Reflect.call(part, "getSpin")).intValue(); }
                catch (NoSuchMethodException ignored) {}
                result.add(new Entry(stack, side, spin, false));
            }
            if (side == ForgeDirection.UNKNOWN || facades == null) continue;
            Object facade = Reflect.call(facades, "getFacade", new Class<?>[] {ForgeDirection.class}, side);
            if (facade != null) result.add(new Entry((ItemStack) Reflect.call(facade, "getItemStack"), side, 0, true));
        }
        return result;
    }

    public static ItemStack[] items(TileEntity tile) throws ReflectiveOperationException {
        List<Entry> entries = entries(tile);
        ItemStack[] items = new ItemStack[entries.size()];
        for (int i = 0; i < items.length; i++) items[i] = entries.get(i).stack;
        return items;
    }

    public static List<ItemStack> missing(TileEntity expected, TileEntity actual, boolean ignoreState)
        throws ReflectiveOperationException {
        List<Entry> available = entries(actual);
        List<ItemStack> missing = new ArrayList<>();
        boolean[] used = new boolean[available.size()];
        for (Entry wanted : entries(expected)) {
            int found = -1;
            for (int i = 0; i < used.length; i++) {
                if (!used[i] && wanted.matches(available.get(i), ignoreState)) { found = i; break; }
            }
            if (found < 0) missing.add(wanted.stack);
            else used[found] = true;
        }
        return missing;
    }
}

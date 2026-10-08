// Native multipart items and placement state, by HackerRouter, 2026.
package com.github.lunatrius.schematica.compat;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public final class MultipartItems {
    private MultipartItems() {}

    public static boolean supports(TileEntity tile) { return Reflect.is(tile, "codechicken.multipart.TileMultipart"); }

    public static final class Part {
        public final String type;
        public final ItemStack stack;
        public final NBTTagCompound state;

        private Part(Object part, MovingObjectPosition hit) throws ReflectiveOperationException {
            type = (String) Reflect.call(part, "getType");
            stack = (ItemStack) Reflect.call(part, "pickItem", new Class<?>[] {MovingObjectPosition.class}, hit);
            state = new NBTTagCompound();
            Reflect.call(part, "save", new Class<?>[] {NBTTagCompound.class}, state);
        }

        public boolean matches(Part other) {
            return matches(other, false);
        }

        private boolean matches(Part other, boolean ignoreOrientation) {
            return other != null && type.equals(other.type) && sameItem(stack, other.stack)
                && (ignoreOrientation || sameOrientation(state, other.state));
        }
    }

    public static Part describe(Object part) throws ReflectiveOperationException {
        return part == null ? null : new Part(part, new MovingObjectPosition(0, 0, 0, 0, Vec3.createVectorHelper(0.5, 0.5, 0.5)));
    }

    public static List<Part> parts(TileEntity tile) throws ReflectiveOperationException {
        List<Part> result = new ArrayList<>();
        if (supports(tile)) {
            MovingObjectPosition hit = new MovingObjectPosition(tile.xCoord, tile.yCoord, tile.zCoord, 0,
                Vec3.createVectorHelper(tile.xCoord + 0.5, tile.yCoord + 0.5, tile.zCoord + 0.5));
            for (Object part : (List<?>) Reflect.call(tile, "jPartList")) result.add(new Part(part, hit));
        }
        return result;
    }

    public static ItemStack[] items(TileEntity tile) throws ReflectiveOperationException {
        List<Part> parts = parts(tile);
        ItemStack[] items = new ItemStack[parts.size()];
        for (int i = 0; i < items.length; i++) {
            ItemStack stack = parts.get(i).stack;
            if (stack == null || stack.getItem() == null) return null;
            items[i] = stack.copy();
            items[i].stackSize = 1;
        }
        return items;
    }

    public static List<Part> missing(List<Part> expected, List<Part> actual) {
        return missing(expected, actual, false);
    }

    public static List<Part> missing(List<Part> expected, List<Part> actual, boolean ignoreOrientation) {
        List<Part> result = new ArrayList<>();
        boolean[] used = new boolean[actual.size()];
        for (Part part : expected) {
            int found = -1;
            for (int i = 0; i < actual.size(); i++) if (!used[i] && part.matches(actual.get(i), ignoreOrientation)) { found = i; break; }
            if (found < 0) result.add(part);
            else used[found] = true;
        }
        return result;
    }

    static boolean sameItem(ItemStack a, ItemStack b) {
        return a != null && b != null && a.isItemEqual(b) && ItemStack.areItemStackTagsEqual(a, b);
    }

    /** Connections and powered state are recomputed by the real world, not requested by placement. */
    static boolean sameOrientation(NBTTagCompound a, NBTTagCompound b) {
        for (String key : new String[] {"orient", "side", "shape"}) {
            if (a.hasKey(key) != b.hasKey(key) || !java.util.Objects.equals(a.getTag(key), b.getTag(key))) return false;
        }
        return true;
    }
}

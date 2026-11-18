package com.github.lunatrius.schematica.world.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.api.SchematicRegion;
import com.github.lunatrius.schematica.util.SchematicTransform;

public final class SchematicFluids {
    public static final String NBT = "SchematicaPlusStoredFluids";
    private static final int BITS = 21;
    private static final long MASK = (1L << BITS) - 1;

    private SchematicFluids() {}

    public static long key(int x, int y, int z) {
        return ((long) x & MASK) << (BITS * 2) | ((long) y & MASK) << BITS | (long) z & MASK;
    }

    public static int x(long key) { return (int) (key >>> (BITS * 2) & MASK); }

    public static int y(long key) { return (int) (key >>> BITS & MASK); }

    public static int z(long key) { return (int) (key & MASK); }

    public static void copy(ISchematic source, ISchematic target, int dx, int dy, int dz) {
        for (Map.Entry<Long, String> entry : new ArrayList<>(source.getStoredFluids().entrySet())) {
            long key = entry.getKey();
            target.setStoredFluid(x(key) + dx, y(key) + dy, z(key) + dz, entry.getValue());
        }
    }

    public static void copyBox(ISchematic source, ISchematic target, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        for (Map.Entry<Long, String> entry : new ArrayList<>(source.getStoredFluids().entrySet())) {
            long key = entry.getKey();
            int x = x(key), y = y(key), z = z(key);
            if (x < minX || y < minY || z < minZ || x > maxX || y > maxY || z > maxZ) continue;
            target.setStoredFluid(x - minX, y - minY, z - minZ, entry.getValue());
        }
    }

    public static void clearBox(ISchematic target, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        List<Long> keys = new ArrayList<>(target.getStoredFluids().keySet());
        for (long key : keys) {
            int x = x(key), y = y(key), z = z(key);
            if (x >= minX && y >= minY && z >= minZ && x <= maxX && y <= maxY && z <= maxZ) target.setStoredFluid(x, y, z, null);
        }
    }

    public static void transform(ISchematic source, ISchematic target, char operation) {
        int w = source.getWidth() - 1, h = source.getHeight() - 1, l = source.getLength() - 1;
        for (Map.Entry<Long, String> entry : new ArrayList<>(source.getStoredFluids().entrySet())) {
            long key = entry.getKey();
            double[] point = SchematicTransform.point(operation, x(key), y(key), z(key), w, h, l);
            target.setStoredFluid((int) Math.round(point[0]), (int) Math.round(point[1]), (int) Math.round(point[2]), entry.getValue());
        }
    }

    public static void write(NBTTagCompound tag, ISchematic schematic) {
        write(tag, schematic, null);
    }

    public static void write(NBTTagCompound tag, ISchematic schematic, SchematicRegion region) {
        if (schematic.getStoredFluids().isEmpty()) return;
        NBTTagList list = new NBTTagList();
        for (Map.Entry<Long, String> entry : schematic.getStoredFluids().entrySet()) {
            NBTTagCompound fluid = new NBTTagCompound();
            long key = entry.getKey();
            int x = x(key), y = y(key), z = z(key);
            if (region != null && !region.contains(x, y, z)) continue;
            fluid.setInteger("x", region == null ? x : x - region.minX);
            fluid.setInteger("y", region == null ? y : y - region.minY);
            fluid.setInteger("z", region == null ? z : z - region.minZ);
            fluid.setString("FluidName", entry.getValue());
            list.appendTag(fluid);
        }
        if (list.tagCount() > 0) tag.setTag(NBT, list);
    }

    public static void read(NBTTagCompound tag, ISchematic schematic) {
        NBTTagList list = tag.getTagList(NBT, 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound fluid = list.getCompoundTagAt(i);
            String name = fluid.getString("FluidName");
            if (name.isEmpty()) continue;
            int x = fluid.getInteger("x"), y = fluid.getInteger("y"), z = fluid.getInteger("z");
            if (schematic.containsBlock(x, y, z)) schematic.setStoredFluid(x, y, z, name);
        }
    }
}

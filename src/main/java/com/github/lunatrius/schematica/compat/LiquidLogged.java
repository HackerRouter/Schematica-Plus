package com.github.lunatrius.schematica.compat;

import java.lang.reflect.Method;

import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import com.github.lunatrius.schematica.reference.Reference;

public final class LiquidLogged {
    private static final String API = "com.gtnewhorizons.liquidlogged.api.LiquidLoggedAPI";
    public static final int MIN_Y = 0, MAX_Y = 255;
    private static Method get, set, canHold;
    private static boolean resolved;

    private LiquidLogged() {}

    private static synchronized boolean resolve() {
        if (!resolved) {
            resolved = true;
            try {
                Class<?> api = Class.forName(API);
                get = api.getMethod("getFluid", IBlockAccess.class, int.class, int.class, int.class);
                set = api.getMethod("setFluid", World.class, int.class, int.class, int.class, Fluid.class);
            } catch (ClassNotFoundException | LinkageError | NoSuchMethodException error) {
                get = null;
                set = null;
            }
        }
        return get != null;
    }

    public static boolean present() { return resolve(); }

    public static boolean canFill(World world, int x, int y, int z, Fluid fluid) {
        if (!resolve() || fluid == null || !storable(x, y, z)) return false;
        try {
            if (canHold == null) canHold = set.getDeclaringClass().getMethod("canHold", World.class, int.class, int.class, int.class, Fluid.class);
            return Boolean.TRUE.equals(canHold.invoke(null, world, x, y, z, fluid));
        } catch (ReflectiveOperationException | RuntimeException error) {
            return false;
        }
    }

    public static boolean storable(int x, int y, int z) { return y >= MIN_Y && y <= MAX_Y; }

    public static String fluid(IBlockAccess world, int x, int y, int z) {
        if (world == null || y < MIN_Y || y > MAX_Y || !resolve()) return null;
        try {
            Object fluid = get.invoke(null, world, x, y, z);
            return fluid instanceof Fluid ? ((Fluid) fluid).getName() : null;
        } catch (ReflectiveOperationException | RuntimeException error) {
            Reference.logger.debug("Could not read a stored fluid at {} {} {}", x, y, z, error);
            return null;
        }
    }

    public static boolean store(World world, int x, int y, int z, String name) {
        if (world == null || world.isRemote || !resolve()) return false;
        Fluid fluid = name == null ? null : FluidRegistry.getFluid(name);
        if (name != null && fluid == null) return false;
        if (y < MIN_Y || y > MAX_Y) return fluid == null;
        try {
            return Boolean.TRUE.equals(set.invoke(null, world, x, y, z, fluid));
        } catch (ReflectiveOperationException | RuntimeException error) {
            Reference.logger.debug("Could not store a fluid at {} {} {}", x, y, z, error);
            return false;
        }
    }
}

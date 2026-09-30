package com.github.lunatrius.schematica.compat;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import com.github.lunatrius.schematica.api.ISchematicVisualAdapter;

final class ThaumicExplorationVisualAdapter implements ISchematicVisualAdapter {
    private static final String CHEST = "flaxbeard.thaumicexploration.tile.TileEntityBoundChest";
    private static final String JAR = "flaxbeard.thaumicexploration.tile.TileEntityBoundJar";
    private static final String[] FIELDS = {"accessTicks", "lidAngle", "prevLidAngle", "numUsingPlayers"};
    private final Map<TileEntity, Long> animationTicks = new WeakHashMap<>();

    @Override public String id() { return "thaumicexploration:bound_storage"; }
    @Override public boolean supports(TileEntity tile) { return Reflect.is(tile, CHEST) || Reflect.is(tile, JAR); }

    @Override public NBTTagCompound capture(TileEntity tile) throws Exception {
        return captureState(tile, tile.hasWorldObj() && tile.getWorldObj().isRemote);
    }

    static NBTTagCompound captureState(Object tile, boolean client) throws ReflectiveOperationException {
        NBTTagCompound tag = new NBTTagCompound();
        VisualFields.capture(tile, tag, FIELDS);
        tag.setInteger("SealColor", client ? (Integer) Reflect.get(tile, "clientColor") : (Integer) Reflect.call(tile, "getSealColor"));
        return tag;
    }

    @Override public void restore(TileEntity tile, NBTTagCompound tag) throws Exception { restoreState(tile, tag); }

    static void restoreState(Object tile, NBTTagCompound tag) throws ReflectiveOperationException {
        int color = tag.getInteger("SealColor");
        if (color < 0 || color > 15) throw new IllegalArgumentException("Invalid bound storage color");
        VisualFields.restore(tile, tag, FIELDS);
        Reflect.field(tile.getClass(), "clientColor").setInt(tile, color);
        if (Reflect.is(tile, JAR)) Reflect.field(tile.getClass(), "colour").setInt(tile, color);
    }

    @Override public void beforeRender(TileEntity tile, float partialTicks) throws Exception {
        if (!supports(tile) || !tile.hasWorldObj()) return;
        long now = tile.getWorldObj().getTotalWorldTime();
        Long previous = animationTicks.put(tile, now);
        if (previous == null || previous == now) return;
        java.lang.reflect.Field access = Reflect.field(tile.getClass(), "accessTicks");
        access.setInt(tile, Math.max(0, access.getInt(tile) - 1));
        if (Reflect.is(tile, CHEST)) {
            float angle = (Float) Reflect.get(tile, "lidAngle");
            Reflect.field(tile.getClass(), "prevLidAngle").setFloat(tile, angle);
            float target = (Integer) Reflect.get(tile, "numUsingPlayers") > 0 ? 1 : 0;
            Reflect.field(tile.getClass(), "lidAngle").setFloat(tile, angle + Math.max(-0.1f, Math.min(0.1f, target - angle)));
        }
    }
}

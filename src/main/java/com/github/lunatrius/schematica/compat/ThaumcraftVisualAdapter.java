// Thaumcraft display state its tiles only work out while ticking, by HackerRouter, 2026.
package com.github.lunatrius.schematica.compat;

import java.lang.reflect.Array;

import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import com.github.lunatrius.schematica.api.ISchematicVisualAdapter;

/**
 * Thaumcraft 4.2.3.5 renderers read fields that the client tile settles in updateEntity, which preview tiles never
 * run: the infusion matrix's start-up spin, a closed tube valve's handle, the arcane bore's focus, the colors of
 * essentia reservoirs and crystallizers, and the grown petals of an ethereal bloom. They are set to their settled
 * values from the saved state before each frame.
 */
final class ThaumcraftVisualAdapter implements ISchematicVisualAdapter {
    private static final String TILES = "thaumcraft.common.tiles.";
    private static final String MATRIX = TILES + "TileInfusionMatrix", VALVE = TILES + "TileTubeValve", BORE = TILES + "TileArcaneBore",
        RESERVOIR = TILES + "TileEssentiaReservoir", CRYSTALLIZER = TILES + "TileEssentiaCrystalizer", BLOOM = TILES + "TileEtherealBloom";
    private static final String[] TYPES = {MATRIX, VALVE, BORE, RESERVOIR, CRYSTALLIZER, BLOOM};

    @Override public String id() { return "thaumcraft:settled"; }

    @Override public boolean supports(TileEntity tile) {
        if (tile == null || !tile.getClass().getName().startsWith("thaumcraft.")) return false;
        for (String type : TYPES) if (Reflect.is(tile, type)) return true;
        return false;
    }

    @Override public NBTTagCompound capture(TileEntity tile) { return null; }
    @Override public void restore(TileEntity tile, NBTTagCompound data) {}
    @Override public boolean replacesDescriptionPacket(TileEntity tile) { return false; }

    @Override public void beforeRender(TileEntity tile, float partialTicks) throws Exception {
        if (!supports(tile)) return;
        if (Reflect.is(tile, MATRIX)) {
            set(tile, "startUp", (Boolean) Reflect.get(tile, "active") ? 1.0F : 0.0F);
        } else if (Reflect.is(tile, VALVE)) {
            set(tile, "rotation", (Boolean) Reflect.get(tile, "allowFlow") ? 0.0F : 360.0F);
        } else if (Reflect.is(tile, BORE)) {
            Reflect.field(tile.getClass(), "hasFocus").setBoolean(tile, ((IInventory) tile).getStackInSlot(0) != null);
        } else if (Reflect.is(tile, RESERVOIR)) {
            Object aspects = Reflect.call(Reflect.get(tile, "essentia"), "getAspects", new Class<?>[0]);
            int count = aspects == null ? 0 : Array.getLength(aspects);
            Object shown = count == 0 ? null : Array.get(aspects, (int) (System.currentTimeMillis() / 1000 % count));
            Reflect.field(tile.getClass(), "displayAspect").set(tile, shown);
            color(tile, shown, 255.0F);
        } else if (Reflect.is(tile, CRYSTALLIZER)) {
            color(tile, Reflect.get(tile, "aspect"), 220.0F);
        } else if (Reflect.is(tile, BLOOM)) {
            java.lang.reflect.Field growth = Reflect.field(tile.getClass(), "growthCounter");
            if (growth.getInt(tile) < 100) growth.setInt(tile, 100);
        }
    }

    private static void set(TileEntity tile, String name, float value) throws ReflectiveOperationException {
        Reflect.field(tile.getClass(), name).setFloat(tile, value);
    }

    /** cr/cg/cb as the tile would fade them to: the aspect's color over `scale`, white without one. */
    private static void color(TileEntity tile, Object aspect, float scale) throws ReflectiveOperationException {
        int color = aspect == null ? 0xFFFFFF : (Integer) Reflect.call(aspect, "getColor", new Class<?>[0]);
        float r = aspect == null ? 1.0F : (color >> 16 & 0xFF) / scale, g = aspect == null ? 1.0F : (color >> 8 & 0xFF) / scale,
            b = aspect == null ? 1.0F : (color & 0xFF) / scale;
        set(tile, "cr", r);
        set(tile, "cg", g);
        set(tile, "cb", b);
    }
}

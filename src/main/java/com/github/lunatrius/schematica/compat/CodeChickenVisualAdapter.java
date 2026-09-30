package com.github.lunatrius.schematica.compat;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import com.github.lunatrius.schematica.api.ISchematicVisualAdapter;

final class CodeChickenVisualAdapter implements ISchematicVisualAdapter {
    private static final String TANK = "codechicken.enderstorage.storage.liquid.TileEnderTank";
    private static final String CHEST = "codechicken.enderstorage.storage.item.TileEnderChest";
    private static final String TRANSLOCATOR = "codechicken.translocator.TileTranslocator";
    private static final String GRID = "codechicken.translocator.TileCraftingGrid";
    private static final String IRON_CHEST = "cpw.mods.ironchest.TileEntityIronChest";
    private static final String[] ROOT = {"rotation", "lidAngle", "prevLidAngle", "c_numOpen", "items", "result", "topStacks", "numUsingPlayers"};
    private static final String[] LIQUID = {"s_liquid", "c_liquid", "f_liquid"};
    private static final String[] PRESSURE = {"a_pressure", "b_pressure", "a_rotate", "b_rotate"};
    private static final String[] ATTACHMENT = {"a_eject", "b_eject", "a_insertpos", "b_insertpos", "redstone", "fast",
        "regulate", "signal", "a_powering", "b_powering"};
    private final Map<TileEntity, Long> animationTicks = new WeakHashMap<>();

    @Override public String id() { return "codechicken:visual_fields"; }
    @Override public boolean supports(TileEntity tile) {
        return Reflect.is(tile, TANK) || Reflect.is(tile, CHEST) || Reflect.is(tile, TRANSLOCATOR)
            || Reflect.is(tile, GRID) || Reflect.is(tile, IRON_CHEST);
    }

    @Override public NBTTagCompound capture(TileEntity tile) throws Exception {
        NBTTagCompound tag = new NBTTagCompound();
        transfer(tile, tag, false);
        return tag;
    }

    @Override public void restore(TileEntity tile, NBTTagCompound tag) throws Exception { transfer(tile, tag, true); }

    private void transfer(TileEntity tile, NBTTagCompound tag, boolean restore) throws ReflectiveOperationException {
        fields(tile, tag, "Tile", ROOT, restore);
        if (Reflect.is(tile, TANK)) {
            fields(Reflect.get(tile, "liquid_state"), tag, "Liquid", LIQUID, restore);
            fields(Reflect.get(tile, "pressure_state"), tag, "Pressure", PRESSURE, restore);
        } else if (Reflect.is(tile, TRANSLOCATOR)) {
            Object[] attachments = (Object[]) Reflect.get(tile, "attachments");
            for (int side = 0; side < attachments.length; side++) {
                if (attachments[side] != null) fields(attachments[side], tag, "Side" + side, ATTACHMENT, restore);
            }
        }
    }

    private static void fields(Object target, NBTTagCompound tag, String key, String[] names, boolean restore)
        throws ReflectiveOperationException {
        if (restore) VisualFields.restore(target, tag.getCompoundTag(key), names);
        else {
            NBTTagCompound data = new NBTTagCompound();
            VisualFields.capture(target, data, names);
            tag.setTag(key, data);
        }
    }

    @Override public void beforeRender(TileEntity tile, float partialTicks) throws Exception {
        if (!supports(tile) || !tile.hasWorldObj()) return;
        long now = tile.getWorldObj().getTotalWorldTime();
        Long previous = animationTicks.put(tile, now);
        if (previous == null || previous == now) return;
        if (Reflect.is(tile, TANK)) {
            Reflect.call(Reflect.get(tile, "pressure_state"), "update", new Class<?>[] {boolean.class}, true);
            Reflect.call(Reflect.get(tile, "liquid_state"), "update", new Class<?>[] {boolean.class}, true);
        } else if (Reflect.is(tile, CHEST) || Reflect.is(tile, IRON_CHEST)) {
            float angle = (Float) Reflect.get(tile, "lidAngle");
            Reflect.field(tile.getClass(), "prevLidAngle").setFloat(tile, angle);
            float target = (Integer) Reflect.get(tile, Reflect.is(tile, CHEST) ? "c_numOpen" : "numUsingPlayers") > 0 ? 1 : 0;
            Reflect.field(tile.getClass(), "lidAngle").setFloat(tile, angle + Math.max(-0.1f, Math.min(0.1f, target - angle)));
        } else if (Reflect.is(tile, TRANSLOCATOR)) {
            for (Object attachment : (Object[]) Reflect.get(tile, "attachments")) {
                if (attachment != null) Reflect.call(attachment, "update", new Class<?>[] {boolean.class}, true);
            }
        }
    }
}

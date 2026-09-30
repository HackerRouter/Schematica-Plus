package com.github.lunatrius.schematica.compat;

import java.lang.reflect.Field;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import com.github.lunatrius.schematica.api.ISchematicVisualAdapter;

final class MalisisVisualAdapter implements ISchematicVisualAdapter {
    private static final String[] FIELDS = {"state", "moving", "powered", "centered"};
    @Override public String id() { return "malisisdoors:animation"; }
    @Override public boolean supports(TileEntity tile) { return Reflect.is(tile, "net.malisis.doors.door.tileentity.DoorTileEntity"); }
    @Override public NBTTagCompound capture(TileEntity tile) throws Exception {
        NBTTagCompound tag = new NBTTagCompound();
        VisualFields.capture(tile, tag, FIELDS);
        if ((Boolean) Reflect.get(tile, "moving")) {
            tag.setLong("Elapsed", Math.max(0, Math.min(60000, (Long) Reflect.call(Reflect.get(tile, "timer"), "elapsedTime"))));
        }
        return tag;
    }
    @Override public void restore(TileEntity tile, NBTTagCompound tag) throws Exception {
        VisualFields.restore(tile, tag, FIELDS);
        Reflect.call(Reflect.get(tile, "timer"), "setRelativeStart", new Class<?>[] {long.class},
            -Math.max(0, Math.min(60000, tag.getLong("Elapsed"))));
    }
    @Override public void beforeRender(TileEntity tile, float partialTicks) throws Exception {
        if (!supports(tile) || !(Boolean) Reflect.get(tile, "moving")) return;
        long ticks = (Long) Reflect.call(Reflect.get(tile, "timer"), "elapsedTick");
        if (ticks <= (Integer) Reflect.call(tile, "getOpeningTime")) return;
        Field state = Reflect.field(tile.getClass(), "state");
        String target = ((Enum<?>) state.get(tile)).name().equals("CLOSING") ? "CLOSED" : "OPENED";
        for (Object value : state.getType().getEnumConstants()) if (((Enum<?>) value).name().equals(target)) state.set(tile, value);
        Reflect.field(tile.getClass(), "moving").setBoolean(tile, false);
    }
}

package com.github.lunatrius.schematica.compat;

import java.lang.reflect.Field;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import com.github.lunatrius.schematica.api.ISchematicVisualAdapter;

final class BinnieVisualAdapter implements ISchematicVisualAdapter {
    private static final String MACHINE = "binnie.core.machines.TileEntityMachine";
    private static final String FLOWER = "binnie.botany.flower.TileEntityFlower";
    private static final String[] FLOWER_FLAGS = {"age", "section", "wilted", "flowered"};
    private static final String[] FLOWER_ENUMS = {"primary", "secondary", "stem", "type"};

    @Override public String id() { return "binnie:visual_state"; }
    @Override public boolean supports(TileEntity tile) { return Reflect.is(tile, MACHINE) || Reflect.is(tile, FLOWER); }

    @Override public NBTTagCompound capture(TileEntity tile) throws Exception {
        NBTTagCompound tag = new NBTTagCompound();
        if (Reflect.is(tile, MACHINE)) {
            Object machine = Reflect.call(tile, "getMachine");
            if (machine != null) Reflect.call(machine, "syncToNBT", new Class<?>[] {NBTTagCompound.class}, tag);
        } else {
            Object render = Reflect.get(tile, "renderInfo");
            if (render == null) {
                Object flower = Reflect.call(tile, "getFlower");
                if (flower == null || Reflect.call(flower, "getGenome") == null) return null;
                ClassLoader loader = tile.getClass().getClassLoader();
                render = Reflect.field(tile.getClass(), "renderInfo").getType()
                    .getConstructor(Class.forName("binnie.botany.api.IFlower", false, loader),
                        Reflect.type(tile.getClass(), FLOWER)).newInstance(flower, tile);
            }
            VisualFields.capture(render, tag, FLOWER_FLAGS);
            for (String name : FLOWER_ENUMS) {
                Object value = Reflect.get(render, name);
                if (value instanceof Enum<?>) tag.setString(name, ((Enum<?>) value).name());
            }
        }
        return tag;
    }

    @Override public void restore(TileEntity tile, NBTTagCompound tag) throws Exception {
        if (Reflect.is(tile, MACHINE)) {
            Object machine = Reflect.call(tile, "getMachine");
            if (machine != null) Reflect.call(machine, "syncFromNBT", new Class<?>[] {NBTTagCompound.class}, tag);
        } else {
            Field field = Reflect.field(tile.getClass(), "renderInfo");
            Object render = field.getType().getConstructor().newInstance();
            VisualFields.restore(render, tag, FLOWER_FLAGS);
            for (String name : FLOWER_ENUMS) {
                String enumClass = "binnie.botany.genetics." + (name.equals("type") ? "EnumFlowerType" : "EnumFlowerColor");
                Object[] values = Class.forName(enumClass, false, tile.getClass().getClassLoader()).getEnumConstants();
                Object selected = null;
                for (Object value : values) if (((Enum<?>) value).name().equals(tag.getString(name))) selected = value;
                if (selected == null) throw new IllegalArgumentException("Unknown Binnie flower appearance");
                Reflect.field(render.getClass(), name).set(render, selected);
            }
            Reflect.call(tile, "setRender", new Class<?>[] {field.getType()}, render);
        }
    }
}

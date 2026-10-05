package com.github.lunatrius.schematica.compat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import com.github.lunatrius.schematica.api.ISchematicVisualAdapter;

final class GalacticraftVisualAdapter implements ISchematicVisualAdapter {
    private static final String VECTOR = "micdoodle8.mods.galacticraft.api.vector.BlockVec3";
    private static final String ENERGY = "micdoodle8.mods.galacticraft.core.energy.tile.EnergyStorage";
    private static final String ANNOTATION = "micdoodle8.mods.galacticraft.core.util.Annotations$NetworkedField";
    private static final String[] ENERGY_FIELDS = {"energy", "capacity", "maxReceive", "maxExtract", "maxExtractRemaining"};

    @Override public String id() { return "galacticraft:networked_fields"; }
    @Override public boolean supports(TileEntity tile) {
        return Reflect.is(tile, "micdoodle8.mods.galacticraft.core.tile.TileEntityAdvanced");
    }

    private boolean clientField(Field field) throws ReflectiveOperationException {
        for (Annotation annotation : field.getAnnotations()) {
            if (annotation.annotationType().getName().equals(ANNOTATION)) {
                Object side = annotation.annotationType().getMethod("targetSide").invoke(annotation);
                return side instanceof Enum<?> && ((Enum<?>) side).name().equals("CLIENT");
            }
        }
        return false;
    }

    @Override public NBTTagCompound capture(TileEntity tile) throws Exception {
        NBTTagCompound tag = new NBTTagCompound();
        for (Field field : tile.getClass().getFields()) {
            if (!clientField(field)) continue;
            Object value = field.get(tile);
            if (field.getType().getName().equals(VECTOR)) {
                NBTTagCompound vector = new NBTTagCompound();
                if (value == null) vector.setBoolean("Null", true);
                else {
                    int x = (Integer) Reflect.get(value, "x"), y = (Integer) Reflect.get(value, "y"), z = (Integer) Reflect.get(value, "z");
                    vector.setBoolean("Invalid", x == -1 && y == -1 && z == -1);
                    vector.setInteger("x", x - tile.xCoord);
                    vector.setInteger("y", y - tile.yCoord);
                    vector.setInteger("z", z - tile.zCoord);
                }
                tag.setTag(VisualFields.key(field), vector);
            } else if (field.getType().getName().equals(ENERGY)) {
                NBTTagCompound energy = new NBTTagCompound();
                if (value != null) VisualFields.capture(value, energy, ENERGY_FIELDS);
                tag.setTag(VisualFields.key(field), energy);
            } else VisualFields.capture(tile, field, tag);
        }
        return tag;
    }

    @Override public void restore(TileEntity tile, NBTTagCompound tag) throws Exception {
        for (Field field : tile.getClass().getFields()) {
            if (!clientField(field) || !tag.hasKey(VisualFields.key(field), 10)) continue;
            NBTTagCompound value = tag.getCompoundTag(VisualFields.key(field));
            if (field.getType().getName().equals(VECTOR)) {
                boolean invalid = value.getBoolean("Invalid");
                field.set(tile, value.getBoolean("Null") ? null : field.getType().getConstructor(int.class, int.class, int.class)
                    .newInstance(invalid ? -1 : value.getInteger("x") + tile.xCoord,
                        invalid ? -1 : value.getInteger("y") + tile.yCoord, invalid ? -1 : value.getInteger("z") + tile.zCoord));
            } else if (field.getType().getName().equals(ENERGY)) {
                Object energy = field.get(tile);
                if (energy != null) VisualFields.restore(energy, value, ENERGY_FIELDS);
            } else VisualFields.restore(tile, field, tag);
        }
    }

    private static final String[] SOLAR = {"micdoodle8.mods.galacticraft.core.tile.TileEntitySolar",
        "galaxyspace.core.tile.machine.TileEntitySolarPanel", "galaxyspace.core.tile.machine.TileEntitySolarWind"};

    /** Solar panels turn toward the sun only while ticking; the preview shows them where they would settle now. */
    @Override public void beforeRender(TileEntity tile, float partialTicks) throws Exception {
        String type = null;
        for (String solar : SOLAR) if (Reflect.is(tile, solar)) type = solar;
        if (type == null) return;
        net.minecraft.world.World world = ClientWorld.current();
        if (world == null) return;
        boolean simple = type == SOLAR[0] && ((Number) Reflect.get(tile, "tierGC")).intValue() == 1;
        Reflect.field(tile.getClass(), "currentAngle").setFloat(tile, solarAngle(world.getCelestialAngle(1.0F),
            world.isDaytime() && !world.isRaining() && !world.isThundering(), simple));
    }

    /** The angle TileEntitySolar.updateEntity converges to for a celestial angle (0-1) and weather. */
    static float solarAngle(float celestial, boolean clearDay, boolean simple) {
        if (simple) return clearDay ? 77.5F : 257.5F;
        float angle = (celestial + (celestial - 0.7845194F < 0 ? 1.0F - 0.7845194F : -0.7845194F)) * 360.0F % 360.0F;
        if (angle > 30 && angle < 150) return angle;
        if (!clearDay) return 257.5F;
        return angle < 50 ? 50.0F : 150.0F;
    }
}

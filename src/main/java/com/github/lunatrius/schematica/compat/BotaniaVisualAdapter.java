// Botania mana spreader aim and turntable direction turned with the schematic, by HackerRouter, 2026.
package com.github.lunatrius.schematica.compat;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.Constants.NBT;

import com.github.lunatrius.schematica.api.ISchematicVisualAdapter;
import com.github.lunatrius.schematica.util.SchematicTransform;

/**
 * A mana spreader aims by rotationX/rotationY; its burst flies along (sin a, sin p, -cos a) with a = -(rotationX +
 * 90) and p = rotationY (EntityManaBurst). That vector is turned and the angles recomputed. A turntable spins its
 * spreader the other way when mirrored on x or z.
 */
final class BotaniaVisualAdapter implements ISchematicVisualAdapter {
    static final String SPREADER = "vazkii.botania.common.block.tile.mana.TileSpreader", TURNTABLE = "vazkii.botania.common.block.tile.mana.TileTurntable";

    static void spreader(NBTTagCompound tag, char operation) {
        if (!tag.hasKey("rotationX", NBT.TAG_FLOAT) || !tag.hasKey("rotationY", NBT.TAG_FLOAT)) return;
        double yaw = Math.toRadians(-(tag.getFloat("rotationX") + 90)), pitch = Math.toRadians(tag.getFloat("rotationY"));
        double[] d = SchematicTransform.point(operation, Math.sin(yaw) * Math.cos(pitch), Math.sin(pitch), -Math.cos(yaw) * Math.cos(pitch), 0, 0, 0);
        double horizontal = Math.hypot(d[0], d[2]);
        double newPitch = Math.toDegrees(Math.atan2(d[1], horizontal));
        double newYaw = horizontal < 1e-9 ? yaw : Math.atan2(d[0], -d[2]);
        double rotationX = -Math.toDegrees(newYaw) - 90;
        rotationX = (rotationX % 360 + 360) % 360;
        tag.setFloat("rotationX", (float) round(rotationX));
        tag.setFloat("rotationY", (float) round(newPitch));
    }

    private static double round(double angle) { return Math.round(angle * 1000) / 1000.0; }

    static void turntable(NBTTagCompound tag, char operation) {
        if ((operation == 'x' || operation == 'z') && tag.hasKey("backwards", NBT.TAG_BYTE)) tag.setBoolean("backwards", !tag.getBoolean("backwards"));
    }

    static void transform(TileEntity tile, NBTTagCompound tag, char operation) {
        if (Reflect.is(tile, SPREADER)) spreader(tag, operation);
        else turntable(tag, operation);
    }

    @Override public String id() { return "plus:botania"; }
    @Override public boolean supports(TileEntity tile) { return Reflect.is(tile, SPREADER) || Reflect.is(tile, TURNTABLE); }
    @Override public NBTTagCompound capture(TileEntity tile) { return null; }
    @Override public void restore(TileEntity tile, NBTTagCompound data) {}
    @Override public boolean replacesDescriptionPacket(TileEntity tile) { return false; }
    @Override public boolean transformsNBT(TileEntity tile) { return true; }
    @Override public void transformNBT(TileEntity tile, NBTTagCompound data, char operation) { transform(tile, data, operation); }

    @Override public void transformPreview(TileEntity tile, char operation) {
        NBTTagCompound data = new NBTTagCompound();
        tile.writeToNBT(data);
        transform(tile, data, operation);
        tile.readFromNBT(data);
    }
}

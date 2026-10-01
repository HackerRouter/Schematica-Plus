package com.github.lunatrius.schematica.handler.client;

import java.util.function.BooleanSupplier;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;

final class PickBlockInput {
    private PickBlockInput() {}

    static void dispatch(KeyBinding key, BooleanSupplier pick) {
        if (!key.isPressed()) return;
        boolean handled = false;
        try { handled = pick.getAsBoolean(); }
        finally { if (!handled) KeyBinding.onTick(key.getKeyCode()); }
    }

    static boolean schematicFirst(MovingObjectPosition schematic, MovingObjectPosition world, Vec3 eye, int x, int y, int z) {
        if (schematic == null || schematic.typeOfHit != MovingObjectType.BLOCK || schematic.hitVec == null || eye == null) return false;
        if (world == null || world.typeOfHit == MovingObjectType.MISS) return true;
        if (world.typeOfHit == MovingObjectType.BLOCK && world.blockX == schematic.blockX + x
            && world.blockY == schematic.blockY + y && world.blockZ == schematic.blockZ + z) return false;
        if (world.hitVec == null) return false;
        Vec3 point = schematic.hitVec.addVector(x, y, z);
        return eye.squareDistanceTo(point) + 1.0E-7 < eye.squareDistanceTo(world.hitVec);
    }
}

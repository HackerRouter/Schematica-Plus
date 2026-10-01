package com.github.lunatrius.schematica.handler.client;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.junit.Test;
import static org.junit.Assert.*;

public class PickBlockInputTest {
    private static MovingObjectPosition block(int x, int y, int z) {
        return new MovingObjectPosition(x, y, z, 4, Vec3.createVectorHelper(x, y + 0.5, z + 0.5));
    }

    @Test public void absentSchematicHitLeavesExactlyOneVanillaPickEvenAcrossRepeatedInputEvents() {
        KeyBinding key = new KeyBinding("test.pick", -98, "test");
        KeyBinding.onTick(key.getKeyCode());
        for (int i = 0; i < 3; i++) PickBlockInput.dispatch(key,
            () -> PickBlockInput.schematicFirst(null, block(2, 64, 0), Vec3.createVectorHelper(0, 64.5, 0.5), 0, 0, 0));
        assertTrue(key.isPressed());
        assertFalse(key.isPressed());
    }

    @Test public void onlySuccessfulSchematicPickConsumesInput() {
        KeyBinding key = new KeyBinding("test.pick", -98, "test");
        PickBlockInput.dispatch(key, () -> { fail("No input must not invoke the pick hook"); return true; });
        KeyBinding.onTick(key.getKeyCode());
        PickBlockInput.dispatch(key, () -> true);
        assertFalse(key.isPressed());
        KeyBinding.onTick(key.getKeyCode());
        PickBlockInput.dispatch(key, () -> false);
        assertTrue(key.isPressed()); assertFalse(key.isPressed());
    }

    @Test public void throwingModPickHookRestoresInputWithoutDuplicatingIt() {
        KeyBinding key = new KeyBinding("test.pick", -98, "test");
        KeyBinding.onTick(key.getKeyCode()); KeyBinding.onTick(key.getKeyCode());
        assertThrows(IllegalStateException.class, () -> PickBlockInput.dispatch(key, () -> { throw new IllegalStateException("mod hook"); }));
        assertTrue(key.isPressed()); assertTrue(key.isPressed()); assertFalse(key.isPressed());
    }

    @Test public void missAndNonBlockSchematicHitsFallBackToTheWorld() {
        Vec3 eye = Vec3.createVectorHelper(0, 64.5, 0.5);
        MovingObjectPosition miss = new MovingObjectPosition(1, 64, 0, -1, eye, false);
        assertFalse(PickBlockInput.schematicFirst(miss, block(2, 64, 0), eye, 0, 0, 0));
        assertFalse(PickBlockInput.schematicFirst(new MovingObjectPosition(null, eye), block(2, 64, 0), eye, 0, 0, 0));
        assertFalse(PickBlockInput.schematicFirst(block(1, 64, 0), null, null, 0, 0, 0));
        assertTrue(PickBlockInput.schematicFirst(block(1, 64, 0), null, eye, 0, 0, 0));
        assertTrue(PickBlockInput.schematicFirst(block(1, 64, 0), miss, eye, 0, 0, 0));
    }

    @Test public void nearestTargetUsesWorldCoordinatesForShiftedNegativePlacements() {
        Vec3 eye = Vec3.createVectorHelper(-32, 64.5, -16.5);
        MovingObjectPosition schematic = block(3, 0, 0);
        assertFalse(PickBlockInput.schematicFirst(schematic, block(-30, 64, -17), eye, -32, 64, -17));
        assertTrue(PickBlockInput.schematicFirst(schematic, block(-28, 64, -17), eye, -32, 64, -17));
        assertFalse(PickBlockInput.schematicFirst(schematic, block(-29, 64, -17), eye, -32, 64, -17));
        MovingObjectPosition entity = new MovingObjectPosition(null, Vec3.createVectorHelper(-30, 64.5, -16.5));
        assertFalse(PickBlockInput.schematicFirst(schematic, entity, eye, -32, 64, -17));
        entity.hitVec = Vec3.createVectorHelper(-28, 64.5, -16.5);
        assertTrue(PickBlockInput.schematicFirst(schematic, entity, eye, -32, 64, -17));
    }

    @Test public void coincidentRealBlockAlwaysKeepsVanillaPickAndEqualDistanceDoesNotOverrideIt() {
        Vec3 eye = Vec3.createVectorHelper(0, 64.5, 0.5);
        MovingObjectPosition real = block(3, 64, 0);
        real.hitVec = Vec3.createVectorHelper(4, 64.5, 0.5);
        assertFalse(PickBlockInput.schematicFirst(block(3, 0, 0), real, eye, 0, 64, 0));
        assertFalse(PickBlockInput.schematicFirst(block(3, 0, 0), block(-3, 64, 0), eye, 0, 64, 0));
    }
}

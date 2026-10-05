package com.github.lunatrius.schematica.compat;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

import static org.junit.Assert.*;

public class BotaniaVisualAdapterTest {
    private static float[] turned(float x, float y, char operation) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setFloat("rotationX", x);
        tag.setFloat("rotationY", y);
        BotaniaVisualAdapter.spreader(tag, operation);
        return new float[] {tag.getFloat("rotationX"), tag.getFloat("rotationY")};
    }

    /** The burst direction of EntityManaBurst. */
    private static double[] aim(float x, float y) {
        double yaw = Math.toRadians(-(x + 90)), pitch = Math.toRadians(y);
        return new double[] {Math.sin(yaw) * Math.cos(pitch), Math.sin(pitch), -Math.cos(yaw) * Math.cos(pitch)};
    }

    @Test public void theBurstDirectionTurnsWithTheSchematic() {
        // rotationX 270: yaw -360, the burst flies north (0, 0, -1); turned Y it flies east (+x)
        float[] east = turned(270, 0, 'Y');
        double[] d = aim(east[0], east[1]);
        assertEquals(1, d[0], 1e-6);
        assertEquals(0, d[2], 1e-6);
        // aimed north and 30 degrees up, mirrored on z: south and still up
        float[] south = turned(270, 30, 'z');
        d = aim(south[0], south[1]);
        assertEquals(0, d[0], 1e-6);
        assertTrue(d[2] > 0.8);
        assertEquals(30, south[1], 1e-3);
        // mirrored on y the pitch flips
        assertEquals(-30, turned(123, 30, 'y')[1], 1e-3);
        // four quarter turns come back
        float[] angles = {37, 12};
        for (int i = 0; i < 4; i++) angles = turned(angles[0], angles[1], 'Y');
        assertEquals(37, angles[0], 1e-2);
        assertEquals(12, angles[1], 1e-2);
    }

    @Test public void turntablesSpinTheOtherWayWhenMirrored() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("backwards", false);
        BotaniaVisualAdapter.turntable(tag, 'x');
        assertTrue(tag.getBoolean("backwards"));
        BotaniaVisualAdapter.turntable(tag, 'Y');
        BotaniaVisualAdapter.turntable(tag, 'y');
        assertTrue(tag.getBoolean("backwards"));
    }
}

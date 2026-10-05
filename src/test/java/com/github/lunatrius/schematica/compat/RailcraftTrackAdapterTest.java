package com.github.lunatrius.schematica.compat;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

import static org.junit.Assert.*;

public class RailcraftTrackAdapterTest {
    private static boolean turned(String key, boolean reversed, int newShape, char operation) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean(key, reversed);
        RailcraftTrackAdapter.transform(tag, newShape, operation);
        return tag.getBoolean(key);
    }

    @Test public void forwardFollowsTheTurn() {
        // north-south pointing north, turned Y: now east-west pointing east -> not reversed
        assertFalse(turned("direction", false, 1, 'Y'));
        // east-west pointing east, turned Y: north-south pointing south -> reversed
        assertTrue(turned("direction", false, 0, 'Y'));
        // mirrored along its own axis it points the other way; across it, it stays
        assertTrue(turned("reversed", false, 0, 'z'));
        assertFalse(turned("reversed", false, 0, 'x'));
        assertTrue(turned("reversed", false, 1, 'x'));
        // curves and vertical turns are left alone
        assertFalse(turned("direction", false, 6, 'Y'));
        assertFalse(turned("direction", false, 0, 'X'));
    }

    @Test public void switchesChangeHandsWhenMirrored() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("Direction", false);
        RailcraftTrackAdapter.transform(tag, 1, 'x');
        assertTrue(tag.getBoolean("Direction"));
        RailcraftTrackAdapter.transform(tag, 0, 'Y');
        assertTrue(tag.getBoolean("Direction"));
    }
}

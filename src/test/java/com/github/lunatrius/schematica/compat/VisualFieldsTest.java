package com.github.lunatrius.schematica.compat;

import java.util.EnumSet;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import org.junit.Test;
import static org.junit.Assert.*;

public class VisualFieldsTest {
    private static class State {
        int flags = 19;
        final int[] channels = {8, 16, 0, 32, 0, 0};
        short[] power = {1, 200, 30000};
        boolean active = true;
        ForgeDirection facing = ForgeDirection.WEST;
        EnumSet<ForgeDirection> connections = EnumSet.of(ForgeDirection.DOWN, ForgeDirection.SOUTH);
        Object world = new Object();
        String label;
    }

    @Test public void roundTripsClientStateWithoutSharingMutableValues() throws Exception {
        State source = new State();
        NBTTagCompound tag = new NBTTagCompound();
        String[] fields = {"flags", "channels", "power", "active", "facing", "connections", "label", "world"};
        VisualFields.capture(source, tag, fields);
        assertFalse(tag.hasKey(State.class.getName() + "#world"));
        State target = new State();
        int[] originalArray = target.channels;
        target.channels[0] = 0;
        target.connections.clear();
        target.active = false;
        target.facing = ForgeDirection.UP;
        target.label = "old";
        VisualFields.restore(target, tag, fields);
        assertSame(originalArray, target.channels);
        assertArrayEquals(source.channels, target.channels);
        assertArrayEquals(source.power, target.power);
        assertEquals(source.connections, target.connections);
        assertTrue(target.active);
        assertEquals(ForgeDirection.WEST, target.facing);
        assertNull(target.label);
        target.connections.clear();
        target.channels[0] = 99;
        assertEquals(2, source.connections.size());
        assertEquals(8, source.channels[0]);
    }

    @Test public void rejectsTypeConfusionWithoutWritingTheField() throws Exception {
        State state = new State();
        NBTTagCompound tag = new NBTTagCompound();
        VisualFields.capture(state, tag, "flags");
        tag.getCompoundTag(State.class.getName() + "#flags").setString("Type", "java.lang.Object");
        assertThrows(IllegalArgumentException.class, () -> VisualFields.restore(state, tag, "flags"));
        assertEquals(19, state.flags);
    }

    @Test public void rejectsUnknownEnumAndMissingPayload() throws Exception {
        State state = new State();
        NBTTagCompound tag = new NBTTagCompound();
        VisualFields.capture(state, tag, "facing", "active");
        tag.getCompoundTag(State.class.getName() + "#facing").setString("Value", "OTHER");
        tag.getCompoundTag(State.class.getName() + "#active").removeTag("Value");
        assertThrows(IllegalArgumentException.class, () -> VisualFields.restore(state, tag, "facing"));
        assertThrows(IllegalArgumentException.class, () -> VisualFields.restore(state, tag, "active"));
        assertEquals(ForgeDirection.WEST, state.facing);
        assertTrue(state.active);
    }

    @Test public void keepsUnlistedStateAndToleratesFieldsAbsentInOlderMods() throws Exception {
        State state = new State();
        Object world = state.world;
        NBTTagCompound tag = new NBTTagCompound();
        VisualFields.capture(state, tag, "flags", "futureField");
        NBTTagCompound hostile = new NBTTagCompound();
        hostile.setString("Type", "java.lang.Object");
        tag.setTag(State.class.getName() + "#world", hostile);
        VisualFields.restore(state, tag, "flags", "futureField");
        assertSame(world, state.world);
    }
}

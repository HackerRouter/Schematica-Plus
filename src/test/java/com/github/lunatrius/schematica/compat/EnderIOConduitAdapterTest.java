package com.github.lunatrius.schematica.compat;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import org.junit.Test;
import static org.junit.Assert.*;

public class EnderIOConduitAdapterTest {
    @Test public void rotatesConnectionsModesFiltersAndSignalsTogether() {
        NBTTagCompound tag = conduit();
        EnderIOConduitAdapter.transformConduitNBT(tag, 'Y');
        assertArrayEquals(new int[] {5, 3}, tag.getIntArray("connections"));
        assertArrayEquals(new int[] {2}, tag.getIntArray("externalConnections"));
        assertArrayEquals(new byte[] {0, 1, 4, 5, 3, 2}, tag.getByteArray("conModes"));
        assertEquals("north", tag.getCompoundTag("inFilts.EAST").getString("filter"));
        assertFalse(tag.hasKey("inFilts.NORTH"));
        assertEquals(8, tag.getInteger("priority.SOUTH"));
        assertEquals(32, tag.getInteger("roundRobin"));
        assertEquals(9000, tag.getInteger("energyStoredRF"));
    }

    @Test public void everyRotationAndMirrorRoundTripsAllDirectionalData() {
        NBTTagCompound original = conduit();
        for (char operation : "XYZxyz".toCharArray()) {
            NBTTagCompound tag = (NBTTagCompound) original.copy();
            for (int i = 0; i < (Character.isUpperCase(operation) ? 4 : 2); i++) {
                EnderIOConduitAdapter.transformConduitNBT(tag, operation);
            }
            assertEquals("operation " + operation, original, tag);
        }
    }

    @Test public void movesLiveSideCollectionsWithoutReplacingFiltersOrTouchingStaticData() throws Exception {
        ConduitFields conduit = new ConduitFields();
        Object filter = new Object();
        conduit.filters.put(ForgeDirection.NORTH, filter);
        conduit.signals.put(ForgeDirection.WEST, 7);
        EnderIOConduitAdapter.transformSideCollections(conduit, ConduitFields.class, 'Y');
        assertEquals(EnumSet.of(ForgeDirection.EAST, ForgeDirection.SOUTH), conduit.connections);
        assertSame(filter, conduit.filters.get(ForgeDirection.EAST));
        assertEquals(Integer.valueOf(7), conduit.signals.get(ForgeDirection.NORTH));
        assertEquals(EnumSet.of(ForgeDirection.NORTH), ConduitFields.STATIC_SIDES);
        assertEquals(Integer.valueOf(42), conduit.other.get("NORTH"));
    }

    private static NBTTagCompound conduit() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setIntArray("connections", new int[] {2, 5});
        tag.setIntArray("externalConnections", new int[] {4});
        for (String name : new String[] {"conModes", "forcedConnections", "signalColors", "signalStrengths"}) {
            tag.setByteArray(name, new byte[] {0, 1, 2, 3, 4, 5});
        }
        NBTTagCompound filter = new NBTTagCompound();
        filter.setString("filter", "north");
        tag.setTag("inFilts.NORTH", filter);
        tag.setInteger("priority.EAST", 8);
        tag.setShort("pRsMode.WEST", (short) 2);
        tag.setShort("extSC.SOUTH", (short) 9);
        tag.setInteger("roundRobin", 4);
        tag.setInteger("energyStoredRF", 9000);
        return tag;
    }

    private static final class ConduitFields {
        private static final Set<ForgeDirection> STATIC_SIDES = EnumSet.of(ForgeDirection.NORTH);
        private final Set<ForgeDirection> connections = EnumSet.of(ForgeDirection.NORTH, ForgeDirection.EAST);
        private final Map<ForgeDirection, Object> filters = new EnumMap<>(ForgeDirection.class);
        private final Map<ForgeDirection, Integer> signals = new HashMap<>();
        private final Map<String, Integer> other = new HashMap<>();

        private ConduitFields() { other.put("NORTH", 42); }
    }
}

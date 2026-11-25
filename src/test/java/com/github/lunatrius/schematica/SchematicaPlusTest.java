package com.github.lunatrius.schematica;

import java.util.Arrays;
import java.util.Collections;

import cpw.mods.fml.relauncher.Side;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.github.lunatrius.schematica.reference.Reference;

public class SchematicaPlusTest {
    @Test public void rejectsLegacyIdsWithoutRejectingPlusOrOtherMods() {
        assertEquals("Schematica", SchematicaPlus.findLegacyMod(Arrays.asList("gregtech", "Schematica", "schematica_plus")));
        assertEquals("schematica", SchematicaPlus.findLegacyMod(Arrays.asList("schematica_plus", "schematica")));
        assertNull(SchematicaPlus.findLegacyMod(Arrays.asList("schematica_plus", "LunatriusCore", "gregtech")));
        assertNull(SchematicaPlus.findLegacyMod(Collections.emptyList()));
    }

    @Test public void handshakeAllowsAnyRemoteVersionIncludingOlderClients() {
        for (String version : Arrays.asList("1.0.0-beta.3", "1.0.0-beta.5", "1.0.0", "db05cb5-dirty", "different-build")) {
            assertTrue(version, SchematicaPlus.acceptsRemoteMods("1.0.0-beta.5", Collections.singletonMap(Reference.MODID, version), Side.CLIENT));
            assertTrue(version, SchematicaPlus.acceptsRemoteMods("1.0.0-beta.5", Collections.singletonMap(Reference.MODID, version), Side.SERVER));
        }
        assertTrue(SchematicaPlus.acceptsRemoteMods("db05cb5", Collections.emptyMap(), Side.CLIENT));
        assertTrue(SchematicaPlus.acceptsRemoteMods("db05cb5", Collections.singletonMap("other_mod", "0.1.0"), Side.SERVER));
    }

    @Test public void protocolCompatibilityRequiresClientVersionAtLeastServerVersion() {
        assertTrue(SchematicaPlus.isClientCompatibleWithServer("db05cb5", "db05cb5"));
        assertTrue(SchematicaPlus.isClientCompatibleWithServer("1.0.0-beta.4", "1.0.0-beta.4"));
        assertTrue(SchematicaPlus.isClientCompatibleWithServer("1.0.0-beta.4", "1.0.0-beta.5"));
        assertTrue(SchematicaPlus.isClientCompatibleWithServer("1.0.0-beta.4", "1.0.0"));
        assertTrue(SchematicaPlus.isClientCompatibleWithServer("1.0.0-beta.5", "1.0.0-beta.5-master.10+abc"));
        for (String version : Arrays.asList("1.0.0-beta.3", "1.0.0.beta.2", "db05cb5-dirty", "different-build", "", null)) {
            assertFalse(String.valueOf(version), SchematicaPlus.isClientCompatibleWithServer("1.0.0-beta.4", version));
        }
    }
}

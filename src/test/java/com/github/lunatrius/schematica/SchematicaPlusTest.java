package com.github.lunatrius.schematica;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

import com.github.lunatrius.schematica.reference.Reference;

import cpw.mods.fml.relauncher.Side;

public class SchematicaPlusTest {
    @Test public void rejectsLegacyIdsWithoutRejectingPlusOrOtherMods() {
        assertEquals("Schematica", SchematicaPlus.findLegacyMod(Arrays.asList("gregtech", "Schematica", "schematica_plus")));
        assertEquals("schematica", SchematicaPlus.findLegacyMod(Arrays.asList("schematica_plus", "schematica")));
        assertNull(SchematicaPlus.findLegacyMod(Arrays.asList("schematica_plus", "LunatriusCore", "gregtech")));
        assertNull(SchematicaPlus.findLegacyMod(Collections.emptyList()));
    }

    @Test public void acceptsMatchingDevelopmentVersions() {
        assertTrue(SchematicaPlus.acceptsRemoteMods("db05cb5", Collections.singletonMap(Reference.MODID, "db05cb5"), Side.CLIENT));
        assertTrue(SchematicaPlus.acceptsRemoteMods("db05cb5-dirty", Collections.singletonMap(Reference.MODID, "db05cb5-dirty"), Side.CLIENT));
    }

    @Test public void acceptsSupportedReleaseClients() {
        for (String version : Arrays.asList("1.0.0-beta.1", "1.0.0-beta.2", "1.0.0.beta.2", "1.0.0", "1.1.0")) {
            assertTrue(version, SchematicaPlus.acceptsRemoteMods("db05cb5", Collections.singletonMap(Reference.MODID, version), Side.CLIENT));
        }
    }

    @Test public void rejectsUnsupportedClientVersions() {
        for (String version : Arrays.asList("0.9.0", "1.0.0-alpha.1", "1.0.0-beta.0", "different-build")) {
            assertFalse(version, SchematicaPlus.acceptsRemoteMods("db05cb5", Collections.singletonMap(Reference.MODID, version), Side.CLIENT));
        }
    }

    @Test public void allowsClientsWithoutPlus() {
        assertTrue(SchematicaPlus.acceptsRemoteMods("db05cb5", Collections.emptyMap(), Side.CLIENT));
        assertTrue(SchematicaPlus.acceptsRemoteMods("db05cb5", Collections.singletonMap("other_mod", "0.1.0"), Side.CLIENT));
    }

    @Test public void allowsServersRegardlessOfVersion() {
        for (String version : Arrays.asList("0.9.0", "1.0.0-beta.1", "different-build")) {
            assertTrue(version, SchematicaPlus.acceptsRemoteMods("db05cb5", Collections.singletonMap(Reference.MODID, version), Side.SERVER));
        }
        assertTrue(SchematicaPlus.acceptsRemoteMods("db05cb5", Collections.emptyMap(), Side.SERVER));
    }
}

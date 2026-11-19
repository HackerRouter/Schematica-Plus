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

    @Test public void serverRequiresClientVersionAtLeastServerVersion() {
        assertTrue(SchematicaPlus.acceptsRemoteMods("db05cb5", Collections.singletonMap(Reference.MODID, "db05cb5"), Side.CLIENT));
        assertTrue(SchematicaPlus.acceptsRemoteMods("1.0.0-beta.4", Collections.singletonMap(Reference.MODID, "1.0.0-beta.4"), Side.CLIENT));
        assertTrue(SchematicaPlus.acceptsRemoteMods("1.0.0-beta.4", Collections.singletonMap(Reference.MODID, "1.0.0-beta.5"), Side.CLIENT));
        assertTrue(SchematicaPlus.acceptsRemoteMods("1.0.0-beta.4", Collections.singletonMap(Reference.MODID, "1.0.0"), Side.CLIENT));
        assertTrue(SchematicaPlus.acceptsRemoteMods("1.0.0-beta.5", Collections.singletonMap(Reference.MODID, "1.0.0-beta.5-master.10+abc"), Side.CLIENT));
        for (String version : Arrays.asList("1.0.0-beta.3", "1.0.0.beta.2", "db05cb5-dirty", "different-build")) {
            assertFalse(version, SchematicaPlus.acceptsRemoteMods("1.0.0-beta.4", Collections.singletonMap(Reference.MODID, version), Side.CLIENT));
        }
    }

    @Test public void clientAcceptsServerVersionAtMostClientVersion() {
        assertTrue(SchematicaPlus.acceptsRemoteMods("1.0.0-beta.5", Collections.singletonMap(Reference.MODID, "1.0.0-beta.5"), Side.SERVER));
        assertTrue(SchematicaPlus.acceptsRemoteMods("1.0.0-beta.5", Collections.singletonMap(Reference.MODID, "1.0.0-beta.4"), Side.SERVER));
        assertTrue(SchematicaPlus.acceptsRemoteMods("1.0.0", Collections.singletonMap(Reference.MODID, "1.0.0-beta.5"), Side.SERVER));
        assertTrue(SchematicaPlus.acceptsRemoteMods("1.0.0-beta.5-master.10+abc", Collections.singletonMap(Reference.MODID, "1.0.0-beta.5"), Side.SERVER));
        for (String version : Arrays.asList("1.0.0-beta.6", "1.0.0", "1.1.0", "db05cb5-dirty", "different-build")) {
            assertFalse(version, SchematicaPlus.acceptsRemoteMods("1.0.0-beta.5", Collections.singletonMap(Reference.MODID, version), Side.SERVER));
        }
    }

    @Test public void allowsPeersWithoutPlus() {
        assertTrue(SchematicaPlus.acceptsRemoteMods("db05cb5", Collections.emptyMap(), Side.CLIENT));
        assertTrue(SchematicaPlus.acceptsRemoteMods("db05cb5", Collections.singletonMap("other_mod", "0.1.0"), Side.SERVER));
    }
}

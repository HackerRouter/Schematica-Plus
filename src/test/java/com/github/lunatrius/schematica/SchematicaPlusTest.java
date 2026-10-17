package com.github.lunatrius.schematica;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

import com.github.lunatrius.schematica.reference.Reference;


public class SchematicaPlusTest {
    @Test public void rejectsLegacyIdsWithoutRejectingPlusOrOtherMods() {
        assertEquals("Schematica", SchematicaPlus.findLegacyMod(Arrays.asList("gregtech", "Schematica", "schematica_plus")));
        assertEquals("schematica", SchematicaPlus.findLegacyMod(Arrays.asList("schematica_plus", "schematica")));
        assertNull(SchematicaPlus.findLegacyMod(Arrays.asList("schematica_plus", "LunatriusCore", "gregtech")));
        assertNull(SchematicaPlus.findLegacyMod(Collections.emptyList()));
    }

    @Test public void acceptsOnlyTheSameVersionOnEitherSide() {
        assertTrue(SchematicaPlus.acceptsRemoteMods("db05cb5", Collections.singletonMap(Reference.MODID, "db05cb5")));
        assertTrue(SchematicaPlus.acceptsRemoteMods("1.0.0-beta.4", Collections.singletonMap(Reference.MODID, "1.0.0-beta.4")));
        for (String version : Arrays.asList("1.0.0-beta.3", "1.0.0", "db05cb5-dirty", "different-build")) {
            assertFalse(version, SchematicaPlus.acceptsRemoteMods("1.0.0-beta.4", Collections.singletonMap(Reference.MODID, version)));
        }
    }

    @Test public void allowsPeersWithoutPlus() {
        assertTrue(SchematicaPlus.acceptsRemoteMods("db05cb5", Collections.emptyMap()));
        assertTrue(SchematicaPlus.acceptsRemoteMods("db05cb5", Collections.singletonMap("other_mod", "0.1.0")));
    }
}

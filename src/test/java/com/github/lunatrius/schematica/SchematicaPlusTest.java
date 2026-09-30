package com.github.lunatrius.schematica;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public class SchematicaPlusTest {
    @Test public void rejectsLegacyIdsWithoutRejectingPlusOrOtherMods() {
        assertEquals("Schematica", SchematicaPlus.findLegacyMod(Arrays.asList("gregtech", "Schematica", "schematica_plus")));
        assertEquals("schematica", SchematicaPlus.findLegacyMod(Arrays.asList("schematica_plus", "schematica")));
        assertNull(SchematicaPlus.findLegacyMod(Arrays.asList("schematica_plus", "LunatriusCore", "gregtech")));
        assertNull(SchematicaPlus.findLegacyMod(Collections.emptyList()));
    }
}

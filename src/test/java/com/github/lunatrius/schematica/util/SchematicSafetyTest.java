package com.github.lunatrius.schematica.util;

import java.io.File;
import java.io.IOException;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class SchematicSafetyTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void rejectsEscapingNamesAndAcceptsUnicode() throws IOException {
        File directory = temporary.newFolder();
        for (String name : new String[] {"../other.schematic", "..\\other.schematic", "/absolute", "C:stream", ""}) {
            assertThrows(IOException.class, () -> FileUtils.resolveSchematicFile(directory, name));
        }
        assertEquals(new File(directory, "建筑.schematic").getCanonicalFile(),
            FileUtils.resolveSchematicFile(directory, "建筑.schematic"));
    }

    @Test public void checksDimensionsBeforeNarrowingOrAllocating() {
        assertEquals(4096, SchematicLimits.volume(16, 16, 16));
        assertThrows(IllegalArgumentException.class, () -> SchematicLimits.dimension(Integer.MIN_VALUE, Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> SchematicLimits.volume(1024, 256, 1024));
        assertThrows(IllegalArgumentException.class, () -> SchematicLimits.volume(0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> SchematicLimits.worldBounds(0, 255, 0, 1, 256, 1));
        SchematicLimits.worldBounds(-1, 0, -1, 0, 255, 0);
    }
}

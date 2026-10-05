package com.github.lunatrius.schematica.world.schematic;

import java.util.Arrays;
import java.util.ArrayList;

import org.junit.Test;

import static org.junit.Assert.*;

public class ImportReportTest {
    @Test public void keepsTheFirstReplacementPerNameAndOnlyTheLastDecode() {
        ImportReport.begin();
        ImportReport.unknownBlock("mod:a", "minecraft:air");
        ImportReport.unknownBlock("mod:b[x=1]", "minecraft:stone");
        ImportReport.unknownBlock("mod:a", "minecraft:stone");
        ImportReport.end();
        assertEquals(Arrays.asList("mod:a", "mod:b[x=1]"), new ArrayList<>(ImportReport.last().keySet()));
        assertEquals("minecraft:air", ImportReport.last().get("mod:a"));
        ImportReport.unknownBlock("mod:c", "minecraft:air");
        ImportReport.begin();
        ImportReport.end();
        assertTrue(ImportReport.last().isEmpty());
    }
}

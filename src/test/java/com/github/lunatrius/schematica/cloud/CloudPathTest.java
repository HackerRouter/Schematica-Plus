package com.github.lunatrius.schematica.cloud;

import java.io.File;

import org.junit.Test;

import static org.junit.Assert.*;

public class CloudPathTest {
    @Test public void parsesScopesAndRejectsEscapes() {
        CloudPath path = CloudPath.parse("public/Builds/house.litematic");
        assertEquals(CloudPath.PUBLIC, path.scope);
        assertEquals("house.litematic", path.name());
        assertEquals("Builds/house.litematic", path.relative());
        assertEquals(CloudPath.parse("public/Builds"), path.parent());
        assertTrue(path.within(CloudPath.root(CloudPath.PUBLIC)));
        assertFalse(path.within(CloudPath.root(CloudPath.PRIVATE)));
        assertEquals(new File(new File(new File("base"), "Builds"), "house.litematic"), path.resolve(new File("base")));
        for (String bad : new String[] {"other/a.litematic", "public/../a.litematic", "public/./a", "public/a/b\\..", "public/.hidden",
            "public/a:b", "public/CON.schematic", "public//a", "public/name ", ""}) {
            assertThrows(bad, IllegalArgumentException.class, () -> CloudPath.parse(bad));
        }
    }

    @Test public void recognisesSchematicNames() {
        assertTrue(CloudPath.schematicName("a.litematic"));
        assertTrue(CloudPath.schematicName("b.SCHEM"));
        assertFalse(CloudPath.schematicName(".litematic"));
        assertFalse(CloudPath.schematicName("notes.txt"));
        StringBuilder deep = new StringBuilder("private");
        for (int i = 0; i <= CloudPath.MAX_DEPTH; i++) deep.append("/d");
        assertThrows(IllegalArgumentException.class, () -> CloudPath.parse(deep.toString()));
    }
}

package com.github.lunatrius.schematica.client.gui.save;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

public class SchematicSaveTargetTest {

    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void normalizesKnownSuffixesToSelectedFormatWithoutDoubling() {
        assertEquals("工厂 2.schemplus", SchematicSaveTarget.filename(" 工厂 2 ", true));
        assertEquals("Test.schemplus", SchematicSaveTarget.filename("Test.SCHEMATIC", true));
        assertEquals("Test.schematic", SchematicSaveTarget.filename("Test.SCHEMPLUS", false));
        assertEquals("rev.2.schematic", SchematicSaveTarget.filename("rev.2", false));
    }

    @Test public void rejectsPathsIconSyntaxReservedNamesAndUnsupportedExport() {
        for (String input : new String[] {"", "..", ".schematic", "../secret", "C:\\target", "a/b", "a\\b",
            "CON", "aux.txt", "LPT1", "file.", "a?b", "a\nb\nc", "icon;file", "world.litematic"}) {
            try {
                SchematicSaveTarget.filename(input, true);
                fail("Accepted " + input);
            } catch (IllegalArgumentException expected) {}
        }
    }

    @Test public void resolvingExistingTargetNeverWritesOrCreatesFiles() throws IOException {
        File root = temporary.newFolder("schematics");
        File folder = new File(root, "子目录");
        Files.createDirectory(folder.toPath());
        File existing = new File(folder, "test.schemplus");
        Files.write(existing.toPath(), new byte[] {42});
        assertEquals(existing.getCanonicalFile(), SchematicSaveTarget.resolve(root, folder, "test", true));
        assertArrayEquals(new byte[] {42}, Files.readAllBytes(existing.toPath()));
        File next = SchematicSaveTarget.resolve(root, folder, "next", false);
        assertFalse(next.exists());
        assertEquals(1, folder.list().length);
    }

    @Test public void rejectsSiblingDirectoryAndDirectoryAsOutputFile() throws IOException {
        File root = temporary.newFolder("schematics");
        File sibling = temporary.newFolder("schematics-other");
        Files.createDirectory(new File(root, "folder.schemplus").toPath());
        for (File directory : new File[] {sibling, new File(root, "missing"), root}) {
            try {
                SchematicSaveTarget.resolve(root, directory, "folder", true);
                fail("Accepted invalid target in " + directory);
            } catch (IOException expected) {}
        }
    }
}

package com.github.lunatrius.schematica.client.gui.browser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.FileAlreadyExistsException;
import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.junit.Assert.*;

public class SchematicFileOperationsTest {

    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    private SchematicBrowserModel browser(File root) throws IOException {
        SchematicBrowserModel model = new SchematicBrowserModel(root);
        model.refresh();
        return model;
    }

    private File write(File root, String name, byte[] bytes) throws IOException {
        return Files.write(new File(root, name).toPath(), bytes).toFile();
    }

    @Test public void copyAndRenamePreserveAllThreeFormatsByteForByte() throws IOException {
        for (String extension : Arrays.asList(".schematic", ".schemplus", ".LITEMATIC")) {
            File root = temporary.newFolder();
            byte[] bytes = {0, 1, -1, 42, 99};
            File original = write(root, "original" + extension, bytes);
            SchematicBrowserModel model = browser(root);
            File copy = model.copy(model.entries().get(0), "副本" + extension);
            assertArrayEquals(bytes, Files.readAllBytes(copy.toPath()));
            File renamed = model.rename(model.entries().get(0), "renamed" + extension);
            assertFalse(original.exists());
            assertArrayEquals(bytes, Files.readAllBytes(renamed.toPath()));
            assertEquals(2, root.list().length);
        }
    }

    @Test public void neverOverwritesExistingTargetsOrChangesFormat() throws IOException {
        File root = temporary.newFolder();
        write(root, "a.schematic", new byte[] {1});
        File existing = write(root, "b.schematic", new byte[] {2});
        SchematicBrowserModel model = browser(root);
        for (boolean copy : new boolean[] {false, true}) {
            try {
                if (copy) model.copy(model.entries().get(0), existing.getName());
                else model.rename(model.entries().get(0), existing.getName());
                fail("Overwrote target");
            } catch (FileAlreadyExistsException expected) {}
            try {
                if (copy) model.copy(model.entries().get(0), "wrong.litematic");
                else model.rename(model.entries().get(0), "wrong.litematic");
                fail("Changed extension");
            } catch (SchematicBrowserModel.FileOperationException expected) {
                assertTrue(expected.translationKey.endsWith("extension"));
            }
        }
        assertArrayEquals(new byte[] {2}, Files.readAllBytes(existing.toPath()));
        assertEquals(2, root.list().length);
    }

    @Test public void rejectsUnsafeNamesAndOnlyCreatesOneDirectoryLevel() throws IOException {
        File root = temporary.newFolder();
        SchematicBrowserModel model = browser(root);
        for (String name : Arrays.asList("", " ", ".", "..", "../outside", "a/b", "a\\b", "C:test", "a?b",
            "a*", "a\"", "a|b", "a<b", "a>b", "a\u0000b", "CON", "con.txt", "LPT1", "a.", "a ")) {
            try {
                model.createDirectory(name);
                fail(name);
            } catch (SchematicBrowserModel.FileOperationException expected) {}
        }
        File created = model.createDirectory("蓝图 folder");
        assertTrue(created.isDirectory());
        model.navigate(created);
        assertTrue(model.createDirectory("sub").isDirectory());
        try {
            model.createDirectory("sub");
            fail("Reused existing folder");
        } catch (FileAlreadyExistsException expected) {}
    }

    @Test public void changedOrStaleSelectionsCannotBeMutated() throws IOException {
        File root = temporary.newFolder();
        File file = write(root, "a.schematic", new byte[] {1});
        SchematicBrowserModel model = browser(root);
        SchematicBrowserModel.Entry selected = model.entries().get(0);
        Files.write(file.toPath(), new byte[] {1, 2});
        try {
            model.delete(selected);
            fail("Deleted changed file");
        } catch (SchematicBrowserModel.FileOperationException expected) {
            assertTrue(expected.translationKey.endsWith("changed"));
        }
        model.navigate(model.createDirectory("sub"));
        try {
            model.rename(selected, "b.schematic");
            fail("Renamed stale selection");
        } catch (SchematicBrowserModel.FileOperationException expected) {}
        assertTrue(file.exists());
    }

    @Test public void deleteOnlyRemovesTheSelectedFileAndRefusesDirectories() throws IOException {
        File root = temporary.newFolder();
        File first = write(root, "a.schematic", new byte[] {1});
        File second = write(root, "b.schematic", new byte[] {2});
        SchematicBrowserModel model = browser(root);
        model.delete(model.entries().get(0));
        assertFalse(first.exists());
        assertTrue(second.exists());
        model.createDirectory("sub");
        model.refresh();
        try {
            model.delete(model.entries().get(0));
            fail("Deleted a directory");
        } catch (SchematicBrowserModel.FileOperationException expected) {}
    }

    @Test public void renameUpdatesEveryMatchingSessionWithoutChangingPlacementState() throws IOException {
        File root = temporary.newFolder();
        write(root, "a.schemplus", new byte[] {9});
        File index = write(root, "LoadedSchematics.json", ("{\"server@0\":[{\"filename\":\"a.schemplus\","
            + "\"directory\":\"\",\"displayName\":\"custom\",\"transforms\":[\"RX\"],\"X\":42,\"future\":true}],"
            + "\"server@1\":[{\"filename\":\"a.schemplus\"},{\"filename\":\"other.schemplus\"}]}")
            .getBytes(StandardCharsets.UTF_8));
        SchematicBrowserModel model = browser(root);
        model.rename(model.entries().get(0), "new.schemplus");
        JsonObject sessions = new JsonParser().parse(new String(Files.readAllBytes(index.toPath()), StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject entry = sessions.getAsJsonArray("server@0").get(0).getAsJsonObject();
        assertEquals("new.schemplus", entry.get("filename").getAsString());
        assertEquals(root.getAbsolutePath(), entry.get("directory").getAsString());
        assertEquals("custom", entry.get("displayName").getAsString());
        assertEquals(42, entry.get("X").getAsInt());
        assertTrue(entry.get("future").getAsBoolean());
        assertEquals("RX", entry.getAsJsonArray("transforms").get(0).getAsString());
        JsonArray other = sessions.getAsJsonArray("server@1");
        assertEquals("new.schemplus", other.get(0).getAsJsonObject().get("filename").getAsString());
        assertEquals("other.schemplus", other.get(1).getAsJsonObject().get("filename").getAsString());
    }

    @Test public void invalidSessionIndexPreventsRenameAndPreservesBothFiles() throws IOException {
        File root = temporary.newFolder();
        File original = write(root, "a.schematic", new byte[] {1});
        byte[] invalid = "broken {".getBytes(StandardCharsets.UTF_8);
        File index = write(root, "LoadedSchematics.json", invalid);
        SchematicBrowserModel model = browser(root);
        try {
            model.rename(model.entries().get(0), "b.schematic");
            fail("Ignored invalid index");
        } catch (SchematicBrowserModel.FileOperationException expected) {
            assertTrue(expected.translationKey.endsWith("index"));
        }
        assertTrue(original.exists());
        assertFalse(new File(root, "b.schematic").exists());
        assertArrayEquals(invalid, Files.readAllBytes(index.toPath()));
    }

    @Test public void refusesToOverwriteAnIndexChangedAfterPreparation() throws IOException {
        File root = temporary.newFolder();
        File source = write(root, "a.schematic", new byte[] {1});
        File index = write(root, "LoadedSchematics.json", "{}".getBytes(StandardCharsets.UTF_8));
        SchematicSourceIndex update = SchematicSourceIndex.prepare(root, source, new File(root, "b.schematic"));
        byte[] changed = "{\"new\":[]}".getBytes(StandardCharsets.UTF_8);
        Files.write(index.toPath(), changed);
        try {
            update.save();
            fail("Overwrote updated index");
        } catch (IOException expected) {}
        assertArrayEquals(changed, Files.readAllBytes(index.toPath()));
    }
}

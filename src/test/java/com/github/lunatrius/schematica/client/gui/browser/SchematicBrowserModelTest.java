package com.github.lunatrius.schematica.client.gui.browser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.lunatrius.schematica.client.gui.framework.UiListModel;

import static org.junit.Assert.*;

public class SchematicBrowserModelTest {

    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void listsFoldersFirstAndAllThreeFormatsWithoutParsingFileContents() throws IOException {
        File root = temporary.newFolder("schematics");
        Files.createDirectory(new File(root, "z-folder").toPath());
        for (String name : new String[] {"b.SCHEMPLUS", "a.schematic", "世界.litematic", "ignored.json", "save.tmp"}) {
            Files.write(new File(root, name).toPath(), new byte[] {42});
        }
        SchematicBrowserModel browser = new SchematicBrowserModel(root);
        browser.refresh();
        List<String> names = browser.entries().stream().map(SchematicBrowserModel.Entry::name).collect(Collectors.toList());
        assertEquals(java.util.Arrays.asList("z-folder", "a.schematic", "b.SCHEMPLUS", "世界.litematic"), names);
        assertTrue(browser.entries().get(0).directory);
        assertEquals(new File(root, "a.schematic"), browser.readableFile(browser.entries().get(1)));
        assertFalse(browser.canGoUp());
    }

    @Test public void navigationCannotLeaveConfiguredRootAndUpStopsAtRoot() throws IOException {
        File root = temporary.newFolder("schematics");
        File child = new File(root, "sub");
        Files.createDirectory(child.toPath());
        SchematicBrowserModel browser = new SchematicBrowserModel(root);
        browser.navigate(child);
        assertEquals("/sub", browser.relativeDirectory());
        browser.up();
        browser.up();
        assertEquals(root.getCanonicalFile(), browser.directory());
        try {
            browser.navigate(temporary.getRoot());
            fail("Escaped root");
        } catch (IOException expected) {
            assertEquals(root.getCanonicalFile(), browser.directory());
        }
    }

    @Test public void refreshReplacesSelectedMetadataAndDeletedFileCannotBeLoaded() throws IOException {
        File root = temporary.newFolder("schematics");
        File file = new File(root, "test.schematic");
        Files.write(file.toPath(), new byte[] {1});
        SchematicBrowserModel browser = new SchematicBrowserModel(root);
        browser.refresh();
        UiListModel<SchematicBrowserModel.Entry> list = new UiListModel<>(20, SchematicBrowserModel.Entry::label);
        list.setEntries(browser.entries());
        list.select(0);
        Files.write(file.toPath(), new byte[] {1, 2, 3});
        browser.refresh();
        list.setEntries(browser.entries());
        assertEquals(3, list.selected().size);
        Files.delete(file.toPath());
        try {
            browser.readableFile(list.selected());
            fail("Read deleted file");
        } catch (IOException expected) {}
        browser.refresh();
        list.setEntries(browser.entries());
        assertNull(list.selected());
    }

    @Test public void removedDirectoryClearsStaleEntriesAndReportsFailure() throws IOException {
        File root = temporary.newFolder("schematics");
        File child = new File(root, "sub");
        Files.createDirectory(child.toPath());
        File file = new File(child, "test.schematic");
        Files.write(file.toPath(), new byte[] {1});
        SchematicBrowserModel browser = new SchematicBrowserModel(root);
        browser.navigate(child);
        Files.delete(file.toPath());
        Files.delete(child.toPath());
        try {
            browser.refresh();
            fail("Read removed directory");
        } catch (IOException expected) {
            assertTrue(browser.entries().isEmpty());
        }
        browser.up();
        assertEquals(root.getCanonicalFile(), browser.directory());
    }

    @Test public void hiddenSelectionIsNotAnActionTargetAfterFilteringOrNavigation() throws IOException {
        File root = temporary.newFolder("schematics");
        Files.write(new File(root, "first.schematic").toPath(), new byte[] {1});
        Files.createDirectory(new File(root, "sub").toPath());
        SchematicBrowserModel browser = new SchematicBrowserModel(root);
        browser.refresh();
        UiListModel<SchematicBrowserModel.Entry> list = new UiListModel<>(20, SchematicBrowserModel.Entry::label);
        list.setEntries(browser.entries());
        list.select(1);
        SchematicBrowserModel.Entry previous = list.selected();
        list.setQuery("sub");
        assertEquals(-1, list.selectedIndex());
        browser.navigate(new File(root, "sub"));
        try {
            browser.readableFile(previous);
            fail("Loaded stale selection from previous folder");
        } catch (IOException expected) {}
    }
}

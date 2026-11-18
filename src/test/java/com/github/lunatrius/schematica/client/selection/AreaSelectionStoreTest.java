package com.github.lunatrius.schematica.client.selection;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Area;
import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class AreaSelectionStoreTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    @org.junit.Before public void normalMode() { AreaSelectionLibrary.defaultMode = AreaSelectionLibrary.Mode.NORMAL; }
    @org.junit.After public void simpleMode() { AreaSelectionLibrary.defaultMode = AreaSelectionLibrary.Mode.SIMPLE; }

    @Test public void newWorldsStartInTheDefaultSelectionMode() throws Exception {
        AreaSelectionLibrary.defaultMode = AreaSelectionLibrary.Mode.SIMPLE;
        AreaSelectionStore store = new AreaSelectionStore(settings(), areas("fresh"), "fresh|0");
        assertEquals(AreaSelectionLibrary.Mode.SIMPLE, store.library().mode());
        assertTrue(store.library().areas().isEmpty());
        assertNotNull(store.library().selected());
    }

    private void write(File file, String text) throws IOException {
        Files.createDirectories(file.toPath().toAbsolutePath().getParent());
        Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
    }
    private JsonObject read(File file) throws IOException {
        return new JsonParser().parse(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)).getAsJsonObject();
    }
    private File settings() { return new File(temporary.getRoot(), "AreaSelection.json"); }
    private File areas(String world) { return new File(temporary.getRoot(), world); }

    @Test public void longMultibyteNamesStillFitAFileName() throws Exception {
        AreaSelectionStore store = new AreaSelectionStore(settings(), areas("long"), "long|0");
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < 90; i++) name.append('\u697c');
        store.library().create(name.toString(), new Vector3i(), new Vector3i());
        store.save();
        boolean found = false;
        for (Area area : new AreaSelectionStore(settings(), areas("long"), "long|0").library().areas()) found |= area.name().equals(name.toString());
        assertTrue(found);
    }

    @Test public void removingOneOfTwoNamesSharingAFileSavesAndKeepsTheOther() throws Exception {
        AreaSelectionStore store = new AreaSelectionStore(settings(), areas("names"), "names|0");
        Area first = store.library().create("A:B", new Vector3i(), new Vector3i());
        store.library().create("A?B", new Vector3i(1, 0, 0), new Vector3i(1, 0, 0));
        store.save();
        assertTrue(new File(areas("names"), "A_B.json").isFile());
        assertTrue(new File(areas("names"), "A_B 1.json").isFile());
        store.library().remove(first);
        store.save();
        assertFalse(new File(areas("names"), "A_B.json").exists());
        int named = 0;
        for (Area area : new AreaSelectionStore(settings(), areas("names"), "names|0").library().areas()) {
            if (area.name().startsWith("A")) named++;
        }
        assertEquals(1, named);
    }

    private static void moveFirstCorner(File file, int x) throws IOException {
        JsonObject data = new JsonParser().parse(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject box = data.getAsJsonArray("boxes").get(0).getAsJsonObject();
        JsonArray old = box.getAsJsonArray("pos1"), pos = new JsonArray();
        pos.add(new JsonPrimitive(x));
        pos.add(old.get(1));
        pos.add(old.get(2));
        box.add("pos1", pos);
        Files.write(file.toPath(), data.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Test public void externalChangesAreTakenInAndMergedWithLocalChanges() throws Exception {
        AreaSelectionStore store = new AreaSelectionStore(settings(), areas("sync"), "sync|0");
        store.save();
        File file = new File(areas("sync"), "Unnamed.json");
        Area area = store.library().selected();
        boolean guide = area.guide();
        moveFirstCorner(file, 77);
        store.library().setGuide(area, !guide);
        store.save();
        assertEquals(77, area.boxes().get(0).first().x);
        assertEquals(!guide, area.guide());
        assertTrue(store.conflicts().isEmpty());
        AreaSelectionStore reopened = new AreaSelectionStore(settings(), areas("sync"), "sync|0");
        assertEquals(77, reopened.library().selected().boxes().get(0).first().x);
        assertEquals(!guide, reopened.library().selected().guide());

        moveFirstCorner(file, 5);
        store.library().setPoints(area, new Vector3i(9, 0, 0), area.second());
        store.save();
        assertEquals(5, area.boxes().get(0).first().x);
        assertEquals(1, store.conflicts().size());

        assertTrue(file.delete());
        store.save();
        assertFalse(store.library().areas().contains(area));
        assertFalse(file.exists());
    }

    @Test public void savesAndServersOfOneNameUseSeparateDirectories() throws Exception {
        File base = new File(temporary.getRoot(), "per_world"), saves = new File(temporary.getRoot(), "saves");
        File legacy = AreaSelections.directory(base, "save:localhost|dimension:0", saves);
        assertEquals(new File(new File(base, "localhost"), "area_selections"), legacy);
        assertEquals(new File(new File(base, "server_localhost"), "area_selections"),
            AreaSelections.directory(base, "server:localhost|dimension:0", saves));
        assertTrue(legacy.mkdirs());
        write(new File(legacy, "Old.json"), "{}");
        File moved = AreaSelections.directory(base, "server:localhost|dimension:0", saves);
        assertEquals(new File(new File(base, "server_localhost"), "area_selections"), moved);
        assertTrue(new File(moved, "Old.json").isFile());
        assertFalse(legacy.exists());
        assertTrue(legacy.mkdirs());
        assertTrue(new File(saves, "localhost").mkdirs());
        assertEquals(moved, AreaSelections.directory(base, "server:localhost|dimension:0", saves));
        assertTrue(legacy.isDirectory());
    }

    @Test public void everyCreatableFolderDepthIsReadBack() throws Exception {
        AreaSelectionStore store = new AreaSelectionStore(settings(), areas("deep"), "deep|0");
        String folder = "";
        for (int i = 0; i < AreaSelectionLibrary.MAX_FOLDER_DEPTH; i++) folder = store.library().createFolder(folder, "d" + i);
        String deepest = folder;
        try {
            store.library().createFolder(deepest, "too_deep");
            fail("A folder below the readable depth was created");
        } catch (IllegalArgumentException expected) {}
        store.library().setTargetFolder(deepest);
        store.library().create("tower", new Vector3i(), new Vector3i());
        store.save();
        boolean found = false;
        for (Area area : new AreaSelectionStore(settings(), areas("deep"), "deep|0").library().areas()) {
            found |= area.name().equals("tower") && area.folder().equals(deepest);
        }
        assertTrue(found);
    }

    @Test public void multipleBoxesAndExplicitBoxDeselectionPersistAcrossSessions() throws Exception {
        AreaSelectionStore store = new AreaSelectionStore(settings(), areas("world"), "world|0");
        Area area = store.library().selected();
        store.library().addBox(area, "Pipes", new Vector3i(10, 64, 0), new Vector3i(20, 64, 1));
        store.library().selectBox(area, null);
        store.save();
        store.save();
        AreaSelectionStore restored = new AreaSelectionStore(settings(), areas("world"), "world|0");
        assertNull(restored.library().selected().selectedBox());
        assertEquals(2, restored.library().selected().boxes().size());
        assertTrue(new File(areas("world"), "Unnamed.json").isFile());
    }

    @Test public void deletedAndDeselectedAreasStayGoneAndOtherWorldsKeepTheirOwnFiles() throws Exception {
        AreaSelectionStore first = new AreaSelectionStore(settings(), areas("one"), "one|0");
        first.library().select(null);
        first.save();
        AreaSelectionStore second = new AreaSelectionStore(settings(), areas("two"), "two|0");
        second.library().remove(second.library().selected());
        second.save();
        first.save();
        assertNull(new AreaSelectionStore(settings(), areas("one"), "one|0").library().selected());
        assertTrue(new AreaSelectionStore(settings(), areas("two"), "two|0").library().areas().isEmpty());
        assertFalse(new File(areas("two"), "Unnamed.json").exists());
        assertNotNull(new AreaSelectionStore(settings(), areas("three"), "three|0").library().selected());
    }

    @Test public void corruptAndExternallyChangedSettingsAreNotOverwritten() throws Exception {
        write(settings(), "{}");
        AreaSelectionStore store = new AreaSelectionStore(settings(), areas("world"), "world");
        store.save();
        AreaSelectionStore external = new AreaSelectionStore(settings(), areas("world"), "world");
        external.library().select(null);
        external.save();
        byte[] changed = Files.readAllBytes(settings().toPath());
        assertThrows(IOException.class, store::save);
        assertArrayEquals(changed, Files.readAllBytes(settings().toPath()));
        write(settings(), "{broken");
        byte[] broken = Files.readAllBytes(settings().toPath());
        assertThrows(IOException.class, store::save);
        assertThrows(IOException.class, () -> new AreaSelectionStore(settings(), areas("world"), "world"));
        assertArrayEquals(broken, Files.readAllBytes(settings().toPath()));
    }

    @Test public void switchingAndReloadingPreservesEverySelection() throws Exception {
        AreaSelectionStore store = new AreaSelectionStore(settings(), areas("server"), "server|0");
        Area first = store.library().selected();
        store.library().rename(first, "Factory");
        store.library().renameBox(first, "Assembly");
        store.library().setPoints(first, new Vector3i(20, 80, 9), new Vector3i(-5, 60, -2));
        Area second = store.library().copy(first, "Station");
        store.library().setPoints(second, new Vector3i(30, 90, 40), new Vector3i(31, 100, 50));
        store.library().setGuide(second, true);
        store.library().select(second);
        store.save();
        AreaSelectionStore restored = new AreaSelectionStore(settings(), areas("server"), "server|0");
        assertEquals(2, restored.library().areas().size());
        assertEquals("Station", restored.library().selected().name());
        assertTrue(restored.library().selected().guide());
        Area restoredFirst = restored.library().areas().get(0);
        assertEquals("Assembly", restoredFirst.boxName());
        assertEquals(new Vector3i(20, 80, 9), restoredFirst.first());
        assertEquals(new Vector3i(-5, 60, -2), restoredFirst.second());
        assertFalse(restoredFirst.guide());
        restored.library().select(restoredFirst);
        restored.save();
        AreaSelectionStore again = new AreaSelectionStore(settings(), areas("server"), "server|0");
        assertEquals("Factory", again.library().selected().name());
        assertEquals(new Vector3i(30, 90, 40), again.library().areas().get(1).first());
        assertFalse(new File(areas("server"), "Selection.json").exists());
    }

    @Test public void foldersRenamesAndLitematicaFilesWorkLikeTheUpstreamBrowser() throws Exception {
        write(new File(areas("world"), "farms/Wheat.json"), "{\"name\":\"Wheat\",\"current\":\"Field\",\"boxes\":[{\"name\":\"Field\","
            + "\"pos1\":[0,64,0],\"pos2\":[15,64,15]},{\"name\":\"Water\",\"pos1\":[7,63,7],\"pos2\":[7,63,7]}],\"origin\":[1,2,3]}");
        AreaSelectionStore store = new AreaSelectionStore(settings(), areas("world"), "world|0");
        AreaSelectionLibrary library = store.library();
        assertTrue(library.folders().contains("farms"));
        Area wheat = null;
        for (Area area : library.areas()) if (area.name().equals("Wheat")) wheat = area;
        assertNotNull(wheat);
        assertEquals("farms", wheat.folder());
        assertEquals("Field", wheat.boxName());
        assertEquals(2, wheat.boxes().size());
        assertEquals(new Vector3i(1, 2, 3), wheat.manualOrigin());
        library.setTargetFolder("farms");
        String trees = library.createFolder("farms", "trees");
        library.setTargetFolder(trees);
        Area oak = library.create("Oak", new Vector3i(0, 0, 0), new Vector3i(1, 1, 1));
        library.create("Wheat", new Vector3i(0, 0, 0), new Vector3i(1, 1, 1));
        library.rename(wheat, "Wheat farm");
        library.createFolder("", "empty");
        store.save();
        assertTrue(new File(areas("world"), "farms/trees/Oak.json").isFile());
        assertTrue(new File(areas("world"), "farms/trees/Wheat.json").isFile());
        assertTrue(new File(areas("world"), "farms/Wheat farm.json").isFile());
        assertFalse(new File(areas("world"), "farms/Wheat.json").exists());
        assertTrue(new File(areas("world"), "empty").isDirectory());
        library.select(oak);
        store.save();
        AreaSelectionStore restored = new AreaSelectionStore(settings(), areas("world"), "world|0");
        assertEquals("Oak", restored.library().selected().name());
        assertEquals("farms/trees", restored.library().selected().folder());
        assertTrue(restored.library().folders().contains("empty"));
        assertThrows(IllegalArgumentException.class, () -> library.createFolder("", "a:b"));
        assertThrows(AreaSelectionLibrary.NameConflictException.class, () -> library.createFolder("", "farms"));
    }
}

package com.github.lunatrius.schematica.client.selection;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Area;
import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class AreaSelectionStoreTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    private void write(File file, String text) throws IOException {
        Files.createDirectories(file.toPath().toAbsolutePath().getParent());
        Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
    }
    private JsonObject read(File file) throws IOException {
        return new JsonParser().parse(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)).getAsJsonObject();
    }
    private File settings() { return new File(temporary.getRoot(), "AreaSelection.json"); }
    private File areas(String world) { return new File(temporary.getRoot(), world); }

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
        assertTrue(new File(areas("world"), "Selection.json").isFile());
    }

    @Test public void migratesLegacyDataIntoFilesAndPreservesOtherWorldsAndUnknownFields() throws Exception {
        write(settings(), "{\"world|0\":{\"ax\":1,\"ay\":60,\"az\":-3,\"bx\":20,\"by\":80,\"bz\":9,\"renderingGuide\":true,\"future\":42},\"world|-1\":{\"future\":true}}");
        AreaSelectionStore store = new AreaSelectionStore(settings(), areas("world"), "world|0");
        Area area = store.library().selected();
        assertEquals(new Vector3i(1, 60, -3), area.first());
        assertTrue(area.guide());
        store.library().rename(area, "工厂");
        store.library().renameBox(area, "机器");
        store.save();
        store.save();
        JsonObject data = read(settings());
        assertEquals(42, data.getAsJsonObject("world|0").get("future").getAsInt());
        assertEquals(6, data.getAsJsonObject("world|0").get("version").getAsInt());
        assertFalse(data.getAsJsonObject("world|0").has("selections"));
        assertTrue(data.getAsJsonObject("world|-1").get("future").getAsBoolean());
        JsonObject file = read(new File(areas("world"), "工厂.json"));
        assertEquals("工厂", file.get("name").getAsString());
        assertEquals("机器", file.get("current").getAsString());
        assertEquals(20, file.getAsJsonArray("boxes").get(0).getAsJsonObject().getAsJsonArray("pos2").get(0).getAsInt());
        Area restored = new AreaSelectionStore(settings(), areas("world"), "world|0").library().selected();
        assertEquals("工厂", restored.name());
        assertEquals("机器", restored.boxName());
        assertEquals(new Vector3i(20, 80, 9), restored.second());
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
        assertFalse(new File(areas("two"), "Selection.json").exists());
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

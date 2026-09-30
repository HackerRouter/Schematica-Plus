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

    private void write(File file, String text) throws IOException { Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8)); }
    private JsonObject read(File file) throws IOException {
        return new JsonParser().parse(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test public void migratesLegacyDataAndPreservesOtherDimensionsAndUnknownFields() throws Exception {
        File file = temporary.newFile();
        write(file, "{\"world|0\":{\"ax\":1,\"ay\":60,\"az\":-3,\"bx\":20,\"by\":80,\"bz\":9,\"renderingGuide\":true,\"future\":42},\"world|-1\":{\"future\":true}}");
        AreaSelectionStore store = new AreaSelectionStore(file, "world|0");
        Area area = store.library().selected();
        assertEquals(new Vector3i(1, 60, -3), area.first());
        assertTrue(area.guide());
        store.library().rename(area, "工厂");
        store.library().renameBox(area, "机器");
        store.save();
        store.save();
        JsonObject data = read(file);
        assertEquals(42, data.getAsJsonObject("world|0").get("future").getAsInt());
        assertTrue(data.getAsJsonObject("world|-1").get("future").getAsBoolean());
        Area restored = new AreaSelectionStore(file, "world|0").library().selected();
        assertEquals("工厂", restored.name());
        assertEquals("机器", restored.boxName());
        assertEquals(new Vector3i(20, 80, 9), restored.second());
    }

    @Test public void keepsEachWorldIndependentAndNeverRecreatesDeletedOrDeselectedAreas() throws Exception {
        File file = new File(temporary.getRoot(), "AreaSelection.json");
        AreaSelectionStore first = new AreaSelectionStore(file, "one|0");
        AreaSelectionStore second = new AreaSelectionStore(file, "one|-1");
        first.library().select(null);
        first.save();
        second.library().remove(second.library().selected());
        second.save();
        first.save();
        assertNull(new AreaSelectionStore(file, "one|0").library().selected());
        assertTrue(new AreaSelectionStore(file, "one|-1").library().areas().isEmpty());
        assertNotNull(new AreaSelectionStore(file, "two|0").library().selected());
    }

    @Test public void corruptAndExternallyChangedSettingsAreNotOverwritten() throws Exception {
        File file = temporary.newFile();
        write(file, "{}");
        AreaSelectionStore store = new AreaSelectionStore(file, "world");
        store.save();
        AreaSelectionStore external = new AreaSelectionStore(file, "world");
        external.library().rename(external.library().selected(), "External change");
        external.save();
        byte[] changed = Files.readAllBytes(file.toPath());
        assertThrows(IOException.class, store::save);
        assertArrayEquals(changed, Files.readAllBytes(file.toPath()));
        write(file, "{broken");
        byte[] broken = Files.readAllBytes(file.toPath());
        assertThrows(IOException.class, store::save);
        assertThrows(IOException.class, () -> new AreaSelectionStore(file, "world"));
        assertArrayEquals(broken, Files.readAllBytes(file.toPath()));
    }
}

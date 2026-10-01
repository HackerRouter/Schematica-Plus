package com.github.lunatrius.schematica.client.input;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class HotkeyStoreTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    @Test public void chordsAndAllAdvancedSettingsSurviveRestartAndReset() throws Exception {
        Path file = temporary.getRoot().toPath().resolve("hotkeys.json");
        Hotkey key = new Hotkey("action", new Hotkey.Settings(), 50);
        key.setKeys(Arrays.asList(29, -98)); key.settings.action = Hotkey.Action.BOTH; key.settings.context = Hotkey.Context.ANY;
        key.settings.allowExtra = key.settings.allowEmpty = key.settings.exclusive = true; key.settings.ordered = key.settings.cancel = false;
        new HotkeyStore(file).save(Collections.singletonList(key));
        Hotkey loaded = new Hotkey("action", new Hotkey.Settings(), 50);
        assertTrue(new HotkeyStore(file).load(Collections.singletonList(loaded)));
        assertEquals(key.keys(), loaded.keys()); assertEquals(key.settings, loaded.settings); assertTrue(loaded.modified());
        loaded.reset(); assertFalse(loaded.modified()); assertEquals(Collections.singletonList(50), loaded.keys());
        assertThrows(UnsupportedOperationException.class, () -> loaded.keys().clear());
    }
    @Test public void unreadableOrFutureConfigurationIsPreservedAndNeverPartiallyApplied() throws Exception {
        Path file = temporary.getRoot().toPath().resolve("hotkeys.json");
        for (String text : new String[] {"{", "{\"version\":2}", "{\"version\":1,\"hotkeys\":{\"action\":{\"keys\":[30,30],\"settings\":{}}}}"}) {
            byte[] original = text.getBytes(StandardCharsets.UTF_8); Files.write(file, original);
            Hotkey key = new Hotkey("action", new Hotkey.Settings(), 50); HotkeyStore store = new HotkeyStore(file);
            assertThrows(java.io.IOException.class, () -> store.load(Collections.singletonList(key)));
            assertFalse(key.modified()); assertThrows(java.io.IOException.class, () -> store.save(Collections.singletonList(key)));
            assertArrayEquals(original, Files.readAllBytes(file));
        }
    }
    @Test public void unknownEntriesAndFieldsAreRetainedWhenSavingKnownKeys() throws Exception {
        Path file = temporary.getRoot().toPath().resolve("hotkeys.json");
        Files.write(file, "{\"version\":1,\"extra\":42,\"hotkeys\":{\"future\":{\"value\":7}}}".getBytes(StandardCharsets.UTF_8));
        HotkeyStore store = new HotkeyStore(file); Hotkey key = new Hotkey("action", new Hotkey.Settings());
        store.load(Collections.singletonList(key)); store.save(Collections.singletonList(key));
        com.google.gson.JsonObject data = new com.google.gson.JsonParser().parse(new String(Files.readAllBytes(file), StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals(42, data.get("extra").getAsInt()); assertEquals(7, data.getAsJsonObject("hotkeys").getAsJsonObject("future").get("value").getAsInt());
        try (java.util.stream.Stream<Path> paths = Files.list(temporary.getRoot().toPath())) {
            assertFalse(paths.anyMatch(path -> path.toString().endsWith(".tmp")));
        }
    }
}

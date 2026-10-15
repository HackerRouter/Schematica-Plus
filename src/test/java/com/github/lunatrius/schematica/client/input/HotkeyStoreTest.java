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
    @Test public void renamedPrinterKeyRetainsItsChordAndSettingsAcrossSaves() throws Exception {
        Path file = temporary.getRoot().toPath().resolve("hotkeys.json");
        Hotkey old = new Hotkey("workingSwitch", new Hotkey.Settings(), 66);
        old.settings.action = Hotkey.Action.RELEASE; old.settings.allowExtra = true;
        new HotkeyStore(file).save(Collections.singletonList(old));
        Hotkey renamed = new Hotkey("printerWorkingSwitch", new Hotkey.Settings());
        HotkeyStore store = new HotkeyStore(file);
        assertTrue(store.load(Collections.singletonList(renamed)));
        assertEquals(old.keys(), renamed.keys()); assertEquals(old.settings, renamed.settings);
        store.save(Collections.singletonList(renamed));
        String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        assertFalse(text.contains("\"workingSwitch\"")); assertTrue(text.contains("\"printerWorkingSwitch\""));
        Hotkey reloaded = new Hotkey("printerWorkingSwitch", new Hotkey.Settings());
        assertTrue(new HotkeyStore(file).load(Collections.singletonList(reloaded)));
        assertEquals(old.keys(), reloaded.keys()); assertEquals(old.settings, reloaded.settings);
    }

    @Test public void explicitNewPrinterBindingWinsOverThePreviousName() throws Exception {
        Path file = temporary.getRoot().toPath().resolve("hotkeys.json");
        new HotkeyStore(file).save(Arrays.asList(new Hotkey("workingSwitch", new Hotkey.Settings(), 66),
            new Hotkey("printerWorkingSwitch", new Hotkey.Settings(), 67)));
        String saved = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        com.google.gson.JsonObject data = new com.google.gson.JsonParser().parse(saved).getAsJsonObject();
        com.google.gson.JsonObject entries = data.getAsJsonObject("hotkeys");
        com.google.gson.JsonObject old = new com.google.gson.JsonParser().parse(entries.get("printerWorkingSwitch").toString()).getAsJsonObject();
        com.google.gson.JsonArray keys = new com.google.gson.JsonArray(); keys.add(new com.google.gson.JsonPrimitive(66));
        old.add("keys", keys); entries.add("workingSwitch", old);
        Files.write(file, data.toString().getBytes(StandardCharsets.UTF_8));
        Hotkey renamed = new Hotkey("printerWorkingSwitch", new Hotkey.Settings());
        assertTrue(new HotkeyStore(file).load(Collections.singletonList(renamed)));
        assertEquals(Collections.singletonList(67), renamed.keys());
    }

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

package com.github.lunatrius.schematica.client.input;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class HotkeyStore {
    private final Path file;
    private JsonObject root = new JsonObject();
    private boolean writable = true;

    public HotkeyStore(Path file) { this.file = file; }
    public boolean load(List<Hotkey> bindings) throws IOException {
        if (!Files.exists(file)) return false;
        try {
            if (Files.size(file) > 1024 * 1024) throw new IllegalArgumentException("Hotkey file too large");
            root = new JsonParser().parse(new String(Files.readAllBytes(file), StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.get("version").getAsInt() != 1) throw new IllegalArgumentException("Unsupported hotkey version");
            JsonObject values = root.getAsJsonObject("hotkeys");
            List<List<Integer>> keys = new ArrayList<>();
            List<Hotkey.Settings> settings = new ArrayList<>();
            for (Hotkey binding : bindings) {
                List<Integer> chord = new ArrayList<>(binding.keys()); Hotkey.Settings config = binding.settings.copy();
                if (values.has(binding.id)) {
                    JsonObject entry = values.getAsJsonObject(binding.id); chord.clear();
                    for (JsonElement key : entry.getAsJsonArray("keys")) chord.add(key.getAsInt());
                    config = new com.google.gson.Gson().fromJson(entry.get("settings"), Hotkey.Settings.class);
                    if (config == null || config.context == null || config.action == null) throw new IllegalArgumentException("Invalid settings");
                    new Hotkey(binding.id, config).setKeys(chord);
                }
                keys.add(chord); settings.add(config);
            }
            for (int i = 0; i < bindings.size(); i++) { bindings.get(i).setKeys(keys.get(i)); bindings.get(i).settings = settings.get(i); }
            return true;
        } catch (RuntimeException error) { writable = false; throw new IOException("Invalid hotkey configuration: " + file, error); }
        catch (IOException error) { writable = false; throw error; }
    }

    public void save(List<Hotkey> bindings) throws IOException {
        if (!writable) throw new IOException("Preserving unreadable hotkey configuration: " + file);
        JsonObject values = root.has("hotkeys") ? root.getAsJsonObject("hotkeys") : new JsonObject();
        for (Hotkey binding : bindings) {
            JsonObject entry = values.has(binding.id) ? values.getAsJsonObject(binding.id) : new JsonObject();
            JsonArray keys = new JsonArray();
            for (int key : binding.keys()) keys.add(new com.google.gson.JsonPrimitive(key));
            entry.add("keys", keys);
            entry.add("settings", new com.google.gson.Gson().toJsonTree(binding.settings));
            values.add(binding.id, entry);
        }
        root.addProperty("version", 1); root.add("hotkeys", values);
        Path directory = file.toAbsolutePath().getParent(); Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, "hotkeys-", ".tmp");
        try {
            Files.write(temporary, new GsonBuilder().setPrettyPrinting().create().toJson(root).getBytes(StandardCharsets.UTF_8));
            try { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (java.nio.file.AtomicMoveNotSupportedException error) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
}

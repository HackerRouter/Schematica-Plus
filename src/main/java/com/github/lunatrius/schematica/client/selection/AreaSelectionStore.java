package com.github.lunatrius.schematica.client.selection;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Area;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.util.FileUtils;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Litematica's SelectionManager storage: every normal selection is a JSON file in a directory tree, as in
 * area_selections(_per_world); the world's modes, simple selection and selected file stay in the settings file.
 * Selections of older versions (kept inside the settings) are moved to files on the first save.
 */
public final class AreaSelectionStore {
    private static final int MAX_DEPTH = 8;
    private final File file, directory;
    private final String key;
    private final AreaSelectionLibrary library;
    private final Map<Area, String> paths = new IdentityHashMap<>();
    private final Map<Area, String> written = new IdentityHashMap<>();
    private JsonElement previous;

    public AreaSelectionStore(File file, File directory, String key) throws IOException {
        if (key == null || key.isEmpty()) throw new IllegalArgumentException("Missing world session");
        this.file = file;
        this.directory = directory;
        this.key = key;
        previous = read().get(key);
        library = new AreaSelectionLibrary();
        Map<String, Area> byPath = new HashMap<>();
        load(directory, "", 0, byPath);
        JsonObject data = previous == null || !previous.isJsonObject() ? null : previous.getAsJsonObject();
        if (data != null && data.has("version") && data.get("version").getAsInt() == 6) {
            JsonElement selected = data.get("selected");
            library.restoreState(data, selected == null || selected.isJsonNull() ? null : byPath.get(selected.getAsString()));
        } else if (data == null && library.areas().isEmpty() && AreaSelectionLibrary.defaultMode == AreaSelectionLibrary.Mode.SIMPLE) {
            // defaultSelectionMode: a new world starts in Simple mode with its one box at 0, 0, 0
            library.setMode(AreaSelectionLibrary.Mode.SIMPLE);
        } else if (data != null || library.areas().isEmpty()) {
            AreaSelectionLibrary legacy = AreaSelectionLibrary.fromJson(data);
            Area selected = null;
            for (Area area : new ArrayList<>(legacy.areas())) {
                Area adopted = library.adopt(area);
                if (area == legacy.normalSelection()) selected = adopted;
            }
            library.adoptSettings(legacy, selected);
        }
    }

    public String key() { return key; }
    public AreaSelectionLibrary library() { return library; }
    public File directory() { return directory; }

    private void load(File folder, String relative, int depth, Map<String, Area> byPath) {
        File[] entries = folder.listFiles();
        if (entries == null || depth > MAX_DEPTH) return;
        java.util.Arrays.sort(entries);
        for (File entry : entries) {
            String name = entry.getName();
            String path = relative.isEmpty() ? name : relative + "/" + name;
            if (entry.isDirectory()) {
                try {
                    String folderPath = AreaSelectionLibrary.validFolder(path);
                    library.createFolder(AreaSelectionLibrary.parentFolder(folderPath), name);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                load(entry, path, depth + 1, byPath);
            } else if (name.toLowerCase(java.util.Locale.ROOT).endsWith(".json") && entry.length() <= 4L * 1024 * 1024) {
                try (Reader reader = Files.newBufferedReader(entry.toPath(), StandardCharsets.UTF_8)) {
                    JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
                    Area area = library.addFromFile(json, relative, name.substring(0, name.length() - 5));
                    paths.put(area, path);
                    written.put(area, text(AreaSelectionLibrary.toFile(area)));
                    byPath.put(path, area);
                } catch (IOException | RuntimeException e) {
                    Reference.logger.warn("Skipped the area selection file {}", entry, e);
                }
            }
        }
    }

    private static String text(JsonObject json) {
        return new GsonBuilder().setPrettyPrinting().create().toJson(json);
    }

    /** The file of each selection: its folder and safe name, with a number when two names map to one file. */
    private Map<Area, String> targets() {
        Map<Area, String> targets = new IdentityHashMap<>();
        Set<String> used = new HashSet<>();
        for (Area area : library.areas()) {
            String current = paths.get(area);
            String base = AreaSelectionLibrary.safeFileName(area.name());
            if (base.isEmpty()) base = "selection";
            String prefix = area.folder().isEmpty() ? "" : area.folder() + "/";
            String target = prefix + base + ".json";
            if (current != null && current.equalsIgnoreCase(target)) target = current;
            for (int i = 1; used.contains(target.toLowerCase(java.util.Locale.ROOT)); i++) target = prefix + base + " " + i + ".json";
            used.add(target.toLowerCase(java.util.Locale.ROOT));
            targets.put(area, target);
        }
        return targets;
    }

    public void save() throws IOException {
        JsonObject sessions = read();
        if (!Objects.equals(previous, sessions.get(key))) throw new IOException("Area selections changed on disk; preserving the file");
        Files.createDirectories(directory.toPath());
        for (String folder : library.folders()) Files.createDirectories(new File(directory, folder).toPath());
        Map<Area, String> targets = targets();
        for (Map.Entry<Area, String> entry : targets.entrySet()) {
            Area area = entry.getKey();
            String target = entry.getValue(), json = text(AreaSelectionLibrary.toFile(area));
            String old = paths.get(area);
            if (target.equals(old) && json.equals(written.get(area))) continue;
            File output = new File(directory, target);
            if (!target.equalsIgnoreCase(old) && output.exists()) throw new IOException("Area selection file already exists: " + target);
            FileUtils.writeUtf8Atomically(output, json);
            if (old != null && !old.equalsIgnoreCase(target)) Files.deleteIfExists(new File(directory, old).toPath());
            paths.put(area, target);
            written.put(area, json);
        }
        for (Area area : new ArrayList<>(paths.keySet())) {
            if (targets.containsKey(area)) continue;
            Files.deleteIfExists(new File(directory, paths.remove(area)).toPath());
            written.remove(area);
        }
        Area selected = library.normalSelection();
        JsonObject data = library.stateJson(selected == null ? null : targets.get(selected));
        sessions.add(key, data);
        Files.createDirectories(file.toPath().toAbsolutePath().getParent());
        String json = new GsonBuilder().setPrettyPrinting().serializeNulls().create().toJson(sessions);
        if (json.getBytes(StandardCharsets.UTF_8).length > 16L * 1024 * 1024) throw new IOException("Area selection settings exceed 16 MiB");
        FileUtils.writeUtf8Atomically(file, json);
        previous = data;
    }

    private JsonObject read() throws IOException {
        if (!file.exists()) return new JsonObject();
        if (file.length() > 16L * 1024 * 1024) throw new IOException("Area selection settings exceed 16 MiB");
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            return new JsonParser().parse(reader).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IOException("Area selection settings are unreadable; preserving the file", e);
        }
    }
}

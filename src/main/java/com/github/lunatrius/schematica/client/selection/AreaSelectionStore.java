package com.github.lunatrius.schematica.client.selection;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Area;
import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.util.FileUtils;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Litematica's SelectionManager storage: every normal selection is a JSON file in a directory tree, as in
 * area_selections(_per_world); the world's modes, simple selection and selected file stay in the settings file.
 */
public final class AreaSelectionStore {
    private static final int MAX_DEPTH = AreaSelectionLibrary.MAX_FOLDER_DEPTH;
    private final File file, directory;
    private final String key;
    private final AreaSelectionLibrary library;
    private final Map<Area, String> paths = new IdentityHashMap<>();
    private final Map<Area, String> written = new IdentityHashMap<>();
    private final Map<Area, String> disk = new IdentityHashMap<>();
    private final List<String> conflicts = new ArrayList<>();
    private boolean synced;
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
        if (data != null) {
            JsonElement selected = data.get("selected");
            library.restoreState(data, selected == null || selected.isJsonNull() ? null : byPath.get(selected.getAsString()));
        } else if (library.areas().isEmpty() && AreaSelectionLibrary.defaultMode == AreaSelectionLibrary.Mode.SIMPLE) {
            // defaultSelectionMode: a new world starts in Simple mode with its one box at 0, 0, 0
            library.setMode(AreaSelectionLibrary.Mode.SIMPLE);
        } else if (library.areas().isEmpty()) {
            Area area = library.create(AreaSelectionLibrary.DEFAULT_NAME, new Vector3i(), new Vector3i());
            library.setGuide(area, false);
            library.select(area);
        }
    }

    public String key() { return key; }
    public AreaSelectionLibrary library() { return library; }
    public File directory() { return directory; }

    private void load(File folder, String relative, int depth, Map<String, Area> byPath) {
        File[] entries = folder.listFiles();
        if (entries == null || depth > MAX_DEPTH) return;
        Arrays.sort(entries);
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
            } else if (name.toLowerCase(Locale.ROOT).endsWith(".json") && entry.length() <= 4L * 1024 * 1024) {
                try {
                    String content = new String(Files.readAllBytes(entry.toPath()), StandardCharsets.UTF_8);
                    JsonObject json = new JsonParser().parse(content).getAsJsonObject();
                    Area area = library.addFromFile(json, relative, name.substring(0, name.length() - 5));
                    paths.put(area, path);
                    written.put(area, text(AreaSelectionLibrary.toFile(area)));
                    disk.put(area, content);
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

    /**
     * The file of each selection: its folder and safe name, with a number when two names map to one file. A selection
     * keeps its current file while that is still a valid name for it, so removing another one renames nothing.
     */
    private Map<Area, String> targets() {
        Map<Area, String> targets = new IdentityHashMap<>();
        Set<String> used = new HashSet<>();
        for (Area area : library.areas()) {
            String current = paths.get(area);
            if (current == null || !current.toLowerCase(Locale.ROOT).matches(Pattern.quote(prefix(area).toLowerCase(Locale.ROOT)
                + base(area).toLowerCase(Locale.ROOT)) + "( [0-9]+)?\\.json")) continue;
            if (used.add(current.toLowerCase(Locale.ROOT))) targets.put(area, current);
        }
        for (Area area : library.areas()) {
            if (targets.containsKey(area)) continue;
            String prefix = prefix(area), base = base(area);
            String target = prefix + base + ".json";
            for (int i = 1; used.contains(target.toLowerCase(Locale.ROOT)); i++) target = prefix + base + " " + i + ".json";
            used.add(target.toLowerCase(Locale.ROOT));
            targets.put(area, target);
        }
        return targets;
    }

    private static String prefix(Area area) { return area.folder().isEmpty() ? "" : area.folder() + "/"; }

    private static final int MAX_NAME_BYTES = 200;

    private static String base(Area area) {
        String base = AreaSelectionLibrary.safeFileName(area.name());
        int end = base.length();
        while (end > 0 && base.substring(0, end).getBytes(StandardCharsets.UTF_8).length > MAX_NAME_BYTES) {
            end = Character.isLowSurrogate(base.charAt(end - 1)) && end > 1 ? end - 2 : end - 1;
        }
        base = base.substring(0, end).trim();
        return base.isEmpty() ? "selection" : base;
    }

    /** The selections whose files changed outside the game in both places during the last save; the file values won. */
    public List<String> conflicts() { return Collections.unmodifiableList(conflicts); }

    /** Whether the last save took in changes made outside the game. */
    public boolean synced() { return synced; }

    /**
     * Takes in selection files changed outside the game since they were read: a selection unchanged here takes the
     * file, one changed in both places is merged by field with the file winning conflicts, a deleted file removes an
     * unchanged selection and a file changed after its selection was removed brings it back.
     */
    private void syncWithDisk() throws IOException {
        conflicts.clear();
        synced = false;
        Set<Area> current = new HashSet<>(library.areas());
        for (Area area : new ArrayList<>(paths.keySet())) {
            String path = paths.get(area);
            File stored = new File(directory, path);
            String expected = disk.get(area);
            String actual = stored.isFile() ? new String(Files.readAllBytes(stored.toPath()), StandardCharsets.UTF_8) : null;
            if (Objects.equals(actual, expected)) continue;
            synced = true;
            boolean present = current.contains(area);
            boolean local = present && !text(AreaSelectionLibrary.toFile(area)).equals(written.get(area));
            if (actual == null) {
                if (present && !local) library.remove(area);
                if (present && local) conflicts.add(area.name());
                paths.remove(area);
                written.remove(area);
                disk.remove(area);
                continue;
            }
            String folder = AreaSelectionLibrary.parentFolder(path);
            String fallback = stored.getName().substring(0, stored.getName().length() - 5);
            JsonObject remote;
            try {
                remote = AreaSelectionLibrary.normalizedFile(new JsonParser().parse(actual).getAsJsonObject(), folder, fallback);
            } catch (RuntimeException e) {
                throw new IOException("Area selection file changed on disk and is unreadable; preserving it: " + path, e);
            }
            if (!present) {
                Area restored = library.addFromFile(remote, folder, fallback);
                conflicts.add(restored.name());
                paths.remove(area);
                written.remove(area);
                disk.remove(area);
                paths.put(restored, path);
                written.put(restored, text(AreaSelectionLibrary.toFile(restored)));
                disk.put(restored, actual);
                continue;
            }
            JsonObject merged = remote;
            if (local) {
                boolean[] conflict = {false};
                merged = merge(new JsonParser().parse(written.get(area)).getAsJsonObject(), AreaSelectionLibrary.toFile(area), remote, conflict);
                if (conflict[0]) conflicts.add(area.name());
            }
            library.replaceFromFile(area, merged);
            String normalized = text(AreaSelectionLibrary.toFile(area));
            written.put(area, local ? text(remote) : normalized);
            disk.put(area, actual);
        }
    }

    private static JsonObject merge(JsonObject base, JsonObject local, JsonObject remote, boolean[] conflict) {
        JsonObject result = new JsonObject();
        Set<String> keys = new LinkedHashSet<>();
        for (Map.Entry<String, JsonElement> entry : remote.entrySet()) keys.add(entry.getKey());
        for (Map.Entry<String, JsonElement> entry : local.entrySet()) keys.add(entry.getKey());
        for (String key : keys) {
            JsonElement b = base.get(key), l = local.get(key), r = remote.get(key);
            JsonElement chosen;
            if ("schematica_plus".equals(key) && l != null && r != null && l.isJsonObject() && r.isJsonObject()) {
                JsonObject plus = merge(b != null && b.isJsonObject() ? b.getAsJsonObject() : new JsonObject(), l.getAsJsonObject(), r.getAsJsonObject(), conflict);
                if (l.getAsJsonObject().has("id")) plus.add("id", l.getAsJsonObject().get("id"));
                chosen = plus;
            } else if (Objects.equals(l, b)) chosen = r;
            else if (Objects.equals(r, b) || Objects.equals(r, l)) chosen = l;
            else {
                chosen = r;
                conflict[0] = true;
            }
            if (chosen != null) result.add(key, chosen);
        }
        return result;
    }

    public void save() throws IOException {
        JsonObject sessions = read();
        if (!Objects.equals(previous, sessions.get(key))) throw new IOException("Area selections changed on disk; preserving the file");
        Files.createDirectories(directory.toPath());
        for (String folder : library.folders()) Files.createDirectories(new File(directory, folder).toPath());
        syncWithDisk();
        Map<Area, String> targets = targets();
        Set<String> leaving = new HashSet<>();
        for (Map.Entry<Area, String> entry : paths.entrySet()) {
            String target = targets.get(entry.getKey());
            if (target == null || !target.equalsIgnoreCase(entry.getValue())) leaving.add(entry.getValue().toLowerCase(Locale.ROOT));
        }
        for (Map.Entry<Area, String> entry : targets.entrySet()) {
            String target = entry.getValue(), old = paths.get(entry.getKey());
            if (target.equalsIgnoreCase(old) || leaving.contains(target.toLowerCase(Locale.ROOT))) continue;
            if (new File(directory, target).exists()) throw new IOException("Area selection file already exists: " + target);
        }
        for (Area area : new ArrayList<>(paths.keySet())) {
            if (targets.containsKey(area)) continue;
            Files.deleteIfExists(new File(directory, paths.remove(area)).toPath());
            written.remove(area);
            disk.remove(area);
        }
        Map<Area, File> moved = new IdentityHashMap<>();
        for (Map.Entry<Area, String> entry : targets.entrySet()) {
            String old = paths.get(entry.getKey());
            if (old == null || old.equalsIgnoreCase(entry.getValue())) continue;
            File source = new File(directory, old);
            if (!source.isFile()) continue;
            File temporary = File.createTempFile(".selection-", ".tmp", source.getParentFile());
            Files.move(source.toPath(), temporary.toPath(), StandardCopyOption.REPLACE_EXISTING);
            moved.put(entry.getKey(), temporary);
        }
        for (Map.Entry<Area, String> entry : targets.entrySet()) {
            Area area = entry.getKey();
            String target = entry.getValue(), json = text(AreaSelectionLibrary.toFile(area));
            String old = paths.get(area);
            if (target.equals(old) && json.equals(written.get(area))) continue;
            FileUtils.writeUtf8Atomically(new File(directory, target), json);
            File temporary = moved.remove(area);
            if (temporary != null) Files.deleteIfExists(temporary.toPath());
            paths.put(area, target);
            written.put(area, json);
            disk.put(area, json);
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

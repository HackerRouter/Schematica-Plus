package com.github.lunatrius.schematica.client.util;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.util.FileUtils;
import com.github.lunatrius.schematica.util.PlusDataFiles;

/**
 * What Litematica's DataManager remembers between sessions: the config GUI tab, create_placement_on_load, the
 * last directory of each file browser (last_directories) and the tool mode per world (operation_mode); also the
 * favorite and recently loaded schematic files of the load browser (after Buildprint).
 */
public final class UiState {
    private static JsonObject root;

    private UiState() {}

    private static synchronized JsonObject root() {
        if (root == null) {
            root = new JsonObject();
            File file = PlusDataFiles.file("UiState.json");
            if (file.isFile()) {
                try {
                    JsonElement parsed = new JsonParser().parse(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
                    if (parsed.isJsonObject()) root = parsed.getAsJsonObject();
                } catch (Exception e) {
                    Reference.logger.warn("Could not read {}", file, e);
                }
            }
        }
        return root;
    }

    private static synchronized void save() {
        try {
            FileUtils.writeUtf8Atomically(PlusDataFiles.file("UiState.json"), new GsonBuilder().setPrettyPrinting().create().toJson(root()));
        } catch (Exception e) {
            Reference.logger.warn("Could not save the UI state", e);
        }
    }

    public static synchronized String get(String key, String fallback) {
        JsonElement value = root().get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : fallback;
    }

    public static synchronized void set(String key, String value) {
        if (value.equals(get(key, null))) return;
        root().addProperty(key, value);
        save();
    }

    private static synchronized String entry(String group, String key) {
        JsonElement map = root().get(group);
        JsonElement value = map != null && map.isJsonObject() ? map.getAsJsonObject().get(key) : null;
        return value != null && value.isJsonPrimitive() ? value.getAsString() : null;
    }

    private static synchronized void setEntry(String group, String key, String value) {
        if (key == null || value.equals(entry(group, key))) return;
        JsonElement map = root().get(group);
        if (map == null || !map.isJsonObject()) root().add(group, map = new JsonObject());
        map.getAsJsonObject().addProperty(key, value);
        save();
    }

    public static String lastDirectory(String browser) { return entry("last_directories", browser); }

    public static void setLastDirectory(String browser, File directory) { setEntry("last_directories", browser, directory.getAbsolutePath()); }

    /** The paths of a list, newest or first added first. */
    public static synchronized java.util.List<String> paths(String key) {
        java.util.List<String> result = new java.util.ArrayList<>();
        JsonElement list = root().get(key);
        if (list != null && list.isJsonArray()) {
            for (JsonElement value : list.getAsJsonArray()) if (value.isJsonPrimitive()) result.add(value.getAsString());
        }
        return result;
    }

    private static synchronized void setPaths(String key, java.util.List<String> paths) {
        com.google.gson.JsonArray array = new com.google.gson.JsonArray();
        for (String path : paths) array.add(new com.google.gson.JsonPrimitive(path));
        root().add(key, array);
        save();
    }

    private static String canonical(File file) {
        try {
            return file.getCanonicalPath();
        } catch (java.io.IOException e) {
            return file.getAbsolutePath();
        }
    }

    public static synchronized boolean favorite(File file) { return paths("favorite_schematics").contains(canonical(file)); }

    /** Stars or unstars a file; returns whether it is a favorite now. */
    public static synchronized boolean toggleFavorite(File file) {
        java.util.List<String> paths = paths("favorite_schematics");
        boolean added = !paths.remove(canonical(file));
        if (added) paths.add(canonical(file));
        setPaths("favorite_schematics", paths);
        return added;
    }

    /** Puts a loaded file at the top of the recent list, which keeps the last 20. */
    public static synchronized void addRecent(File file) {
        java.util.List<String> paths = paths("recent_schematics");
        paths.remove(canonical(file));
        paths.add(0, canonical(file));
        while (paths.size() > 20) paths.remove(paths.size() - 1);
        setPaths("recent_schematics", paths);
    }

    public static String toolMode(String world) { return world == null ? null : entry("operation_mode", world); }

    public static void setToolMode(String world, String mode) { setEntry("operation_mode", world, mode); }
}

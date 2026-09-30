package com.github.lunatrius.schematica.client.gui.browser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.Arrays;
import java.util.Map;

import com.github.lunatrius.schematica.util.FileUtils;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

final class SchematicSourceIndex {

    private final File file;
    private final byte[] original;
    private final String updated;

    private SchematicSourceIndex(File file, byte[] original, String updated) {
        this.file = file;
        this.original = original;
        this.updated = updated;
    }

    static SchematicSourceIndex prepare(File root, File source, File target) throws IOException {
        File file = new File(root, "LoadedSchematics.json");
        if (!Files.exists(file.toPath(), LinkOption.NOFOLLOW_LINKS)) return new SchematicSourceIndex(file, null, null);
        if (!file.getCanonicalFile().equals(file.getAbsoluteFile())) throw new SchematicBrowserModel.FileOperationException("index");
        byte[] original = Files.readAllBytes(file.toPath());
        try {
            JsonElement parsed = new JsonParser().parse(new String(original, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) throw new IllegalArgumentException("Expected a session map");
            boolean changed = false;
            for (Map.Entry<String, JsonElement> session : parsed.getAsJsonObject().entrySet()) {
                if (session.getValue().isJsonNull()) continue;
                for (JsonElement value : session.getValue().getAsJsonArray()) {
                    JsonObject entry = value.getAsJsonObject();
                    String filename = string(entry, "filename");
                    if (filename == null || filename.isEmpty()) continue;
                    String directory = string(entry, "directory");
                    File parent = directory == null || directory.isEmpty() ? root : new File(directory);
                    if (new File(parent, filename).getCanonicalFile().equals(source.getCanonicalFile())) {
                        entry.addProperty("filename", target.getName());
                        entry.addProperty("directory", target.getParentFile().getAbsolutePath());
                        changed = true;
                    }
                }
            }
            return new SchematicSourceIndex(file, original,
                changed ? new GsonBuilder().setPrettyPrinting().create().toJson(parsed) : null);
        } catch (RuntimeException e) {
            throw new SchematicBrowserModel.FileOperationException("index");
        }
    }

    private static String string(JsonObject entry, String key) {
        JsonElement value = entry.get(key);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }

    void save() throws IOException {
        byte[] current = Files.exists(file.toPath(), LinkOption.NOFOLLOW_LINKS) ? Files.readAllBytes(file.toPath()) : null;
        if (!Arrays.equals(original, current)) throw new SchematicBrowserModel.FileOperationException("index");
        if (updated != null) FileUtils.writeUtf8Atomically(file, updated);
    }
}

package com.github.lunatrius.schematica.client.selection;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;

import com.github.lunatrius.schematica.util.FileUtils;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class AreaSelectionStore {
    private final File file;
    private final String key;
    private final AreaSelectionLibrary library;
    private JsonElement previous;

    public AreaSelectionStore(File file, String key) throws IOException {
        if (key == null || key.isEmpty()) throw new IllegalArgumentException("Missing world session");
        this.file = file;
        this.key = key;
        previous = read().get(key);
        library = AreaSelectionLibrary.fromJson(previous == null ? null : previous.getAsJsonObject());
    }

    public String key() { return key; }
    public AreaSelectionLibrary library() { return library; }

    public void save() throws IOException {
        JsonObject sessions = read();
        if (!Objects.equals(previous, sessions.get(key))) throw new IOException("Area selections changed on disk; preserving the file");
        JsonObject data = library.toJson();
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

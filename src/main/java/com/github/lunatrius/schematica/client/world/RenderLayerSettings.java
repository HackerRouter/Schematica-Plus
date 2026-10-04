package com.github.lunatrius.schematica.client.world;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.util.FileUtils;

public final class RenderLayerSettings {
    public static final RenderLayerRange RANGE = new RenderLayerRange();

    private RenderLayerSettings() {}

    public static void save(String key) {
        if (key == null || key.isEmpty()) return;
        try {
            save(file(), key, RANGE);
        } catch (Exception e) {
            Reference.logger.error("Could not save render layers; preserving existing settings", e);
        }
    }

    public static void restore(String key) {
        RANGE.load(null);
        if (key == null || key.isEmpty()) return;
        try {
            JsonObject sessions = read(file());
            if (sessions.has(key)) RANGE.load(sessions.getAsJsonObject(key));
        } catch (Exception e) {
            Reference.logger.error("Could not restore render layers", e);
        }
    }

    private static File file() { return com.github.lunatrius.schematica.util.PlusDataFiles.file("RenderLayers.json"); }

    static void save(File file, String key, RenderLayerRange range) throws IOException {
        JsonObject sessions = read(file);
        sessions.add(key, range.toJson());
        FileUtils.writeUtf8Atomically(file, new GsonBuilder().setPrettyPrinting().create().toJson(sessions));
    }

    static JsonObject read(File file) throws IOException {
        if (!file.exists()) return new JsonObject();
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            return new JsonParser().parse(reader).getAsJsonObject();
        }
    }
}

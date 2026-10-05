// SPDX-License-Identifier: LGPL-3.0-only
// Litematica MaterialListCustom file formats (from Stormatica by CubicMetre), adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.material;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * A custom material list file: JSON {"name": ..., "items": [{"id": "minecraft:wool", "count": 64}]} or text lines
 * "minecraft:wool 64" (# comments). 1.7.10 items also need their damage: a "damage" field, or "id@damage" as the
 * material list keys write it; upstream files without it read as damage 0.
 */
public final class CustomMaterialListFile {
    public static final String JSON_EXTENSION = ".json", TEXT_EXTENSION = ".txt";
    static final long MAX_SIZE = 4L << 20;

    public static final class Item {
        public final String id;
        public final int damage, count;
        public Item(String id, int damage, int count) { this.id = id; this.damage = damage; this.count = count; }
        String key() { return id + "@" + damage; }
    }

    public final String name;
    public final List<Item> items;

    CustomMaterialListFile(String name, List<Item> items) { this.name = name; this.items = Collections.unmodifiableList(items); }

    public static boolean isListFile(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(JSON_EXTENSION) || lower.endsWith(TEXT_EXTENSION);
    }

    public static CustomMaterialListFile read(File file) throws IOException {
        if (file.length() > MAX_SIZE) throw new IOException("Material list file too large: " + file.getName());
        String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        return file.getName().toLowerCase(Locale.ROOT).endsWith(TEXT_EXTENSION) ? parseText(text, file.getName()) : parseJson(text, file.getName());
    }

    static CustomMaterialListFile parseJson(String text, String fallbackName) throws IOException {
        JsonElement element;
        try {
            element = new JsonParser().parse(text);
        } catch (RuntimeException e) {
            throw new IOException("Invalid JSON", e);
        }
        if (!element.isJsonObject()) throw new IOException("The root must be an object");
        JsonObject root = element.getAsJsonObject();
        if (!root.has("items") || !root.get("items").isJsonArray()) throw new IOException("Missing 'items' array");
        Map<String, Item> items = new LinkedHashMap<>();
        for (JsonElement entry : root.getAsJsonArray("items")) {
            try {
                JsonObject object = entry.getAsJsonObject();
                if (!object.has("id") || !object.has("count")) continue;
                add(items, object.get("id").getAsString(), object.has("damage") ? object.get("damage").getAsInt() : -1, object.get("count").getAsInt());
            } catch (RuntimeException ignored) {
                // skipped like upstream's invalid entries
            }
        }
        if (items.isEmpty()) throw new IOException("No valid items");
        String name = root.has("name") && root.get("name").isJsonPrimitive() ? root.get("name").getAsString() : fallbackName;
        return new CustomMaterialListFile(name, new ArrayList<>(items.values()));
    }

    static CustomMaterialListFile parseText(String text, String fallbackName) throws IOException {
        Map<String, Item> items = new LinkedHashMap<>();
        for (String line : text.split("\r?\n")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] parts = line.split("\\s+");
            if (parts.length != 2) continue;
            try {
                add(items, parts[0], -1, Integer.parseInt(parts[1]));
            } catch (NumberFormatException ignored) {
                // skipped like upstream's invalid lines
            }
        }
        if (items.isEmpty()) throw new IOException("No valid items");
        return new CustomMaterialListFile(fallbackName, new ArrayList<>(items.values()));
    }

    private static void add(Map<String, Item> items, String id, int damage, int count) {
        if (count <= 0 || id == null) return;
        int at = id.lastIndexOf('@');
        if (at > 0) {
            if (damage < 0) damage = Integer.parseInt(id.substring(at + 1));
            id = id.substring(0, at);
        }
        if (!id.contains(":")) id = "minecraft:" + id;
        if (id.indexOf(':') == id.length() - 1) return;
        Item item = new Item(id, Math.max(0, damage), count);
        Item previous = items.get(item.key());
        items.put(item.key(), previous == null ? item : new Item(id, item.damage, (int) Math.min(Integer.MAX_VALUE, (long) previous.count + count)));
    }

    /** MaterialListCustom.toJsonFile; the damage is written only when it is not 0. */
    public static String toJson(String name, List<Item> items) {
        JsonObject root = new JsonObject();
        root.addProperty("name", name);
        JsonArray array = new JsonArray();
        for (Item item : items) {
            JsonObject object = new JsonObject();
            object.addProperty("id", item.id);
            if (item.damage != 0) object.addProperty("damage", item.damage);
            object.addProperty("count", item.count);
            array.add(object);
        }
        root.add("items", array);
        return new GsonBuilder().setPrettyPrinting().create().toJson(root);
    }
}

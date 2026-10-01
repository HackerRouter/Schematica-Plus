// SPDX-License-Identifier: LGPL-3.0-only
// Litematica SchematicVersion, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.projects;

import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class SchematicVersion {
    public static final int MAX_DESCRIPTION_LENGTH = 512;

    public final String name, fileName, description;
    private final Vector3i areaOffset;
    public final int version;
    public final long timeStamp;

    SchematicVersion(String name, String fileName, String description, Vector3i areaOffset, int version, long timeStamp) {
        this.name = name;
        this.fileName = fileName;
        this.description = description == null ? "" : abbreviate(description);
        this.areaOffset = areaOffset.clone();
        this.version = version;
        this.timeStamp = timeStamp;
    }

    public Vector3i areaOffset() { return areaOffset.clone(); }

    static String abbreviate(String text) {
        return text.length() <= MAX_DESCRIPTION_LENGTH ? text : text.substring(0, MAX_DESCRIPTION_LENGTH - 3) + "...";
    }

    JsonObject toJson() {
        JsonObject data = new JsonObject();
        data.addProperty("name", name);
        data.addProperty("file_name", fileName);
        data.addProperty("description", description);
        data.add("area_offset", SchematicProject.pointJson(areaOffset));
        data.addProperty("version", version);
        data.addProperty("timestamp", timeStamp);
        return data;
    }

    static SchematicVersion fromJson(JsonObject data) {
        Vector3i offset = SchematicProject.point(data.get("area_offset"));
        if (offset == null || !string(data, "name") || !string(data, "file_name")) return null;
        String name = data.get("name").getAsString();
        String description = string(data, "description") ? data.get("description").getAsString() : name;
        int version = number(data, "version") ? data.get("version").getAsInt() : 0;
        long time = number(data, "timestamp") ? data.get("timestamp").getAsLong() : 0;
        return new SchematicVersion(name, data.get("file_name").getAsString(), description, offset, version, time);
    }

    static boolean string(JsonObject data, String key) {
        JsonElement value = data.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString();
    }

    static boolean number(JsonObject data, String key) {
        JsonElement value = data.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber();
    }
}

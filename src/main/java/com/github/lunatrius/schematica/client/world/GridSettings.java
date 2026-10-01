// SPDX-License-Identifier: LGPL-3.0-only
// Litematica GridSettings (maruohon, liteloader_1.12.2), adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.world;

import java.util.Arrays;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/** Grid repeat of a placement: the grid cell size and how many copies to add towards each axis direction. */
public final class GridSettings {
    public static final int MAX_SIZE = 1000000, MAX_REPEAT = 10000;

    private int[] size = new int[3], defaultSize = new int[3], repeatNegative = new int[3], repeatPositive = new int[3];
    private boolean enabled, initialized;

    public boolean isInitialized() { return initialized; }

    public boolean isEnabled() { return enabled; }

    public boolean isAtDefaultValues() {
        return (Arrays.equals(size, new int[3]) || Arrays.equals(size, defaultSize))
            && Arrays.equals(repeatNegative, new int[3]) && Arrays.equals(repeatPositive, new int[3]);
    }

    public int[] size() { return size.clone(); }

    public int[] defaultSize() { return defaultSize.clone(); }

    public int[] repeatNegative() { return repeatNegative.clone(); }

    public int[] repeatPositive() { return repeatPositive.clone(); }

    public boolean toggleEnabled() {
        enabled = !enabled;
        initialized = true;
        return enabled;
    }

    public void resetSize() {
        setSize(defaultSize);
        initialized = true;
    }

    /** The placement's enclosing box size; the grid size never gets smaller than it. */
    public void setDefaultSize(int[] value) {
        defaultSize = value.clone();
        setSize(size);
    }

    public void setSize(int[] value) {
        int[] next = new int[3];
        for (int i = 0; i < 3; i++) next[i] = Math.min(MAX_SIZE, Math.max(value[i], defaultSize[i]));
        size = next;
    }

    public void setSize(int axis, int value) {
        int[] next = size.clone();
        next[axis] = value;
        setSize(next);
        initialized = true;
    }

    public void setRepeatNegative(int[] value) {
        repeatNegative = repeat(value);
        initialized = true;
    }

    public void setRepeatPositive(int[] value) {
        repeatPositive = repeat(value);
        initialized = true;
    }

    private static int[] repeat(int[] value) {
        int[] next = new int[3];
        for (int i = 0; i < 3; i++) next[i] = Math.max(0, Math.min(MAX_REPEAT, value[i]));
        return next;
    }

    public void copyFrom(GridSettings other) {
        size = other.size.clone();
        defaultSize = other.defaultSize.clone();
        repeatNegative = other.repeatNegative.clone();
        repeatPositive = other.repeatPositive.clone();
        enabled = other.enabled;
        initialized = other.initialized;
    }

    public JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("enabled", enabled);
        object.add("size", array(size));
        object.add("repeatNegative", array(repeatNegative));
        object.add("repeatPositive", array(repeatPositive));
        return object;
    }

    public void fromJson(JsonObject object) {
        if (object == null) return;
        enabled = object.has("enabled") && object.get("enabled").getAsBoolean();
        size = vector(object.get("size"), defaultSize);
        repeatNegative = repeat(vector(object.get("repeatNegative"), new int[3]));
        repeatPositive = repeat(vector(object.get("repeatPositive"), new int[3]));
        setSize(size);
        initialized = !isAtDefaultValues();
    }

    private static JsonArray array(int[] value) {
        JsonArray array = new JsonArray();
        for (int component : value) array.add(new JsonPrimitive(component));
        return array;
    }

    private static int[] vector(JsonElement element, int[] fallback) {
        if (element == null || !element.isJsonArray() || element.getAsJsonArray().size() != 3) return fallback.clone();
        int[] value = new int[3];
        for (int i = 0; i < 3; i++) value[i] = element.getAsJsonArray().get(i).getAsInt();
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof GridSettings)) return false;
        GridSettings that = (GridSettings) other;
        return enabled == that.enabled && Arrays.equals(size, that.size) && Arrays.equals(repeatNegative, that.repeatNegative)
            && Arrays.equals(repeatPositive, that.repeatPositive);
    }

    @Override
    public int hashCode() {
        return ((Arrays.hashCode(size) * 31 + Arrays.hashCode(repeatNegative)) * 31 + Arrays.hashCode(repeatPositive)) * 31 + (enabled ? 1 : 0);
    }
}

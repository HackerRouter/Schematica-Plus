// SPDX-License-Identifier: LGPL-3.0-only
// MaLiLib LayerRange behavior, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.world;

import java.util.Locale;

import com.google.gson.JsonObject;

public final class RenderLayerRange {
    public enum Mode {
        ALL, SINGLE_LAYER, LAYER_RANGE, ALL_BELOW, ALL_ABOVE;

        public String translationKey() { return "malilib.gui.label.layer_mode." + name().toLowerCase(Locale.ROOT); }
    }

    public enum Axis { X, Y, Z }

    private Mode mode = Mode.ALL;
    private Axis axis = Axis.Y;
    private int single, above, below, min, max;
    private boolean moveMin, moveMax;
    private long revision;

    public Mode mode() { return mode; }
    public Axis axis() { return axis; }
    public long revision() { return revision; }
    public boolean moveMin() { return moveMin; }
    public boolean moveMax() { return moveMax; }
    public void setMoveMin(boolean value) { moveMin = value; }
    public void setMoveMax(boolean value) { moveMax = value; }

    public void setMode(Mode value) {
        if (value != null && mode != value) { mode = value; revision++; }
    }

    public void setAxis(Axis value) {
        if (value != null && axis != value) { axis = value; revision++; }
    }

    public int value(boolean upper) {
        switch (mode) {
            case SINGLE_LAYER: return single;
            case ALL_ABOVE: return above;
            case ALL_BELOW: return below;
            case LAYER_RANGE: return upper ? max : min;
            default: return 0;
        }
    }

    public void setValue(boolean upper, long value) {
        int next = bounded(value);
        if (mode == Mode.LAYER_RANGE) next = upper ? Math.max(min, next) : Math.min(max, next);
        if (next == value(upper)) return;
        switch (mode) {
            case SINGLE_LAYER: single = next; break;
            case ALL_ABOVE: above = next; break;
            case ALL_BELOW: below = next; break;
            case LAYER_RANGE: if (upper) max = next; else min = next; break;
            default: return;
        }
        revision++;
    }

    public void setHere(int coordinate) {
        if (mode == Mode.LAYER_RANGE) {
            if (min != coordinate || max != coordinate) { min = max = coordinate; revision++; }
        } else setValue(false, coordinate);
    }

    public void move(int amount, double cameraCoordinate) {
        if (mode != Mode.LAYER_RANGE) { setValue(false, (long) value(false) + amount); return; }
        boolean nearestMin = cameraCoordinate < min + 0.5
            || Math.abs(cameraCoordinate - (min + 0.5)) < Math.abs(cameraCoordinate - (max + 0.5));
        boolean lower = moveMin || (nearestMin && !moveMax);
        boolean upper = moveMax || (!nearestMin && !moveMin);
        if (lower && upper) {
            long delta = Math.max((long) Integer.MIN_VALUE - min, Math.min((long) Integer.MAX_VALUE - max, amount));
            if (delta != 0) { min += delta; max += delta; revision++; }
        } else setValue(upper, (long) value(upper) + amount);
    }

    public long minimum() {
        return mode == Mode.ALL || mode == Mode.ALL_BELOW ? Long.MIN_VALUE : value(false);
    }

    public long maximum() {
        return mode == Mode.ALL || mode == Mode.ALL_ABOVE ? Long.MAX_VALUE : value(true);
    }

    public boolean contains(long x, long y, long z) {
        long coordinate = axis == Axis.X ? x : axis == Axis.Y ? y : z;
        return coordinate >= minimum() && coordinate <= maximum();
    }

    public int[] localBounds(int x, int y, int z, int width, int height, int length, boolean singleLayer, int layer) {
        int[] bounds = {0, 0, 0, width, height, length};
        if (mode != Mode.ALL) {
            int i = axis.ordinal();
            long origin = i == 0 ? x : i == 1 ? y : z;
            long lower = minimum() == Long.MIN_VALUE ? 0 : minimum() - origin;
            long upper = maximum() == Long.MAX_VALUE ? bounds[i + 3] : maximum() - origin + 1;
            bounds[i] = (int) Math.max(0, Math.min(bounds[i + 3], lower));
            bounds[i + 3] = (int) Math.max(bounds[i], Math.min(bounds[i + 3], upper));
        }
        if (singleLayer) {
            int lower = Math.max(bounds[1], Math.min(height, Math.max(0, layer)));
            int upper = (int) Math.max(lower, Math.min(bounds[4], (long) layer + 1));
            bounds[1] = lower;
            bounds[4] = upper;
        }
        return bounds;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("mode", mode.name());
        json.addProperty("axis", axis.name());
        json.addProperty("layer_single", single);
        json.addProperty("layer_above", above);
        json.addProperty("layer_below", below);
        json.addProperty("layer_range_min", min);
        json.addProperty("layer_range_max", max);
        json.addProperty("hotkey_range_min", moveMin);
        json.addProperty("hotkey_range_max", moveMax);
        return json;
    }

    public void load(JsonObject json) {
        RenderLayerRange next = new RenderLayerRange();
        if (json != null) {
            next.mode = Mode.valueOf(json.get("mode").getAsString());
            next.axis = Axis.valueOf(json.get("axis").getAsString());
            next.single = integer(json, "layer_single");
            next.above = integer(json, "layer_above");
            next.below = integer(json, "layer_below");
            next.min = integer(json, "layer_range_min");
            next.max = Math.max(next.min, integer(json, "layer_range_max"));
            next.moveMin = json.get("hotkey_range_min").getAsBoolean();
            next.moveMax = json.get("hotkey_range_max").getAsBoolean();
        }
        mode = next.mode; axis = next.axis;
        single = next.single; above = next.above; below = next.below;
        min = next.min; max = next.max; moveMin = next.moveMin; moveMax = next.moveMax;
        revision++;
    }

    private static int integer(JsonObject json, String key) {
        return new java.math.BigDecimal(json.get(key).getAsString()).intValueExact();
    }

    private static int bounded(long value) { return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, value)); }
}

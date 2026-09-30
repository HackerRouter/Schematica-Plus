package com.github.lunatrius.schematica.client.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.api.SchematicOrigin;
import com.github.lunatrius.schematica.api.SchematicRegion;
import com.github.lunatrius.schematica.util.SchematicLimits;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class SubRegionPlacements {
    private final List<Region> regions;
    public final String selected;

    private SubRegionPlacements(List<Region> regions, String selected) {
        this.regions = Collections.unmodifiableList(new ArrayList<>(regions));
        this.selected = selected;
    }

    public static SubRegionPlacements create(ISchematic source) {
        List<SchematicRegion> bounds = source.getRegions();
        if (bounds.isEmpty()) bounds = Collections.singletonList(new SchematicRegion("Region", 0, 0, 0,
            source.getWidth() - 1, source.getHeight() - 1, source.getLength() - 1));
        if (bounds.size() > 256) throw new IllegalArgumentException("Too many subregions");
        List<Region> regions = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (SchematicRegion box : bounds) {
            if (!names.add(box.name)) throw new IllegalArgumentException("Duplicate subregion name");
            regions.add(new Region(box, source.getOrigin()));
        }
        return new SubRegionPlacements(regions, null);
    }

    public List<Region> regions() { return regions; }

    public Region get(String name) {
        for (Region region : regions) if (region.name().equals(name)) return region;
        throw new IllegalArgumentException("Unknown subregion: " + name);
    }

    public boolean hasEnabled() {
        for (Region region : regions) if (region.enabled) return true;
        return false;
    }

    public boolean modified() {
        for (Region region : regions) if (region.modified()) return true;
        return false;
    }

    public SubRegionPlacements select(String name) {
        if (name != null) get(name);
        return new SubRegionPlacements(regions, name);
    }

    public SubRegionPlacements replace(Region replacement) {
        Region current = get(replacement.name());
        if (current.box != replacement.box) throw new IllegalArgumentException("Subregion belongs to another source");
        List<Region> next = new ArrayList<>(regions);
        next.set(next.indexOf(current), replacement);
        return new SubRegionPlacements(next, selected);
    }

    public SubRegionPlacements enabled(boolean enabled) {
        List<Region> next = new ArrayList<>();
        for (Region region : regions) next.add(region.enabled(enabled));
        return new SubRegionPlacements(next, selected);
    }

    public SubRegionPlacements reset() {
        List<Region> next = new ArrayList<>();
        for (Region region : regions) next.add(region.reset());
        return new SubRegionPlacements(next, selected);
    }

    public Layout layout() { return new Layout(regions); }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        root.addProperty("selected", selected);
        JsonArray entries = new JsonArray();
        for (Region region : regions) {
            if (!region.modified()) continue;
            JsonObject entry = new JsonObject();
            entry.addProperty("name", region.name());
            JsonArray position = new JsonArray();
            for (int coordinate : region.position.coordinates()) position.add(new com.google.gson.JsonPrimitive(coordinate));
            entry.add("position", position);
            entry.addProperty("rotation", region.rotation);
            entry.addProperty("mirror", region.mirror);
            entry.addProperty("enabled", region.enabled);
            entry.addProperty("rendering", region.rendering);
            entry.addProperty("ignoreEntities", region.ignoreEntities);
            entry.addProperty("locks", region.locks);
            entries.add(entry);
        }
        root.add("regions", entries);
        return root;
    }

    public SubRegionPlacements restore(JsonObject root) {
        if (root == null) return this;
        if (integer(root.get("version")) != 1 || !root.has("regions") || !root.get("regions").isJsonArray()) {
            throw new IllegalArgumentException("Invalid subregion placement data");
        }
        JsonArray entries = root.getAsJsonArray("regions");
        if (entries.size() > 256) throw new IllegalArgumentException("Too many subregion placements");
        SubRegionPlacements result = this;
        Set<String> names = new HashSet<>();
        for (JsonElement element : entries) {
            if (!element.isJsonObject()) throw new IllegalArgumentException("Invalid subregion placement");
            JsonObject entry = element.getAsJsonObject();
            String name = string(entry.get("name"));
            if (!names.add(name)) throw new IllegalArgumentException("Duplicate subregion placement");
            JsonArray pos = entry.getAsJsonArray("position");
            if (pos == null || pos.size() != 3) throw new IllegalArgumentException("Invalid subregion position");
            SchematicOrigin position = new SchematicOrigin(integer(pos.get(0)), integer(pos.get(1)), integer(pos.get(2)));
            int rotation = integer(entry.get("rotation")), mirror = integer(entry.get("mirror")), locks = integer(entry.get("locks"));
            boolean enabled = bool(entry.get("enabled")), rendering = bool(entry.get("rendering")), ignore = bool(entry.get("ignoreEntities"));
            validate(position, rotation, mirror, locks);
            for (Region region : regions) if (region.name().equals(name)) {
                result = result.replace(new Region(region, position, rotation, mirror, enabled, rendering, ignore, locks));
                break;
            }
        }
        if (root.has("selected") && !root.get("selected").isJsonNull()) {
            String name = string(root.get("selected"));
            for (Region region : regions) if (region.name().equals(name)) return result.select(name);
        }
        return result.select(null);
    }

    private static int integer(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Invalid integer");
        }
        try { return value.getAsBigDecimal().intValueExact(); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("Invalid integer", e); }
    }

    private static String string(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) throw new IllegalArgumentException("Invalid name");
        String name = value.getAsString();
        if (name.trim().isEmpty() || name.length() > 200) throw new IllegalArgumentException("Invalid name");
        return name;
    }

    private static boolean bool(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException("Invalid flag");
        return value.getAsBoolean();
    }

    private static void validate(SchematicOrigin position, int rotation, int mirror, int locks) {
        for (int value : position.coordinates()) if (value < -60000000 || value > 60000000) throw new IllegalArgumentException("Invalid relative position");
        if (rotation < 0 || rotation > 3 || mirror < 0 || mirror > 2 || locks < 0 || locks > 7) throw new IllegalArgumentException("Invalid subregion transform");
    }

    public static SchematicOrigin vector(SchematicOrigin point, List<String> operations, boolean inverse) {
        for (int i = 0; i < operations.size(); i++) {
            String value = operations.get(inverse ? operations.size() - 1 - i : i);
            if (value == null || value.length() != 1 || "XYZxyz".indexOf(value.charAt(0)) < 0) throw new IllegalArgumentException("Invalid transform");
            char operation = value.charAt(0);
            int count = inverse && Character.isUpperCase(operation) ? 3 : 1;
            for (int j = 0; j < count; j++) point = point.transform(operation, 1, 1, 1);
        }
        return point;
    }

    public static final class Region {
        public final SchematicRegion box;
        public final SchematicOrigin defaultPosition, position;
        public final int rotation, mirror, locks;
        public final boolean enabled, rendering, ignoreEntities;

        private Region(SchematicRegion box, SchematicOrigin origin) {
            this.box = box;
            defaultPosition = new SchematicOrigin(Math.subtractExact(box.minX, origin.x), Math.subtractExact(box.minY, origin.y), Math.subtractExact(box.minZ, origin.z));
            position = defaultPosition;
            rotation = 0; mirror = 0; locks = 0;
            enabled = true; rendering = true; ignoreEntities = false;
        }

        private Region(Region source, SchematicOrigin position, int rotation, int mirror, boolean enabled, boolean rendering, boolean ignoreEntities, int locks) {
            validate(position, rotation, mirror, locks);
            box = source.box; defaultPosition = source.defaultPosition;
            this.position = position; this.rotation = rotation; this.mirror = mirror;
            this.enabled = enabled; this.rendering = rendering; this.ignoreEntities = ignoreEntities; this.locks = locks;
        }

        public String name() { return box.name; }
        public Region position(SchematicOrigin target) {
            return new Region(this, new SchematicOrigin((locks & 1) == 0 ? target.x : position.x,
                (locks & 2) == 0 ? target.y : position.y, (locks & 4) == 0 ? target.z : position.z), rotation, mirror, enabled, rendering, ignoreEntities, locks);
        }
        public Region rotation(int value) { return new Region(this, position, Math.floorMod(value, 4), mirror, enabled, rendering, ignoreEntities, locks); }
        public Region mirror(int value) { return new Region(this, position, rotation, Math.floorMod(value, 3), enabled, rendering, ignoreEntities, locks); }
        public Region enabled(boolean value) { return new Region(this, position, rotation, mirror, value, rendering, ignoreEntities, locks); }
        public Region rendering(boolean value) { return new Region(this, position, rotation, mirror, enabled, value, ignoreEntities, locks); }
        public Region ignoreEntities(boolean value) { return new Region(this, position, rotation, mirror, enabled, rendering, value, locks); }
        public Region locks(int value) { return new Region(this, position, rotation, mirror, enabled, rendering, ignoreEntities, value); }
        public Region reset() { return new Region(this, defaultPosition, 0, 0, true, true, false, 0); }
        public boolean modified() {
            return position.x != defaultPosition.x || position.y != defaultPosition.y || position.z != defaultPosition.z
                || rotation != 0 || mirror != 0 || !enabled || !rendering || ignoreEntities || locks != 0;
        }
        public List<String> operations() {
            List<String> result = new ArrayList<>();
            if (mirror != 0) result.add(mirror == 1 ? "z" : "x");
            for (int i = 0; i < rotation; i++) result.add("Y");
            return result;
        }
        public SchematicRegion bounds() {
            SchematicOrigin end = vector(new SchematicOrigin(box.maxX - box.minX, box.maxY - box.minY, box.maxZ - box.minZ), operations(), false)
                .atMinimum(position.x, position.y, position.z);
            return new SchematicRegion(name(), position.x, position.y, position.z, end.x, end.y, end.z);
        }
    }

    public static final class Layout {
        public final SchematicOrigin minimum;
        public final int width, height, length;
        public final List<SchematicRegion> bounds;

        private Layout(List<Region> regions) {
            List<SchematicRegion> boxes = new ArrayList<>();
            int minX = Integer.MAX_VALUE, minY = minX, minZ = minX;
            int maxX = Integer.MIN_VALUE, maxY = maxX, maxZ = maxX;
            for (Region region : regions) if (region.enabled) {
                SchematicRegion box = region.bounds();
                boxes.add(box);
                minX = Math.min(minX, box.minX); minY = Math.min(minY, box.minY); minZ = Math.min(minZ, box.minZ);
                maxX = Math.max(maxX, box.maxX); maxY = Math.max(maxY, box.maxY); maxZ = Math.max(maxZ, box.maxZ);
            }
            minimum = boxes.isEmpty() ? SchematicOrigin.ZERO : new SchematicOrigin(minX, minY, minZ);
            width = boxes.isEmpty() ? 1 : SchematicLimits.dimension(minX, maxX);
            height = boxes.isEmpty() ? 1 : SchematicLimits.dimension(minY, maxY);
            length = boxes.isEmpty() ? 1 : SchematicLimits.dimension(minZ, maxZ);
            SchematicLimits.volume(width, height, length);
            bounds = Collections.unmodifiableList(boxes);
        }
    }
}

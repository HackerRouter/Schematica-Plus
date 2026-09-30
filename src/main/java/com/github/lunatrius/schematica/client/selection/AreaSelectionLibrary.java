package com.github.lunatrius.schematica.client.selection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;
import com.github.lunatrius.schematica.util.SchematicLimits;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class AreaSelectionLibrary {
    private final List<Area> areas = new ArrayList<>();
    private Area selected;
    private JsonObject extra = new JsonObject();

    public List<Area> areas() { return Collections.unmodifiableList(areas); }
    public Area selected() { return selected; }
    public boolean contains(Area area) { return areas.contains(area); }

    public Area create(String name, Vector3i first, Vector3i second) {
        if (areas.size() >= 4096) throw new IllegalArgumentException("Too many selections");
        String checked = uniqueName(name, null);
        checkPoint(first);
        checkPoint(second);
        Area area = new Area(UUID.randomUUID().toString(), checked);
        Box box = new Box(checked, first, second);
        area.boxes.add(box);
        area.selectedBox = box;
        areas.add(area);
        return area;
    }

    public Area copy(Area source, String name) {
        require(source);
        if (areas.size() >= 4096) throw new IllegalArgumentException("Too many selections");
        Area area = new Area(UUID.randomUUID().toString(), uniqueName(name, null));
        for (Box box : source.boxes) {
            Box copy = new Box(box.name, box.first, box.second);
            copy.extra = copyJson(box.extra);
            area.boxes.add(copy);
            if (box == source.selectedBox) area.selectedBox = copy;
        }
        area.guide = source.guide;
        area.extra = copyJson(source.extra);
        areas.add(area);
        return area;
    }

    public Area createFromRegions(String name, List<com.github.lunatrius.schematica.api.SchematicRegion> regions) {
        if (areas.size() >= 4096 || regions.isEmpty() || regions.size() > 256) throw new IllegalArgumentException("Invalid selection size");
        Area area = new Area(UUID.randomUUID().toString(), uniqueName(name, null));
        for (com.github.lunatrius.schematica.api.SchematicRegion region : regions) {
            Vector3i first = new Vector3i(region.minX, region.minY, region.minZ);
            Vector3i second = new Vector3i(region.maxX, region.maxY, region.maxZ);
            checkPoint(first); checkPoint(second);
            area.boxes.add(new Box(uniqueBoxName(area, region.name, null), first, second));
        }
        area.selectedBox = area.boxes.get(0);
        areas.add(area);
        return area;
    }

    public void select(Area area) {
        if (area != null) require(area);
        selected = area;
    }

    public void remove(Area area) {
        require(area);
        areas.remove(area);
        if (selected == area) selected = null;
    }

    public void rename(Area area, String name) {
        require(area);
        area.name = uniqueName(name, area);
    }

    public Box addBox(Area area, String name, Vector3i first, Vector3i second) {
        require(area);
        if (area.boxes.size() >= 256) throw new IllegalArgumentException("Too many subregions");
        String checked = uniqueBoxName(area, name, null);
        checkPoint(first); checkPoint(second);
        Box box = new Box(checked, first, second);
        area.boxes.add(box);
        area.selectedBox = box;
        return box;
    }

    public void selectBox(Area area, Box box) {
        require(area);
        if (box != null) requireBox(area, box);
        area.selectedBox = box;
    }

    public void removeBox(Area area, Box box) {
        requireBox(area, box);
        area.boxes.remove(box);
        if (area.selectedBox == box) area.selectedBox = null;
    }

    public void renameBox(Area area, String name) { renameBox(area, area.selectedBox, name); }

    public void renameBox(Area area, Box box, String name) {
        requireBox(area, box);
        box.name = uniqueBoxName(area, name, box);
    }

    public void setPoints(Area area, Vector3i first, Vector3i second) { setPoints(area, area.selectedBox, first, second); }

    public void setPoints(Area area, Box box, Vector3i first, Vector3i second) {
        requireBox(area, box);
        checkPoint(first); checkPoint(second);
        box.first.set(first); box.second.set(second);
    }

    private void requireBox(Area area, Box box) {
        require(area);
        if (!area.boxes.contains(box)) throw new IllegalArgumentException("Subregion is no longer in this selection");
    }

    private static String uniqueBoxName(Area area, String name, Box except) {
        String checked = validName(name);
        for (Box box : area.boxes) {
            if (box != except && box.name.equalsIgnoreCase(checked)) throw new NameConflictException(checked);
        }
        return checked;
    }

    public void setGuide(Area area, boolean guide) { require(area); area.guide = guide; }

    private void require(Area area) {
        if (!contains(area)) throw new IllegalArgumentException("Selection is no longer in this session");
    }

    private String uniqueName(String name, Area except) {
        String checked = validName(name);
        for (Area area : areas) {
            if (area != except && area.name.equalsIgnoreCase(checked)) throw new NameConflictException(checked);
        }
        return checked;
    }

    private static String validName(String value) {
        if (value == null || value.trim().isEmpty() || value.length() > 200) throw new IllegalArgumentException("Invalid selection name");
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) throw new IllegalArgumentException("Invalid selection name");
        }
        return value.trim();
    }

    private static void checkPoint(Vector3i point) {
        SchematicLimits.worldBounds(point.x, point.y, point.z, point.x, point.y, point.z);
    }

    public JsonObject toJson() {
        JsonObject data = copyJson(extra);
        data.addProperty("version", 3);
        data.add("selected", selected == null ? JsonNull.INSTANCE : new com.google.gson.JsonPrimitive(selected.id));
        JsonArray entries = new JsonArray();
        for (Area area : areas) {
            JsonObject entry = copyJson(area.extra);
            entry.addProperty("id", area.id);
            entry.addProperty("name", area.name);
            JsonArray boxes = new JsonArray();
            for (Box box : area.boxes) {
                JsonObject value = copyJson(box.extra);
                value.addProperty("name", box.name);
                value.addProperty("ax", box.first.x); value.addProperty("ay", box.first.y); value.addProperty("az", box.first.z);
                value.addProperty("bx", box.second.x); value.addProperty("by", box.second.y); value.addProperty("bz", box.second.z);
                boxes.add(value);
            }
            entry.add("boxes", boxes);
            entry.add("selectedBox", area.selectedBox == null ? JsonNull.INSTANCE : new com.google.gson.JsonPrimitive(area.selectedBox.name));
            for (String key : new String[] {"boxName", "ax", "ay", "az", "bx", "by", "bz"}) entry.remove(key);
            entry.addProperty("renderingGuide", area.guide);
            entries.add(entry);
        }
        data.add("selections", entries);
        for (String key : new String[] {"ax", "ay", "az", "bx", "by", "bz", "renderingGuide"}) data.remove(key);
        return data;
    }

    public static AreaSelectionLibrary fromJson(JsonObject data) {
        AreaSelectionLibrary library = new AreaSelectionLibrary();
        if (data == null) {
            Area area = library.create("Selection", new Vector3i(), new Vector3i());
            library.setGuide(area, false);
            library.select(area);
            return library;
        }
        library.extra = copyJson(data);
        if (!data.has("version")) {
            if (data.has("selections")) throw new IllegalArgumentException("Missing selection library version");
            Area area = library.create("Selection", point(data, "a"), point(data, "b"));
            library.setGuide(area, guide(data));
            library.select(area);
            return library;
        }
        int version = integer(data, "version");
        if (version != 2 && version != 3) throw new IllegalArgumentException("Unsupported selection library version");
        for (JsonElement element : data.getAsJsonArray("selections")) {
            JsonObject entry = element.getAsJsonObject();
            String id = UUID.fromString(entry.get("id").getAsString()).toString();
            for (Area area : library.areas) if (area.id.equals(id)) throw new IllegalArgumentException("Duplicate selection ID");
            Area checked;
            if (version == 2) {
                checked = library.create(entry.get("name").getAsString(), point(entry, "a"), point(entry, "b"));
                library.renameBox(checked, entry.get("boxName").getAsString());
            } else {
                if (library.areas.size() >= 4096) throw new IllegalArgumentException("Too many selections");
                checked = new Area(id, library.uniqueName(entry.get("name").getAsString(), null));
                library.areas.add(checked);
                for (JsonElement boxElement : entry.getAsJsonArray("boxes")) {
                    JsonObject value = boxElement.getAsJsonObject();
                    Box box = library.addBox(checked, value.get("name").getAsString(), point(value, "a"), point(value, "b"));
                    box.extra = copyJson(value);
                }
                JsonElement selectedBox = entry.get("selectedBox");
                if (selectedBox == null) throw new IllegalArgumentException("Missing selected subregion");
                checked.selectedBox = null;
                if (!selectedBox.isJsonNull()) {
                    for (Box box : checked.boxes) if (box.name.equals(selectedBox.getAsString())) checked.selectedBox = box;
                    if (checked.selectedBox == null) throw new IllegalArgumentException("Unknown selected subregion");
                }
            }
            checked.id = id;
            checked.guide = guide(entry);
            checked.extra = copyJson(entry);
        }
        JsonElement selected = data.get("selected");
        if (selected == null) throw new IllegalArgumentException("Missing selected area");
        if (!selected.isJsonNull()) {
            for (Area area : library.areas) if (area.id.equals(selected.getAsString())) library.selected = area;
            if (library.selected == null) throw new IllegalArgumentException("Unknown selected area");
        }
        return library;
    }

    private static Vector3i point(JsonObject data, String prefix) {
        return new Vector3i(integer(data, prefix + "x"), integer(data, prefix + "y"), integer(data, prefix + "z"));
    }

    private static int integer(JsonObject data, String key) {
        return data.get(key).getAsBigDecimal().intValueExact();
    }

    private static boolean guide(JsonObject data) {
        if (!data.get("renderingGuide").getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException("Invalid guide flag");
        return data.get("renderingGuide").getAsBoolean();
    }

    private static JsonObject copyJson(JsonObject data) { return new JsonParser().parse(data.toString()).getAsJsonObject(); }

    public static final class Area {
        private String id;
        private String name;
        private final List<Box> boxes = new ArrayList<>();
        private Box selectedBox;
        private boolean guide = true;
        private JsonObject extra = new JsonObject();

        private Area(String id, String name) { this.id = id; this.name = name; }

        public String name() { return name; }
        public List<Box> boxes() { return Collections.unmodifiableList(boxes); }
        public Box selectedBox() { return selectedBox; }
        public String boxName() { return selectedBox == null ? "" : selectedBox.name(); }
        public Vector3i first() { return selectedBox == null ? new Vector3i() : selectedBox.first(); }
        public Vector3i second() { return selectedBox == null ? new Vector3i() : selectedBox.second(); }
        public boolean guide() { return guide; }
        public List<com.github.lunatrius.schematica.api.SchematicRegion> regions() {
            List<com.github.lunatrius.schematica.api.SchematicRegion> result = new ArrayList<>();
            for (Box box : boxes) result.add(new com.github.lunatrius.schematica.api.SchematicRegion(box.name,
                box.first.x, box.first.y, box.first.z, box.second.x, box.second.y, box.second.z));
            return result;
        }
    }

    public static final class Box {
        private String name;
        private final Vector3i first, second;
        private JsonObject extra = new JsonObject();

        private Box(String name, Vector3i first, Vector3i second) {
            this.name = name; this.first = first.clone(); this.second = second.clone();
        }
        public String name() { return name; }
        public Vector3i first() { return first.clone(); }
        public Vector3i second() { return second.clone(); }
    }

    public static final class NameConflictException extends IllegalArgumentException {
        public NameConflictException(String name) { super(name); }
    }
}

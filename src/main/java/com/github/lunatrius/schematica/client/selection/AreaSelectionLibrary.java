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
        Area area = new Area(UUID.randomUUID().toString(), checked, checked, first, second, true);
        areas.add(area);
        return area;
    }

    public Area copy(Area source, String name) {
        require(source);
        Area area = create(name, source.first, source.second);
        area.boxName = source.boxName;
        area.guide = source.guide;
        area.extra = copyJson(source.extra);
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

    public void renameBox(Area area, String name) {
        require(area);
        area.boxName = validName(name);
    }

    public void setPoints(Area area, Vector3i first, Vector3i second) {
        require(area);
        checkPoint(first);
        checkPoint(second);
        area.first.set(first);
        area.second.set(second);
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
        data.addProperty("version", 2);
        data.add("selected", selected == null ? JsonNull.INSTANCE : new com.google.gson.JsonPrimitive(selected.id));
        JsonArray entries = new JsonArray();
        for (Area area : areas) {
            JsonObject entry = copyJson(area.extra);
            entry.addProperty("id", area.id);
            entry.addProperty("name", area.name);
            entry.addProperty("boxName", area.boxName);
            entry.addProperty("ax", area.first.x); entry.addProperty("ay", area.first.y); entry.addProperty("az", area.first.z);
            entry.addProperty("bx", area.second.x); entry.addProperty("by", area.second.y); entry.addProperty("bz", area.second.z);
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
        if (integer(data, "version") != 2) throw new IllegalArgumentException("Unsupported selection library version");
        for (JsonElement element : data.getAsJsonArray("selections")) {
            JsonObject entry = element.getAsJsonObject();
            String id = UUID.fromString(entry.get("id").getAsString()).toString();
            for (Area area : library.areas) if (area.id.equals(id)) throw new IllegalArgumentException("Duplicate selection ID");
            Area checked = library.create(entry.get("name").getAsString(), point(entry, "a"), point(entry, "b"));
            checked.id = id;
            checked.boxName = validName(entry.get("boxName").getAsString());
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
        private String boxName;
        private final Vector3i first;
        private final Vector3i second;
        private boolean guide;
        private JsonObject extra = new JsonObject();

        private Area(String id, String name, String boxName, Vector3i first, Vector3i second, boolean guide) {
            this.id = id;
            this.name = name;
            this.boxName = boxName;
            this.first = first.clone();
            this.second = second.clone();
            this.guide = guide;
        }

        public String name() { return name; }
        public String boxName() { return boxName; }
        public Vector3i first() { return first.clone(); }
        public Vector3i second() { return second.clone(); }
        public boolean guide() { return guide; }
    }

    public static final class NameConflictException extends IllegalArgumentException {
        public NameConflictException(String name) { super(name); }
    }
}

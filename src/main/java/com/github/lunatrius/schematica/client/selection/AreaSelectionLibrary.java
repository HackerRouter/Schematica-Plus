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
    private Area simple = defaultSimple();
    private Mode mode = Mode.NORMAL;
    private CornerMode cornerMode = CornerMode.CORNERS;
    private JsonObject extra = new JsonObject();

    public enum Mode { NORMAL, SIMPLE }
    public enum CornerMode { CORNERS, EXPAND }
    public enum Corner { NONE, FIRST, SECOND }

    private static Area defaultSimple() {
        Area area = new Area(UUID.randomUUID().toString(), "Simple selection");
        area.selectedBox = new Box(area.name, new Vector3i(), new Vector3i());
        area.boxes.add(area.selectedBox);
        area.guide = false;
        return area;
    }

    public List<Area> areas() { return Collections.unmodifiableList(areas); }
    public Area selected() { return mode == Mode.SIMPLE ? simple : selected; }
    public Area normalSelection() { return selected; }
    public Mode mode() { return mode; }
    public CornerMode cornerMode() { return cornerMode; }
    public void setMode(Mode value) { mode = java.util.Objects.requireNonNull(value); }
    public void setCornerMode(CornerMode value) { cornerMode = java.util.Objects.requireNonNull(value); }
    public boolean contains(Area area) { return area == simple || areas.contains(area); }

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
        area.manualOrigin = source.manualOrigin();
        area.originSelected = source.originSelected;
        area.corner = source.corner;
        area.extra = copyJson(source.extra);
        areas.add(area);
        return area;
    }

    public Area createFromRegions(String name, List<com.github.lunatrius.schematica.api.SchematicRegion> regions) {
        return createFromRegions(name, regions, null);
    }

    public Area createFromRegions(String name, List<com.github.lunatrius.schematica.api.SchematicRegion> regions, Vector3i origin) {
        if (origin != null) checkPoint(origin);
        if (areas.size() >= 4096 || regions.isEmpty() || regions.size() > 256) throw new IllegalArgumentException("Invalid selection size");
        Area area = new Area(UUID.randomUUID().toString(), uniqueName(name, null));
        for (com.github.lunatrius.schematica.api.SchematicRegion region : regions) {
            Vector3i first = new Vector3i(region.minX, region.minY, region.minZ);
            Vector3i second = new Vector3i(region.maxX, region.maxY, region.maxZ);
            checkPoint(first); checkPoint(second);
            area.boxes.add(new Box(uniqueBoxName(area, region.name, null), first, second));
        }
        area.selectedBox = area.boxes.get(0);
        area.manualOrigin = origin == null ? null : origin.clone();
        areas.add(area);
        return area;
    }

    public void select(Area area) {
        if (area != null && !areas.contains(area)) throw new IllegalArgumentException("Not a normal selection");
        selected = area;
        mode = Mode.NORMAL;
    }

    public void remove(Area area) {
        require(area);
        if (area == simple) throw new IllegalArgumentException("Cannot remove the simple selection");
        areas.remove(area);
        if (selected == area) selected = null;
    }

    public void rename(Area area, String name) {
        require(area);
        String checked = area == simple ? validName(name) : uniqueName(name, area);
        if (area.boxes.size() == 1 && area.boxes.get(0).name.equals(area.name)) area.boxes.get(0).name = checked;
        area.name = checked;
    }

    public Box addBox(Area area, String name, Vector3i first, Vector3i second) {
        require(area);
        if (area == simple) throw new IllegalArgumentException("Simple mode has exactly one subregion");
        if (area.boxes.size() >= 256) throw new IllegalArgumentException("Too many subregions");
        String checked = uniqueBoxName(area, name, null);
        checkPoint(first); checkPoint(second);
        Box box = new Box(checked, first, second);
        area.boxes.add(box);
        area.selectedBox = box;
        area.originSelected = false;
        area.corner = Corner.NONE;
        return box;
    }

    public void selectBox(Area area, Box box) {
        require(area);
        if (area == simple && box == null) box = simple.boxes.get(0);
        if (box != null) requireBox(area, box);
        area.selectedBox = box;
        area.originSelected = false;
        area.corner = Corner.NONE;
    }

    public void removeBox(Area area, Box box) {
        requireBox(area, box);
        if (area == simple) throw new IllegalArgumentException("Simple mode has exactly one subregion");
        area.boxes.remove(box);
        if (area.selectedBox == box) { area.selectedBox = null; area.corner = Corner.NONE; }
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

    public void setOrigin(Area area, Vector3i origin) {
        require(area);
        if (origin != null) checkPoint(origin);
        area.manualOrigin = origin == null ? null : origin.clone();
        if (origin == null) area.originSelected = false;
    }

    public void selectOrigin(Area area, boolean selected) {
        require(area);
        if (selected && area.manualOrigin == null) throw new IllegalArgumentException("Manual origin is disabled");
        area.originSelected = selected;
        if (selected) area.corner = Corner.NONE;
    }

    public void selectCorner(Area area, Box box, Corner corner) {
        java.util.Objects.requireNonNull(corner);
        requireBox(area, box);
        selectBox(area, box);
        area.corner = corner;
    }

    public void setTargetPoint(Area area, boolean first, Vector3i point) {
        require(area);
        if (area.originSelected) setOrigin(area, point);
        else {
            setPoints(area, first ? point : area.first(), first ? area.second() : point);
            area.corner = first ? Corner.FIRST : Corner.SECOND;
        }
    }

    public void click(Area area, boolean first, Vector3i point) {
        require(area);
        checkPoint(point);
        if (area.originSelected || cornerMode == CornerMode.CORNERS) setTargetPoint(area, first, point);
        else if (!first) setPoints(area, point, point);
        else {
            requireBox(area, area.selectedBox);
            Vector3i a = area.first(), b = area.second();
            setPoints(area, new Vector3i(Math.min(point.x, Math.min(a.x, b.x)), Math.min(point.y, Math.min(a.y, b.y)), Math.min(point.z, Math.min(a.z, b.z))),
                new Vector3i(Math.max(point.x, Math.max(a.x, b.x)), Math.max(point.y, Math.max(a.y, b.y)), Math.max(point.z, Math.max(a.z, b.z))));
        }
    }

    public void moveSelected(Area area, int x, int y, int z) {
        require(area);
        if (area.originSelected) setOrigin(area, offset(area.manualOrigin, x, y, z));
        else {
            requireBox(area, area.selectedBox);
            Vector3i a = area.first(), b = area.second();
            if (area.corner != Corner.SECOND) a = offset(a, x, y, z);
            if (area.corner != Corner.FIRST) b = offset(b, x, y, z);
            setPoints(area, a, b);
        }
    }

    private static Vector3i offset(Vector3i point, int x, int y, int z) {
        return new Vector3i(Math.addExact(point.x, x), Math.addExact(point.y, y), Math.addExact(point.z, z));
    }

    public void moveEntire(Area area, int x, int y, int z) {
        require(area);
        List<Vector3i> points = new ArrayList<>();
        for (Box box : area.boxes) {
            Vector3i a = offset(box.first, x, y, z), b = offset(box.second, x, y, z);
            checkPoint(a); checkPoint(b); points.add(a); points.add(b);
        }
        Vector3i origin = area.manualOrigin == null ? null : offset(area.manualOrigin, x, y, z);
        if (origin != null) checkPoint(origin);
        for (int i = 0; i < area.boxes.size(); i++) {
            area.boxes.get(i).first.set(points.get(i * 2)); area.boxes.get(i).second.set(points.get(i * 2 + 1));
        }
        area.manualOrigin = origin;
    }

    public void growSelected(Area area, int amount) {
        require(area); requireBox(area, area.selectedBox);
        Vector3i a = area.first(), b = area.second();
        int[] first = {a.x, a.y, a.z}, second = {b.x, b.y, b.z};
        for (int axis = 0; axis < 3; axis++) {
            long min = Math.min(first[axis], second[axis]), max = Math.max(first[axis], second[axis]);
            long lower = min - amount, upper = max + amount;
            if (lower > upper) lower = upper = (min + max) >> 1;
            boolean ascending = first[axis] <= second[axis];
            first[axis] = Math.toIntExact(ascending ? lower : upper);
            second[axis] = Math.toIntExact(ascending ? upper : lower);
        }
        setPoints(area, new Vector3i(first[0], first[1], first[2]), new Vector3i(second[0], second[1], second[2]));
    }

    private void requireBox(Area area, Box box) {
        require(area);
        if (!area.boxes.contains(box)) throw new IllegalArgumentException("Subregion is no longer in this selection");
    }

    private static String uniqueBoxName(Area area, String name, Box except) {
        String checked = validName(name);
        for (Box box : area.boxes) {
            if (box != except && box.name.equalsIgnoreCase(checked)) throw new NameConflictException(checked, true);
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
            if (area != except && area.name.equalsIgnoreCase(checked)) throw new NameConflictException(checked, false);
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
        data.addProperty("version", 5);
        data.addProperty("mode", mode.name());
        data.addProperty("cornerMode", cornerMode.name());
        data.add("simple", writeArea(simple));
        data.add("selected", selected == null ? JsonNull.INSTANCE : new com.google.gson.JsonPrimitive(selected.id));
        JsonArray entries = new JsonArray();
        for (Area area : areas) entries.add(writeArea(area));
        data.add("selections", entries);
        for (String key : new String[] {"ax", "ay", "az", "bx", "by", "bz", "renderingGuide"}) data.remove(key);
        return data;
    }

    private static JsonObject writeArea(Area area) {
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
        JsonObject origin = new JsonObject();
        if (area.manualOrigin != null) {
            origin.addProperty("x", area.manualOrigin.x); origin.addProperty("y", area.manualOrigin.y); origin.addProperty("z", area.manualOrigin.z);
        }
        entry.add("origin", area.manualOrigin == null ? JsonNull.INSTANCE : origin);
        entry.addProperty("originSelected", area.originSelected);
        entry.addProperty("selectedCorner", area.corner.name());
        return entry;
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
        if (version < 2 || version > 5) throw new IllegalArgumentException("Unsupported selection library version");
        for (JsonElement element : data.getAsJsonArray("selections")) {
            if (library.areas.size() >= 4096) throw new IllegalArgumentException("Too many selections");
            Area checked = readArea(element.getAsJsonObject(), version);
            library.uniqueName(checked.name, null);
            for (Area area : library.areas) if (area.id.equals(checked.id)) throw new IllegalArgumentException("Duplicate selection ID");
            library.areas.add(checked);
        }
        JsonElement selected = data.get("selected");
        if (selected == null) throw new IllegalArgumentException("Missing selected area");
        if (!selected.isJsonNull()) {
            for (Area area : library.areas) if (area.id.equals(selected.getAsString())) library.selected = area;
            if (library.selected == null) throw new IllegalArgumentException("Unknown selected area");
        }
        if (version >= 5) {
            library.simple = readArea(data.getAsJsonObject("simple"), version);
            if (library.simple.boxes.size() != 1 || library.simple.selectedBox == null) throw new IllegalArgumentException("Invalid simple selection");
            library.mode = Mode.valueOf(data.get("mode").getAsString());
            library.cornerMode = CornerMode.valueOf(data.get("cornerMode").getAsString());
        }
        return library;
    }

    private static Area readArea(JsonObject entry, int version) {
        Area area = new Area(UUID.fromString(entry.get("id").getAsString()).toString(), validName(entry.get("name").getAsString()));
        if (version == 2) {
            area.selectedBox = readBox(entry, entry.get("boxName").getAsString());
            area.boxes.add(area.selectedBox);
        } else {
            for (JsonElement element : entry.getAsJsonArray("boxes")) {
                if (area.boxes.size() >= 256) throw new IllegalArgumentException("Too many subregions");
                JsonObject value = element.getAsJsonObject();
                area.boxes.add(readBox(value, uniqueBoxName(area, value.get("name").getAsString(), null)));
            }
            JsonElement selectedBox = entry.get("selectedBox");
            if (selectedBox == null) throw new IllegalArgumentException("Missing selected subregion");
            if (!selectedBox.isJsonNull()) {
                for (Box box : area.boxes) if (box.name.equals(selectedBox.getAsString())) area.selectedBox = box;
                if (area.selectedBox == null) throw new IllegalArgumentException("Unknown selected subregion");
            }
        }
        if (version >= 4) {
            JsonElement origin = entry.get("origin");
            if (origin == null) throw new IllegalArgumentException("Missing origin state");
            if (!origin.isJsonNull()) { area.manualOrigin = point(origin.getAsJsonObject(), ""); checkPoint(area.manualOrigin); }
            if (!entry.get("originSelected").getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException("Invalid origin selection flag");
            area.originSelected = entry.get("originSelected").getAsBoolean();
            if (area.originSelected && area.manualOrigin == null) throw new IllegalArgumentException("Manual origin is disabled");
        }
        area.guide = guide(entry);
        area.extra = copyJson(entry);
        if (version >= 5) readCorner(area, entry);
        return area;
    }

    private static Box readBox(JsonObject value, String name) {
        Vector3i a = point(value, "a"), b = point(value, "b");
        checkPoint(a); checkPoint(b);
        Box box = new Box(validName(name), a, b);
        box.extra = copyJson(value);
        return box;
    }

    private static void readCorner(Area area, JsonObject entry) {
        area.corner = Corner.valueOf(entry.get("selectedCorner").getAsString());
        if (area.corner != Corner.NONE && (area.selectedBox == null || area.originSelected)) throw new IllegalArgumentException("Invalid selected corner");
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
        private Vector3i manualOrigin;
        private boolean originSelected;
        private Corner corner = Corner.NONE;
        private JsonObject extra = new JsonObject();

        private Area(String id, String name) { this.id = id; this.name = name; }

        public String name() { return name; }
        public List<Box> boxes() { return Collections.unmodifiableList(boxes); }
        public Box selectedBox() { return selectedBox; }
        public String boxName() { return selectedBox == null ? "" : selectedBox.name(); }
        public Vector3i first() { return selectedBox == null ? new Vector3i() : selectedBox.first(); }
        public Vector3i second() { return selectedBox == null ? new Vector3i() : selectedBox.second(); }
        public boolean guide() { return guide; }
        public Vector3i manualOrigin() { return manualOrigin == null ? null : manualOrigin.clone(); }
        public boolean originSelected() { return originSelected; }
        public Corner selectedCorner() { return corner; }
        public Vector3i origin() {
            if (manualOrigin != null) return manualOrigin.clone();
            if (boxes.isEmpty()) return new Vector3i();
            Vector3i min = boxes.get(0).first();
            for (Box box : boxes) {
                min.x = Math.min(min.x, Math.min(box.first.x, box.second.x));
                min.y = Math.min(min.y, Math.min(box.first.y, box.second.y));
                min.z = Math.min(min.z, Math.min(box.first.z, box.second.z));
            }
            return min;
        }
        public com.github.lunatrius.schematica.world.storage.RegionSelection snapshot() {
            Vector3i point = origin();
            return new com.github.lunatrius.schematica.world.storage.RegionSelection(regions(),
                new com.github.lunatrius.schematica.api.SchematicOrigin(point.x, point.y, point.z));
        }
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
        public final boolean subregion;

        public NameConflictException(String name, boolean subregion) { super(name); this.subregion = subregion; }
    }
}

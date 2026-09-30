package com.github.lunatrius.schematica.client.world;

import com.github.lunatrius.schematica.api.SchematicOrigin;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class PlacementSettings {
    public static final PlacementSettings DEFAULT = new PlacementSettings(true, false, 0, false);
    public static final String LOCKED_MESSAGE = "litematica.message.placement.cant_modify_is_locked";

    public final boolean enabled, locked, enclosingBox;
    public final int coordinateLocks;

    private PlacementSettings(boolean enabled, boolean locked, int coordinateLocks, boolean enclosingBox) {
        if (coordinateLocks < 0 || coordinateLocks > 7) throw new IllegalArgumentException("Invalid coordinate locks");
        this.enabled = enabled;
        this.locked = locked;
        this.coordinateLocks = coordinateLocks;
        this.enclosingBox = enclosingBox;
    }

    public PlacementSettings enabled(boolean value) { return new PlacementSettings(value, locked, coordinateLocks, enclosingBox); }
    public PlacementSettings locked(boolean value) { return new PlacementSettings(enabled, value, coordinateLocks, enclosingBox); }
    public PlacementSettings enclosingBox(boolean value) { return new PlacementSettings(enabled, locked, coordinateLocks, value); }
    public PlacementSettings coordinateLocks(int value) { return new PlacementSettings(enabled, locked, value, enclosingBox); }
    public boolean renders(boolean rendering) { return enabled && rendering; }
    public boolean coordinateLocked(int axis) {
        if (axis < 0 || axis > 2) throw new IllegalArgumentException("Invalid coordinate axis");
        return (coordinateLocks & (1 << axis)) != 0;
    }

    public SchematicOrigin constrainOrigin(SchematicOrigin current, SchematicOrigin requested) {
        if (locked) return current;
        return new SchematicOrigin(coordinateLocked(0) ? current.x : requested.x,
            coordinateLocked(1) ? current.y : requested.y, coordinateLocked(2) ? current.z : requested.z);
    }

    public boolean allowsRegionChange(SubRegionPlacements before, SubRegionPlacements after) {
        return !locked || before.sameGeometry(after);
    }

    public JsonObject toJson() {
        JsonObject data = new JsonObject();
        data.addProperty("version", 1);
        data.addProperty("enabled", enabled);
        data.addProperty("locked", locked);
        data.addProperty("coordinateLocks", coordinateLocks);
        data.addProperty("enclosingBox", enclosingBox);
        return data;
    }

    public static PlacementSettings fromJson(JsonObject data) {
        if (data == null) return DEFAULT.enclosingBox(true);
        if (integer(data.get("version")) != 1) throw new IllegalArgumentException("Unsupported placement settings");
        return new PlacementSettings(bool(data.get("enabled")), bool(data.get("locked")),
            integer(data.get("coordinateLocks")), bool(data.get("enclosingBox")));
    }

    private static boolean bool(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException("Invalid placement flag");
        return value.getAsBoolean();
    }

    private static int integer(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Invalid placement integer");
        try { return value.getAsBigDecimal().intValueExact(); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("Invalid placement integer", e); }
    }
}

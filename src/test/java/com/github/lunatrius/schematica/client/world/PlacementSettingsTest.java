package com.github.lunatrius.schematica.client.world;

import com.github.lunatrius.schematica.api.SchematicOrigin;
import com.google.gson.JsonObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class PlacementSettingsTest {
    @Test public void enabledAndRenderingRemainIndependent() {
        PlacementSettings state = PlacementSettings.DEFAULT;
        assertTrue(state.enabled);
        assertTrue(state.renders(true));
        assertFalse(state.renders(false));
        PlacementSettings disabled = state.enabled(false);
        assertFalse(disabled.renders(true));
        assertFalse(disabled.renders(false));
        assertTrue(disabled.enabled(true).renders(true));
        assertTrue(state.enabled);
    }

    @Test public void allCoordinateMasksConstrainWorldOriginAndKeepSourceOffset() {
        SchematicOrigin current = new SchematicOrigin(101, 65, -40), requested = new SchematicOrigin(-80, 120, 16);
        SchematicOrigin offset = new SchematicOrigin(-2, 30, 9);
        for (int mask = 0; mask < 8; mask++) {
            PlacementSettings settings = PlacementSettings.DEFAULT.coordinateLocks(mask);
            SchematicOrigin target = settings.constrainOrigin(current, requested);
            for (int axis = 0; axis < 3; axis++) {
                assertEquals((mask & 1 << axis) != 0, settings.coordinateLocked(axis));
                assertEquals((mask & 1 << axis) != 0 ? current.coordinates()[axis] : requested.coordinates()[axis], target.coordinates()[axis]);
            }
            SchematicOrigin min = offset.minimumAt(target);
            assertArrayEquals(target.coordinates(), offset.atMinimum(min.x, min.y, min.z).coordinates());
            assertSame(current, settings.locked(true).constrainOrigin(current, requested));
            assertArrayEquals(target.coordinates(), settings.locked(true).locked(false).constrainOrigin(current, requested).coordinates());
        }
    }

    @Test public void displayFlagsAndCoordinateLocksSurvivePersistenceWhileLocked() {
        for (boolean enabled : new boolean[] {true, false}) for (boolean locked : new boolean[] {true, false}) {
            for (boolean box : new boolean[] {true, false}) for (int mask = 0; mask < 8; mask++) {
                PlacementSettings original = PlacementSettings.DEFAULT.enabled(enabled).locked(locked).enclosingBox(box).coordinateLocks(mask);
                JsonObject saved = original.toJson();
                PlacementSettings restored = PlacementSettings.fromJson(saved);
                assertEquals(saved, restored.toJson());
                assertEquals(enabled, restored.enabled);
                assertEquals(locked, restored.locked);
                assertEquals(box, restored.enclosingBox);
                assertEquals(mask, restored.coordinateLocks);
                saved.addProperty("enabled", !enabled);
                assertEquals(enabled, restored.enabled);
                assertEquals(enabled, original.enabled);
            }
        }
    }

    @Test public void legacySessionsKeepTheirExistingEnclosingOutlineAndVisibilityMeaning() {
        PlacementSettings legacy = PlacementSettings.fromJson(null);
        assertTrue(legacy.enabled);
        assertTrue(legacy.enclosingBox);
        assertFalse(legacy.locked);
        assertEquals(0, legacy.coordinateLocks);
        assertFalse(legacy.renders(false));
        assertTrue(legacy.renders(true));
        assertFalse(PlacementSettings.DEFAULT.enclosingBox);
    }

    @Test public void invalidSavedSettingsFailWithoutChangingDefaults() {
        JsonObject version = PlacementSettings.DEFAULT.toJson(); version.addProperty("version", 2);
        assertThrows(IllegalArgumentException.class, () -> PlacementSettings.fromJson(version));
        for (String key : new String[] {"enabled", "locked", "enclosingBox", "coordinateLocks", "version"}) {
            JsonObject missing = PlacementSettings.DEFAULT.toJson(); missing.remove(key);
            assertThrows(IllegalArgumentException.class, () -> PlacementSettings.fromJson(missing));
            JsonObject string = PlacementSettings.DEFAULT.toJson(); string.addProperty(key, "true");
            assertThrows(IllegalArgumentException.class, () -> PlacementSettings.fromJson(string));
        }
        for (Number value : new Number[] {-1, 8, 1.5, 4294967296L}) {
            JsonObject bad = PlacementSettings.DEFAULT.toJson(); bad.addProperty("coordinateLocks", value);
            assertThrows(IllegalArgumentException.class, () -> PlacementSettings.fromJson(bad));
        }
        assertThrows(IllegalArgumentException.class, () -> PlacementSettings.DEFAULT.coordinateLocked(3));
        assertThrows(IllegalArgumentException.class, () -> PlacementSettings.DEFAULT.coordinateLocks(8));
        assertTrue(PlacementSettings.DEFAULT.enabled);
    }
}

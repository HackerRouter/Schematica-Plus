package com.github.lunatrius.schematica.client.selection;

import org.junit.Test;
import static org.junit.Assert.*;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Area;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Corner;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.CornerMode;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Mode;
import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;
import com.google.gson.JsonObject;

public class SelectionOperationsTest {
    @Test public void modeSwitchPreservesIndependentGeometryOriginsAndSelectedCorners() {
        AreaSelectionLibrary library = AreaSelectionLibrary.fromJson(null);
        Area normal = library.selected();
        library.setPoints(normal, new Vector3i(10, 20, 30), new Vector3i(40, 50, 60));
        library.selectCorner(normal, normal.selectedBox(), Corner.SECOND);
        library.setMode(Mode.SIMPLE);
        Area simple = library.selected();
        library.setPoints(simple, new Vector3i(-4, 5, 6), new Vector3i(-3, 8, 9));
        library.setOrigin(simple, new Vector3i(-10, 0, 0));
        library.selectOrigin(simple, true);
        library.setCornerMode(CornerMode.EXPAND);
        AreaSelectionLibrary restored = AreaSelectionLibrary.fromJson(library.toJson());
        assertEquals(Mode.SIMPLE, restored.mode());
        assertEquals(CornerMode.EXPAND, restored.cornerMode());
        assertEquals(simple.first(), restored.selected().first());
        assertTrue(restored.selected().originSelected());
        restored.setMode(Mode.NORMAL);
        assertEquals(normal.second(), restored.selected().second());
        assertEquals(Corner.SECOND, restored.selected().selectedCorner());
        assertEquals(1, restored.areas().size());
    }

    @Test public void simpleAlwaysHasOneSelectedBoxAndCanBeCopiedToNormal() {
        AreaSelectionLibrary library = AreaSelectionLibrary.fromJson(null);
        library.setMode(Mode.SIMPLE);
        Area simple = library.selected();
        library.selectBox(simple, null);
        assertNotNull(simple.selectedBox());
        assertThrows(IllegalArgumentException.class, () -> library.addBox(simple, "extra", new Vector3i(), new Vector3i()));
        assertThrows(IllegalArgumentException.class, () -> library.removeBox(simple, simple.selectedBox()));
        assertThrows(IllegalArgumentException.class, () -> library.remove(simple));
        library.rename(simple, "Named");
        assertEquals("Named", simple.boxName());
        library.select(library.copy(simple, "Copy"));
        assertEquals(Mode.NORMAL, library.mode());
        library.setPoints(library.selected(), new Vector3i(1, 2, 3), new Vector3i(4, 5, 6));
        assertEquals(new Vector3i(), simple.first());
    }

    @Test public void expandIncludesClickedPointNormalizesReversedCornersAndRightClickResets() {
        AreaSelectionLibrary library = AreaSelectionLibrary.fromJson(null);
        Area area = library.selected();
        library.setPoints(area, new Vector3i(10, 70, 20), new Vector3i(0, 60, -5));
        library.setCornerMode(CornerMode.EXPAND);
        library.click(area, true, new Vector3i(-2, 65, 30));
        assertEquals(new Vector3i(-2, 60, -5), area.first());
        assertEquals(new Vector3i(10, 70, 30), area.second());
        library.click(area, true, new Vector3i(2, 63, 1));
        assertEquals(new Vector3i(-2, 60, -5), area.first());
        library.click(area, false, new Vector3i(7, 80, 9));
        assertEquals(new Vector3i(7, 80, 9), area.first());
        assertEquals(area.first(), area.second());
    }

    @Test public void selectedCornerBoxAndOriginMoveIndependentlyAndBoundsFailAtomically() {
        AreaSelectionLibrary library = AreaSelectionLibrary.fromJson(null);
        Area area = library.selected();
        library.setPoints(area, new Vector3i(0, 250, 0), new Vector3i(4, 255, 4));
        library.selectCorner(area, area.selectedBox(), Corner.FIRST);
        library.moveSelected(area, 0, 1, 0);
        assertEquals(251, area.first().y); assertEquals(255, area.second().y);
        library.selectCorner(area, area.selectedBox(), Corner.NONE);
        assertThrows(IllegalArgumentException.class, () -> library.moveSelected(area, 0, 1, 0));
        assertEquals(251, area.first().y);
        library.moveSelected(area, 1, 0, -2);
        assertEquals(new Vector3i(1, 251, -2), area.first());
        assertEquals(new Vector3i(5, 255, 2), area.second());
        library.setOrigin(area, new Vector3i(0, 64, 0));
        library.selectOrigin(area, true);
        library.setCornerMode(CornerMode.EXPAND);
        library.click(area, false, new Vector3i(5, 60, 5));
        library.moveSelected(area, -1, 0, 0);
        assertEquals(new Vector3i(4, 60, 5), area.manualOrigin());
        assertEquals(new Vector3i(1, 251, -2), area.first());
    }

    @Test public void legacyModeDefaultsAndInvalidNewStateAreRejected() {
        JsonObject data = AreaSelectionLibrary.fromJson(null).toJson();
        data.addProperty("version", 4);
        data.remove("simple"); data.remove("mode"); data.remove("cornerMode");
        AreaSelectionLibrary migrated = AreaSelectionLibrary.fromJson(data);
        assertEquals(Mode.NORMAL, migrated.mode());
        assertEquals(Corner.NONE, migrated.selected().selectedCorner());
        data = migrated.toJson();
        data.getAsJsonObject("simple").add("selectedBox", com.google.gson.JsonNull.INSTANCE);
        final JsonObject invalidSimple = data;
        assertThrows(IllegalArgumentException.class, () -> AreaSelectionLibrary.fromJson(invalidSimple));
        data = migrated.toJson(); data.addProperty("mode", "unknown");
        final JsonObject invalidMode = data;
        assertThrows(IllegalArgumentException.class, () -> AreaSelectionLibrary.fromJson(invalidMode));
    }

    @Test public void cornerClicksSelectOnlyTheMovedCornerAndDeselectionClearsIt() {
        AreaSelectionLibrary library = AreaSelectionLibrary.fromJson(null);
        Area area = library.selected();
        library.click(area, true, new Vector3i(1, 2, 3));
        assertEquals(Corner.FIRST, area.selectedCorner());
        library.click(area, false, new Vector3i(4, 5, 6));
        assertEquals(Corner.SECOND, area.selectedCorner());
        library.selectBox(area, null);
        assertEquals(Corner.NONE, area.selectedCorner());
        assertThrows(IllegalArgumentException.class, () -> library.click(area, true, new Vector3i()));
    }

    @Test public void raySelectsCornersBodiesAndOriginsWithDistanceClipping() {
        AreaSelectionLibrary library = AreaSelectionLibrary.fromJson(null);
        Area area = library.selected();
        library.setPoints(area, new Vector3i(0, 60, 0), new Vector3i(4, 64, 4));
        SelectionRayTrace.Hit hit = SelectionRayTrace.trace(area, 0.5, 60.5, -5, 0, 0, 1, 20);
        assertSame(area.selectedBox(), hit.box); assertEquals(Corner.FIRST, hit.corner); assertEquals(5, hit.distance, 0);
        assertEquals(Corner.NONE, SelectionRayTrace.trace(area, 2.5, 62.5, -5, 0, 0, 1, 20).corner);
        assertNull(SelectionRayTrace.trace(area, 0.5, 60.5, -5, 0, 0, 1, 4.9));
        assertNull(SelectionRayTrace.trace(area, 20, 60.5, -5, 0, 0, 1, 20));
        library.setOrigin(area, new Vector3i(0, 60, 0));
        assertNull(SelectionRayTrace.trace(area, 0.5, 60.5, -5, 0, 0, 1, 20).box);
        assertEquals(0, SelectionRayTrace.trace(area, 2.5, 62.5, 2.5, 1, 0, 0, 20).distance, 0);
    }
}

package com.github.lunatrius.schematica.client.selection;

import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;
import com.github.lunatrius.schematica.world.storage.RegionSelection;
import com.google.gson.JsonObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class AreaOriginTest {
    @Test public void manualOriginOutsideBoxesDoesNotExpandOrMoveCapturedArea() {
        AreaSelectionLibrary library = new AreaSelectionLibrary();
        AreaSelectionLibrary.Area area = library.create("Factory", new Vector3i(10, 60, -4), new Vector3i(13, 63, -1));
        library.setOrigin(area, new Vector3i(-5, 70, 20));
        RegionSelection snapshot = area.snapshot();
        assertEquals(10, snapshot.minX); assertEquals(13, snapshot.maxX);
        assertEquals(60, snapshot.minY); assertEquals(63, snapshot.maxY);
        assertArrayEquals(new int[] {-15, 10, 24}, snapshot.localOrigin.coordinates());
        library.setOrigin(area, new Vector3i(1, 2, 3));
        assertArrayEquals(new int[] {-15, 10, 24}, snapshot.localOrigin.coordinates());
        assertEquals(new Vector3i(10, 60, -4), area.first());
    }

    @Test public void toolTargetAndCopyKeepOriginSeparateFromCorners() {
        AreaSelectionLibrary library = AreaSelectionLibrary.fromJson(null);
        AreaSelectionLibrary.Area area = library.selected();
        library.setOrigin(area, new Vector3i(5, 64, 6));
        library.selectOrigin(area, true);
        library.setTargetPoint(area, false, new Vector3i(8, 65, 9));
        assertEquals(new Vector3i(), area.second());
        assertEquals(new Vector3i(8, 65, 9), area.origin());
        AreaSelectionLibrary.Area copy = library.copy(area, "Copy");
        library.setTargetPoint(copy, true, new Vector3i(10, 66, 11));
        assertEquals(new Vector3i(8, 65, 9), area.origin());
        library.selectBox(area, area.selectedBox());
        assertFalse(area.originSelected());
        library.setTargetPoint(area, true, new Vector3i(1, 2, 3));
        assertEquals(new Vector3i(1, 2, 3), area.first());
        assertEquals(new Vector3i(8, 65, 9), area.origin());
        library.setOrigin(copy, null);
        assertFalse(copy.originSelected());
        assertThrows(IllegalArgumentException.class, () -> library.selectOrigin(copy, true));
    }

    @Test public void persistenceAndDisablingOriginRestoreCalculatedMinimum() {
        AreaSelectionLibrary library = AreaSelectionLibrary.fromJson(null);
        AreaSelectionLibrary.Area area = library.selected();
        library.setPoints(area, new Vector3i(20, 70, 4), new Vector3i(15, 60, -2));
        library.addBox(area, "Pipes", new Vector3i(8, 64, -10), new Vector3i(10, 66, -5));
        library.setOrigin(area, new Vector3i(40, 80, 50));
        library.selectOrigin(area, true);
        AreaSelectionLibrary restored = AreaSelectionLibrary.fromJson(library.toJson());
        assertTrue(restored.selected().originSelected());
        assertEquals(new Vector3i(40, 80, 50), restored.selected().origin());
        restored.setOrigin(restored.selected(), null);
        assertEquals(new Vector3i(8, 60, -10), restored.selected().origin());
        assertNull(AreaSelectionLibrary.fromJson(restored.toJson()).selected().manualOrigin());
        assertThrows(IllegalArgumentException.class, () -> library.setOrigin(area, new Vector3i(0, 256, 0)));
        assertEquals(new Vector3i(40, 80, 50), area.origin());
        JsonObject version3 = library.toJson(); version3.addProperty("version", 3);
        assertNull(AreaSelectionLibrary.fromJson(version3).selected().manualOrigin());
    }
}

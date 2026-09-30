package com.github.lunatrius.schematica.client.selection;

import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Area;
import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;
import com.google.gson.JsonObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class AreaSelectionLibraryTest {
    @Test public void selectionsAndCopiesKeepIndependentCornersNamesAndVisibility() {
        AreaSelectionLibrary library = new AreaSelectionLibrary();
        Vector3i first = new Vector3i(10, 64, -8);
        Area original = library.create("Factory", first, new Vector3i(20, 80, 9));
        library.renameBox(original, "Machines");
        library.setGuide(original, false);
        Area copy = library.copy(original, "Factory copy");
        first.set(0, 0, 0);
        copy.first().set(0, 0, 0);
        library.setPoints(copy, new Vector3i(1, 2, 3), new Vector3i(4, 5, 6));
        library.renameBox(copy, "Pipes");
        library.setGuide(copy, true);
        assertEquals(new Vector3i(10, 64, -8), original.first());
        assertEquals("Machines", original.boxName());
        assertEquals("Pipes", copy.boxName());
        assertFalse(original.guide());
        assertTrue(copy.guide());
        library.select(original);
        library.remove(copy);
        assertSame(original, library.selected());
        library.remove(original);
        assertNull(library.selected());
        assertTrue(library.areas().isEmpty());
    }

    @Test public void rejectsStaleAreasDuplicateNamesAndInvalidCoordinatesWithoutPartialChanges() {
        AreaSelectionLibrary library = new AreaSelectionLibrary();
        Area area = library.create("Area", new Vector3i(), new Vector3i(1, 1, 1));
        Area other = library.create("Other", new Vector3i(), new Vector3i());
        assertThrows(AreaSelectionLibrary.NameConflictException.class, () -> library.rename(other, " area "));
        assertEquals("Other", other.name());
        assertThrows(IllegalArgumentException.class, () -> library.rename(area, "\n"));
        assertThrows(IllegalArgumentException.class, () -> library.setPoints(area, new Vector3i(2, 2, 2), new Vector3i(0, 256, 0)));
        assertEquals(new Vector3i(), area.first());
        assertThrows(IllegalArgumentException.class, () -> new AreaSelectionLibrary().select(area));
        library.remove(area);
        assertThrows(IllegalArgumentException.class, () -> library.copy(area, "Removed"));
        assertThrows(UnsupportedOperationException.class, () -> library.areas().clear());
    }

    @Test public void roundTripRetainsExplicitDeselectionAndEmptyLibrary() {
        AreaSelectionLibrary library = AreaSelectionLibrary.fromJson(null);
        Area original = library.selected();
        library.select(null);
        AreaSelectionLibrary restored = AreaSelectionLibrary.fromJson(library.toJson());
        assertEquals(1, restored.areas().size());
        assertNull(restored.selected());
        library.remove(original);
        restored = AreaSelectionLibrary.fromJson(library.toJson());
        assertTrue(restored.areas().isEmpty());
        assertNull(restored.selected());
    }

    @Test public void rejectsUnsupportedVersionDuplicateIdsAndFractionalOrOverflowingCoordinates() {
        JsonObject valid = AreaSelectionLibrary.fromJson(null).toJson();
        valid.addProperty("version", 3);
        assertThrows(IllegalArgumentException.class, () -> AreaSelectionLibrary.fromJson(valid));
        valid.addProperty("version", 2);
        JsonObject entry = valid.getAsJsonArray("selections").get(0).getAsJsonObject();
        entry.addProperty("ax", 0.5);
        assertThrows(ArithmeticException.class, () -> AreaSelectionLibrary.fromJson(valid));
        entry.addProperty("ax", 4294967296L);
        assertThrows(ArithmeticException.class, () -> AreaSelectionLibrary.fromJson(valid));
        entry.addProperty("ax", 0);
        valid.getAsJsonArray("selections").add(entry);
        assertThrows(IllegalArgumentException.class, () -> AreaSelectionLibrary.fromJson(valid));
    }
}

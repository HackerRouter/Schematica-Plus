package com.github.lunatrius.schematica.client.selection;

import java.util.Arrays;
import java.util.List;

import com.github.lunatrius.schematica.api.SchematicRegion;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Area;
import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;

import com.google.gson.JsonObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class AreaSelectionLibraryTest {
    @Test public void placementImportIsAtomicAndKeepsEveryTransformedBox() {
        AreaSelectionLibrary library = AreaSelectionLibrary.fromJson(null);
        Area original = library.selected();
        List<SchematicRegion> regions = Arrays.asList(
            new SchematicRegion("Machines", -5, 60, 2, -3, 62, 4),
            new SchematicRegion("Pipes", 6, 64, 2, 10, 64, 2));
        Area imported = library.createFromRegions("Import", regions);
        assertEquals(2, imported.boxes().size());
        assertEquals(new Vector3i(-5, 60, 2), imported.first());
        assertEquals("Pipes", imported.boxes().get(1).name());
        assertSame(original, library.selected());
        assertThrows(IllegalArgumentException.class, () -> library.createFromRegions("Invalid", Arrays.asList(regions.get(0),
            new SchematicRegion("Bad", 0, 256, 0, 0, 257, 0))));
        assertEquals(2, library.areas().size());
        assertSame(original, library.selected());
        library.select(imported);
        Area restored = AreaSelectionLibrary.fromJson(library.toJson()).selected();
        assertEquals(2, restored.boxes().size());
        assertEquals(new Vector3i(10, 64, 2), restored.boxes().get(1).second());
    }
    @Test public void subregionsCopyRenameSelectAndDeleteIndependently() {
        AreaSelectionLibrary library = AreaSelectionLibrary.fromJson(null);
        Area area = library.selected();
        AreaSelectionLibrary.Box original = area.selectedBox();
        AreaSelectionLibrary.Box pipes = library.addBox(area, "Pipes", new Vector3i(20, 64, 3), new Vector3i(24, 70, 5));
        assertSame(pipes, area.selectedBox());
        assertThrows(AreaSelectionLibrary.NameConflictException.class, () -> library.renameBox(area, pipes, "unnamed"));
        Area copy = library.copy(area, "Copy");
        library.setPoints(copy, new Vector3i(1, 2, 3), new Vector3i(4, 5, 6));
        assertEquals(new Vector3i(20, 64, 3), pipes.first());
        library.removeBox(area, pipes);
        assertNull(area.selectedBox());
        assertThrows(IllegalArgumentException.class, () -> library.selectBox(area, pipes));
        AreaSelectionLibrary restored = AreaSelectionLibrary.fromJson(library.toJson());
        assertNull(restored.selected().selectedBox());
        assertEquals(1, restored.selected().boxes().size());
        library.removeBox(area, original);
        restored = AreaSelectionLibrary.fromJson(library.toJson());
        assertTrue(restored.selected().boxes().isEmpty());
        assertEquals(2, restored.areas().get(1).boxes().size());
        assertEquals("Pipes", restored.areas().get(1).selectedBox().name());
    }

    @Test public void preservesUnknownSelectionAndSubregionFields() {
        JsonObject data = AreaSelectionLibrary.fromJson(null).toJson();
        JsonObject entry = data.getAsJsonArray("selections").get(0).getAsJsonObject();
        entry.addProperty("future", 42);
        entry.getAsJsonArray("boxes").get(0).getAsJsonObject().addProperty("futureBox", true);
        JsonObject saved = AreaSelectionLibrary.fromJson(data).toJson().getAsJsonArray("selections").get(0).getAsJsonObject();
        assertEquals(42, saved.get("future").getAsInt());
        assertTrue(saved.getAsJsonArray("boxes").get(0).getAsJsonObject().get("futureBox").getAsBoolean());
    }
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
        valid.addProperty("version", 6);
        assertThrows(IllegalArgumentException.class, () -> AreaSelectionLibrary.fromJson(valid));
        valid.addProperty("version", 5);
        JsonObject entry = valid.getAsJsonArray("selections").get(0).getAsJsonObject();
        JsonObject box = entry.getAsJsonArray("boxes").get(0).getAsJsonObject();
        box.addProperty("ax", 0.5);
        assertThrows(ArithmeticException.class, () -> AreaSelectionLibrary.fromJson(valid));
        box.addProperty("ax", 4294967296L);
        assertThrows(ArithmeticException.class, () -> AreaSelectionLibrary.fromJson(valid));
        box.addProperty("ax", 0);
        valid.getAsJsonArray("selections").add(entry);
        assertThrows(IllegalArgumentException.class, () -> AreaSelectionLibrary.fromJson(valid));
    }

    @Test public void defaultSelectionsAreUnnamed() {
        assertEquals("Unnamed", AreaSelectionLibrary.fromJson(null).normalSelection().name());
        assertEquals("Unnamed", new AreaSelectionLibrary().simpleSelection().name());
    }
}

package com.github.lunatrius.schematica.client.world;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.api.SchematicOrigin;
import com.github.lunatrius.schematica.api.SchematicRegion;
import com.google.gson.JsonObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class SubRegionPlacementsTest {
    private SubRegionPlacements model(SchematicOrigin origin, SchematicRegion... regions) {
        ISchematic source = (ISchematic) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {ISchematic.class},
            (proxy, method, args) -> {
                switch (method.getName()) {
                    case "getWidth": return 20;
                    case "getHeight": return 6;
                    case "getLength": return 10;
                    case "getOrigin": return origin;
                    case "getRegions": return Arrays.asList(regions);
                    default: throw new UnsupportedOperationException(method.getName());
                }
            });
        return SubRegionPlacements.create(source);
    }

    private SubRegionPlacements model() {
        return model(new SchematicOrigin(2, 1, -3), new SchematicRegion("A", 0, 0, 0, 2, 3, 4), new SchematicRegion("B", 10, 0, 0, 12, 3, 4));
    }

    private List<String> operations(String sequence) {
        List<String> operations = new ArrayList<>();
        for (char operation : sequence.toCharArray()) operations.add(String.valueOf(operation));
        return operations;
    }

    @Test public void independentChangesKeepDefaultsAndOtherRegionsUntouched() {
        SubRegionPlacements initial = model();
        SubRegionPlacements next = initial.replace(initial.get("A").position(new SchematicOrigin(-9, 4, 7)).rotation(1).mirror(2));
        assertFalse(initial.modified());
        assertTrue(next.modified());
        assertSame(initial.get("B"), next.get("B"));
        assertArrayEquals(new int[] {-2, -1, 3}, next.get("A").defaultPosition.coordinates());
        assertArrayEquals(new int[] {-9, 4, 7}, next.get("A").position.coordinates());
        assertFalse(next.reset().modified());
        assertArrayEquals(initial.get("A").position.coordinates(), next.reset().get("A").position.coordinates());
    }

    @Test public void localTransformsAndGlobalInverseAgreeForEveryCorner() {
        for (int r = 0; r < 4; r++) for (int m = 0; m < 3; m++) {
            SubRegionPlacements.Region region = model().get("A").rotation(r).mirror(m);
            SchematicRegion bounds = region.bounds();
            for (String sequence : Arrays.asList("", "YxzYYz", "XYZxyzXYZ", "XXX", "yZX")) {
                List<String> global = operations(sequence);
                for (int x : new int[] {0, 2}) for (int y : new int[] {0, 3}) for (int z : new int[] {0, 4}) {
                    SchematicOrigin point = SubRegionPlacements.vector(new SchematicOrigin(x, y, z), region.operations(), false)
                        .atMinimum(region.position.x, region.position.y, region.position.z);
                    assertTrue(bounds.contains(point.x, point.y, point.z));
                    SchematicOrigin transformed = SubRegionPlacements.vector(point, global, false);
                    assertArrayEquals(point.coordinates(), SubRegionPlacements.vector(transformed, global, true).coordinates());
                }
            }
        }
    }

    @Test public void disablingEveryRegionDoesNotBecomeAnImplicitFullBox() {
        SubRegionPlacements original = model();
        SubRegionPlacements none = original.enabled(false);
        assertFalse(none.hasEnabled());
        assertTrue(none.layout().bounds.isEmpty());
        assertEquals(1, none.layout().width);
        SubRegionPlacements onlyB = none.replace(none.get("B").enabled(true));
        assertEquals(1, onlyB.layout().bounds.size());
        assertEquals(3, onlyB.layout().width);
        assertArrayEquals(new int[] {8, -1, 3}, onlyB.layout().minimum.coordinates());
        assertEquals(13, original.layout().width);
    }

    @Test public void locksAndVisibilityRemainIndependentOfEnabledState() {
        SubRegionPlacements.Region region = model().get("A").locks(5).position(new SchematicOrigin(80, 9, 30));
        assertArrayEquals(new int[] {-2, 9, 3}, region.position.coordinates());
        region = region.rendering(false).ignoreEntities(true);
        assertTrue(region.enabled);
        assertFalse(region.rendering);
        assertTrue(region.ignoreEntities);
        assertTrue(region.enabled(false).rendering(true).rendering);
    }

    @Test public void persistenceReloadAndSelectionUseNamesWithoutMutatingTheSource() {
        SubRegionPlacements original = model();
        SubRegionPlacements changed = original.replace(original.get("B").rotation(2).enabled(false).locks(3)).select("B");
        JsonObject saved = changed.toJson();
        SubRegionPlacements restored = model().restore(saved);
        assertEquals(saved, restored.toJson());
        assertFalse(original.modified());
        assertNull(original.selected);
        assertEquals("B", restored.selected);
        SubRegionPlacements reloaded = model(SchematicOrigin.ZERO, new SchematicRegion("A", 3, 1, 5, 9, 2, 6),
            new SchematicRegion("C", 0, 0, 0, 1, 1, 1)).restore(saved);
        assertArrayEquals(new int[] {3, 1, 5}, reloaded.get("A").position.coordinates());
        assertFalse(reloaded.modified());
        assertNull(reloaded.selected);
    }

    @Test public void oversizedEditsAndMalformedSavedStateFailWithoutPartialChanges() {
        SubRegionPlacements original = model();
        SubRegionPlacements far = original.replace(original.get("B").position(new SchematicOrigin(40000, 0, 0)));
        assertThrows(IllegalArgumentException.class, far::layout);
        assertEquals(13, original.layout().width);
        JsonObject saved = original.replace(original.get("B").rotation(2)).toJson();
        saved.getAsJsonArray("regions").get(0).getAsJsonObject().addProperty("rotation", 8);
        assertThrows(IllegalArgumentException.class, () -> original.restore(saved));
        saved.addProperty("version", 2);
        assertThrows(IllegalArgumentException.class, () -> original.restore(saved));
        assertFalse(original.modified());
        assertThrows(IllegalArgumentException.class, () -> original.select("missing"));
        assertThrows(IllegalArgumentException.class, () -> original.replace(model().get("B")));
    }

    @Test public void legacyFilesExposeOneEditableFullRegion() {
        SubRegionPlacements legacy = model(SchematicOrigin.ZERO);
        assertEquals(1, legacy.regions().size());
        assertEquals("Region", legacy.regions().get(0).name());
        assertEquals(20, legacy.layout().width);
        assertEquals(6, legacy.layout().height);
        assertEquals(10, legacy.layout().length);
        assertEquals(Collections.emptyList(), legacy.regions().get(0).operations());
        assertThrows(UnsupportedOperationException.class, () -> legacy.regions().clear());
    }
}

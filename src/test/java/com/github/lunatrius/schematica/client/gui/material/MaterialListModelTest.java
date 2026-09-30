package com.github.lunatrius.schematica.client.gui.material;

import java.util.Arrays;

import org.junit.Test;
import static org.junit.Assert.*;

public class MaterialListModelTest {
    private MaterialListModel.Entry<String> entry(String name, int total, int missing, long available) {
        MaterialListModel.Entry<String> entry = new MaterialListModel.Entry<>(name, name, "mod:" + name, total, missing, 0, 0);
        entry.available = available;
        return entry;
    }

    @Test public void multiplierDoesNotCreditOneBuiltPlacementToExtraCopies() {
        MaterialListModel<String> model = new MaterialListModel<>();
        MaterialListModel.Entry<String> entry = entry("stone", Integer.MAX_VALUE, 1, 2);
        model.setEntries(Arrays.asList(entry));
        assertEquals(1, model.missing(entry));
        model.setMultiplier(Integer.MAX_VALUE);
        assertEquals(4611686014132420609L, model.total(entry));
        assertEquals(model.total(entry), model.missing(entry));
        model.setMultiplier(-1);
        assertEquals(1, model.multiplier());
        assertEquals(1, model.missing(entry));
    }

    @Test public void filteringAndIgnoringDoNotChangePlacementProgress() {
        MaterialListModel<String> model = new MaterialListModel<>();
        model.setEntries(Arrays.asList(entry("Alpha", 10, 4, 5), entry("Beta", 8, 7, 3)));
        long[] progress = model.progress();
        model.setHideAvailable(true);
        assertEquals("Beta", model.visible().get(0).key);
        model.ignore("Beta");
        assertTrue(model.visible().isEmpty());
        model.setEntries(Arrays.asList(entry("Beta", 8, 7, 0)));
        assertTrue(model.visible().isEmpty());
        model.clearIgnored();
        assertEquals(1, model.visible().size());
        model.setQuery("MOD:beta");
        assertEquals(1, model.visible().size());
        model.setQuery("missing");
        assertTrue(model.visible().isEmpty());
        assertEquals(18, progress[0]);
        assertEquals(7, progress[1]);
        assertEquals(8, model.progress()[0]);
    }

    @Test public void numericSortAndLegacySortPreferencesRemainStable() {
        MaterialListModel<String> model = new MaterialListModel<>();
        model.setEntries(Arrays.asList(entry("Beta", 20, 1, 0), entry("Alpha", 4, 4, 9)));
        assertEquals("Beta", model.visible().get(0).key);
        model.sortBy(MaterialListModel.Sort.MISSING);
        assertEquals("Alpha", model.visible().get(0).key);
        model.sortBy(MaterialListModel.Sort.MISSING);
        assertEquals("Beta", model.visible().get(0).key);
        String saved = model.savedSort();
        model.restoreSort("NAME_ASC");
        assertEquals("Alpha", model.visible().get(0).key);
        model.restoreSort(saved);
        assertEquals("Beta", model.visible().get(0).key);
        model.restoreSort("MATERIAL_BROKEN");
        assertEquals(saved, model.savedSort());
    }

    @Test public void unknownPositionsAreNotReportedAsCompletedOrKnownMissing() {
        MaterialListModel<String> model = new MaterialListModel<>();
        model.setEntries(Arrays.asList(new MaterialListModel.Entry<>("a", "a", "a", 20, 15, 3, 7)));
        assertArrayEquals(new long[] {20, 5, 5, 3, 7}, model.progress());
    }
}

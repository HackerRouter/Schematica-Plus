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

    @Test public void replacementsMergeRowsChainAndUndo() {
        MaterialListModel<String> model = new MaterialListModel<>();
        model.setEntries(java.util.Arrays.asList(entry("oak", 10, 4, 1), entry("birch", 5, 5, 0), entry("planks", 3, 0, 7)));
        model.replace("oak", new MaterialListModel.Replacement<>("planks", "Planks", "mod:planks"));
        assertEquals(2, model.entries().size());
        MaterialListModel.Entry<String> planks = find(model, "planks");
        assertEquals(13, planks.total);
        assertEquals(4, planks.missing);
        assertEquals(7, planks.available);
        assertEquals(java.util.Collections.singletonList("oak"), model.replacedNames("planks"));
        // birch -> oak resolves to planks; planks -> wood moves oak and birch along
        model.replace("birch", new MaterialListModel.Replacement<>("oak", "Oak", "mod:oak"));
        assertEquals(18, find(model, "planks").total);
        model.replace("planks", new MaterialListModel.Replacement<>("wood", "Wood", "mod:wood"));
        assertEquals(1, model.entries().size());
        assertEquals(18, find(model, "wood").total);
        model.restoreReplaced("wood");
        assertEquals(3, model.entries().size());
        assertEquals(3, model.rawEntries().size());
    }

    @Test public void starredEntriesComeFirst() {
        MaterialListModel<String> model = new MaterialListModel<>();
        model.setEntries(java.util.Arrays.asList(entry("a", 100, 0, 0), entry("b", 1, 0, 0)));
        model.setSort(MaterialListModel.Sort.TOTAL, true);
        assertEquals("a", model.visible().get(0).key);
        model.toggleStar("b");
        assertEquals("b", model.visible().get(0).key);
        model.toggleStar("b");
        assertEquals("a", model.visible().get(0).key);
    }

    private static MaterialListModel.Entry<String> find(MaterialListModel<String> model, String key) {
        for (MaterialListModel.Entry<String> entry : model.entries()) if (entry.key.equals(key)) return entry;
        throw new AssertionError(key);
    }
}

package com.github.lunatrius.schematica.client.gui.framework;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import org.junit.Test;

import static org.junit.Assert.*;

public class UiListModelTest {

    @Test public void filterUsesStableCaseFoldingAndKeepsSelectionByEntry() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            UiListModel<String> model = new UiListModel<>(20, value -> value);
            model.setEntries(Arrays.asList("IRON", "Copper", "Stone"));
            model.select(0);
            model.setQuery(" iron ");
            assertEquals(Collections.singletonList("IRON"), model.entries());
            model.setQuery("stone");
            assertEquals(-1, model.selectedIndex());
            model.setQuery("");
            assertEquals(0, model.selectedIndex());
            model.setEntries(Collections.singletonList("Copper"));
            assertNull(model.selected());
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test public void filterAndResizeClampScrollAndMouseCannotSelectOutsideViewport() {
        UiListModel<String> model = new UiListModel<>(20, value -> value);
        model.setEntries(Arrays.asList("a", "b", "c", "d", "e"));
        model.setViewportHeight(35);
        model.setOffset(500);
        assertEquals(65, model.offset());
        assertEquals(3, model.indexAt(0));
        assertEquals(4, model.indexAt(34));
        assertEquals(-1, model.indexAt(35));
        assertEquals(-1, model.indexAt(-1));
        model.setViewportHeight(80);
        assertEquals(20, model.offset());
        model.setQuery("missing");
        assertEquals(0, model.offset());
        assertEquals(-1, model.indexAt(0));
        model.moveSelection(1);
        assertNull(model.selected());
    }

    @Test public void keyboardSelectionScrollsIntoView() {
        UiListModel<String> model = new UiListModel<>(20, value -> value);
        model.setEntries(Arrays.asList("a", "b", "c", "d"));
        model.setViewportHeight(40);
        model.moveSelection(1);
        model.moveSelection(3);
        assertEquals("d", model.selected());
        assertEquals(40, model.offset());
        model.moveSelection(-3);
        assertEquals(0, model.offset());
    }
}

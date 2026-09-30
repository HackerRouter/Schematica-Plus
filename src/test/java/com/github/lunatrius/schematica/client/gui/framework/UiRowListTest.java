package com.github.lunatrius.schematica.client.gui.framework;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import static org.junit.Assert.*;

public class UiRowListTest {
    @Test public void scrollingAndFilteringReplaceActionTargetsAndCancelOldCapture() {
        UiListModel<Integer> model = new UiListModel<>(22, Object::toString);
        List<Integer> entries = new ArrayList<>();
        for (int i = 0; i < 200; i++) entries.add(i);
        model.setEntries(entries);
        AtomicInteger activated = new AtomicInteger(-1);
        AtomicInteger created = new AtomicInteger();
        UiRowList<Integer> list = new UiRowList<>(model, (value, index) -> {
            created.incrementAndGet();
            return new UiPanel() {
                final UiButton button = add(new UiButton(() -> value.toString(), mouse -> activated.set(value)));
                @Override public void layout(UiBounds screen) {
                    button.setBounds(bounds().x, bounds().y, 80, 20);
                }
            };
        });
        list.setBounds(10, 10, 100, 44);
        UiInput input = new UiInput(list);
        assertEquals(4, created.get());
        input.mouseDown(15, 15, 0);
        model.setQuery("199");
        list.sync();
        input.mouseUp(15, 15, 0);
        assertEquals(-1, activated.get());
        input.mouseDown(15, 15, 0);
        input.mouseUp(15, 15, 0);
        assertEquals(199, activated.get());
        model.setQuery("");
        list.sync();
        input.scroll(15, 15, -1);
        input.mouseDown(15, 15, 0);
        input.mouseUp(15, 15, 0);
        assertEquals(3, activated.get());
        assertTrue(created.get() < 20);
    }

    @Test public void partialRowsCannotReceiveClicksOutsideTheViewport() {
        UiListModel<Integer> model = new UiListModel<>(22, Object::toString);
        model.setEntries(java.util.Arrays.asList(1, 2, 3, 4));
        AtomicInteger activated = new AtomicInteger();
        UiRowList<Integer> list = new UiRowList<>(model, (value, index) -> new UiPanel() {
            final UiButton button = add(new UiButton(() -> "test", mouse -> activated.incrementAndGet()));
            @Override public void layout(UiBounds screen) {
                button.setBounds(bounds().x, bounds().y, 60, 20);
            }
        });
        list.setBounds(10, 10, 100, 25);
        UiInput input = new UiInput(list);
        input.mouseDown(15, 40, 0);
        input.mouseUp(15, 40, 0);
        assertEquals(0, activated.get());
        list.setBounds(10, 10, 100, 0);
        model.setEntries(java.util.Collections.emptyList());
        list.sync();
        assertEquals(0, model.offset());
    }
}

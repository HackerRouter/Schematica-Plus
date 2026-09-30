package com.github.lunatrius.schematica.client.gui.framework;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.lwjgl.input.Keyboard;

import static org.junit.Assert.*;

public class UiControlsTest {

    private UiPanel root() {
        UiPanel root = new UiPanel();
        root.setBounds(0, 0, 320, 240);
        return root;
    }

    @Test public void buttonRequiresReleaseInsideAllAncestorClips() {
        UiPanel root = root();
        UiPanel panel = root.add(new UiPanel());
        panel.setBounds(0, 0, 20, 20);
        AtomicInteger clicks = new AtomicInteger();
        UiButton button = panel.add(new UiButton(() -> "Test", clicks::addAndGet));
        button.setBounds(10, 0, 20, 20);
        UiInput input = new UiInput(root);
        input.mouseDown(15, 5, 1);
        input.mouseUp(25, 5, 1);
        assertEquals(0, clicks.get());
        input.mouseDown(15, 5, 1);
        input.mouseUp(15, 5, 1);
        assertEquals(1, clicks.get());
    }

    @Test public void disablingAncestorCancelsPendingButtonAndKeyboardActivation() {
        UiPanel root = root();
        AtomicInteger clicks = new AtomicInteger();
        UiButton button = root.add(new UiButton(() -> "Test", ignored -> clicks.incrementAndGet()));
        button.setBounds(10, 10, 20, 20);
        UiInput input = new UiInput(root);
        input.mouseDown(15, 15, 0);
        root.setEnabled(false);
        input.mouseUp(15, 15, 0);
        input.keyTyped(' ', Keyboard.KEY_SPACE);
        assertFalse(button.isEnabled());
        assertEquals(0, clicks.get());
        root.setEnabled(true);
        input.focus(button);
        input.keyTyped('\n', Keyboard.KEY_RETURN);
        assertEquals(1, clicks.get());
    }

    @Test public void listDrawsOnlyRowsInViewportEvenWithManyEntries() {
        UiListModel<String> model = new UiListModel<>(20, value -> value);
        List<String> entries = new ArrayList<>();
        for (int i = 0; i < 10000; i++) entries.add("Entry " + i);
        model.setEntries(entries);
        UiPanel root = root();
        UiList<String> list = root.add(new UiList<>(model, "Empty", value -> {}));
        list.setBounds(10, 10, 100, 82);
        model.setOffset(10000);
        RecordingDraw draw = new RecordingDraw();
        root.draw(draw, 0, 0);
        assertTrue(draw.strings.size() <= 6);
        assertEquals("Entry 500", draw.strings.get(0));
        assertEquals(0, draw.clipDepth);
    }

    @Test public void filterHidesSelectionFromActivationAndScrollbarDragClampsOutside() {
        UiListModel<String> model = new UiListModel<>(20, value -> value);
        model.setEntries(Arrays.asList("a", "b", "c", "d", "e", "f"));
        AtomicInteger actions = new AtomicInteger();
        UiPanel root = root();
        UiList<String> list = root.add(new UiList<>(model, "Empty", value -> actions.incrementAndGet()));
        list.setBounds(10, 10, 100, 42);
        UiInput input = new UiInput(root);
        input.mouseDown(105, 12, 0);
        input.mouseDrag(500, 500, 0);
        input.mouseUp(500, 500, 0);
        assertEquals(model.maxOffset(), model.offset());
        input.keyTyped('\0', Keyboard.KEY_HOME);
        assertEquals("a", model.selected());
        assertEquals(0, model.offset());
        model.setQuery("b");
        input.keyTyped('\n', Keyboard.KEY_RETURN);
        assertEquals(0, actions.get());
        input.keyTyped('\0', Keyboard.KEY_DOWN);
        input.keyTyped('\n', Keyboard.KEY_RETURN);
        assertEquals(1, actions.get());
    }

    @Test public void failedWidgetDrawingStillClosesAllClipScopes() {
        UiPanel root = root();
        UiWidget broken = root.add(new UiWidget() {
            @Override public void draw(UiDraw draw, int x, int y) {
                throw new IllegalStateException("test failure");
            }
        });
        broken.setBounds(0, 0, 20, 20);
        RecordingDraw draw = new RecordingDraw();
        try {
            root.draw(draw, 0, 0);
            fail("Expected drawing failure");
        } catch (IllegalStateException expected) {
            assertEquals(0, draw.clipDepth);
        }
    }

    private static final class RecordingDraw implements UiDraw {
        int clipDepth;
        final List<String> strings = new ArrayList<>();

        @Override public void item(net.minecraft.item.ItemStack stack, int x, int y) {}

        @Override public void texture(String texture, UiBounds destination, int u, int v, int sw, int sh, int tw, int th) {}
        @Override public void fill(UiBounds bounds, int color) {}
        @Override public void colorGrid(UiBounds bounds, int columns, int rows, int[] colors) {}
        @Override public void triangle(float x1, float y1, float x2, float y2, float x3, float y3, int color) {}
        @Override public void text(String text, int x, int y, int color) { strings.add(text); }
        @Override public int textWidth(String text) { return text.length() * 6; }
        @Override public String trim(String text, int width) { return text.substring(0, Math.min(text.length(), Math.max(0, width / 6))); }
        @Override public Clip clip(UiBounds bounds) { clipDepth++; return () -> clipDepth--; }
    }
}

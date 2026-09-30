package com.github.lunatrius.schematica.client.gui.framework;

import org.junit.Test;

import static org.junit.Assert.*;

public class UiInputTest {

    private UiPanel panel(int x, int y, int width, int height) {
        UiPanel panel = new UiPanel();
        panel.setBounds(x, y, width, height);
        return panel;
    }

    private Probe probe(UiPanel parent, int x, int y) {
        Probe probe = parent.add(new Probe());
        probe.setBounds(x, y, 20, 20);
        return probe;
    }

    @Test public void topmostWidgetConsumesClickAndReleaseOutsideKeepsCapture() {
        UiPanel root = panel(0, 0, 100, 100);
        Probe behind = probe(root, 5, 5);
        Probe front = probe(root, 5, 5);
        UiInput input = new UiInput(root);
        input.mouseDown(10, 10, 0);
        input.mouseDrag(200, 200, 0);
        input.mouseUp(200, 200, 1);
        input.mouseUp(200, 200, 0);
        assertEquals(0, behind.presses);
        assertEquals(1, front.presses);
        assertEquals(1, front.drags);
        assertEquals(1, front.releases);
    }

    @Test public void disabledTopWidgetBlocksUnderlyingButtons() {
        UiPanel root = panel(0, 0, 100, 100);
        Probe behind = probe(root, 5, 5);
        probe(root, 5, 5).setEnabled(false);
        UiInput input = new UiInput(root);
        input.mouseDown(10, 10, 0);
        assertEquals(0, behind.presses);
        assertNull(input.focused());
    }

    @Test public void parentClipLimitsMouseAndKeyboardTargets() {
        UiPanel root = panel(0, 0, 100, 100);
        UiPanel child = root.add(panel(0, 0, 30, 30));
        Probe inside = probe(child, 20, 20);
        probe(child, 35, 35);
        UiInput input = new UiInput(root);
        assertNotSame(inside, input.hit(35, 25));
        assertSame(inside, input.hit(25, 25));
        input.cycleFocus(false);
        assertSame(inside, input.focused());
        input.cycleFocus(false);
        assertSame(inside, input.focused());
    }

    @Test public void tabSkipsHiddenAndDisabledAndWrapsBothWays() {
        UiPanel root = panel(0, 0, 100, 100);
        Probe first = probe(root, 0, 0);
        probe(root, 20, 0).setVisible(false);
        probe(root, 40, 0).setEnabled(false);
        Probe last = probe(root, 60, 0);
        UiInput input = new UiInput(root);
        input.cycleFocus(true);
        assertSame(last, input.focused());
        input.cycleFocus(false);
        assertSame(first, input.focused());
        input.keyTyped('a', 30);
        assertEquals(1, first.keys);
        assertEquals(0, last.keys);
    }

    @Test public void nestedModalsBlockOutsideInputAndRestoreFocus() {
        UiPanel root = panel(0, 0, 100, 100);
        Probe base = probe(root, 0, 0);
        UiPanel modal = panel(30, 30, 50, 50);
        Probe dialog = probe(modal, 35, 35);
        UiInput input = new UiInput(root);
        input.focus(base);
        input.pushModal(modal);
        input.mouseDown(5, 5, 0);
        assertEquals(0, base.presses);
        assertNull(input.hit(5, 5));
        input.focus(dialog);
        input.pushModal(panel(40, 40, 10, 10));
        assertNull(input.focused());
        assertTrue(input.popModal());
        assertSame(dialog, input.focused());
        input.popModal();
        assertSame(base, input.focused());
        assertFalse(input.popModal());
    }

    @Test public void removingFocusedOrCapturedWidgetsCancelsThem() {
        UiPanel root = panel(0, 0, 100, 100);
        Probe target = probe(root, 0, 0);
        UiInput input = new UiInput(root);
        input.mouseDown(5, 5, 0);
        root.remove(target);
        input.mouseUp(5, 5, 0);
        input.keyTyped('a', 30);
        assertEquals(1, target.cancels);
        assertEquals(0, target.releases);
        assertEquals(0, target.keys);
        assertFalse(target.isFocused());
    }

    @Test public void openingModalDuringMouseDownDoesNotCaptureOldLayer() {
        UiPanel root = panel(0, 0, 100, 100);
        UiInput input = new UiInput(root);
        Probe target = root.add(new Probe() {
            @Override public boolean mouseDown(int x, int y, int button) {
                input.pushModal(panel(30, 30, 50, 50));
                return true;
            }
        });
        target.setBounds(0, 0, 20, 20);
        input.mouseDown(5, 5, 0);
        input.mouseUp(5, 5, 0);
        assertEquals(0, target.releases);
    }

    @Test(expected = IllegalArgumentException.class)
    public void widgetTreeRejectsCycles() {
        UiPanel root = panel(0, 0, 100, 100);
        root.add(panel(0, 0, 20, 20)).add(root);
    }

    private static class Probe extends UiWidget {
        int presses;
        int releases;
        int drags;
        int cancels;
        int keys;

        @Override public boolean isFocusable() { return true; }
        @Override public void draw(UiDraw draw, int x, int y) {}
        @Override public boolean mouseDown(int x, int y, int button) { presses++; return true; }
        @Override public void mouseUp(int x, int y, int button) { releases++; }
        @Override public void mouseDrag(int x, int y, int button) { drags++; }
        @Override public void cancelMouse() { cancels++; }
        @Override public boolean keyTyped(char character, int keyCode) { keys++; return true; }
    }
}

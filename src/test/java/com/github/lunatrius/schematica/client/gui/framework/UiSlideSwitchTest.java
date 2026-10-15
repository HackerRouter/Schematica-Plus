package com.github.lunatrius.schematica.client.gui.framework;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.lwjgl.input.Keyboard;

import static org.junit.Assert.*;

public class UiSlideSwitchTest {
    private final AtomicBoolean value = new AtomicBoolean();
    private final UiPanel root = new UiPanel();
    private final UiSlideSwitch toggle = root.add(new UiSlideSwitch(() -> "Image", () -> "3D", value::get, value::set));
    private final UiInput input = new UiInput(root);

    public UiSlideSwitchTest() {
        root.setBounds(0, 0, 300, 200);
        toggle.setBounds(20, 20, 100, 20);
    }

    @Test public void draggingCommitsOnceReleasedEvenOutsideTheTrack() {
        input.mouseDown(30, 30, 0);
        input.mouseDrag(200, 30, 0);
        assertFalse(value.get());
        input.mouseUp(200, 30, 0);
        assertTrue(value.get());
        input.mouseDown(100, 30, 0);
        input.mouseDrag(-20, 30, 0);
        input.mouseUp(-20, 30, 0);
        assertFalse(value.get());
    }

    @Test public void aModalCancelsAnUncommittedDragAndKeyboardFocus() {
        input.mouseDown(100, 30, 0);
        UiPanel modal = new UiPanel();
        modal.setBounds(0, 0, 300, 200);
        input.pushModal(modal);
        input.mouseUp(100, 30, 0);
        input.keyTyped(' ', Keyboard.KEY_SPACE);
        assertFalse(value.get());
        input.popModal();
        input.keyTyped(' ', Keyboard.KEY_SPACE);
        assertTrue(value.get());
    }

    @Test public void hiddenOrClippedControlsCannotSwitchTheView() {
        root.setBounds(0, 0, 50, 50);
        input.mouseDown(100, 30, 0);
        input.mouseUp(100, 30, 0);
        assertFalse(value.get());
        input.focus(toggle);
        toggle.setVisible(false);
        input.keyTyped('\0', Keyboard.KEY_RIGHT);
        assertFalse(value.get());
    }

    @Test public void arrowsSelectAndSpaceTogglesOnlyWithFocus() {
        input.keyTyped('\0', Keyboard.KEY_RIGHT);
        assertFalse(value.get());
        input.focus(toggle);
        input.keyTyped('\0', Keyboard.KEY_RIGHT);
        assertTrue(value.get());
        input.keyTyped('\0', Keyboard.KEY_LEFT);
        assertFalse(value.get());
        input.keyTyped(' ', Keyboard.KEY_SPACE);
        assertTrue(value.get());
    }
}

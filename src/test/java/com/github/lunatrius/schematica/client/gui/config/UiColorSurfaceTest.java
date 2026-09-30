package com.github.lunatrius.schematica.client.gui.config;

import org.junit.Test;
import org.lwjgl.input.Keyboard;
import static org.junit.Assert.*;

import com.github.lunatrius.schematica.client.gui.config.ColorPickerModel.Channel;
import com.github.lunatrius.schematica.client.gui.framework.UiInput;
import com.github.lunatrius.schematica.client.gui.framework.UiPanel;

public class UiColorSurfaceTest {
    @Test public void squareDragClampsOutsideAndStopsAfterRelease() {
        UiPanel root = new UiPanel();
        root.setBounds(0, 0, 320, 240);
        ColorPickerModel model = new ColorPickerModel(0x44FF0000);
        UiColorSurface square = root.add(new UiColorSurface(model, null, false, () -> {}));
        square.setBounds(10, 10, 104, 104);
        UiInput input = new UiInput(root);
        input.mouseDown(112, 11, 0);
        assertEquals(0x44FF0000, model.color());
        input.mouseDrag(999, 999, 0);
        assertEquals(0x44FFFFFF, model.color());
        input.mouseUp(999, 999, 0);
        input.mouseDrag(0, 0, 0);
        assertEquals(0x44FFFFFF, model.color());
        input.keyTyped('\0', Keyboard.KEY_UP);
        assertEquals(1, model.component(Channel.S));
        input.keyTyped('\0', Keyboard.KEY_LEFT);
        assertEquals(99, model.component(Channel.V));
    }

    @Test public void hiddenColorSurfaceCannotKeepCapturingDrag() {
        UiPanel root = new UiPanel();
        root.setBounds(0, 0, 320, 240);
        ColorPickerModel model = new ColorPickerModel(0xFFABCDEF);
        UiColorSurface alpha = root.add(new UiColorSurface(model, Channel.A, false, () -> {}));
        alpha.setBounds(10, 10, 92, 14);
        UiInput input = new UiInput(root);
        input.mouseDown(11, 11, 0);
        assertEquals(0x00ABCDEF, model.color());
        alpha.setVisible(false);
        input.mouseDrag(100, 11, 0);
        input.keyTyped('\0', Keyboard.KEY_RIGHT);
        assertEquals(0x00ABCDEF, model.color());
    }
}

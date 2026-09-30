package com.github.lunatrius.schematica.client.gui.config;

import java.util.Arrays;

import net.minecraftforge.common.config.Property;
import org.junit.Test;
import org.lwjgl.input.Keyboard;

import com.github.lunatrius.schematica.client.gui.framework.UiInput;
import com.github.lunatrius.schematica.client.gui.framework.UiListModel;
import com.github.lunatrius.schematica.client.gui.framework.UiPanel;

import static org.junit.Assert.*;

public class UiConfigSliderTest {
    @Test public void dragClampsAtEdgesAndReleaseStopsEditing() {
        ConfigPropertyDraft draft = new ConfigPropertyDraft(new Property("delay", "1", Property.Type.INTEGER)
            .setMinValue(0).setMaxValue(20));
        UiPanel root = new UiPanel();
        root.setBounds(0, 0, 320, 240);
        UiConfigSlider slider = root.add(new UiConfigSlider(draft));
        slider.setBounds(10, 10, 112, 20);
        UiInput input = new UiInput(root);
        input.mouseDown(64, 20, 0);
        assertEquals("10", draft.text());
        input.mouseDrag(500, 20, 0);
        assertEquals("20", draft.text());
        input.mouseUp(500, 20, 0);
        input.mouseDrag(-100, 20, 0);
        assertEquals("20", draft.text());
        input.keyTyped('\0', Keyboard.KEY_LEFT);
        assertEquals("19", draft.text());
        assertEquals("1", draft.property.getString());
    }

    @Test public void filteringKeepsDraftsAndCancelsRemovedSliderCapture() {
        ConfigPropertyDraft draft = new ConfigPropertyDraft(new Property("alpha", "1.0", Property.Type.DOUBLE)
            .setMinValue(0.0).setMaxValue(1.0));
        UiListModel<ConfigPropertyDraft> model = new UiListModel<>(22, entry -> entry.property.getName());
        model.setEntries(Arrays.asList(draft));
        UiPanel root = new UiPanel();
        root.setBounds(0, 0, 320, 240);
        UiConfigSlider slider = root.add(new UiConfigSlider(draft));
        slider.setBounds(10, 10, 112, 20);
        UiInput input = new UiInput(root);
        input.mouseDown(64, 20, 0);
        model.setQuery("missing");
        assertTrue(model.entries().isEmpty());
        root.remove(slider);
        input.mouseDrag(500, 20, 0);
        input.mouseUp(500, 20, 0);
        model.setQuery("");
        assertSame(draft, model.entries().get(0));
        assertEquals("0.5", model.entries().get(0).text());
        assertEquals("1.0", draft.property.getString());
    }
}

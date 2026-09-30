package com.github.lunatrius.schematica.client.gui.config;

import java.util.Random;

import org.junit.Test;
import static org.junit.Assert.*;

import com.github.lunatrius.schematica.client.gui.config.ColorPickerModel.Channel;
import com.github.lunatrius.schematica.util.ColorValue;

public class ColorPickerModelTest {
    @Test public void parsesUnsignedArgbAndOpaqueRgbWithoutSwappingAlpha() {
        assertEquals(0x89ABCDEF, ColorValue.parse("#89abcdef"));
        assertEquals(0xFFABCDEF, ColorValue.parse("0xABCDEF"));
        assertEquals(0x00112233, ColorValue.parse("00112233"));
        assertEquals("#89ABCDEF", ColorValue.format(ColorValue.parse("89abcdef")));
        for (String invalid : new String[] {"#123", "#12345", "#GGFFFFFF", "#FFFFFFFF0", "-1", " NaN", "#FFFFFF "}) {
            assertThrows(IllegalArgumentException.class, () -> ColorValue.parse(invalid));
        }
        assertEquals(0x89 / 255f, ColorValue.alpha(0x89ABCDEF), 0);
        assertEquals(0xAB / 255f, ColorValue.red(0x89ABCDEF), 0);
        assertEquals(0xCD / 255f, ColorValue.green(0x89ABCDEF), 0);
        assertEquals(0xEF / 255f, ColorValue.blue(0x89ABCDEF), 0);
    }

    @Test public void hsvRoundTripPreservesRgbAndAlphaAcrossRandomColors() {
        Random random = new Random(8124);
        for (int i = 0; i < 10000; i++) {
            int color = random.nextInt();
            ColorPickerModel model = new ColorPickerModel(color);
            model.setFraction(Channel.H, model.fraction(Channel.H));
            assertEquals(color, model.color());
            assertEquals(ColorValue.format(color), model.hex());
        }
    }

    @Test public void squareUsesHorizontalValueAndReversedVerticalSaturation() {
        ColorPickerModel model = new ColorPickerModel(0x8044FF44);
        model.setFraction(Channel.H, 0);
        model.setSquare(1, 0);
        assertEquals(0x80FF0000, model.color());
        model.setSquare(1, 1);
        assertEquals(0x80FFFFFF, model.color());
        model.setSquare(-100, 100);
        assertEquals(0x80000000, model.color());
        model.setFraction(Channel.H, 2f / 3);
        model.setSquare(1, 0);
        assertEquals(0x800000FF, model.color());
        assertEquals(0xFF0000FF, model.squareColor(1, 0));
        assertEquals(0xFFFFFFFF, model.squareColor(1, 1));
        assertEquals(0xFF000000, model.squareColor(0, 1));
    }

    @Test public void editingAlphaDoesNotLoseHueOrSaturationAtBlack() {
        ColorPickerModel model = new ColorPickerModel(0xFF00FF00);
        model.setComponent(Channel.V, 0);
        model.setComponent(Channel.A, 1);
        assertEquals(120, model.component(Channel.H));
        assertEquals(100, model.component(Channel.S));
        model.setComponent(Channel.V, 100);
        assertEquals(0x0100FF00, model.color());
        model.setComponent(Channel.R, 300);
        model.setComponent(Channel.B, -20);
        assertEquals(0x01FFFF00, model.color());
    }

    @Test public void invalidEditsKeepLastColorAndBarsDoNotMutateIt() {
        ColorPickerModel model = new ColorPickerModel(0x12345678);
        assertFalse(model.setHex("#12"));
        model.setFraction(Channel.H, Float.NaN);
        model.setSquare(Float.POSITIVE_INFINITY, 0);
        for (Channel channel : Channel.values()) {
            model.barColor(channel, 0);
            model.barColor(channel, 1);
        }
        assertEquals(0x12345678, model.color());
        assertEquals(0x00345678, model.barColor(Channel.A, 0));
        assertEquals(0xFF345678, model.barColor(Channel.A, 1));
    }
}

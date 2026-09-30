package com.github.lunatrius.schematica.client.gui.config;

import net.minecraftforge.common.config.Property;
import org.junit.Test;
import static org.junit.Assert.*;

public class ConfigPropertyDraftTest {
    @Test public void retainsDraftWithoutChangingLiveConfiguration() {
        Property property = new Property("delay", "1", Property.Type.INTEGER).setMinValue(0).setMaxValue(20);
        ConfigPropertyDraft draft = new ConfigPropertyDraft(property);
        draft.setText("12");
        assertEquals("1", property.getString());
        assertTrue(draft.apply());
        assertEquals("12", property.getString());
        assertFalse(draft.apply());
        draft.setText("-");
        assertFalse(draft.valid());
        assertFalse(draft.apply());
        assertEquals("12", property.getString());
    }

    @Test public void rejectsNonFiniteOutOfRangeAndOverflowValues() {
        ConfigPropertyDraft alpha = new ConfigPropertyDraft(new Property("alpha", "1", Property.Type.DOUBLE)
            .setMinValue(0.0).setMaxValue(1.0));
        for (String invalid : new String[] {"", "NaN", "Infinity", "-0.1", "1.1", "1e999"}) {
            alpha.setText(invalid);
            assertFalse(invalid, alpha.valid());
        }
        ConfigPropertyDraft integer = new ConfigPropertyDraft(new Property("int", "0", Property.Type.INTEGER));
        integer.setText("2147483648");
        assertFalse(integer.valid());
        integer.setText("1.5");
        assertFalse(integer.valid());
    }

    @Test public void resetUsesDefaultsAndEquivalentNumbersAreUnmodified() {
        Property property = new Property("alpha", "0.5", Property.Type.DOUBLE).setDefaultValue(1.0);
        ConfigPropertyDraft draft = new ConfigPropertyDraft(property);
        assertTrue(draft.modified());
        draft.reset();
        assertFalse(draft.modified());
        assertEquals("0.5", property.getString());
        draft.setText("1.0000");
        assertFalse(draft.modified());
        assertTrue(draft.apply());
    }

    @Test public void listEditsAndDefaultsAreDefensivelyCopied() {
        Property property = new Property("air", new String[] {"mod:old"}, Property.Type.STRING)
            .setDefaultValues(new String[0]);
        ConfigPropertyDraft draft = new ConfigPropertyDraft(property);
        String[] edited = {"mod:new", "mod:other"};
        draft.setValues(edited);
        edited[0] = "wrong";
        draft.values()[0] = "also wrong";
        assertTrue(draft.apply());
        assertArrayEquals(new String[] {"mod:new", "mod:other"}, property.getStringList());
        draft.reset();
        assertTrue(draft.apply());
        assertArrayEquals(new String[0], property.getStringList());
    }

    @Test public void respectsPatternsAllowedValuesAndListLimits() {
        Property property = new Property("air", new String[] {"a"}, Property.Type.STRING)
            .setValidValues(new String[] {"a", "b"}).setMaxListLength(2);
        ConfigPropertyDraft draft = new ConfigPropertyDraft(property);
        draft.setValues(new String[] {"a", "a", "b"});
        assertFalse(draft.valid());
        draft.setValues(new String[] {"c"});
        assertFalse(draft.valid());
        draft.setValues(new String[] {"b"});
        assertTrue(draft.valid());
    }

    @Test public void sliderClampsAndPreservesIntegerValues() {
        ConfigPropertyDraft draft = new ConfigPropertyDraft(new Property("delay", "1", Property.Type.INTEGER)
            .setMinValue(0).setMaxValue(20));
        draft.setFraction(0.26);
        assertEquals("5", draft.text());
        assertEquals(0.25, draft.fraction(), 0.0001);
        draft.setFraction(2);
        assertEquals("20", draft.text());
        draft.setFraction(-1);
        assertEquals("0", draft.text());
        draft.setFraction(Double.NaN);
        assertEquals("0", draft.text());
    }
}

package com.github.lunatrius.schematica.client.gui.config;

import java.io.File;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

import com.github.lunatrius.schematica.handler.RenderColors;

public class ColorConfigTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void colorDraftValidatesCanonicalizesAndResetsWithoutSavingInvalidInput() {
        Property property = new Property("color", "#80112233", Property.Type.STRING).setDefaultValue("#80112233");
        ConfigPropertyDraft draft = new ConfigPropertyDraft(property, true);
        draft.setText("0x80112233");
        assertFalse(draft.modified());
        assertFalse(draft.apply());
        draft.setText("#FFFFFF");
        assertTrue(draft.apply());
        assertEquals("#FFFFFFFF", property.getString());
        draft.setText("#100000000");
        assertFalse(draft.valid());
        assertFalse(draft.apply());
        assertEquals("#FFFFFFFF", property.getString());
        draft.reset();
        assertTrue(draft.apply());
        assertEquals("#80112233", property.getString());
    }

    @Test public void colorsSurviveForgePersistenceAndBrokenValuesKeepRuntimeDefaults() throws Exception {
        java.lang.reflect.Field minecraftHomeField = cpw.mods.fml.relauncher.FMLInjectionData.class.getDeclaredField("minecraftHome");
        minecraftHomeField.setAccessible(true);
        Object previousHome = minecraftHomeField.get(null);
        minecraftHomeField.set(null, temporary.getRoot());
        try {
        File file = new File(temporary.getRoot(), "colors.cfg");
        Configuration config = new Configuration(file);
        RenderColors.load(config);
        assertEquals(11, config.getCategory(RenderColors.CATEGORY).size());
        config.getCategory(RenderColors.CATEGORY).get(RenderColors.EXTRA.key).set("#01020304");
        config.getCategory(RenderColors.CATEGORY).get(RenderColors.MISSING.key).set("broken");
        config.save();
        Configuration loaded = new Configuration(file);
        loaded.load();
        RenderColors.load(loaded);
        assertEquals(0x01020304, RenderColors.EXTRA.color());
        assertEquals(RenderColors.MISSING.defaultColor, RenderColors.MISSING.color());
        assertEquals("broken", loaded.getCategory(RenderColors.CATEGORY).get(RenderColors.MISSING.key).getString());
        } finally {
            RenderColors.load(new Configuration());
            minecraftHomeField.set(null, previousHome);
        }
    }
}

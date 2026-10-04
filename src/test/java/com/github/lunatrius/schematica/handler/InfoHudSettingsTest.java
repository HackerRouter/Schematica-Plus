package com.github.lunatrius.schematica.handler;

import java.io.File;
import java.io.InputStream;
import java.util.Map;
import net.minecraft.util.StringTranslate;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

import com.github.lunatrius.schematica.client.gui.config.ConfigPropertyDraft;
import com.github.lunatrius.schematica.client.gui.config.ConfigTranslations;
import com.github.lunatrius.schematica.util.HudAlignment;

public class InfoHudSettingsTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private java.lang.reflect.Field minecraftHome;
    private Object previousHome;
    @Before public void initializeForgeDirectory() throws Exception {
        minecraftHome = cpw.mods.fml.relauncher.FMLInjectionData.class.getDeclaredField("minecraftHome");
        minecraftHome.setAccessible(true);
        previousHome = minecraftHome.get(null);
        minecraftHome.set(null, temporary.getRoot());
    }
    @After public void reset() throws Exception {
        try { InfoHudSettings.load(new Configuration()); }
        finally { minecraftHome.set(null, previousHome); }
    }

    @Test public void defaultsAndTranslationKeysMatchUpstream() throws Exception {
        Configuration config = new Configuration();
        InfoHudSettings.load(config);
        assertEquals(1, InfoHudSettings.scale, 0);
        assertEquals(1, InfoHudSettings.offsetX);
        assertEquals(1, InfoHudSettings.offsetY);
        assertEquals(HudAlignment.BOTTOM_RIGHT, InfoHudSettings.alignment);
        assertEquals(1, InfoHudSettings.toolScale, 0);
        assertEquals(1, InfoHudSettings.toolOffsetX);
        assertEquals(1, InfoHudSettings.toolOffsetY);
        assertEquals(HudAlignment.BOTTOM_LEFT, InfoHudSettings.toolAlignment);
        assertEquals(8, config.getCategory(BlockInfoHudSettings.CATEGORY).size());
        for (String language : new String[] {"en_US", "zh_CN"}) {
            try (InputStream stream = getClass().getResourceAsStream("/assets/schematica_plus_litematica/lang/" + language + ".lang")) {
                Map<String, String> translations = StringTranslate.parseLangFile(stream);
                for (Property property : config.getCategory(BlockInfoHudSettings.CATEGORY).values()) {
                    assertTrue(property.getLanguageKey(), translations.containsKey(property.getLanguageKey()));
                    assertTrue(property.getName(), translations.containsKey(ConfigTranslations.comment(property.getLanguageKey())));
                }
            }
        }
    }

    @Test public void configEditorPersistsPositionScaleAndAlignment() {
        File path = new File(temporary.getRoot(), "hud.cfg");
        Configuration config = new Configuration(path);
        InfoHudSettings.load(config);
        config.get("render", "alpha", 0.4);
        edit(config, "infoHudAlignment", "top_left");
        edit(config, "infoHudOffsetX", "20");
        edit(config, "infoHudOffsetY", "35");
        edit(config, "infoHudScale", "0.75");
        config.save();
        Configuration loaded = new Configuration(path);
        loaded.load();
        InfoHudSettings.load(loaded);
        assertEquals(HudAlignment.TOP_LEFT, InfoHudSettings.alignment);
        assertEquals(20, InfoHudSettings.offsetX);
        assertEquals(35, InfoHudSettings.offsetY);
        assertEquals(0.75, InfoHudSettings.scale, 0);
        assertEquals(0.4, loaded.get("render", "alpha", 0.0).getDouble(), 0);
    }

    private static void edit(Configuration config, String key, String value) {
        ConfigPropertyDraft draft = new ConfigPropertyDraft(config.getCategory(BlockInfoHudSettings.CATEGORY).get(key));
        draft.setText(value);
        assertTrue(draft.valid());
        assertTrue(draft.apply());
    }

    @Test public void malformedValuesCannotBreakHudRendering() {
        Configuration config = new Configuration();
        String category = BlockInfoHudSettings.CATEGORY;
        config.get(category, "infoHudScale", 1.0).set(Double.NaN);
        config.get(category, "infoHudOffsetX", 1).set(-20);
        config.get(category, "infoHudOffsetY", 1).set(Integer.MAX_VALUE);
        config.get(category, "infoHudAlignment", "bottom_right").set("invalid");
        InfoHudSettings.load(config);
        assertEquals(1, InfoHudSettings.scale, 0);
        assertEquals(0, InfoHudSettings.offsetX);
        assertEquals(32000, InfoHudSettings.offsetY);
        assertEquals(HudAlignment.BOTTOM_RIGHT, InfoHudSettings.alignment);
    }
}

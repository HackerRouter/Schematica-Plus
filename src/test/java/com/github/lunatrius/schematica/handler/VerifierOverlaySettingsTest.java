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
import com.github.lunatrius.schematica.reference.Names;

public class VerifierOverlaySettingsTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private java.lang.reflect.Field minecraftHome;
    private Object previousHome;
    @Before public void initializeForgeDirectory() throws Exception {
        minecraftHome = cpw.mods.fml.relauncher.FMLInjectionData.class.getDeclaredField("minecraftHome");
        minecraftHome.setAccessible(true); previousHome = minecraftHome.get(null);
        minecraftHome.set(null, temporary.getRoot());
    }
    @After public void reset() throws Exception {
        try { VerifierOverlaySettings.load(new Configuration()); }
        finally { minecraftHome.set(null, previousHome); }
    }

    @Test public void defaultsAndEveryDynamicTranslationMatchUpstream() throws Exception {
        Configuration config = new Configuration(); VerifierOverlaySettings.load(config);
        assertTrue(VerifierOverlaySettings.enabled); assertTrue(VerifierOverlaySettings.sides); assertFalse(VerifierOverlaySettings.connections);
        assertEquals(0.2, VerifierOverlaySettings.alpha, 0);
        assertEquals(1000, VerifierOverlaySettings.maxPositions); assertEquals(10, VerifierOverlaySettings.maxLines);
        assertEquals(6, VerifierOverlaySettings.offsetY); assertEquals("top_center", VerifierOverlaySettings.alignment);
        for (String language : new String[] {"en_US", "zh_CN"}) {
            try (InputStream stream = getClass().getResourceAsStream("/assets/schematica_plus_litematica/lang/" + language + ".lang")) {
                Map<String, String> translations = StringTranslate.parseLangFile(stream);
                for (String category : config.getCategoryNames()) for (Property property : config.getCategory(category).values()) {
                    assertTrue(property.getLanguageKey(), translations.containsKey(property.getLanguageKey()));
                    assertTrue(property.getName(), translations.containsKey(ConfigTranslations.comment(property.getLanguageKey())));
                }
            }
        }
    }

    @Test public void settingsPersistThroughTheConfigEditorWithoutChangingOtherHudOptions() {
        File path = new File(temporary.getRoot(), "overlays.cfg");
        Configuration config = new Configuration(path); InfoHudSettings.load(config); VerifierOverlaySettings.load(config);
        edit(config, BlockInfoHudSettings.CATEGORY, "blockInfoOverlayAlignment", "center");
        edit(config, BlockInfoHudSettings.CATEGORY, "blockInfoOverlayOffsetY", "-30");
        edit(config, BlockInfoHudSettings.CATEGORY, "verifierErrorHilightAlpha", "0.7");
        edit(config, BlockInfoHudSettings.CATEGORY, "verifierErrorHilightMaxPositions", "15");
        edit(config, BlockInfoHudSettings.CATEGORY, "verifierOverlayEnabled", "false");
        edit(config, Names.Config.Category.RENDER, "renderErrorMarkerConnections", "true");
        config.save();
        Configuration loaded = new Configuration(path); loaded.load(); VerifierOverlaySettings.load(loaded);
        assertEquals("center", VerifierOverlaySettings.alignment); assertEquals(-30, VerifierOverlaySettings.offsetY);
        assertEquals(0.7, VerifierOverlaySettings.alpha, 0); assertEquals(15, VerifierOverlaySettings.maxPositions);
        assertFalse(VerifierOverlaySettings.enabled); assertTrue(VerifierOverlaySettings.connections);
        assertEquals("bottom_right", loaded.getCategory(BlockInfoHudSettings.CATEGORY).get("infoHudAlignment").getString());
    }

    private static void edit(Configuration config, String category, String key, String value) {
        ConfigPropertyDraft draft = new ConfigPropertyDraft(config.getCategory(category).get(key));
        draft.setText(value); assertTrue(draft.valid()); assertTrue(draft.apply());
    }

    @Test public void invalidNumbersAndAlignmentAreNormalizedAndSaved() {
        Configuration config = new Configuration(); String category = BlockInfoHudSettings.CATEGORY;
        config.get(category, "verifierErrorHilightAlpha", 0.2).set(Double.NaN);
        config.get(category, "verifierErrorHilightMaxPositions", 1000).set(Integer.MAX_VALUE);
        config.get(category, "infoHudMaxLines", 10).set(-1);
        config.get(category, "blockInfoOverlayOffsetY", 6).set(Integer.MIN_VALUE);
        config.get(category, "blockInfoOverlayAlignment", "top_center").set("invalid");
        VerifierOverlaySettings.load(config);
        assertEquals(0.2, VerifierOverlaySettings.alpha, 0); assertEquals(10000, VerifierOverlaySettings.maxPositions);
        assertEquals(1, VerifierOverlaySettings.maxLines); assertEquals(-2000, VerifierOverlaySettings.offsetY);
        assertEquals("top_center", VerifierOverlaySettings.alignment);
        assertEquals(10000, config.getCategory(category).get("verifierErrorHilightMaxPositions").getInt());
    }
}

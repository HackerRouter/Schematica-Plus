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

public class BlockInfoHudSettingsTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private java.lang.reflect.Field minecraftHome;
    private Object previousHome;
    private Configuration configuration(String name) { return new Configuration(new File(temporary.getRoot(), name)); }
    @Before public void initializeForgeDirectory() throws Exception {
        minecraftHome = cpw.mods.fml.relauncher.FMLInjectionData.class.getDeclaredField("minecraftHome");
        minecraftHome.setAccessible(true);
        previousHome = minecraftHome.get(null);
        minecraftHome.set(null, temporary.getRoot());
    }
    @After public void reset() throws Exception {
        try { BlockInfoHudSettings.load(new Configuration()); }
        finally { if (minecraftHome != null) minecraftHome.set(null, previousHome); }
    }

    @Test public void defaultsMatchUpstreamAndEveryOptionHasAnEnglishAndChineseDescription() throws Exception {
        Configuration config = configuration("defaults.cfg");
        BlockInfoHudSettings.load(config);
        assertTrue(BlockInfoHudSettings.enabled);
        assertFalse(BlockInfoHudSettings.targetFluids);
        assertEquals(0.5, BlockInfoHudSettings.scale, 0);
        assertEquals(4, BlockInfoHudSettings.offsetX);
        assertEquals(4, BlockInfoHudSettings.offsetY);
        assertEquals(HudAlignment.TOP_RIGHT, BlockInfoHudSettings.alignment);
        assertEquals(6, config.getCategory(BlockInfoHudSettings.CATEGORY).size());
        for (String language : new String[] {"en_US", "zh_CN"}) {
            try (InputStream stream = getClass().getResourceAsStream("/assets/schematica_plus_litematica/lang/" + language + ".lang")) {
                Map<String, String> translations = StringTranslate.parseLangFile(stream);
                for (Property property : config.getCategory(BlockInfoHudSettings.CATEGORY).values()) {
                    assertTrue(property.getLanguageKey(), translations.containsKey(property.getLanguageKey()));
                    assertTrue(property.getName(), translations.containsKey(ConfigTranslations.comment(property.getLanguageKey())));
                }
                for (HudAlignment alignment : HudAlignment.values()) assertTrue(translations.containsKey(alignment.translationKey()));
            }
        }
    }

    @Test public void configEditsPersistWithoutChangingExistingOptions() {
        Configuration config = configuration("hud.cfg");
        config.get("render", "alpha", 0.4);
        BlockInfoHudSettings.load(config);
        edit(config, "blockInfoLinesAlignment", "bottom_left");
        edit(config, "blockInfoLinesFontScale", "1.5");
        edit(config, "blockInfoLinesOffsetX", "12");
        edit(config, "blockInfoLinesOffsetY", "20");
        edit(config, "infoOverlaysTargetFluids", "true");
        edit(config, "blockInfoLinesEnabled", "false");
        config.save();
        Configuration reloaded = configuration("hud.cfg");
        reloaded.load();
        BlockInfoHudSettings.load(reloaded);
        assertEquals(HudAlignment.BOTTOM_LEFT, BlockInfoHudSettings.alignment);
        assertEquals(1.5, BlockInfoHudSettings.scale, 0);
        assertEquals(12, BlockInfoHudSettings.offsetX);
        assertEquals(20, BlockInfoHudSettings.offsetY);
        assertTrue(BlockInfoHudSettings.targetFluids);
        assertFalse(BlockInfoHudSettings.enabled);
        assertEquals(0.4, reloaded.get("render", "alpha", 1.0).getDouble(), 0);
    }

    @Test public void sanitizesNonFiniteScaleAndInvalidAlignmentWithoutBreakingDrafts() {
        Configuration config = configuration("invalid.cfg");
        BlockInfoHudSettings.load(config);
        config.getCategory(BlockInfoHudSettings.CATEGORY).get("blockInfoLinesFontScale").set("NaN");
        config.getCategory(BlockInfoHudSettings.CATEGORY).get("blockInfoLinesAlignment").set("invalid");
        config.getCategory(BlockInfoHudSettings.CATEGORY).get("blockInfoLinesOffsetX").set(-5);
        config.getCategory(BlockInfoHudSettings.CATEGORY).get("blockInfoLinesOffsetY").set(5000);
        BlockInfoHudSettings.load(config);
        assertEquals(0.5, BlockInfoHudSettings.scale, 0);
        assertEquals(HudAlignment.TOP_RIGHT, BlockInfoHudSettings.alignment);
        assertEquals(0, BlockInfoHudSettings.offsetX);
        assertEquals(2000, BlockInfoHudSettings.offsetY);
        for (Property property : config.getCategory(BlockInfoHudSettings.CATEGORY).values()) assertTrue(new ConfigPropertyDraft(property).valid());
    }

    private void edit(Configuration config, String name, String value) {
        ConfigPropertyDraft draft = new ConfigPropertyDraft(config.getCategory(BlockInfoHudSettings.CATEGORY).get(name));
        draft.setText(value);
        assertTrue(draft.valid());
        assertTrue(draft.apply());
    }
}

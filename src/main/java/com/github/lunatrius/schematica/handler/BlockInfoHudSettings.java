package com.github.lunatrius.schematica.handler;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import com.github.lunatrius.schematica.util.HudAlignment;

public final class BlockInfoHudSettings {
    public static final String CATEGORY = "info_overlays";
    public static boolean enabled = true;
    public static boolean targetFluids;
    public static double scale = 0.5;
    public static int offsetX = 4, offsetY = 4;
    public static HudAlignment alignment = HudAlignment.TOP_RIGHT;

    private BlockInfoHudSettings() {}

    public static void load(Configuration configuration) {
        enabled = label(configuration.get(CATEGORY, "blockInfoLinesEnabled", true), "blockInfoLinesEnabled").getBoolean(true);
        targetFluids = label(configuration.get(CATEGORY, "infoOverlaysTargetFluids", false), "infoOverlaysTargetFluids").getBoolean(false);
        Property font = label(configuration.get(CATEGORY, "blockInfoLinesFontScale", 0.5), "blockInfoLinesFontScale");
        font.setMinValue(0.0).setMaxValue(10.0);
        scale = font.getDouble(0.5);
        if (!Double.isFinite(scale) || scale < 0 || scale > 10) { scale = 0.5; font.set(scale); }
        offsetX = offset(configuration, "blockInfoLinesOffsetX");
        offsetY = offset(configuration, "blockInfoLinesOffsetY");
        Property anchor = label(configuration.get(CATEGORY, "blockInfoLinesAlignment", "top_right"), "blockInfoLinesAlignment");
        anchor.setValidValues(java.util.Arrays.stream(HudAlignment.values()).map(HudAlignment::value).toArray(String[]::new));
        alignment = HudAlignment.parse(anchor.getString());
        anchor.set(alignment.value());
    }

    private static int offset(Configuration configuration, String key) {
        Property property = label(configuration.get(CATEGORY, key, 4), key);
        property.setMinValue(0).setMaxValue(2000);
        int value = Math.max(0, Math.min(2000, property.getInt(4)));
        property.set(value);
        return value;
    }

    private static Property label(Property property, String key) {
        return property.setLanguageKey("litematica.config.info_overlays.name." + key);
    }
}

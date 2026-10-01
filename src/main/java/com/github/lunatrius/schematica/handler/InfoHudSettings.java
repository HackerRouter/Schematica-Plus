package com.github.lunatrius.schematica.handler;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import com.github.lunatrius.schematica.util.HudAlignment;

public final class InfoHudSettings {
    public static double scale = 1;
    public static int offsetX = 1, offsetY = 1;
    public static HudAlignment alignment = HudAlignment.BOTTOM_RIGHT;

    private InfoHudSettings() {}

    public static void load(Configuration configuration) {
        Property font = label(configuration.get(BlockInfoHudSettings.CATEGORY, "infoHudScale", 1.0), "infoHudScale");
        font.setMinValue(0.1).setMaxValue(4.0);
        scale = font.getDouble(1.0);
        if (!Double.isFinite(scale) || scale < 0.1 || scale > 4) { scale = 1; font.set(scale); }
        offsetX = offset(configuration, "infoHudOffsetX");
        offsetY = offset(configuration, "infoHudOffsetY");
        Property anchor = label(configuration.get(BlockInfoHudSettings.CATEGORY, "infoHudAlignment", "bottom_right"), "infoHudAlignment");
        String[] values = java.util.Arrays.stream(HudAlignment.values()).map(HudAlignment::value).toArray(String[]::new);
        anchor.setValidValues(values);
        alignment = java.util.Arrays.asList(values).contains(anchor.getString()) ? HudAlignment.parse(anchor.getString()) : HudAlignment.BOTTOM_RIGHT;
        anchor.set(alignment.value());
    }

    private static int offset(Configuration configuration, String key) {
        Property property = label(configuration.get(BlockInfoHudSettings.CATEGORY, key, 1), key);
        property.setMinValue(0).setMaxValue(32000);
        int value = Math.max(0, Math.min(32000, property.getInt(1)));
        property.set(value);
        return value;
    }

    private static Property label(Property property, String key) {
        return property.setLanguageKey("litematica.config.info_overlays.name." + key);
    }
}

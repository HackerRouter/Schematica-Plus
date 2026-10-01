package com.github.lunatrius.schematica.handler;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import com.github.lunatrius.schematica.reference.Names;

public final class VerifierOverlaySettings {
    public static boolean enabled = true, sides = true, connections;
    public static double alpha = 0.2;
    public static int maxPositions = 1000, maxLines = 10, offsetY = 6;
    public static String alignment = "top_center";

    private VerifierOverlaySettings() {}

    public static void load(Configuration config) {
        String category = BlockInfoHudSettings.CATEGORY;
        enabled = label(config.get(category, "verifierOverlayEnabled", true), "info_overlays").getBoolean(true);
        Property opacity = label(config.get(category, "verifierErrorHilightAlpha", 0.2), "info_overlays");
        opacity.setMinValue(0.0).setMaxValue(1.0);
        alpha = opacity.getDouble(0.2);
        if (!Double.isFinite(alpha) || alpha < 0 || alpha > 1) { alpha = 0.2; opacity.set(alpha); }
        maxPositions = integer(config, "verifierErrorHilightMaxPositions", 1000, 1, 10000);
        maxLines = integer(config, "infoHudMaxLines", 10, 1, 128);
        offsetY = integer(config, "blockInfoOverlayOffsetY", 6, -2000, 2000);
        Property anchor = label(config.get(category, "blockInfoOverlayAlignment", "top_center"), "info_overlays");
        anchor.setValidValues(new String[] {"top_center", "center"});
        alignment = "center".equals(anchor.getString()) ? "center" : "top_center";
        anchor.set(alignment);
        sides = label(config.get(Names.Config.Category.RENDER, "renderErrorMarkerSides", true), "visuals").getBoolean(true);
        connections = label(config.get(Names.Config.Category.RENDER, "renderErrorMarkerConnections", false), "visuals").getBoolean(false);
    }

    private static int integer(Configuration config, String key, int fallback, int min, int max) {
        Property property = label(config.get(BlockInfoHudSettings.CATEGORY, key, fallback), "info_overlays");
        property.setMinValue(min).setMaxValue(max);
        int value = Math.max(min, Math.min(max, property.getInt(fallback)));
        property.set(value);
        return value;
    }

    private static Property label(Property property, String group) {
        return property.setLanguageKey("litematica.config." + group + ".name." + property.getName());
    }
}

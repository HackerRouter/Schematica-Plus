package com.github.lunatrius.schematica.handler;

import net.minecraftforge.common.config.Configuration;

import com.github.lunatrius.schematica.util.HudAlignment;

public final class InfoHudSettings {
    public static double scale = 1;
    public static int offsetX = 1, offsetY = 1;
    public static HudAlignment alignment = HudAlignment.BOTTOM_RIGHT;
    public static double toolScale = 1;
    public static int toolOffsetX = 1, toolOffsetY = 1;
    public static HudAlignment toolAlignment = HudAlignment.BOTTOM_LEFT;

    private InfoHudSettings() {}

    public static void load(Configuration configuration) {
        ConfigReader overlays = new ConfigReader(configuration, BlockInfoHudSettings.CATEGORY, ConfigReader.INFO_OVERLAYS);
        scale = overlays.decimal("infoHudScale", 1.0, 0.1, 4.0);
        offsetX = overlays.integer("infoHudOffsetX", 1, 0, 32000);
        offsetY = overlays.integer("infoHudOffsetY", 1, 0, 32000);
        alignment = HudAlignment.parse(overlays.choice("infoHudAlignment", "bottom_right", HudAlignment.names()));
        toolScale = overlays.decimal("toolHudScale", 1.0, 0.1, 4.0);
        toolOffsetX = overlays.integer("toolHudOffsetX", 1, 0, 32000);
        toolOffsetY = overlays.integer("toolHudOffsetY", 1, 0, 32000);
        toolAlignment = HudAlignment.parse(overlays.choice("toolHudAlignment", "bottom_left", HudAlignment.names()));
    }
}

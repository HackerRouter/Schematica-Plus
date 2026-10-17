package com.github.lunatrius.schematica.handler;

import net.minecraftforge.common.config.Configuration;
import com.github.lunatrius.schematica.reference.Names;

public final class VerifierOverlaySettings {
    public static boolean enabled = true, sides = true, connections;
    public static double alpha = 0.2;
    public static int maxPositions = 1000, maxLines = 10, offsetY = 6;
    public static String alignment = "top_center";

    private VerifierOverlaySettings() {}

    public static void load(Configuration config) {
        ConfigReader overlays = new ConfigReader(config, BlockInfoHudSettings.CATEGORY, ConfigReader.INFO_OVERLAYS);
        enabled = overlays.flag("verifierOverlayEnabled", true);
        alpha = overlays.decimal("verifierErrorHilightAlpha", 0.2, 0.0, 1.0);
        maxPositions = overlays.integer("verifierErrorHilightMaxPositions", 1000, 1, 10000);
        maxLines = overlays.integer("infoHudMaxLines", 10, 1, 128);
        offsetY = overlays.integer("blockInfoOverlayOffsetY", 6, -2000, 2000);
        alignment = overlays.choice("blockInfoOverlayAlignment", "top_center", "top_center", "center");
        ConfigReader visuals = overlays.in(Names.Config.Category.RENDER, ConfigReader.VISUALS);
        sides = visuals.flag("renderErrorMarkerSides", true);
        connections = visuals.flag("renderErrorMarkerConnections", false);
    }
}

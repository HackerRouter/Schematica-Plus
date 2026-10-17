package com.github.lunatrius.schematica.handler;

import net.minecraftforge.common.config.Configuration;

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
        ConfigReader overlays = new ConfigReader(configuration, CATEGORY, ConfigReader.INFO_OVERLAYS);
        enabled = overlays.flag("blockInfoLinesEnabled", true);
        targetFluids = overlays.flag("infoOverlaysTargetFluids", false);
        scale = overlays.decimal("blockInfoLinesFontScale", 0.5, 0.0, 10.0);
        offsetX = overlays.integer("blockInfoLinesOffsetX", 4, 0, 2000);
        offsetY = overlays.integer("blockInfoLinesOffsetY", 4, 0, 2000);
        alignment = HudAlignment.parse(overlays.choice("blockInfoLinesAlignment", "top_right", HudAlignment.names()));
    }
}

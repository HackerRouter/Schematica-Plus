// SPDX-License-Identifier: LGPL-3.0-only
// Litematica visuals switches and render toggles, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.input.Hotkeys;
import com.github.lunatrius.schematica.reference.Names;

/** Upstream Configs.Visuals master switches that Plus honors, plus the hold-to-invert hotkeys. */
public final class VisualSettings {
    public static boolean rendering = true, schematic = true, blocks = true, areaBoxes = true, placementBoxes = true, overlayThrough;
    public static boolean blockInfoOverlay = true;

    /** A toggleable boolean option and the upstream config group used for its display name. */
    public enum Toggle {
        ALL(Names.Config.Category.RENDER, "enableRendering", "visuals"),
        SCHEMATIC(Names.Config.Category.RENDER, "enableSchematicRendering", "visuals"),
        BLOCKS(Names.Config.Category.RENDER, "enableSchematicBlocksRendering", "visuals"),
        OVERLAY(Names.Config.Category.RENDER, Names.Config.HIGHLIGHT, "visuals", "enableSchematicOverlay"),
        OVERLAY_OUTLINES(Names.Config.Category.RENDER, Names.Config.DRAW_LINES, "visuals", "schematicOverlayEnableOutlines"),
        OVERLAY_SIDES(Names.Config.Category.RENDER, Names.Config.DRAW_QUADS, "visuals", "schematicOverlayEnableSides"),
        TRANSLUCENT(Names.Config.Category.RENDER, Names.Config.ALPHA_ENABLED, "visuals", "renderBlocksAsTranslucent"),
        AREA_BOXES(Names.Config.Category.RENDER, "enableAreaSelectionBoxesRendering", "visuals"),
        PLACEMENT_BOXES(Names.Config.Category.RENDER, "enablePlacementBoxesRendering", "visuals"),
        INFO_OVERLAY(BlockInfoHudSettings.CATEGORY, "blockInfoOverlayEnabled", "info_overlays"),
        VERIFIER_OVERLAY(BlockInfoHudSettings.CATEGORY, "verifierOverlayEnabled", "info_overlays");

        public final String category, key, group, upstream;

        Toggle(String category, String key, String group) { this(category, key, group, key); }
        Toggle(String category, String key, String group, String upstream) {
            this.category = category; this.key = key; this.group = group; this.upstream = upstream;
        }

        public String prettyName() { return VisualSettings.prettyName(group, upstream); }
    }

    private VisualSettings() {}

    public static void load(Configuration config) {
        rendering = flag(config, Names.Config.Category.RENDER, "enableRendering", true, "visuals");
        schematic = flag(config, Names.Config.Category.RENDER, "enableSchematicRendering", true, "visuals");
        blocks = flag(config, Names.Config.Category.RENDER, "enableSchematicBlocksRendering", true, "visuals");
        areaBoxes = flag(config, Names.Config.Category.RENDER, "enableAreaSelectionBoxesRendering", true, "visuals");
        placementBoxes = flag(config, Names.Config.Category.RENDER, "enablePlacementBoxesRendering", true, "visuals");
        overlayThrough = flag(config, Names.Config.Category.RENDER, "schematicOverlayRenderThroughBlocks", false, "visuals");
        blockInfoOverlay = flag(config, BlockInfoHudSettings.CATEGORY, "blockInfoOverlayEnabled", true, "info_overlays");
    }

    private static boolean flag(Configuration config, String category, String key, boolean fallback, String group) {
        Property property = config.get(category, key, fallback);
        property.setLanguageKey("litematica.config." + group + ".name." + key);
        return property.getBoolean(fallback);
    }

    /** Schematic blocks, tile entities and entities are drawn, and schematic blocks can be targeted. */
    public static boolean schematicVisible() {
        return rendering && schematic != Hotkeys.held("invertGhostBlockRenderState");
    }

    public static boolean blocksVisible() { return schematicVisible() && blocks; }

    public static boolean overlayVisible() {
        return rendering && ConfigurationHandler.highlight != Hotkeys.held("invertOverlayRenderState");
    }

    public static boolean overlayThroughBlocks() {
        return overlayThrough || Hotkeys.held("renderOverlayThroughBlocks");
    }

    /** Gates captured once per rendered frame by {@link #beginFrame()}. */
    public static boolean frameBlocks, frameSchematic, frameOverlay, frameThrough;

    public static void beginFrame() {
        frameSchematic = schematicVisible();
        frameBlocks = frameSchematic && blocks;
        frameOverlay = overlayVisible();
        frameThrough = overlayThroughBlocks();
    }

    /** Flips a boolean option, saves it and prints the upstream action bar message; returns the new value. */
    public static boolean toggle(Toggle toggle) {
        Configuration config = ConfigurationHandler.configuration;
        Property property = config.getCategory(toggle.category).get(toggle.key);
        boolean value = !property.getBoolean();
        property.set(value);
        ConfigurationHandler.loadConfiguration();
        config.save();
        printToggle(toggle.prettyName(), value);
        return value;
    }

    public static String prettyName(String group, String name) {
        String key = "litematica.config." + group + ".prettyName." + name;
        String text = UiTranslations.format(key);
        return text.equals(key) ? name : text;
    }

    /** MaLiLib's printBooleanConfigToggleMessage on the action bar. */
    public static void printToggle(String prettyName, boolean value) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.ingameGUI == null) return;
        String state = (value ? EnumChatFormatting.GREEN : EnumChatFormatting.RED) + UiTranslations.format("malilib.message.value." + (value ? "on" : "off"))
            + EnumChatFormatting.RESET;
        mc.ingameGUI.func_110326_a(UiTranslations.format("malilib.message.toggled", prettyName, state), false);
    }
}

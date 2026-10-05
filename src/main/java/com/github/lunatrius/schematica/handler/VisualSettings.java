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
    public static boolean overlayMissing = true, overlayWrongBlock = true, overlayWrongState = true, overlayDiffBlock = true;
    public static double outlineWidth = 1.0, outlineWidthThrough = 3.0;
    public static boolean renderEntities = true, renderTileEntities = true, fluids = true, fakeLighting = true, aoModern, renderColliding;
    public static boolean ignoreExistingFluids, modelOutline = true, modelSides = true, reducedInnerSides, overlayCulling = true, entityHitboxes = true;
    public static boolean ignoreCropAge, ignoreSurvivalStates = true;
    public static boolean areaBoxSides = true, placementBoxSides, enclosingBox = true, enclosingBoxSides;
    public static int fakeLightLevel = 15;
    public static double placementBoxSideAlpha = 0.2;

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
        FLUIDS(Names.Config.Category.RENDER, "enableSchematicFluidRendering", "visuals"),
        OVERLAY_CULLING(Names.Config.Category.RENDER, "enableSchematicOverlayCulling", "visuals"),
        ENTITY_HITBOXES(Names.Config.Category.RENDER, "enableSchematicEntityHitboxes", "visuals"),
        FAKE_LIGHTING(Names.Config.Category.RENDER, "enableSchematicFakeLighting", "visuals"),
        REDUCED_INNER_SIDES(Names.Config.Category.RENDER, "overlayReducedInnerSides", "visuals"),
        AO_MODERN(Names.Config.Category.RENDER, "renderAOModernEnable", "visuals"),
        COLLIDING(Names.Config.Category.RENDER, "renderCollidingSchematicBlocks", "visuals"),
        ENTITIES(Names.Config.Category.RENDER, "renderSchematicEntities", "visuals"),
        TILE_ENTITIES(Names.Config.Category.RENDER, "renderSchematicTileEntities", "visuals"),
        MODEL_OUTLINE(Names.Config.Category.RENDER, "schematicOverlayModelOutline", "visuals"),
        MODEL_SIDES(Names.Config.Category.RENDER, "schematicOverlayModelSides", "visuals"),
        OVERLAY_THROUGH(Names.Config.Category.RENDER, "schematicOverlayRenderThroughBlocks", "visuals"),
        TYPE_DIFF_BLOCK(Names.Config.Category.RENDER, "schematicOverlayTypeDiffBlock", "visuals"),
        TYPE_EXTRA(Names.Config.Category.RENDER, Names.Config.HIGHLIGHT_AIR, "visuals", "schematicOverlayTypeExtra"),
        TYPE_MISSING(Names.Config.Category.RENDER, "schematicOverlayTypeMissing", "visuals"),
        TYPE_WRONG_BLOCK(Names.Config.Category.RENDER, "schematicOverlayTypeWrongBlock", "visuals"),
        TYPE_WRONG_STATE(Names.Config.Category.RENDER, "schematicOverlayTypeWrongState", "visuals"),
        INFO_OVERLAY(BlockInfoHudSettings.CATEGORY, "blockInfoOverlayEnabled", "info_overlays"),
        VERIFIER_OVERLAY(BlockInfoHudSettings.CATEGORY, "verifierOverlayEnabled", "info_overlays");

        public final String category, key, group, upstream;

        Toggle(String category, String key, String group) { this(category, key, group, key); }
        Toggle(String category, String key, String group, String upstream) {
            this.category = category; this.key = key; this.group = group; this.upstream = upstream;
        }

        public String prettyName() { return VisualSettings.prettyName(group, upstream); }

        /** The option that a ConfigBooleanHotkeyed toggle hotkey (named after the upstream option) flips. */
        public static Toggle byUpstream(String name) {
            for (Toggle toggle : values()) if (toggle.upstream.equals(name)) return toggle;
            return null;
        }

        /** The option stored in a Forge property, for the config rows that carry a toggle hotkey. */
        public static Toggle byProperty(String category, String key) {
            for (Toggle toggle : values()) if (toggle.category.equals(category) && toggle.key.equals(key)) return toggle;
            return null;
        }
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
        overlayMissing = flag(config, Names.Config.Category.RENDER, "schematicOverlayTypeMissing", true, "visuals");
        overlayWrongBlock = flag(config, Names.Config.Category.RENDER, "schematicOverlayTypeWrongBlock", true, "visuals");
        overlayWrongState = flag(config, Names.Config.Category.RENDER, "schematicOverlayTypeWrongState", true, "visuals");
        overlayDiffBlock = flag(config, Names.Config.Category.RENDER, "schematicOverlayTypeDiffBlock", true, "visuals");
        String r = Names.Config.Category.RENDER;
        renderEntities = flag(config, r, "renderSchematicEntities", true, "visuals");
        renderTileEntities = flag(config, r, "renderSchematicTileEntities", true, "visuals");
        fluids = flag(config, r, "enableSchematicFluidRendering", true, "visuals");
        fakeLighting = flag(config, r, "enableSchematicFakeLighting", true, "visuals");
        aoModern = flag(config, r, "renderAOModernEnable", false, "visuals");
        renderColliding = flag(config, r, "renderCollidingSchematicBlocks", false, "visuals");
        ignoreExistingFluids = flag(config, r, "ignoreExistingFluids", false, "visuals");
        ignoreCropAge = flag(config, r, "ignoreCropAge", false, "visuals");
        Property survival = config.get(r, "ignoreSurvivalStates", true);
        survival.setLanguageKey(Names.Config.LANG_PREFIX + ".ignoreSurvivalStates");
        ignoreSurvivalStates = survival.getBoolean(true);
        modelOutline = flag(config, r, "schematicOverlayModelOutline", true, "visuals");
        modelSides = flag(config, r, "schematicOverlayModelSides", true, "visuals");
        reducedInnerSides = flag(config, r, "overlayReducedInnerSides", false, "visuals");
        overlayCulling = flag(config, r, "enableSchematicOverlayCulling", true, "visuals");
        entityHitboxes = flag(config, r, "enableSchematicEntityHitboxes", true, "visuals");
        areaBoxSides = flag(config, r, "renderAreaSelectionBoxSides", true, "visuals");
        placementBoxSides = flag(config, r, "renderPlacementBoxSides", false, "visuals");
        enclosingBox = flag(config, r, "renderPlacementEnclosingBox", true, "visuals");
        enclosingBoxSides = flag(config, r, "renderPlacementEnclosingBoxSides", false, "visuals");
        Property light = config.get(r, "renderFakeLightingLevel", 15);
        light.setLanguageKey("litematica.config.visuals.name.renderFakeLightingLevel");
        light.setMinValue(0).setMaxValue(15);
        fakeLightLevel = Math.max(0, Math.min(15, light.getInt(15)));
        Property distance = config.get(r, "schematicRenderDistance", 0);
        distance.setLanguageKey(Names.Config.LANG_PREFIX + ".schematicRenderDistance");
        distance.setMinValue(0).setMaxValue(1024);
        com.github.lunatrius.schematica.client.renderer.RenderBudget.configuredDistance = Math.max(0, Math.min(1024, distance.getInt(0)));
        Property minFps = config.get(r, "adaptiveRenderingMinFps", 30);
        minFps.setLanguageKey(Names.Config.LANG_PREFIX + ".adaptiveRenderingMinFps");
        minFps.setMinValue(0).setMaxValue(240);
        com.github.lunatrius.schematica.client.renderer.RenderBudget.minFps = Math.max(0, Math.min(240, minFps.getInt(30)));
        Property sideAlpha = config.get(r, "placementBoxSideAlpha", 0.2);
        sideAlpha.setLanguageKey("litematica.config.visuals.name.placementBoxSideAlpha");
        sideAlpha.setMinValue(0.0).setMaxValue(1.0);
        placementBoxSideAlpha = sideAlpha.getDouble(0.2);
        if (!Double.isFinite(placementBoxSideAlpha) || placementBoxSideAlpha < 0 || placementBoxSideAlpha > 1) placementBoxSideAlpha = 0.2;
        outlineWidth = width(config, "schematicOverlayOutlineWidth", 1.0);
        outlineWidthThrough = width(config, "schematicOverlayOutlineWidthThrough", 3.0);
    }

    private static double width(Configuration config, String key, double fallback) {
        Property property = config.get(Names.Config.Category.RENDER, key, fallback);
        property.setLanguageKey("litematica.config.visuals.name." + key);
        property.setMinValue(0.0).setMaxValue(64.0);
        double value = property.getDouble(fallback);
        return Double.isFinite(value) && value >= 0 && value <= 64 ? value : fallback;
    }

    /** The overlay outline width: wider when the overlay is drawn through blocks. */
    public static float frameOutlineWidth() {
        return (float) Math.max(0.1, frameThrough ? outlineWidthThrough : outlineWidth);
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
        com.github.lunatrius.schematica.client.renderer.hud.StatusInfoHud.startOverride();
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

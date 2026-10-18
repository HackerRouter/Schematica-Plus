// SPDX-License-Identifier: LGPL-3.0-only
// Litematica visuals switches and render toggles, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.handler;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.input.Hotkeys;
import com.github.lunatrius.schematica.client.renderer.RenderBudget;
import com.github.lunatrius.schematica.client.renderer.hud.StatusInfoHud;
import com.github.lunatrius.schematica.client.util.InfoUtils;
import com.github.lunatrius.schematica.reference.Names;

/** Upstream Configs.Visuals master switches that Plus honors, plus the hold-to-invert hotkeys. */
public final class VisualSettings {
    public static boolean rendering = true, schematic = true, blocks = true, areaBoxes = true, placementBoxes = true, overlayThrough;
    public static boolean blockInfoOverlay = true;
    public static boolean overlayMissing = true, overlayWrongBlock = true, overlayWrongState = true, overlayDiffBlock = true;
    public static double outlineWidth = 1.0, outlineWidthThrough = 3.0;
    public static boolean renderEntities = true, renderTileEntities = true, fluids = true, fakeLighting = true, aoModern, renderColliding;
    public static boolean ignoreExistingFluids, modelOutline = true, modelSides = true, reducedInnerSides, overlayCulling = true, entityHitboxes = true;
    public static boolean ignoreCropAge, ignoreSurvivalStates;
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
        ConfigReader visuals = new ConfigReader(config, Names.Config.Category.RENDER, ConfigReader.VISUALS);
        rendering = visuals.flag("enableRendering", true);
        schematic = visuals.flag("enableSchematicRendering", true);
        blocks = visuals.flag("enableSchematicBlocksRendering", true);
        areaBoxes = visuals.flag("enableAreaSelectionBoxesRendering", true);
        placementBoxes = visuals.flag("enablePlacementBoxesRendering", true);
        overlayThrough = visuals.flag("schematicOverlayRenderThroughBlocks", false);
        blockInfoOverlay = visuals.in(BlockInfoHudSettings.CATEGORY, ConfigReader.INFO_OVERLAYS).flag("blockInfoOverlayEnabled", true);
        overlayMissing = visuals.flag("schematicOverlayTypeMissing", true);
        overlayWrongBlock = visuals.flag("schematicOverlayTypeWrongBlock", true);
        overlayWrongState = visuals.flag("schematicOverlayTypeWrongState", true);
        overlayDiffBlock = visuals.flag("schematicOverlayTypeDiffBlock", true);
        renderEntities = visuals.flag("renderSchematicEntities", true);
        renderTileEntities = visuals.flag("renderSchematicTileEntities", true);
        fluids = visuals.flag("enableSchematicFluidRendering", true);
        fakeLighting = visuals.flag("enableSchematicFakeLighting", true);
        aoModern = visuals.flag("renderAOModernEnable", false);
        renderColliding = visuals.flag("renderCollidingSchematicBlocks", false);
        ignoreExistingFluids = visuals.flag("ignoreExistingFluids", false);
        ignoreCropAge = visuals.flag("ignoreCropAge", false);
        ConfigReader legacy = visuals.in(Names.Config.Category.RENDER, ConfigReader.LEGACY);
        ignoreSurvivalStates = legacy.flag("ignoreSurvivalStates", false);
        modelOutline = visuals.flag("schematicOverlayModelOutline", true);
        modelSides = visuals.flag("schematicOverlayModelSides", true);
        reducedInnerSides = visuals.flag("overlayReducedInnerSides", false);
        overlayCulling = visuals.flag("enableSchematicOverlayCulling", true);
        entityHitboxes = visuals.flag("enableSchematicEntityHitboxes", true);
        areaBoxSides = visuals.flag("renderAreaSelectionBoxSides", true);
        placementBoxSides = visuals.flag("renderPlacementBoxSides", false);
        enclosingBox = visuals.flag("renderPlacementEnclosingBox", true);
        enclosingBoxSides = visuals.flag("renderPlacementEnclosingBoxSides", false);
        fakeLightLevel = visuals.integer("renderFakeLightingLevel", 15, 0, 15);
        RenderBudget.configuredDistance = legacy.integer("schematicRenderDistance", 0, 0, 1024);
        RenderBudget.minFps = legacy.integer("adaptiveRenderingMinFps", 30, 0, 240);
        placementBoxSideAlpha = visuals.decimal("placementBoxSideAlpha", 0.2, 0.0, 1.0);
        outlineWidth = visuals.decimal("schematicOverlayOutlineWidth", 1.0, 0.0, 64.0);
        outlineWidthThrough = visuals.decimal("schematicOverlayOutlineWidthThrough", 3.0, 0.0, 64.0);
    }

    /** The overlay outline width: wider when the overlay is drawn through blocks. */
    public static float frameOutlineWidth() {
        return (float) Math.max(0.1, frameThrough ? outlineWidthThrough : outlineWidth);
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
        InfoUtils.printBooleanConfigToggleMessage(toggle.prettyName(), value);
        StatusInfoHud.startOverride();
        return value;
    }

    public static String prettyName(String group, String name) {
        String key = "litematica.config." + group + ".prettyName." + name;
        String text = UiTranslations.format(key);
        return text.equals(key) ? name : text;
    }
}

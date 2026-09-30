// SPDX-License-Identifier: LGPL-3.0-only
// Litematica color options, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.handler;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import com.github.lunatrius.schematica.util.ColorValue;

public enum RenderColors {
    AREA_SIDES("areaSelectionBoxSideColor", 0x30FFFFFF, true),
    INVENTORY("hightlightBlockInInventoryColor", 0x30FF30FF, false),
    MATERIAL_HUD("materialListHudItemCountsColor", 0xFFFFAA00, false),
    REBUILD_BREAK("schematicRebuildBreakPlaceOverlayColor", 0x4C33CC33, false),
    REBUILD_EXCEPT("schematicRebuildBreakExceptPlaceOverlayColor", 0x4CF03030, false),
    REBUILD_REPLACE("schematicRebuildReplaceOverlayColor", 0x4CF0A010, false),
    DIFFERENT_TYPE("schematicOverlayColorDiffBlock", 0x30F8D650, false),
    EXTRA("schematicOverlayColorExtra", 0x4CFF4CE6, true),
    MISSING("schematicOverlayColorMissing", 0x2C33B3E6, true),
    WRONG_BLOCK("schematicOverlayColorWrongBlock", 0x4CFF3333, true),
    WRONG_STATE("schematicOverlayColorWrongState", 0x4CFF9010, true);

    public static final String CATEGORY = "colors";
    public final String key;
    public final int defaultColor;
    public final boolean available;
    private int color;

    RenderColors(String key, int color, boolean available) {
        this.key = key;
        this.defaultColor = this.color = color;
        this.available = available;
    }

    public int color() { return color; }

    public static RenderColors find(String category, String key) {
        if (CATEGORY.equals(category)) for (RenderColors option : values()) if (option.key.equals(key)) return option;
        return null;
    }

    public static void load(Configuration configuration) {
        for (RenderColors option : values()) {
            Property property = configuration.get(CATEGORY, option.key, ColorValue.format(option.defaultColor),
                "ARGB color (#AARRGGBB)." + (option.available ? "" : " Reserved for a feature not yet ported."));
            property.setLanguageKey("litematica.config.colors.name." + option.key);
            property.setValidationPattern(ColorValue.FORMAT);
            try { option.color = ColorValue.parse(property.getString()); }
            catch (IllegalArgumentException e) { option.color = option.defaultColor; }
        }
    }
}

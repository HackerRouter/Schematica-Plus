package com.github.lunatrius.schematica.client.gui.config;

import com.github.lunatrius.schematica.reference.Names;

public final class ConfigTranslations {
    private ConfigTranslations() {}

    public static String label(String key) {
        String prefix = Names.Config.LANG_PREFIX + ".";
        if (!key.startsWith(prefix)) return key;
        switch (key.substring(prefix.length())) {
            case Names.Config.ALPHA_ENABLED: return "litematica.config.visuals.name.renderBlocksAsTranslucent";
            case Names.Config.ALPHA: return "litematica.config.visuals.name.ghostBlockAlpha";
            case Names.Config.HIGHLIGHT: return "litematica.config.visuals.name.enableSchematicOverlay";
            case Names.Config.HIGHLIGHT_AIR: return "litematica.config.visuals.name.schematicOverlayTypeExtra";
            case Names.Config.DRAW_QUADS: return "litematica.config.visuals.name.schematicOverlayEnableSides";
            case Names.Config.DRAW_LINES: return "litematica.config.visuals.name.schematicOverlayEnableOutlines";
            case Names.Config.SCHEMATIC_DIRECTORY: return "litematica.config.generic.name.customSchematicBaseDirectory";
            case Names.Config.TOOL_ITEM: return "litematica.config.generic.name.toolItem";
            default: return key;
        }
    }

    public static String comment(String key) {
        String label = label(key);
        return label.startsWith("litematica.config.") && label.contains(".name.") ? label.replace(".name.", ".comment.") : key + ".tooltip";
    }
}

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

    /** The upstream option-list label key for an enumerated config value, or null when the property has none. */
    public static String optionKey(String name, String value) {
        switch (name) {
            case "pasteReplaceBehavior": return "litematica.gui.label.replace_behavior." + value;
            case "pasteLayerBehavior": return "litematica.gui.label.paste_layer_behavior." + value;
            case "placementRestrictionWarn": return "malilib.label.message_output_type." + value;
            case "schematicVcsDeleteMode": return "litematica.gui.label.placement_deletion_mode." + value;
            case "easyPlaceProtocolVersion": return "litematica.gui.label.easy_place_protocol." + value;
            case "printerBuildOrder": return "schematica.printer.build_order." + value;
            case "printerIteratorShape": return "schematica.printer.shape." + value;
            case "printerIteratorMode": return "schematica.printer.order." + value;
            case "printSelectionType": return "schematica.printer.selection." + value;
            default: return null;
        }
    }

    public static String comment(String key) {
        String label = label(key);
        return label.startsWith("litematica.config.") && label.contains(".name.") ? label.replace(".name.", ".comment.") : key + ".tooltip";
    }
}

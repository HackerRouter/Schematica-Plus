package com.github.lunatrius.schematica.client.gui.config;

public final class ConfigTranslations {
    private ConfigTranslations() {}

    public static String label(String key) {
        switch (key) {
            case "schematica.config.alphaEnabled": return "litematica.config.visuals.name.renderBlocksAsTranslucent";
            case "schematica.config.alpha": return "litematica.config.visuals.name.ghostBlockAlpha";
            case "schematica.config.highlight": return "litematica.config.visuals.name.enableSchematicOverlay";
            case "schematica.config.highlightAir": return "litematica.config.visuals.name.schematicOverlayTypeExtra";
            case "schematica.config.drawQuads": return "litematica.config.visuals.name.schematicOverlayEnableSides";
            case "schematica.config.drawLines": return "litematica.config.visuals.name.schematicOverlayEnableOutlines";
            case "schematica.config.schematicDirectory": return "litematica.config.generic.name.customSchematicBaseDirectory";
            case "schematica.config.toolItem": return "litematica.config.generic.name.toolItem";
            case "schematica.key.load": return "litematica.gui.title.load_schematic";
            case "schematica.key.save": return "litematica.config.hotkeys.name.saveAreaAsSchematicToFile";
            case "schematica.key.control": return "litematica.config.hotkeys.name.openGuiMainMenu";
            case "schematica.key.execute": return "litematica.config.hotkeys.name.executeOperation";
            case "schematica.key.layerInc": return "litematica.config.hotkeys.name.layerNext";
            case "schematica.key.layerDec": return "litematica.config.hotkeys.name.layerPrevious";
            default: return key;
        }
    }

    public static String comment(String key) {
        String label = label(key);
        return label.startsWith("litematica.config.visuals.name.") || label.startsWith("litematica.config.info_overlays.name.")
            ? label.replace(".name.", ".comment.") : key + ".tooltip";
    }
}

package com.github.lunatrius.schematica.tool;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

/**
 * Tool modes ported from Litematica's ToolMode concept, adapted for Schematica's 1.7.10 architecture.
 * <p>
 * Provides a cycleable mode system that controls which operations are active and
 * what the schematic tool item does when used. Integrates with the existing
 * SchematicPrinter, area selection (pointA/pointB), and placement systems.
 *
 * @author HackerRouter (ported from Litematica by masa)
 */
public enum ToolMode {

    AREA_SELECTION("schematica.tool_mode.area_selection", false, false),
    SCHEMATIC_PLACEMENT("schematica.tool_mode.schematic_placement", false, true),
    PASTE_SCHEMATIC("schematica.tool_mode.paste_schematic", true, true),
    MOVE("schematica.tool_mode.move", false, true),
    DELETE("schematica.tool_mode.delete", true, false),
    FILL("schematica.tool_mode.fill", true, false),
    REPLACE_BLOCK("schematica.tool_mode.replace_block", true, false);

    private final String translationKey;
    private final boolean creativeOnly;
    private final boolean usesSchematic;

    private static final ToolMode[] VALUES = values();

    ToolMode(String translationKey, boolean creativeOnly, boolean usesSchematic) {
        this.translationKey = translationKey;
        this.creativeOnly = creativeOnly;
        this.usesSchematic = usesSchematic;
    }

    public boolean isCreativeOnly() {
        return this.creativeOnly;
    }

    public boolean getUsesSchematic() {
        return this.usesSchematic;
    }

    public boolean getUsesAreaSelection() {
        return !this.usesSchematic;
    }

    public String getDisplayName() {
        return I18n.format(this.translationKey);
    }

    /**
     * Cycles to the next available tool mode, skipping creative-only modes when not in creative.
     */
    public ToolMode cycle(boolean forward) {
        boolean isCreative = Minecraft.getMinecraft().thePlayer != null
            && Minecraft.getMinecraft().thePlayer.capabilities.isCreativeMode;
        int numModes = VALUES.length;
        int inc = forward ? 1 : -1;
        int nextId = this.ordinal() + inc;

        for (int i = 0; i < numModes; i++) {
            if (nextId < 0) {
                nextId = numModes - 1;
            } else if (nextId >= numModes) {
                nextId = 0;
            }

            ToolMode mode = VALUES[nextId];
            if (isCreative || !mode.creativeOnly) {
                return mode;
            }
            nextId += inc;
        }

        return this;
    }

    public static ToolMode fromString(String name) {
        try {
            return ToolMode.valueOf(name);
        } catch (IllegalArgumentException e) {
            return AREA_SELECTION;
        }
    }
}

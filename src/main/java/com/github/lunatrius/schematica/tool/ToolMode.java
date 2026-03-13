package com.github.lunatrius.schematica.tool;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

import cpw.mods.fml.common.registry.GameData;

/**
 * Tool modes ported from Litematica's ToolMode concept, adapted for Schematica's 1.7.10 architecture.
 * <p>
 * Provides a cycleable mode system that controls which operations are active and
 * what the schematic tool item does when used. Integrates with the existing
 * SchematicPrinter, area selection (pointA/pointB), and placement systems.
 * <p>
 * Each mode can optionally store a primaryBlock and secondaryBlock for
 * fill/replace operations (picked via crosshair, Litematica-style).
 *
 * @author HackerRouter (ported from Litematica by masa)
 */
public enum ToolMode {

    AREA_SELECTION("schematica.tool_mode.area_selection", false, false, false, false),
    SCHEMATIC_PLACEMENT("schematica.tool_mode.schematic_placement", false, true, false, false),
    PASTE_SCHEMATIC("schematica.tool_mode.paste_schematic", true, true, false, false),
    MOVE("schematica.tool_mode.move", false, true, false, false),
    DELETE("schematica.tool_mode.delete", true, false, false, false),
    FILL("schematica.tool_mode.fill", true, false, true, false),
    REPLACE_BLOCK("schematica.tool_mode.replace_block", true, false, true, true);

    private final String translationKey;
    private final boolean creativeOnly;
    private final boolean usesSchematic;
    private final boolean usesBlockPrimary;
    private final boolean usesBlockSecondary;

    /** Picked primary block (e.g. the block to fill with). */
    private Block primaryBlock = null;
    private int primaryMeta = 0;

    /** Picked secondary block (e.g. the block to be replaced). */
    private Block secondaryBlock = null;
    private int secondaryMeta = 0;

    private static final ToolMode[] VALUES = values();

    ToolMode(String translationKey, boolean creativeOnly, boolean usesSchematic,
             boolean usesBlockPrimary, boolean usesBlockSecondary) {
        this.translationKey = translationKey;
        this.creativeOnly = creativeOnly;
        this.usesSchematic = usesSchematic;
        this.usesBlockPrimary = usesBlockPrimary;
        this.usesBlockSecondary = usesBlockSecondary;
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

    /** Whether this mode uses a primary block (picked via crosshair). */
    public boolean getUsesBlockPrimary() {
        return this.usesBlockPrimary;
    }

    /** Whether this mode uses a secondary block (picked via crosshair). */
    public boolean getUsesBlockSecondary() {
        return this.usesBlockSecondary;
    }

    public Block getPrimaryBlock() {
        return this.primaryBlock;
    }

    public int getPrimaryMeta() {
        return this.primaryMeta;
    }

    public void setPrimaryBlock(Block block, int meta) {
        this.primaryBlock = block;
        this.primaryMeta = meta;
    }

    public Block getSecondaryBlock() {
        return this.secondaryBlock;
    }

    public int getSecondaryMeta() {
        return this.secondaryMeta;
    }

    public void setSecondaryBlock(Block block, int meta) {
        this.secondaryBlock = block;
        this.secondaryMeta = meta;
    }

    /** Returns a display string for the primary block, or null if not set. */
    public String getPrimaryBlockName() {
        if (this.primaryBlock == null) return null;
        String name = GameData.getBlockRegistry().getNameForObject(this.primaryBlock);
        return name + ":" + this.primaryMeta;
    }

    /** Returns a display string for the secondary block, or null if not set. */
    public String getSecondaryBlockName() {
        if (this.secondaryBlock == null) return null;
        String name = GameData.getBlockRegistry().getNameForObject(this.secondaryBlock);
        return name + ":" + this.secondaryMeta;
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

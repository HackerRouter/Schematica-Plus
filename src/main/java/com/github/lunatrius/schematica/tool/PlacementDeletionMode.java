// SPDX-License-Identifier: LGPL-3.0-only
// Litematica PlacementDeletionMode, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.tool;

import java.util.Locale;

public enum PlacementDeletionMode {
    MATCHING_BLOCK("matching_block"),
    NON_MATCHING_BLOCK("non_matching_block"),
    ANY_SCHEMATIC_BLOCK("any_schematic_block"),
    NO_SCHEMATIC_BLOCK("no_schematic_block"),
    ENTIRE_VOLUME("entire_volume");

    public final String value;

    PlacementDeletionMode(String value) { this.value = value; }

    public String translationKey() { return "litematica.gui.label.placement_deletion_mode." + value; }

    /** TaskDeleteBlocksByPlacement's check for a non-air world block, comparing block and metadata. */
    public boolean deletes(boolean schematicAir, boolean same) {
        switch (this) {
            case MATCHING_BLOCK: return !schematicAir && same;
            case NON_MATCHING_BLOCK: return !schematicAir && !same;
            case ANY_SCHEMATIC_BLOCK: return !schematicAir;
            case NO_SCHEMATIC_BLOCK: return schematicAir;
            default: return true;
        }
    }

    public static PlacementDeletionMode parse(String value) {
        if (value != null) {
            for (PlacementDeletionMode mode : values()) if (mode.value.equals(value.toLowerCase(Locale.ROOT))) return mode;
        }
        return MATCHING_BLOCK;
    }

    public static String[] names() {
        PlacementDeletionMode[] values = values();
        String[] names = new String[values.length];
        for (int i = 0; i < values.length; i++) names[i] = values[i].value;
        return names;
    }
}

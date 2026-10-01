// SPDX-License-Identifier: LGPL-3.0-only
// Litematica ReplaceBehavior, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.tool;

import java.util.Locale;

public enum ReplaceBehavior {
    NONE("none"),
    ALL("all"),
    WITH_NON_AIR("with_non_air");

    public final String value;

    ReplaceBehavior(String value) { this.value = value; }

    public String translationKey() { return "litematica.gui.label.replace_behavior." + value; }

    /** Whether a schematic block is pasted over the existing world block (Litematica's paste skip rule). */
    public boolean places(boolean worldAir, boolean schematicAir) {
        return (this != NONE || worldAir) && (this != WITH_NON_AIR || !schematicAir);
    }

    public ReplaceBehavior cycle(boolean reverse) {
        ReplaceBehavior[] values = values();
        return values[Math.floorMod(ordinal() + (reverse ? -1 : 1), values.length)];
    }

    public static ReplaceBehavior parse(String value) {
        if (value != null) {
            for (ReplaceBehavior behavior : values()) if (behavior.value.equals(value.toLowerCase(Locale.ROOT))) return behavior;
        }
        return NONE;
    }

    public static String[] names() {
        ReplaceBehavior[] values = values();
        String[] names = new String[values.length];
        for (int i = 0; i < values.length; i++) names[i] = values[i].value;
        return names;
    }
}

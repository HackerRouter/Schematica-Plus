// SPDX-License-Identifier: LGPL-3.0-only
// Litematica status info HUD (StatusInfoRenderer), adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.renderer.hud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import net.minecraft.util.EnumChatFormatting;

import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.world.RenderLayerRange;
import com.github.lunatrius.schematica.client.world.RenderLayerSettings;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.handler.VisualSettings;

public final class StatusInfoHud {
    private static long overrideUntil;

    private StatusInfoHud() {}

    /** Shows the HUD for ten seconds when statusInfoHudAuto is on and something is hidden or layer-limited. */
    public static void startOverride() {
        if (shouldOverride()) overrideUntil = System.currentTimeMillis() + 10000;
    }

    private static boolean shouldOverride() {
        return ConfigurationHandler.statusInfoHudAuto && (RenderLayerSettings.RANGE.mode() != RenderLayerRange.Mode.ALL
            || !VisualSettings.rendering || !VisualSettings.schematic || !VisualSettings.blocks || !ConfigurationHandler.highlight || !VisualSettings.areaBoxes);
    }

    public static boolean visible() {
        return ConfigurationHandler.statusInfoHud || System.currentTimeMillis() < overrideUntil;
    }

    public static List<String> lines() {
        if (!visible()) return Collections.emptyList();
        List<String> lines = new ArrayList<>();
        if (ConfigurationHandler.easyPlaceMode) lines.add(UiTranslations.format("litematica.hud.misc.easy_place_mode_enabled"));
        else if (ConfigurationHandler.placementRestriction) lines.add(UiTranslations.format("litematica.hud.misc.placement_restriction_mode_enabled"));
        String green = EnumChatFormatting.GREEN.toString(), reset = EnumChatFormatting.RESET.toString();
        RenderLayerRange range = RenderLayerSettings.RANGE;
        String mode = green + UiTranslations.format(range.mode().translationKey()) + reset;
        if (range.mode() == RenderLayerRange.Mode.ALL) lines.add(UiTranslations.format("litematica.hud.misc.render_layer_mode_all", mode));
        else {
            String value = range.mode() == RenderLayerRange.Mode.LAYER_RANGE ? String.format("%d ... %d", range.value(false), range.value(true)) : String.valueOf(range.value(false));
            lines.add(UiTranslations.format("litematica.hud.misc.render_layer_mode", mode,
                String.format("%s%s = %s%s", green, range.axis().name().toLowerCase(Locale.ROOT), value, reset)));
        }
        lines.add(UiTranslations.format("litematica.hud.misc.renderer_status", state(VisualSettings.rendering), state(VisualSettings.schematic),
            state(VisualSettings.blocks), state(ConfigurationHandler.highlight), state(VisualSettings.areaBoxes)));
        if (com.github.lunatrius.schematica.client.projects.SchematicProjects.hasProjectOpen()) lines.add(UiTranslations.format("litematica.hud.schematic_projects_mode"));
        return lines;
    }

    private static String state(boolean on) {
        return (on ? EnumChatFormatting.GREEN : EnumChatFormatting.RED) + UiTranslations.format("litematica.message.value." + (on ? "on" : "off")) + EnumChatFormatting.RESET;
    }
}

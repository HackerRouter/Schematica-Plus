// SPDX-License-Identifier: LGPL-3.0-only
// Litematica ToolHud and MaLiLib RenderUtils.renderText, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.renderer.hud;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import org.lwjgl.opengl.GL11;

import com.github.lunatrius.schematica.api.SchematicOrigin;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.projects.SchematicProject;
import com.github.lunatrius.schematica.client.projects.SchematicProjects;
import com.github.lunatrius.schematica.client.projects.SchematicVersion;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Area;
import com.github.lunatrius.schematica.client.selection.AreaSelections;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.client.world.SubRegionPlacements;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.handler.InfoHudSettings;
import com.github.lunatrius.schematica.handler.VerifierOverlaySettings;
import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.tool.ReplaceBehavior;
import com.github.lunatrius.schematica.tool.ToolManager;
import com.github.lunatrius.schematica.tool.ToolMode;
import com.github.lunatrius.schematica.util.HudAlignment;

/** The tool HUD (default bottom left) with the status info lines after it. */
public final class ToolHud {
    private static final String GREEN = EnumChatFormatting.GREEN.toString(), RST = EnumChatFormatting.RESET.toString();
    private static final String GOLD = EnumChatFormatting.GOLD.toString(), RED = EnumChatFormatting.RED.toString();
    private static final String WHITE = EnumChatFormatting.WHITE.toString(), AQUA = EnumChatFormatting.AQUA.toString();
    private static final SimpleDateFormat DATE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private ToolHud() {}

    private static String format(String key, Object... args) { return UiTranslations.format(key, args); }

    public static List<String> lines() {
        List<String> lines = new ArrayList<>();
        boolean hasTool = ToolManager.toolActive();
        if (hasTool && SchematicProjects.hasProjectOpen()) {
            projectLines(lines);
        } else {
            modeLines(lines, hasTool);
        }
        List<String> status = StatusInfoHud.lines();
        for (int i = 0; i < status.size() && i < VerifierOverlaySettings.maxLines; i++) lines.add(status.get(i));
        return lines;
    }

    private static void projectLines(List<String> lines) {
        SchematicProject project = SchematicProjects.current();
        lines.add(format("litematica.hud.schematic_projects.project_name", GREEN + project.name() + RST));
        SchematicVersion version = project.currentVersion();
        if (version != null) {
            lines.add(format("litematica.hud.schematic_projects.current_version", GREEN + version.version + RST,
                GREEN + project.versionCount() + RST, GREEN + version.name + RST));
            lines.add(format("litematica.hud.schematic_projects.current_version_date", GREEN + DATE.format(new Date(version.timeStamp)) + RST));
            Vector3i o = project.origin();
            lines.add(format("litematica.hud.schematic_projects.origin", GREEN + o.x + ", " + o.y + ", " + o.z + RST));
        } else {
            lines.add(format("litematica.hud.schematic_projects.no_versions"));
        }
        Area area = AreaSelections.library().selected();
        if (area != null && AreaSelections.library().mode() == AreaSelectionLibrary.Mode.NORMAL && area.selectedBox() != null) {
            lines.add(format("litematica.hud.area_selection.selected_sub_region", GREEN + area.boxName() + RST));
        }
        lines.add(format("litematica.hud.area_selection.selection_corners_mode", GREEN + format(AreaSelections.cornerModeKey()) + RST));
        // The Projects Mode indicator is part of the status info HUD when that is shown
        if (!StatusInfoHud.visible()) lines.add(format("litematica.hud.schematic_projects_mode"));
    }

    private static void modeLines(List<String> lines, boolean hasTool) {
        ToolMode mode = ToolManager.getCurrentMode();
        String yes = GREEN + format("litematica.label.yes") + RST, no = RED + format("litematica.label.no") + RST;
        if (hasTool && mode == ToolMode.DELETE) {
            lines.add(format("litematica.hud.delete.target_mode", GREEN + format(ToolMode.deleteUsesPlacement
                ? "litematica.hud.delete.target_mode.placement" : "litematica.hud.delete.target_mode.area") + RST));
        }
        if (hasTool && mode.getUsesAreaSelection()) {
            Area area = AreaSelections.library().selected();
            if (area != null) {
                String name = GREEN + area.name() + RST;
                lines.add(format(AreaSelections.library().mode() == AreaSelectionLibrary.Mode.NORMAL
                    ? "litematica.hud.area_selection.selected_area_normal" : "litematica.hud.area_selection.selected_area_simple", name));
                Vector3i o = area.origin();
                String kind = format(area.manualOrigin() == null ? "litematica.gui.label.origin.auto" : "litematica.gui.label.origin.manual");
                String origin = String.format("%d, %d, %d %s[%s%s%s]", o.x, o.y, o.z, RST, GOLD, kind, RST);
                lines.add(format("litematica.hud.area_selection.origin", GREEN + origin + RST) + " - "
                    + format("litematica.hud.area_selection.box_count", GREEN + area.boxes().size() + RST));
                if (area.selectedBox() != null) {
                    lines.add(format("litematica.hud.area_selection.selected_sub_region", GREEN + area.boxName() + RST));
                    Vector3i a = area.first(), b = area.second();
                    String size = GREEN + String.format("%dx%dx%d", Math.abs(a.x - b.x) + 1, Math.abs(a.y - b.y) + 1, Math.abs(a.z - b.z) + 1) + RST;
                    lines.add(format("litematica.hud.area_selection.dimensions_position", size,
                        GREEN + a.x + ", " + a.y + ", " + a.z + RST, GREEN + b.x + ", " + b.y + ", " + b.z + RST));
                }
            }
            if (mode.getUsesBlockPrimary() && mode.getPrimaryBlock() != null) {
                lines.add(format("litematica.tool_hud.block_1", blockString(mode.getPrimaryBlock(), mode.getPrimaryMeta())));
            }
            if (mode.getUsesBlockSecondary() && mode.getSecondaryBlock() != null) {
                lines.add(format("litematica.tool_hud.block_2", blockString(mode.getSecondaryBlock(), mode.getSecondaryMeta())));
            }
            lines.add(format("litematica.hud.area_selection.selection_corners_mode", GREEN + format(AreaSelections.cornerModeKey()) + RST));
        } else if ((hasTool || mode == ToolMode.REBUILD) && mode.getUsesSchematic()) {
            placementLines(lines, mode, yes, no);
        }
        if (hasTool || mode == ToolMode.REBUILD) {
            String name = mode == ToolMode.REBUILD ? GOLD + mode.getDisplayName() + RST : mode.getDisplayName();
            lines.add(String.format("%s [%s%d%s/%s%d%s]: %s%s%s", format("litematica.hud.selected_mode"), GREEN, mode.ordinal() + 1, WHITE,
                GREEN, ToolMode.values().length, WHITE, GREEN, name, RST));
        }
    }

    private static void placementLines(List<String> lines, ToolMode mode, String yes, String no) {
        SchematicWorld placement = ClientProxy.schematic;
        String label = format("litematica.hud.schematic_placement.selected_placement");
        if (placement == null) {
            lines.add(String.format("%s: %s%s%s", label, WHITE, "<" + format("litematica.label.none_lower") + ">", RST));
            return;
        }
        lines.add(String.format("%s: %s%s%s", label, GREEN, placement.name, RST));
        SubRegionPlacements regions = placement.subregions();
        int count = regions == null ? 1 : regions.regions().size();
        boolean modified = regions != null && regions.modified();
        lines.add(String.format("%s: %s%d%s", format("litematica.hud.schematic_placement.sub_region_count"), GREEN, count, RST)
            + String.format(" - %s: %s", format("litematica.hud.schematic_placement.sub_regions_modified"), modified ? yes : no));
        SchematicOrigin origin = placement.originPosition();
        lines.add(format("litematica.hud.area_selection.origin", GREEN + origin.x + ", " + origin.y + ", " + origin.z + RST));
        ItemStack held = Minecraft.getMinecraft().thePlayer == null ? null : Minecraft.getMinecraft().thePlayer.getCurrentEquippedItem();
        if (mode == ToolMode.REBUILD && mode.getPrimaryBlock() != null && (held == null || ToolManager.isHoldingToolItem())) {
            lines.add(format("litematica.tool_hud.block_1", blockString(mode.getPrimaryBlock(), mode.getPrimaryMeta())));
        }
        SubRegionPlacements.Region region = regions == null || regions.selected == null ? null : regions.get(regions.selected);
        if (region != null) {
            lines.add(String.format("%s: %s%s%s - %s: %s", format("litematica.hud.schematic_placement.selected_sub_region"), GREEN, region.name(), RST,
                format("litematica.hud.schematic_placement.sub_region_modified"), region.modified() ? yes : no));
            SchematicOrigin position = placement.subregionPosition(region.name());
            lines.add(format("litematica.hud.schematic_placement.sub_region_origin", GREEN + position.x + ", " + position.y + ", " + position.z + RST));
        }
        if (mode == ToolMode.PASTE_SCHEMATIC || mode == ToolMode.GRID_PASTE) {
            ReplaceBehavior replace = ConfigurationHandler.pasteReplaceBehavior;
            lines.add(format("litematica.hud.misc.schematic_paste.replace_mode", (replace == ReplaceBehavior.NONE ? RED : GOLD) + format(replace.translationKey()) + RST));
            boolean all = !ConfigurationHandler.pasteRenderLayersOnly;
            lines.add(format("litematica.hud.misc.schematic_paste.layer_mode", (all ? GREEN : AQUA)
                + format(all ? "litematica.gui.label.paste_layer_behavior.all" : "litematica.gui.label.paste_layer_behavior.rendered_only") + RST));
            lines.add(format("litematica.hud.misc.schematic_paste.ignore_inventory_contents", ConfigurationHandler.pasteIgnoreInventories ? yes : no));
        }
    }

    /** The block name (of its item when it has one, which names the variant). */
    static String blockString(Block block, int meta) {
        String name;
        try {
            Item item = Item.getItemFromBlock(block);
            name = item != null ? new ItemStack(item, 1, block.damageDropped(meta)).getDisplayName() : block.getLocalizedName();
        } catch (RuntimeException error) {
            name = block.getLocalizedName();
        }
        return GREEN + name + RST;
    }

    /** RenderUtils.renderText: each line on its own background, shadowed, at the tool HUD alignment, offsets and scale. */
    public static void render(Minecraft mc) {
        List<String> lines = lines();
        if (lines.isEmpty()) return;
        double scale = InfoHudSettings.toolScale;
        if (scale < 0.0125) return;
        FontRenderer font = mc.fontRenderer;
        ScaledResolution screen = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int lineHeight = font.FONT_HEIGHT + 2, contentHeight = lines.size() * lineHeight - 2, bgMargin = 2;
        HudAlignment alignment = InfoHudSettings.toolAlignment;
        int xOff = InfoHudSettings.toolOffsetX, yOff = InfoHudSettings.toolOffsetY;
        double posY = yOff + bgMargin;
        if (alignment == HudAlignment.BOTTOM_LEFT || alignment == HudAlignment.BOTTOM_RIGHT) posY = (int) (screen.getScaledHeight() / scale - contentHeight - yOff);
        else if (alignment == HudAlignment.CENTER) posY = (int) (screen.getScaledHeight() / scale / 2.0 - contentHeight / 2.0 + yOff);
        GL11.glPushMatrix();
        try {
            GL11.glScaled(scale, scale, 1);
            for (String line : lines) {
                int width = font.getStringWidth(line);
                double posX = xOff + bgMargin;
                if (alignment == HudAlignment.TOP_RIGHT || alignment == HudAlignment.BOTTOM_RIGHT) posX = screen.getScaledWidth() / scale - width - xOff - bgMargin;
                else if (alignment == HudAlignment.CENTER) posX = screen.getScaledWidth() / scale / 2 - width / 2.0 - xOff;
                int x = (int) posX, y = (int) posY;
                posY += lineHeight;
                Gui.drawRect(x - bgMargin, y - bgMargin, x + width, y + font.FONT_HEIGHT, 0x80000000);
                font.drawStringWithShadow(line, x, y, 0xFFFFFFFF);
            }
        } finally {
            GL11.glPopMatrix();
        }
    }
}

package com.github.lunatrius.schematica.handler.client;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import org.lwjgl.opengl.GL11;

import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.client.selection.AreaSelections;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Area;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.tool.ToolManager;
import com.github.lunatrius.schematica.tool.ToolMode;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.FMLControlledNamespacedRegistry;
import cpw.mods.fml.common.registry.GameData;

public class OverlayHandler {

    private static final FMLControlledNamespacedRegistry<Block> BLOCK_REGISTRY = GameData.getBlockRegistry();
    private final Minecraft minecraft = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onText(RenderGameOverlayEvent.Text event) {
        if (this.minecraft.gameSettings.showDebugInfo && ConfigurationHandler.showDebugInfo) {
            final SchematicWorld schematic = ClientProxy.schematic;
            if (schematic != null && schematic.isRenderingEnabled()) {
                event.left.add("");
                event.left.add("[§6" + Reference.NAME + "§r] " + schematic.getDebugDimensions());
                event.left.add("[§6" + UiTranslations.format("litematica.hud.selected_mode") + "§r] " + ToolManager.getCurrentMode().getDisplayName());

                final MovingObjectPosition mop = ClientProxy.movingObjectPosition;
                if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    final Block block = schematic.getBlock(mop.blockX, mop.blockY, mop.blockZ);
                    final int metadata = schematic.getBlockMetadata(mop.blockX, mop.blockY, mop.blockZ);

                    event.right.add("");
                    event.right.add(BLOCK_REGISTRY.getNameForObject(block) + " : " + metadata + " [§6S§r]");
                }
            }
        }
    }

    /**
     * Renders a Tool HUD in the bottom-left corner when the player is holding the tool item.
     * Shows: current mode, schematic file name + dimensions, selection coords, primary/secondary block.
     * Automatically adjusts position upward to avoid overlapping the hotbar.
     */
    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }

        if (this.minecraft.currentScreen != null || !ToolManager.isHoldingToolItem()) {
            return;
        }

        final FontRenderer fr = this.minecraft.fontRenderer;
        final ScaledResolution sr = new ScaledResolution(
            this.minecraft, this.minecraft.displayWidth, this.minecraft.displayHeight);

        final ToolMode mode = ToolManager.getCurrentMode();
        final int lineHeight = fr.FONT_HEIGHT + 2;
        final int x = 4;

        // Build lines
        java.util.List<String> lines = new java.util.ArrayList<>();

        // Line 1: Mode
        lines.add(EnumChatFormatting.GOLD + "[" + Reference.NAME + "] " + EnumChatFormatting.WHITE + mode.getDisplayName());

        // Line 2: Schematic file name + dimensions (if loaded)
        final SchematicWorld schematic = ClientProxy.schematic;
        if (schematic != null) {
            String fileName = schematic.sourceFilename != null ? schematic.sourceFilename : schematic.name;
            lines.add(EnumChatFormatting.GRAY + UiTranslations.format("litematica.gui.label.schematic_placement.schematic_file", EnumChatFormatting.YELLOW + fileName)
                + EnumChatFormatting.GRAY + " (" + schematic.getWidth() + "x" + schematic.getHeight() + "x" + schematic.getLength() + ")"
                + EnumChatFormatting.GRAY + " @ " + EnumChatFormatting.WHITE
                + schematic.position.x + ", " + schematic.position.y + ", " + schematic.position.z);
            if (schematic.subregions() != null && schematic.subregions().selected != null && !mode.getUsesAreaSelection()) {
                String name = schematic.subregions().selected;
                com.github.lunatrius.schematica.api.SchematicOrigin origin = schematic.subregionPosition(name);
                lines.add(EnumChatFormatting.AQUA + UiTranslations.format("litematica.hud.schematic_placement.selected_sub_region") + ": " + name);
                lines.add(UiTranslations.format("litematica.hud.schematic_placement.sub_region_origin", origin.x + ", " + origin.y + ", " + origin.z));
            }
        }

        // Line 3: Selection coords (for area-related modes)
        if (mode.getUsesAreaSelection() || mode == ToolMode.FILL || mode == ToolMode.REPLACE_BLOCK || mode == ToolMode.DELETE) {
            Area area = AreaSelections.library().selected();
            if (area == null) {
                lines.add(UiTranslations.format("litematica.message.error.no_area_selected"));
            } else {
                lines.add(UiTranslations.format(AreaSelections.library().mode() == com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Mode.SIMPLE
                    ? "litematica.hud.area_selection.selected_area_simple" : "litematica.hud.area_selection.selected_area_normal", area.name()));
                lines.add(UiTranslations.format("litematica.hud.area_selection.selection_corners_mode",
                    UiTranslations.format(AreaSelections.cornerModeKey())));
                Vector3i origin = area.origin();
                lines.add((area.originSelected() ? EnumChatFormatting.AQUA : EnumChatFormatting.GRAY)
                    + UiTranslations.format("litematica.hud.area_selection.origin", origin.x + ", " + origin.y + ", " + origin.z));
                if (area.selectedBox() == null && !area.originSelected()) {
                    lines.add(UiTranslations.format("litematica.error.area_selection.grow.no_sub_region_selected"));
                } else if (area.selectedBox() != null) {
                    lines.add(UiTranslations.format("litematica.hud.area_selection.selected_sub_region", area.boxName()));
                    Vector3i a = ClientProxy.pointA;
                    Vector3i b = ClientProxy.pointB;
                    String first = (area.selectedCorner() == com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Corner.FIRST
                        ? EnumChatFormatting.YELLOW : EnumChatFormatting.AQUA) + "" + a.x + ", " + a.y + ", " + a.z;
                    String second = (area.selectedCorner() == com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Corner.SECOND
                        ? EnumChatFormatting.YELLOW : EnumChatFormatting.AQUA) + "" + b.x + ", " + b.y + ", " + b.z;
                    String dimensions = (Math.abs(a.x - b.x) + 1) + " x " + (Math.abs(a.y - b.y) + 1) + " x " + (Math.abs(a.z - b.z) + 1);
                    lines.add(UiTranslations.format("litematica.hud.area_selection.dimensions_position", dimensions, first, second));
                }
            }
        }

        // Line 4: Primary/Secondary block (for FILL/REPLACE)
        if (mode.getUsesBlockPrimary()) {
            String primaryName = mode.getPrimaryBlockName();
            String blockText = UiTranslations.format("litematica.tool_hud.block_1", EnumChatFormatting.GREEN
                + (primaryName == null ? UiTranslations.format("litematica.hud.misc.none_brackets") : primaryName));
            if (mode.getUsesBlockSecondary()) {
                String secondaryName = mode.getSecondaryBlockName();
                blockText += "  " + UiTranslations.format("litematica.tool_hud.block_2", EnumChatFormatting.RED
                    + (secondaryName == null ? UiTranslations.format("litematica.hud.misc.none_brackets") : secondaryName));
            }
            lines.add(blockText);
        }

        // Calculate max width of HUD text at normal scale
        int maxWidth = 0;
        for (String line : lines) {
            maxWidth = Math.max(maxWidth, fr.getStringWidth(line));
        }

        // Hotbar is 182px wide, centered. Its left edge = screenWidth/2 - 91.
        // HUD starts at x=4. Available width before overlapping hotbar:
        int hotbarLeft = sr.getScaledWidth() / 2 - 91;
        int availableWidth = hotbarLeft - x - 4; // 4px gap between HUD and hotbar

        // Calculate scale factor: shrink HUD if it's wider than available space
        // Only scale down in the bottom region where hotbar lives
        float scale = 1.0f;
        if (maxWidth > availableWidth && availableWidth > 20) {
            scale = (float) availableWidth / (float) maxWidth;
            if (scale < 0.5f) scale = 0.5f; // don't shrink below 50%
        }

        int scaledLineHeight = (int)(lineHeight * scale);
        int totalHeight = lines.size() * scaledLineHeight;

        // Y position: bottom-left, original position
        int startY = sr.getScaledHeight() - totalHeight - 4;
        if (startY < 2) startY = 2;

        // Apply scale via GL
        GL11.glPushMatrix();
        GL11.glTranslatef(x, startY, 0);
        GL11.glScalef(scale, scale, 1.0f);

        // Background (in scaled coordinates, origin is 0,0)
        int bgWidth = maxWidth + 6;
        int bgHeight = lines.size() * lineHeight + 4;
        net.minecraft.client.gui.Gui.drawRect(
            -2, -2,
            bgWidth, bgHeight,
            0x80000000);

        // Render lines (in scaled coordinates)
        int curY = 0;
        for (String line : lines) {
            fr.drawStringWithShadow(line, 0, curY, 0xFFFFFF);
            curY += lineHeight;
        }

        GL11.glPopMatrix();
    }
}

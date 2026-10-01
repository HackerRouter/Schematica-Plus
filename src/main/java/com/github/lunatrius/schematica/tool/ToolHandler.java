
package com.github.lunatrius.schematica.tool;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.client.selection.AreaSelections;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.util.MessageException;

public class ToolHandler {

    private ToolHandler() {}

    /** Litematica's executeOperation: returns whether the key was used. A second press does not cancel a running edit. */
    public static boolean onExecute(EntityPlayer player) {
        if (player == null) return false;
        ToolMode mode = ToolManager.getCurrentMode();
        if (mode != ToolMode.PASTE_SCHEMATIC && mode != ToolMode.DELETE
            && !(mode == ToolMode.FILL && mode.getPrimaryBlock() != null)
            && !(mode == ToolMode.REPLACE_BLOCK && mode.getPrimaryBlock() != null && mode.getSecondaryBlock() != null)) return false;
        try {
            if (!player.capabilities.isCreativeMode) throw new MessageException("litematica.error.generic.creative_mode_only");
            queueEdit(player, mode);
        } catch (Exception e) {
            if (!(e instanceof MessageException)) Reference.logger.warn("Could not start schematic edit", e);
            sendChat(player, EnumChatFormatting.RED + (e instanceof MessageException
                ? UiTranslations.format(((MessageException) e).key(), ((MessageException) e).arguments())
                : UiTranslations.format("schematica.message.edit.start_failed")));
        }
        return true;
    }

    /** World boxes of an operation: the selected box only, else every box (AreaSelection.getSelectedSubRegionBox). */
    private static java.util.List<com.github.lunatrius.schematica.api.SchematicRegion> targetBoxes(ToolMode mode) {
        java.util.List<com.github.lunatrius.schematica.api.SchematicRegion> boxes = new java.util.ArrayList<>();
        if (mode == ToolMode.DELETE && ToolMode.deleteUsesPlacement) {
            SchematicWorld placement = ClientProxy.schematic;
            if (placement == null) throw new MessageException("litematica.message.error.no_area_selected");
            if (placement.subregions() != null) for (com.github.lunatrius.schematica.client.world.SubRegionPlacements.Region region : placement.subregions().regions()) {
                if (region.enabled) boxes.add(placement.subregionBounds(region.name()));
            }
            if (boxes.isEmpty()) throw new MessageException("litematica.message.error.empty_area_selection");
            return boxes;
        }
        com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Area area = AreaSelections.library().selected();
        if (area == null) throw new MessageException("litematica.message.error.no_area_selected");
        AreaSelections.capture();
        java.util.List<com.github.lunatrius.schematica.api.SchematicRegion> regions = area.regions();
        if (regions.isEmpty()) throw new MessageException("litematica.message.error.empty_area_selection");
        String selected = area.selectedBox() == null ? null : area.boxName();
        if (selected != null) for (com.github.lunatrius.schematica.api.SchematicRegion region : regions) if (region.name.equals(selected)) boxes.add(region);
        return boxes.isEmpty() ? regions : boxes;
    }

    private static void queueEdit(EntityPlayer player, ToolMode mode) {
        WorldEditJob job;
        if (mode == ToolMode.PASTE_SCHEMATIC) {
            SchematicWorld schematic = ClientProxy.schematic;
            if (schematic == null) throw new MessageException("litematica.message.error.no_placement_selected");
            if (!schematic.isEnabled()) throw new MessageException("schematica.ui.placement.disabled");
            if (!schematic.hasEnabledRegions()) throw new MessageException("schematica.ui.placement.no_regions");
            job = new WorldEditJob(player.getUniqueID(), player.dimension, WorldEditJob.Kind.PASTE,
                schematic.position.x, schematic.position.y, schematic.position.z,
                schematic.getWidth(), schematic.getHeight(), schematic.getLength(), null, 0, null, 0,
                com.github.lunatrius.schematica.handler.ConfigurationHandler.pasteWithoutUpdates,
                com.github.lunatrius.schematica.handler.ConfigurationHandler.pasteOnlyAir);
            job.capture(schematic.getSchematic(), schematic.isPastingBlockNBT, schematic.isRenderingEntities);
        } else {
            com.github.lunatrius.schematica.world.storage.RegionSelection selection =
                new com.github.lunatrius.schematica.world.storage.RegionSelection(targetBoxes(mode));
            Vector3i min = new Vector3i(selection.minX, selection.minY, selection.minZ);
            Vector3i max = new Vector3i(selection.maxX, selection.maxY, selection.maxZ);
            Block replacement = mode == ToolMode.DELETE ? Blocks.air : mode.getPrimaryBlock();
            Block target = mode.getSecondaryBlock();
            job = new WorldEditJob(player.getUniqueID(), player.dimension,
                mode == ToolMode.REPLACE_BLOCK ? WorldEditJob.Kind.REPLACE : WorldEditJob.Kind.FILL,
                min.x, min.y, min.z,
                com.github.lunatrius.schematica.util.SchematicLimits.dimension(min.x, max.x),
                com.github.lunatrius.schematica.util.SchematicLimits.dimension(min.y, max.y),
                com.github.lunatrius.schematica.util.SchematicLimits.dimension(min.z, max.z),
                replacement, mode == ToolMode.DELETE ? 0 : mode.getPrimaryMeta(), target, mode.getSecondaryMeta());
            job.setRegions(selection.localRegions);
        }
        MinecraftServer server = Minecraft.getMinecraft().getIntegratedServer();
        if (server != null) {
            if (!com.github.lunatrius.schematica.handler.WorldEditQueue.INSTANCE.submit(server, job)) {
                throw new MessageException("schematica.message.edit.busy");
            }
            sendChat(player, UiTranslations.format("litematica.message.scheduled_task_added"));
        } else {
            com.github.lunatrius.schematica.handler.client.CommandEditQueue.INSTANCE.submit(job, player.worldObj);
            sendChat(player, UiTranslations.format("schematica.message.edit.commands_queued"));
        }
    }

    private static void sendChat(EntityPlayer player, String message) {
        player.addChatMessage(new ChatComponentText(
            EnumChatFormatting.GREEN + "[" + Reference.NAME + "] " + EnumChatFormatting.RESET + message));
    }
}

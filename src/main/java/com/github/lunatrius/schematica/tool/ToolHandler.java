
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
import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.client.selection.AreaSelections;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.task.TaskRegistry;
import com.github.lunatrius.schematica.util.MessageException;

public class ToolHandler {

    private ToolHandler() {}

    /** Litematica's executeOperation: returns whether the key was used. A second press does not cancel a running edit. */
    public static boolean onExecute(EntityPlayer player) {
        if (player == null) return false;
        if (com.github.lunatrius.schematica.client.projects.SchematicProjects.hasProjectOpen()) {
            com.github.lunatrius.schematica.client.projects.SchematicProjects.pasteCurrentVersionToWorld();
            return true;
        }
        ToolMode mode = ToolManager.getCurrentMode();
        if (mode != ToolMode.PASTE_SCHEMATIC && mode != ToolMode.GRID_PASTE && mode != ToolMode.DELETE
            && !(mode == ToolMode.FILL && mode.getPrimaryBlock() != null)
            && !(mode == ToolMode.REPLACE_BLOCK && mode.getPrimaryBlock() != null && mode.getSecondaryBlock() != null)) return false;
        request(player, mode == ToolMode.PASTE_SCHEMATIC || mode == ToolMode.GRID_PASTE ? TaskRegistry.Kind.PASTE
            : mode == ToolMode.DELETE ? TaskRegistry.Kind.DELETE : TaskRegistry.Kind.FILL, p -> queueEdit(p, mode));
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
        if (mode == ToolMode.PASTE_SCHEMATIC) {
            submit(player, pasteJob(player, ClientProxy.schematic), null);
            return;
        }
        if (mode == ToolMode.GRID_PASTE) {
            gridPaste(player);
            return;
        }
        Block replacement = mode == ToolMode.DELETE ? Blocks.air : mode.getPrimaryBlock();
        java.util.List<com.github.lunatrius.schematica.api.SchematicRegion> boxes = targetBoxes(mode);
        WorldEditJob job = areaJob(player, mode == ToolMode.REPLACE_BLOCK ? WorldEditJob.Kind.REPLACE : WorldEditJob.Kind.FILL, boxes,
            replacement, mode == ToolMode.DELETE ? 0 : mode.getPrimaryMeta(), mode.getSecondaryBlock(), mode.getSecondaryMeta());
        if (mode == ToolMode.DELETE) job.removeEntitiesIn(boxes);
        submit(player, job, null);
    }

    /**
     * SchematicPlacingUtils.gridPasteCurrentPlacementToWorld: the selected placement and its grid copies within the
     * loaded area, one after another; without an enabled grid only the placement is pasted, with a note.
     */
    private static void gridPaste(EntityPlayer player) {
        SchematicWorld base = ClientProxy.schematic;
        if (base == null) throw new MessageException("litematica.message.error.no_placement_selected");
        java.util.List<SchematicWorld> placements = new java.util.ArrayList<>();
        placements.add(base);
        if (base.grid.isEnabled()) {
            placements.addAll(com.github.lunatrius.schematica.client.world.GridPlacements.INSTANCE.copiesOf(base));
        } else {
            sendChat(player, EnumChatFormatting.GOLD + UiTranslations.format("schematica.message.grid_paste.grid_disabled"));
        }
        java.util.List<WorldEditJob> jobs = new java.util.ArrayList<>();
        for (SchematicWorld placement : placements) jobs.add(pasteJob(player, placement));
        submitAll(player, jobs, 0);
    }

    /** Submits the jobs in order, each after the previous one finished successfully. */
    private static void submitAll(EntityPlayer player, java.util.List<WorldEditJob> jobs, int index) {
        if (index >= jobs.size()) return;
        submit(player, jobs.get(index), index + 1 < jobs.size() ? () -> {
            EntityPlayer current = Minecraft.getMinecraft().thePlayer;
            if (current != null) request(current, TaskRegistry.Kind.PASTE, p -> submitAll(p, jobs, index + 1));
        } : null);
    }

    private static WorldEditJob pasteJob(EntityPlayer player, SchematicWorld schematic) {
        if (schematic == null) throw new MessageException("litematica.message.error.no_placement_selected");
        if (!schematic.isEnabled()) throw new MessageException("litematica.message.error.placement_paste_rendering_disabled");
        if (!schematic.hasEnabledRegions()) throw new MessageException("schematica.ui.placement.no_regions");
        WorldEditJob job = new WorldEditJob(player.getUniqueID(), player.dimension, WorldEditJob.Kind.PASTE,
            schematic.position.x, schematic.position.y, schematic.position.z,
            schematic.getWidth(), schematic.getHeight(), schematic.getLength(), null, 0, null, 0,
            com.github.lunatrius.schematica.handler.ConfigurationHandler.pasteWithoutUpdates,
            com.github.lunatrius.schematica.handler.ConfigurationHandler.pasteReplaceBehavior);
        job.capture(schematic.getSchematic(), schematic.isPastingBlockNBT, schematic.isRenderingEntities,
            com.github.lunatrius.schematica.handler.ConfigurationHandler.pasteRenderLayersOnly ? schematic.renderBounds() : null,
            com.github.lunatrius.schematica.handler.ConfigurationHandler.pasteIgnoreInventories);
        return job;
    }

    private static WorldEditJob areaJob(EntityPlayer player, WorldEditJob.Kind kind, java.util.List<com.github.lunatrius.schematica.api.SchematicRegion> boxes,
        Block replacement, int replacementMeta, Block target, int targetMeta) {
        if (boxes.isEmpty()) throw new MessageException("litematica.message.error.empty_area_selection");
        com.github.lunatrius.schematica.world.storage.RegionSelection selection = new com.github.lunatrius.schematica.world.storage.RegionSelection(boxes);
        WorldEditJob job = new WorldEditJob(player.getUniqueID(), player.dimension, kind, selection.minX, selection.minY, selection.minZ,
            com.github.lunatrius.schematica.util.SchematicLimits.dimension(selection.minX, selection.maxX),
            com.github.lunatrius.schematica.util.SchematicLimits.dimension(selection.minY, selection.maxY),
            com.github.lunatrius.schematica.util.SchematicLimits.dimension(selection.minZ, selection.maxZ),
            replacement, replacementMeta, target, targetMeta);
        job.setRegions(selection.localRegions);
        return job;
    }

    /**
     * Queues the edit on the integrated server, uploads it to a Plus server with full NBT, or sends it as commands
     * to any other server; onSuccess runs on the client thread.
     */
    private static void submit(EntityPlayer player, WorldEditJob job, Runnable onSuccess) {
        MinecraftServer server = Minecraft.getMinecraft().getIntegratedServer();
        if (server != null) {
            if (onSuccess != null) job.completion = success -> { if (success) Minecraft.getMinecraft().func_152344_a(onSuccess); };
            if (!com.github.lunatrius.schematica.handler.WorldEditQueue.INSTANCE.submit(server, job)) {
                throw new MessageException("schematica.message.edit.busy");
            }
        } else {
            if (com.github.lunatrius.schematica.handler.client.RemoteEditClient.INSTANCE.busy()
                || com.github.lunatrius.schematica.handler.client.CommandEditQueue.INSTANCE.busy()) {
                throw new MessageException("schematica.message.edit.busy");
            }
            if (SchematicaPlus.proxy.supportsRemoteEdit) {
                com.github.lunatrius.schematica.handler.client.RemoteEditClient.INSTANCE.submit(job, player.worldObj, onSuccess);
            } else {
                if (onSuccess != null) job.completion = success -> { if (success) Minecraft.getMinecraft().func_152344_a(onSuccess); };
                com.github.lunatrius.schematica.handler.client.CommandEditQueue.INSTANCE.submit(job, player.worldObj);
            }
        }
        sendChat(player, UiTranslations.format("litematica.message.scheduled_task_added"));
    }

    interface Edit { void run(EntityPlayer player); }

    /** Runs a creative-only edit request, printing the failure like executeOperation; returns whether it was queued. */
    private static boolean request(EntityPlayer player, TaskRegistry.Kind kind, Edit edit) {
        if (player == null) return false;
        try {
            if (!player.capabilities.isCreativeMode) throw new MessageException("litematica.error.generic.creative_mode_only");
            edit.run(player);
            return true;
        } catch (Exception e) {
            if (!(e instanceof MessageException)) Reference.logger.warn("Could not start schematic edit", e);
            sendChat(player, EnumChatFormatting.RED + (e instanceof MessageException
                ? UiTranslations.format(((MessageException) e).key(), ((MessageException) e).arguments())
                : UiTranslations.format(kind.finishedKey(false))));
            return false;
        }
    }

    /** PositionUtils.getClampedBox: the parts of the boxes inside the render layer range and world height. */
    static java.util.List<com.github.lunatrius.schematica.api.SchematicRegion> clampToLayers(java.util.List<com.github.lunatrius.schematica.api.SchematicRegion> boxes) {
        com.github.lunatrius.schematica.client.world.RenderLayerRange range = com.github.lunatrius.schematica.client.world.RenderLayerSettings.RANGE;
        java.util.List<com.github.lunatrius.schematica.api.SchematicRegion> clamped = new java.util.ArrayList<>();
        for (com.github.lunatrius.schematica.api.SchematicRegion box : boxes) {
            long[] min = {box.minX, Math.max(0, box.minY), box.minZ}, max = {box.maxX, Math.min(255, box.maxY), box.maxZ};
            int axis = range.axis().ordinal();
            min[axis] = Math.max(min[axis], range.minimum());
            max[axis] = Math.min(max[axis], range.maximum());
            if (min[0] > max[0] || min[1] > max[1] || min[2] > max[2]) continue;
            clamped.add(new com.github.lunatrius.schematica.api.SchematicRegion(box.name, (int) min[0], (int) min[1], (int) min[2], (int) max[0], (int) max[1], (int) max[2]));
        }
        return clamped;
    }

    /** Pastes a placement, as pastePlacementToWorld. */
    public static boolean paste(EntityPlayer player, SchematicWorld placement, Runnable onSuccess) {
        return request(player, TaskRegistry.Kind.PASTE, p -> submit(p, pasteJob(p, placement), onSuccess));
    }

    /** Clears whole boxes, as deleteSelectionVolumes. */
    public static boolean deleteBoxes(EntityPlayer player, java.util.List<com.github.lunatrius.schematica.api.SchematicRegion> boxes, Runnable onSuccess) {
        return request(player, TaskRegistry.Kind.DELETE,
            p -> {
                WorldEditJob job = areaJob(p, WorldEditJob.Kind.FILL, boxes, Blocks.air, 0, null, 0);
                job.removeEntitiesIn(boxes);
                submit(p, job, onSuccess);
            });
    }

    /** Removes world blocks inside a placement by mode, within the render layer range (deleteBlocksByPlacement). */
    public static boolean deleteByPlacement(EntityPlayer player, SchematicWorld placement, PlacementDeletionMode mode) {
        return request(player, TaskRegistry.Kind.DELETE, p -> {
            if (placement == null) throw new MessageException("litematica.message.error.no_placement_selected");
            WorldEditJob job = new WorldEditJob(p.getUniqueID(), p.dimension, WorldEditJob.Kind.DELETE_PLACEMENT,
                placement.position.x, placement.position.y, placement.position.z,
                placement.getWidth(), placement.getHeight(), placement.getLength(), Blocks.air, 0, null, 0);
            job.capture(placement.getSchematic(), false, false);
            job.deletion = mode;
            job.restrict((x, y, z) -> y >= 0 && y < 256 && com.github.lunatrius.schematica.client.world.RenderLayerSettings.RANGE.contains(x, y, z));
            job.removeEntitiesIn(clampToLayers(placement.enabledRegionBounds()));
            submit(p, job, null);
        });
    }

    private static void sendChat(EntityPlayer player, String message) {
        player.addChatMessage(new ChatComponentText(
            EnumChatFormatting.GREEN + "[" + Reference.NAME + "] " + EnumChatFormatting.RESET + message));
    }
}

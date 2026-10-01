
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

    public static void onExecute(EntityPlayer player) {
        if (player == null) return;
        ToolMode mode = ToolManager.getCurrentMode();
        if (mode != ToolMode.PASTE_SCHEMATIC && mode != ToolMode.DELETE
            && mode != ToolMode.FILL && mode != ToolMode.REPLACE_BLOCK) return;
        try {
            if (com.github.lunatrius.schematica.handler.WorldEditQueue.INSTANCE.cancel(player.getUniqueID())
                || com.github.lunatrius.schematica.handler.client.CommandEditQueue.INSTANCE.cancel()) {
                sendChat(player, UiTranslations.format("schematica.message.edit.cancel_requested"));
                return;
            }
            if (!player.capabilities.isCreativeMode) throw new MessageException("litematica.error.generic.creative_mode_only");
            queueEdit(player, mode);
        } catch (Exception e) {
            Reference.logger.warn("Could not start schematic edit", e);
            sendChat(player, EnumChatFormatting.RED + (e instanceof MessageException
                ? UiTranslations.format(((MessageException) e).key(), ((MessageException) e).arguments())
                : UiTranslations.format("schematica.message.edit.start_failed")));
        }
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
            if (AreaSelections.library().selected() == null) {
                throw new MessageException("litematica.message.error.no_area_selected");
            }
            AreaSelections.capture();
            com.github.lunatrius.schematica.world.storage.RegionSelection selection =
                new com.github.lunatrius.schematica.world.storage.RegionSelection(AreaSelections.library().selected().regions());
            Vector3i min = new Vector3i(selection.minX, selection.minY, selection.minZ);
            Vector3i max = new Vector3i(selection.maxX, selection.maxY, selection.maxZ);
            Block replacement = mode == ToolMode.DELETE ? Blocks.air : mode.getPrimaryBlock();
            if (replacement == null) throw new MessageException("schematica.message.tool.pick_target");
            Block target = mode.getSecondaryBlock();
            if (mode == ToolMode.REPLACE_BLOCK && target == null) {
                throw new MessageException("schematica.message.tool.pick_target");
            }
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
            sendChat(player, UiTranslations.format("schematica.message.edit.queued"));
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

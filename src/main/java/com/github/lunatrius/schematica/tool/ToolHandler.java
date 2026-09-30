
package com.github.lunatrius.schematica.tool;


import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;
import com.github.lunatrius.schematica.client.gui.load.GuiSchematicLoad;
import com.github.lunatrius.schematica.client.gui.save.GuiSchematicSave;
import com.github.lunatrius.schematica.client.renderer.RendererSchematicGlobal;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.client.selection.AreaSelections;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.reference.Reference;

import cpw.mods.fml.common.registry.GameData;

/**
 * Handles tool actions for each ToolMode. Executes the actual operations
 * when the player uses the tool (right-click / left-click with tool item).
 * <p>
 * Ported from Litematica's tool action system, adapted for Schematica 1.7.10.
 *
 * @author HackerRouter (ported from Litematica by masa)
 */
public class ToolHandler {

    private ToolHandler() {}

    /**
     * Called on right-click (use action) with the tool active.
     * PASTE/DELETE/FILL/REPLACE are NOT handled here — they use onExecute() via Enter key.
     * FILL/REPLACE right-click picks secondaryBlock instead.
     */
    public static boolean onToolUse(EntityPlayer player, MovingObjectPosition mop) {
        ToolMode mode = ToolManager.getCurrentMode();

        // Modes that pick secondaryBlock on right-click
        if (mode == ToolMode.REPLACE_BLOCK) {
            return pickBlockFromCrosshair(player, mop, false);
        }

        // Modes with no right-click action (Enter-key only)
        if (mode == ToolMode.PASTE_SCHEMATIC || mode == ToolMode.DELETE || mode == ToolMode.FILL) {
            return false;
        }

        // Modes that don't require a block target
        switch (mode) {
            case SCHEMATIC_PLACEMENT:
                // PLACEMENT: if a schematic is already selected, import a new one;
                // otherwise do precise placement (target block or move to player)
                return handlePlacementUse(player, mop);
            case MOVE:
                // MOVE: precise placement — move current schematic to targeted block or to player
                return handleMoveUse(player, mop);
            default:
                break;
        }

        // All remaining modes require a valid block hit
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return false;
        }

        switch (mode) {
            case AREA_SELECTION:
                return handleAreaSelectionUse(player, mop);
            default:
                return false;
        }
    }

    /**
     * Called on left-click (attack action) with the tool active.
     * FILL/REPLACE left-click picks primaryBlock.
     */
    public static boolean onToolAttack(EntityPlayer player, MovingObjectPosition mop) {
        ToolMode mode = ToolManager.getCurrentMode();

        // Modes that pick primaryBlock on left-click
        if (mode == ToolMode.FILL || mode == ToolMode.REPLACE_BLOCK) {
            return pickBlockFromCrosshair(player, mop, true);
        }

        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return false;
        }

        if (mode == ToolMode.AREA_SELECTION) {
            return handleAreaSelectionAttack(player, mop);
        }

        return false;
    }

    /**
     * Called from the Execute keybinding (default: Enter).
     * Dispatches to the appropriate handler based on current tool mode.
     * Only works when holding the tool item.
     */
    public static void onExecute(EntityPlayer player) {
        if (player == null) return;
        ToolMode mode = ToolManager.getCurrentMode();
        if (mode == ToolMode.AREA_SELECTION) {
            handleAreaSelectionExecute(player);
            return;
        }
        if (mode != ToolMode.PASTE_SCHEMATIC && mode != ToolMode.DELETE
            && mode != ToolMode.FILL && mode != ToolMode.REPLACE_BLOCK) return;
        try {
            if (com.github.lunatrius.schematica.handler.WorldEditQueue.INSTANCE.cancel(player.getUniqueID())
                || com.github.lunatrius.schematica.handler.client.CommandEditQueue.INSTANCE.cancel()) {
                sendChat(player, "Edit cancellation requested. Changes already made are retained.");
                return;
            }
            if (!player.capabilities.isCreativeMode) throw new IllegalArgumentException("Editing requires creative mode.");
            queueEdit(player, mode);
        } catch (Exception e) {
            Reference.logger.warn("Could not start schematic edit", e);
            sendChat(player, EnumChatFormatting.RED + e.getMessage());
        }
    }
    // --- Block Picking (Litematica-style) ---

    /**
     * Picks the block at the crosshair and stores it as primaryBlock or secondaryBlock
     * on the current ToolMode. Works with both real world and schematic world blocks.
     *
     * @param primary true = set primaryBlock, false = set secondaryBlock
     */
    public static boolean pickBlockFromCrosshair(EntityPlayer player, MovingObjectPosition mop, boolean primary) {
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            sendChat(player, EnumChatFormatting.RED + "No block targeted.");
            return false;
        }

        ToolMode mode = ToolManager.getCurrentMode();
        World world = player.worldObj;
        Block block = world.getBlock(mop.blockX, mop.blockY, mop.blockZ);
        int meta = world.getBlockMetadata(mop.blockX, mop.blockY, mop.blockZ);

        if (block == null || block == Blocks.air) {
            sendChat(player, EnumChatFormatting.RED + "Cannot pick air.");
            return false;
        }

        String blockName = GameData.getBlockRegistry().getNameForObject(block);

        if (primary) {
            mode.setPrimaryBlock(block, meta);
        } else {
            mode.setSecondaryBlock(block, meta);
        }

        return true;
    }

    // --- AREA_SELECTION ---

    /**
     * Right-click sets point B (second corner of selection box).
     */
    private static boolean handleAreaSelectionUse(EntityPlayer player, MovingObjectPosition mop) {
        if (AreaSelections.library().selected() == null || AreaSelections.library().selected().selectedBox() == null) {
            sendChat(player, UiTranslations.format("litematica.message.error.no_area_selected"));
            return true;
        }
        ClientProxy.pointB.set(mop.blockX, mop.blockY, mop.blockZ);
        ClientProxy.updatePoints();
        ClientProxy.isRenderingGuide = true;
        AreaSelections.saveCurrent();
        return true;
    }

    /**
     * Left-click sets point A (first corner of selection box).
     */
    private static boolean handleAreaSelectionAttack(EntityPlayer player, MovingObjectPosition mop) {
        if (AreaSelections.library().selected() == null || AreaSelections.library().selected().selectedBox() == null) {
            sendChat(player, UiTranslations.format("litematica.message.error.no_area_selected"));
            return true;
        }
        ClientProxy.pointA.set(mop.blockX, mop.blockY, mop.blockZ);
        ClientProxy.updatePoints();
        ClientProxy.isRenderingGuide = true;
        AreaSelections.saveCurrent();
        return true;
    }

    /**
     * Execute in area selection mode: open the save schematic GUI.
     */
    private static void handleAreaSelectionExecute(EntityPlayer player) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.displayGuiScreen(new GuiSchematicSave(mc.currentScreen));
    }

    // --- SCHEMATIC_PLACEMENT ---

    /**
     * Right-click in placement mode:
     * - If a schematic is already selected/loaded, open load GUI to import a new one.
     * - If no schematic is loaded, do precise placement (target block or move to player).
     */
    private static boolean handlePlacementUse(EntityPlayer player, MovingObjectPosition mop) {
        SchematicWorld schematic = ClientProxy.schematic;
        if (schematic != null) {
            // Already have a selected instance — import a new schematic
            Minecraft.getMinecraft().displayGuiScreen(new GuiSchematicLoad(Minecraft.getMinecraft().currentScreen));
            return true;
        }
        // No schematic loaded — open load GUI to load the first one
        Minecraft.getMinecraft().displayGuiScreen(new GuiSchematicLoad(Minecraft.getMinecraft().currentScreen));
        return true;
    }

    // --- MOVE ---

    /**
     * Right-click in move mode: if targeting a block, place schematic origin on top of it;
     * otherwise move schematic to player position.
     */
    private static boolean handleMoveUse(EntityPlayer player, MovingObjectPosition mop) {
        SchematicWorld schematic = ClientProxy.schematic;
        if (schematic == null) {
            sendChat(player, EnumChatFormatting.RED + "No schematic loaded.");
            return false;
        }

        if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            int placeY = mop.blockY + 1;
            schematic.position.set(mop.blockX, placeY, mop.blockZ);
        } else {
            ClientProxy.moveSchematicToPlayer(schematic);
        }
        RendererSchematicGlobal.INSTANCE.refresh(schematic);
        return true;
    }

    private static void queueEdit(EntityPlayer player, ToolMode mode) {
        WorldEditJob job;
        if (mode == ToolMode.PASTE_SCHEMATIC) {
            SchematicWorld schematic = ClientProxy.schematic;
            if (schematic == null) throw new IllegalArgumentException("No schematic loaded.");
            job = new WorldEditJob(player.getUniqueID(), player.dimension, WorldEditJob.Kind.PASTE,
                schematic.position.x, schematic.position.y, schematic.position.z,
                schematic.getWidth(), schematic.getHeight(), schematic.getLength(), null, 0, null, 0,
                com.github.lunatrius.schematica.handler.ConfigurationHandler.pasteWithoutUpdates,
                com.github.lunatrius.schematica.handler.ConfigurationHandler.pasteOnlyAir);
            job.capture(schematic.getSchematic(), schematic.isPastingBlockNBT, schematic.isRenderingEntities);
        } else {
            if (AreaSelections.library().selected() == null) {
                throw new IllegalArgumentException(UiTranslations.format("litematica.message.error.no_area_selected"));
            }
            AreaSelections.capture();
            com.github.lunatrius.schematica.world.storage.RegionSelection selection =
                new com.github.lunatrius.schematica.world.storage.RegionSelection(AreaSelections.library().selected().regions());
            Vector3i min = new Vector3i(selection.minX, selection.minY, selection.minZ);
            Vector3i max = new Vector3i(selection.maxX, selection.maxY, selection.maxZ);
            Block replacement = mode == ToolMode.DELETE ? Blocks.air : mode.getPrimaryBlock();
            if (replacement == null) replacement = Blocks.air;
            Block target = mode.getSecondaryBlock();
            if (mode == ToolMode.REPLACE_BLOCK && target == null) {
                throw new IllegalArgumentException("Pick the target block before replacing.");
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
                throw new IllegalStateException("Another world edit is still running.");
            }
            sendChat(player, "Edit queued for the server. Press Execute again to cancel.");
        } else {
            com.github.lunatrius.schematica.handler.client.CommandEditQueue.INSTANCE.submit(job, player.worldObj);
            sendChat(player, "Command edit queued. Server permission is required. Press Execute again to cancel.");
        }
    }

    private static void sendChat(EntityPlayer player, String message) {
        player.addChatMessage(new ChatComponentText(
            EnumChatFormatting.GREEN + "[Schematica] " + EnumChatFormatting.RESET + message));
    }
}

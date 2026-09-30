
package com.github.lunatrius.schematica.tool;


import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
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
import com.github.lunatrius.schematica.util.MessageException;

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
    // --- Block Picking (Litematica-style) ---

    /**
     * Picks the block at the crosshair and stores it as primaryBlock or secondaryBlock
     * on the current ToolMode. Works with both real world and schematic world blocks.
     *
     * @param primary true = set primaryBlock, false = set secondaryBlock
     */
    public static boolean pickBlockFromCrosshair(EntityPlayer player, MovingObjectPosition mop, boolean primary) {
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            sendChat(player, EnumChatFormatting.RED + UiTranslations.format("schematica.message.tool.no_block"));
            return false;
        }

        ToolMode mode = ToolManager.getCurrentMode();
        World world = player.worldObj;
        Block block = world.getBlock(mop.blockX, mop.blockY, mop.blockZ);
        int meta = world.getBlockMetadata(mop.blockX, mop.blockY, mop.blockZ);

        if (block == null || block == Blocks.air) {
            sendChat(player, EnumChatFormatting.RED + UiTranslations.format("schematica.message.tool.air"));
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
        return setAreaPoint(player, mop, false);
    }

    private static boolean handleAreaSelectionAttack(EntityPlayer player, MovingObjectPosition mop) {
        return setAreaPoint(player, mop, true);
    }

    private static boolean setAreaPoint(EntityPlayer player, MovingObjectPosition mop, boolean first) {
        if (!com.github.lunatrius.schematica.SchematicaPlus.proxy.isSaveEnabled) return true;
        com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Area area = AreaSelections.library().selected();
        if (area == null || (!area.originSelected() && area.selectedBox() == null)) {
            sendChat(player, UiTranslations.format(area == null
                ? "litematica.message.error.no_area_selected" : "litematica.error.area_selection.grow.no_sub_region_selected"));
            return true;
        }
        AreaSelections.capture();
        Vector3i point = new Vector3i(mop.blockX, mop.blockY, mop.blockZ);
        if (player.isSneaking()) {
            net.minecraftforge.common.util.ForgeDirection side = net.minecraftforge.common.util.ForgeDirection.getOrientation(mop.sideHit);
            point.add(side.offsetX, side.offsetY, side.offsetZ);
        }
        try { AreaSelections.library().click(area, first, point); }
        catch (IllegalArgumentException | ArithmeticException error) {
            sendChat(player, UiTranslations.format("schematica.ui.area.invalid"));
            return true;
        }
        AreaSelections.library().setGuide(area, true);
        AreaSelections.apply();
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
            sendChat(player, EnumChatFormatting.RED + UiTranslations.format("litematica.message.error.no_placement_selected"));
            return false;
        }

        if (schematic.placementSettings().locked) {
            sendChat(player, UiTranslations.format(com.github.lunatrius.schematica.client.world.PlacementSettings.LOCKED_MESSAGE));
            return true;
        }
        if (schematic.subregions() != null && schematic.subregions().selected != null) {
            try {
                int x = mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK ? mop.blockX : MathHelper.floor_double(player.posX);
                int y = mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK ? mop.blockY + 1 : MathHelper.floor_double(player.boundingBox.minY);
                int z = mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK ? mop.blockZ : MathHelper.floor_double(player.posZ);
                schematic.moveSubregionTo(schematic.subregions().selected, x, y, z);
                RendererSchematicGlobal.INSTANCE.createRendererSchematicChunks(schematic);
                com.github.lunatrius.schematica.client.printer.SchematicPrinter.INSTANCE.refresh();
                com.github.lunatrius.schematica.handler.client.WorldHandler.INSTANCE.saveSession();
            } catch (RuntimeException e) {
                com.github.lunatrius.schematica.reference.Reference.logger.warn("Failed to move selected subregion", e);
                sendChat(player, UiTranslations.format("schematica.ui.placement.region_failed"));
            }
            return true;
        }
        if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            int placeY = mop.blockY + 1;
            schematic.moveOriginTo(mop.blockX, placeY, mop.blockZ);
        } else {
            ClientProxy.moveSchematicToPlayer(schematic);
        }
        RendererSchematicGlobal.INSTANCE.refresh(schematic);
        com.github.lunatrius.schematica.handler.client.WorldHandler.INSTANCE.saveSession();
        return true;
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
            if (replacement == null) replacement = Blocks.air;
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

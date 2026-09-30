package com.github.lunatrius.schematica.tool;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary;
import com.github.lunatrius.schematica.client.selection.AreaSelections;
import com.github.lunatrius.schematica.client.selection.SelectionRayTrace;

/**
 * Central state holder for the Litematica-style tool mode system.
 * Tracks the current active tool mode and provides mode-switching logic.
 *
 * @author HackerRouter (ported from Litematica by masa)
 */
public class ToolManager {

    public static final ToolManager INSTANCE = new ToolManager();

    private static ToolMode currentMode = ToolMode.SCHEMATIC_PLACEMENT;

    private ToolManager() {}

    /**
     * Checks if the player is currently holding the configured tool item in their main hand.
     * Reads the cached tool item type/meta from ConfigurationHandler (server-safe).
     */
    public static boolean isHoldingToolItem() {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null || ConfigurationHandler.toolItemType == null) {
            return false;
        }
        ItemStack held = player.getHeldItem();
        if (held == null) {
            return false;
        }
        if (held.getItem() != ConfigurationHandler.toolItemType) {
            return false;
        }
        if (ConfigurationHandler.toolItemMeta >= 0 && held.getItemDamage() != ConfigurationHandler.toolItemMeta) {
            return false;
        }
        return true;
    }

    public static ToolMode getCurrentMode() {
        return currentMode;
    }

    public static void setCurrentMode(ToolMode mode) {
        currentMode = mode;
        Reference.logger.debug("Tool mode changed to: {}", mode.name());
    }

    /**
     * Cycles the tool mode forward, skipping creative-only modes
     * when the player is in survival. Sends a chat notification.
     */
    public void cycleMode() {
        cycleMode(true);
    }

    /**
     * Cycles the tool mode forward or backward, skipping creative-only modes
     * when the player is in survival. Sends a chat notification.
     */
    public static void cycleMode(boolean forward) {
        currentMode = currentMode.cycle(forward);
        Reference.logger.debug("Tool mode cycled to: {}", currentMode.name());
    }

    /** Maximum raycast distance for SCHEMATIC_PLACEMENT mode (effectively unlimited). */
    private static final double PLACEMENT_RAYCAST_DISTANCE = 256.0;

    /**
     * Convenience method called from InputHandler when the tool use key is pressed.
     * Delegates to ToolHandler with the current moving object position.
     */
    public void onToolUse(EntityPlayer player, SchematicWorld schematic) {
        MovingObjectPosition mop;

        if (currentMode == ToolMode.SCHEMATIC_PLACEMENT || currentMode == ToolMode.MOVE || currentMode.getUsesAreaSelection()) {
            // Long-range raycast against real world with no practical distance limit
            mop = longRangeRayTrace(player, PLACEMENT_RAYCAST_DISTANCE);
        } else {
            // For other schematic-based modes, prefer the schematic raycast, fall back to vanilla
            mop = ClientProxy.movingObjectPosition;
            if (mop == null) {
                mop = longRangeRayTrace(player, PLACEMENT_RAYCAST_DISTANCE);
            }
        }

        ToolHandler.onToolUse(player, mop);
    }

    /**
     * Performs a raycast from the player's eyes in the look direction up to the given distance.
     * Returns null if no block is hit within range.
     */
    private static MovingObjectPosition longRangeRayTrace(EntityPlayer player, double distance) {
        net.minecraft.entity.EntityLivingBase camera = camera(player);
        Vec3 eyePos = camera.getPosition(1);
        Vec3 lookVec = camera.getLook(1);
        Vec3 endPos = Vec3.createVectorHelper(
            eyePos.xCoord + lookVec.xCoord * distance,
            eyePos.yCoord + lookVec.yCoord * distance,
            eyePos.zCoord + lookVec.zCoord * distance
        );
        MovingObjectPosition result = player.worldObj.rayTraceBlocks(eyePos, endPos);
        if (result != null && result.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            return result;
        }
        return null;
    }

    private static net.minecraft.entity.EntityLivingBase camera(EntityPlayer player) {
        net.minecraft.entity.EntityLivingBase camera = Minecraft.getMinecraft().renderViewEntity;
        return camera == null ? player : camera;
    }

    public static void selectAreaElement(EntityPlayer player) {
        if (!com.github.lunatrius.schematica.SchematicaPlus.proxy.isSaveEnabled) return;
        AreaSelectionLibrary library = AreaSelections.library();
        AreaSelectionLibrary.Area area = library.selected();
        if (area == null || !area.guide()) return;
        AreaSelections.capture();
        Vec3 eyes = camera(player).getPosition(1), look = camera(player).getLook(1);
        MovingObjectPosition block = longRangeRayTrace(player, PLACEMENT_RAYCAST_DISTANCE);
        double distance = block == null ? PLACEMENT_RAYCAST_DISTANCE : eyes.distanceTo(block.hitVec) + 0.001;
        SelectionRayTrace.Hit hit = SelectionRayTrace.trace(area, eyes.xCoord, eyes.yCoord, eyes.zCoord,
            look.xCoord, look.yCoord, look.zCoord, distance);
        if (hit != null && hit.box == null) library.selectOrigin(area, true);
        else if (hit != null) library.selectCorner(area, hit.box, hit.corner);
        else if (block == null) library.selectBox(area, null);
        AreaSelections.apply();
        AreaSelections.saveCurrent();
    }

    public static void nudgeArea(EntityPlayer player, int amount) {
        if (!com.github.lunatrius.schematica.SchematicaPlus.proxy.isSaveEnabled) return;
        AreaSelectionLibrary library = AreaSelections.library();
        AreaSelectionLibrary.Area area = library.selected();
        if (area == null || (!area.originSelected() && area.selectedBox() == null)) return;
        Vec3 look = camera(player).getLook(1);
        double x = Math.abs(look.xCoord), y = Math.abs(look.yCoord), z = Math.abs(look.zCoord);
        try {
            AreaSelections.capture();
            if (y >= x && y >= z) library.moveSelected(area, 0, look.yCoord < 0 ? -amount : amount, 0);
            else if (x >= z) library.moveSelected(area, look.xCoord < 0 ? -amount : amount, 0, 0);
            else library.moveSelected(area, 0, 0, look.zCoord < 0 ? -amount : amount);
            AreaSelections.apply();
            AreaSelections.saveCurrent();
        } catch (IllegalArgumentException | ArithmeticException error) {
            player.addChatMessage(new net.minecraft.util.ChatComponentText(
                com.github.lunatrius.schematica.client.gui.framework.UiTranslations.format("schematica.ui.area.invalid")));
        }
    }

    /**
     * Convenience method called from InputHandler when the tool attack key is pressed.
     * Delegates to ToolHandler with the current moving object position (for point A).
     */
    public void onToolAttack(EntityPlayer player, SchematicWorld schematic) {
        MovingObjectPosition mop;

        if (currentMode.getUsesAreaSelection()) {
            // Long-range raycast with no practical distance limit
            mop = longRangeRayTrace(player, PLACEMENT_RAYCAST_DISTANCE);
        } else {
            mop = ClientProxy.movingObjectPosition;
            if (mop == null) {
                mop = longRangeRayTrace(player, PLACEMENT_RAYCAST_DISTANCE);
            }
        }

        ToolHandler.onToolAttack(player, mop);
    }

    /**
     * Returns true if the current mode requires a loaded schematic to function.
     */
    public static boolean currentModeUsesSchematic() {
        return currentMode.getUsesSchematic();
    }

    /**
     * Returns true if the current mode uses area selection (pointA/pointB).
     */
    public static boolean currentModeUsesAreaSelection() {
        return currentMode.getUsesAreaSelection();
    }
}

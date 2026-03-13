package com.github.lunatrius.schematica.tool;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Reference;

import cpw.mods.fml.common.registry.GameData;

/**
 * Central state holder for the Litematica-style tool mode system.
 * Tracks the current active tool mode and provides mode-switching logic.
 *
 * @author HackerRouter (ported from Litematica by masa)
 */
public class ToolManager {

    public static final ToolManager INSTANCE = new ToolManager();

    private static ToolMode currentMode = ToolMode.SCHEMATIC_PLACEMENT;

    /** Cached parsed tool item type from config. */
    private static Item toolItemType = null;
    /** Cached parsed tool item meta from config. -1 means ignore meta. */
    private static int toolItemMeta = -1;

    private ToolManager() {}

    /**
     * Parses the tool item config string and caches the result.
     * Supports formats: "minecraft:stick", "minecraft:dye@4"
     */
    public static void parseToolItem(String itemStr) {
        toolItemType = null;
        toolItemMeta = -1;

        if (itemStr == null || itemStr.isEmpty()) {
            return;
        }

        String name = itemStr;
        int atIdx = itemStr.indexOf('@');
        if (atIdx > 0) {
            name = itemStr.substring(0, atIdx);
            try {
                toolItemMeta = Integer.parseInt(itemStr.substring(atIdx + 1));
            } catch (NumberFormatException e) {
                toolItemMeta = -1;
            }
        }

        Item item = (Item) GameData.getItemRegistry().getObject(name);
        if (item != null) {
            toolItemType = item;
            Reference.logger.debug("Tool item set to: {} (meta={})", name, toolItemMeta);
        } else {
            Reference.logger.warn("Tool item not found: {}, falling back to stick", name);
            toolItemType = (Item) GameData.getItemRegistry().getObject("minecraft:stick");
        }
    }

    /**
     * Checks if the player is currently holding the configured tool item in their main hand.
     * When holding the tool item, mouse clicks are intercepted for tool actions.
     */
    public static boolean isHoldingToolItem() {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null || toolItemType == null) {
            return false;
        }
        ItemStack held = player.getHeldItem();
        if (held == null) {
            return false;
        }
        if (held.getItem() != toolItemType) {
            return false;
        }
        if (toolItemMeta >= 0 && held.getItemDamage() != toolItemMeta) {
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
        Vec3 eyePos = Vec3.createVectorHelper(player.posX, player.posY + (double) player.getEyeHeight(), player.posZ);
        Vec3 lookVec = player.getLookVec();
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

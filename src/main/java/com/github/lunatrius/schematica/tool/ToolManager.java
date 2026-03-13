package com.github.lunatrius.schematica.tool;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Reference;

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
        ToolMode previous = currentMode;
        currentMode = currentMode.cycle(forward);

        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player != null && previous != currentMode) {
            player.addChatMessage(new ChatComponentText(
                EnumChatFormatting.GREEN + "[Schematica] " +
                EnumChatFormatting.WHITE + "Tool mode: " +
                EnumChatFormatting.GOLD + currentMode.getDisplayName()));
        }
    }

    /** Maximum raycast distance for SCHEMATIC_PLACEMENT mode (effectively unlimited). */
    private static final double PLACEMENT_RAYCAST_DISTANCE = 256.0;

    /**
     * Convenience method called from InputHandler when the tool use key is pressed.
     * Delegates to ToolHandler with the current moving object position.
     */
    public void onToolUse(EntityPlayer player, SchematicWorld schematic) {
        MovingObjectPosition mop;

        if (currentMode == ToolMode.SCHEMATIC_PLACEMENT || currentMode.getUsesAreaSelection()) {
            // Long-range raycast with no practical distance limit
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

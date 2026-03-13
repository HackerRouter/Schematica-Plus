package com.github.lunatrius.schematica.handler.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.MouseEvent;

import org.lwjgl.input.Keyboard;

import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.tool.ToolManager;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Handles mouse input when the player is holding the configured tool item.
 * <p>
 * When holding the tool item:
 * - Left click (button 0) → tool attack (e.g. set point A)
 * - Right click (button 1) → tool use (e.g. set point B, place schematic)
 * - Ctrl + scroll wheel → cycle tool mode
 * <p>
 * All intercepted events are canceled to prevent normal MC behavior
 * (attacking, placing blocks, switching hotbar slots).
 * <p>
 * Registered on MinecraftForge.EVENT_BUS in ClientProxy.
 */
public class ToolItemHandler {

    public static final ToolItemHandler INSTANCE = new ToolItemHandler();

    private final Minecraft minecraft = Minecraft.getMinecraft();

    private ToolItemHandler() {}

    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        // Don't intercept when a GUI is open
        if (minecraft.currentScreen != null) {
            return;
        }

        // Only intercept when holding the tool item
        if (!ToolManager.isHoldingToolItem()) {
            return;
        }

        EntityPlayer player = minecraft.thePlayer;
        if (player == null) {
            return;
        }

        // === Left click (button=0) → cancel both press and release ===
        if (event.button == 0) {
            event.setCanceled(true);
            if (event.buttonstate) {
                ToolManager.INSTANCE.onToolAttack(player, ClientProxy.schematic);
            }
            return;
        }

        // === Right click (button=1) → cancel both press and release ===
        if (event.button == 1) {
            event.setCanceled(true);
            if (event.buttonstate) {
                ToolManager.INSTANCE.onToolUse(player, ClientProxy.schematic);
            }
            return;
        }

        // === Scroll Wheel → only intercept when Ctrl is held ===
        if (event.dwheel != 0) {
            if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)
                || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
                event.setCanceled(true);
                // dwheel > 0 = scroll up = backward (previous mode)
                // dwheel < 0 = scroll down = forward (next mode)
                boolean forward = event.dwheel < 0;
                ToolManager.cycleMode(forward);
            }
            // If Ctrl is NOT held, don't cancel — let vanilla handle hotbar switching
            return;
        }
    }
}

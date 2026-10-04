package com.github.lunatrius.schematica.handler.client;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.RenderGameOverlayEvent;


import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.tool.ToolManager;

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

    /** The material list HUD inside GUIs (renderMaterialListInGuis) and the looked at block in inventories. */
    @SubscribeEvent
    public void onDrawScreen(net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent.Post event) {
        if (this.minecraft.theWorld == null || !com.github.lunatrius.schematica.handler.VisualSettings.rendering) return;
        if (event.gui instanceof net.minecraft.client.gui.inventory.GuiContainer) {
            com.github.lunatrius.schematica.client.renderer.hud.MaterialListHud.highlightInventory(this.minecraft,
                (net.minecraft.client.gui.inventory.GuiContainer) event.gui);
        }
        com.github.lunatrius.schematica.client.renderer.hud.MaterialListHud.render(this.minecraft, 0, true);
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }

        if (!com.github.lunatrius.schematica.handler.VisualSettings.rendering) {
            com.github.lunatrius.schematica.client.renderer.hud.BlockInfoHud.INSTANCE.clear();
            return;
        }
        boolean verifier = com.github.lunatrius.schematica.client.renderer.hud.VerifierHud.render(this.minecraft, event.partialTicks);
        com.github.lunatrius.schematica.client.renderer.hud.BlockInfoHud.INSTANCE.render(this.minecraft, event.partialTicks, !verifier);
        if (this.minecraft.currentScreen == null && !this.minecraft.gameSettings.hideGUI) {
            com.github.lunatrius.schematica.client.renderer.hud.MaterialListHud.render(this.minecraft,
                com.github.lunatrius.schematica.client.renderer.hud.VerifierHud.textHeight(), false);
        }

        if (this.minecraft.currentScreen != null || this.minecraft.gameSettings.hideGUI) return;
        com.github.lunatrius.schematica.client.renderer.hud.ToolHud.render(this.minecraft);
    }
}

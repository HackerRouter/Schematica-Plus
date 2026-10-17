package com.github.lunatrius.schematica.handler.client;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.gui.material.MaterialItemCache;
import com.github.lunatrius.schematica.client.printer.PrinterMissingMaterials;
import com.github.lunatrius.schematica.client.renderer.hud.BlockInfoHud;
import com.github.lunatrius.schematica.client.renderer.hud.MaterialListHud;
import com.github.lunatrius.schematica.client.renderer.hud.ToolHud;
import com.github.lunatrius.schematica.client.renderer.hud.VerifierHud;
import com.github.lunatrius.schematica.client.world.Placements;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.handler.VisualSettings;
import com.github.lunatrius.schematica.reference.Reference;
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
            final SchematicWorld schematic = Placements.selected;
            if (schematic != null && schematic.isRenderingEnabled()) {
                event.left.add("");
                event.left.add("[§6" + Reference.NAME + "§r] " + schematic.getDebugDimensions());
                event.left.add("[§6" + UiTranslations.format("litematica.hud.selected_mode") + "§r] " + ToolManager.getCurrentMode().getDisplayName());

                final MovingObjectPosition mop = RenderTickHandler.INSTANCE.schematicHit;
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
    private GuiScreen scannedContainer;

    @SubscribeEvent
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        // materialListContainerScan: once per opened container screen, at its first frame
        if (event.gui instanceof GuiContainer && event.gui != this.scannedContainer
            && ConfigurationHandler.materialListContainerScan) {
            this.scannedContainer = event.gui;
            MaterialItemCache.scan(
                ((GuiContainer) event.gui).inventorySlots.inventorySlots);
        }
        if (this.minecraft.theWorld == null || !VisualSettings.rendering) return;
        if (event.gui instanceof GuiContainer) {
            MaterialListHud.highlightInventory(this.minecraft,
                (GuiContainer) event.gui);
        }
        int height = MaterialListHud.render(this.minecraft, 0, true);
        PrinterMissingMaterials.render(this.minecraft, height);
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }

        if (!VisualSettings.rendering) {
            BlockInfoHud.INSTANCE.clear();
            return;
        }
        boolean verifier = VerifierHud.render(this.minecraft, event.partialTicks);
        BlockInfoHud.INSTANCE.render(this.minecraft, event.partialTicks, !verifier);
        if (this.minecraft.currentScreen == null && !this.minecraft.gameSettings.hideGUI) {
            int height = VerifierHud.textHeight();
            height += MaterialListHud.render(this.minecraft, height, false);
            PrinterMissingMaterials.render(this.minecraft, height);
        }

        if (this.minecraft.currentScreen != null || this.minecraft.gameSettings.hideGUI) return;
        ToolHud.render(this.minecraft);
    }
}

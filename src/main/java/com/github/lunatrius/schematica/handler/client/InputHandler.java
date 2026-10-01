package com.github.lunatrius.schematica.handler.client;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.ForgeHooks;


import com.github.lunatrius.schematica.client.world.RenderLayerSettings;
import com.github.lunatrius.schematica.handler.VisualSettings;
import com.github.lunatrius.schematica.client.world.RenderLayerRange;
import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.client.gui.UiDemoScreen;
import com.github.lunatrius.schematica.client.gui.GuiSchematicMainMenu;
import com.github.lunatrius.schematica.client.gui.save.GuiSchematicSave;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.tool.ToolHandler;
import com.github.lunatrius.schematica.tool.ToolManager;


public class InputHandler {

    public static final InputHandler INSTANCE = new InputHandler();

    private final Minecraft minecraft = Minecraft.getMinecraft();

    private InputHandler() {}

    public boolean onHotkey(com.github.lunatrius.schematica.client.input.Hotkey key,
                            com.github.lunatrius.schematica.client.input.Hotkey.Action action) {
        net.minecraft.client.gui.GuiScreen parent = minecraft.currentScreen;
        SchematicWorld placement = ClientProxy.schematic;
        switch (key.id) {
            case "openGuiMainMenu": minecraft.displayGuiScreen(new GuiSchematicMainMenu(parent)); return true;
            case "openGuiSettings": minecraft.displayGuiScreen(new com.github.lunatrius.schematica.client.gui.GuiModConfig(parent)); return true;
            case "uiDemo": minecraft.displayGuiScreen(new UiDemoScreen(parent)); return true;
            default: break;
        }
        if (minecraft.thePlayer == null || minecraft.theWorld == null) return false;
        switch (key.id) {
            case "openGuiLoadedSchematics": minecraft.displayGuiScreen(new com.github.lunatrius.schematica.client.gui.placement.GuiSchematicLoadedList(parent)); break;
            case "openGuiSchematicPlacements": minecraft.displayGuiScreen(new com.github.lunatrius.schematica.client.gui.placement.GuiSchematicPlacementsList(parent)); break;
            case "openGuiSelectionManager": minecraft.displayGuiScreen(new com.github.lunatrius.schematica.client.gui.save.GuiAreaSelectionManager(parent)); break;
            case "openGuiAreaSettings": minecraft.displayGuiScreen(new com.github.lunatrius.schematica.client.gui.save.GuiAreaSelectionEditor(parent)); break;
            case "openGuiPlacementSettings":
                if (placement != null) minecraft.displayGuiScreen(new com.github.lunatrius.schematica.client.gui.placement.GuiPlacementConfiguration(parent, placement));
                break;
            case "openGuiMaterialList":
                if (placement != null) minecraft.displayGuiScreen(new com.github.lunatrius.schematica.client.gui.control.GuiSchematicMaterials(parent, placement));
                break;
            case "openGuiSchematicVerifier":
                if (placement != null) minecraft.displayGuiScreen(new com.github.lunatrius.schematica.client.gui.GuiSchematicVerifier(parent, placement));
                break;
            case "saveAreaAsSchematicToFile": minecraft.displayGuiScreen(new GuiSchematicSave(parent)); break;
            case "layerNext": moveLayer(1); break;
            case "layerPrevious": moveLayer(-1); break;
            case "layerModeNext": cycleLayer(1); break;
            case "layerModePrevious": cycleLayer(-1); break;
            case "layerSetHere": RenderLayerSettings.RANGE.setValue(false, MathHelper.floor_double(layerCoordinate())); break;
            case "rerenderSchematic": com.github.lunatrius.schematica.client.renderer.RendererSchematicGlobal.INSTANCE.refresh(); break;
            case "unloadCurrentSchematic": SchematicaPlus.proxy.unloadSchematic(); break;
            case "pickBlockFirst": return pickBlock();
            case "pickBlockToggle": ToolManager.toggleConfig("pickBlockEnabled"); break;
            case "schematicEditReplaceSelection": return com.github.lunatrius.schematica.tool.SchematicRebuild.replaceSelection();
            case "toggleAllRendering": renderToggle(VisualSettings.Toggle.ALL, false); break;
            case "toggleSchematicRendering": renderToggle(VisualSettings.Toggle.SCHEMATIC, false); break;
            case "toggleSchematicBlockRendering": VisualSettings.toggle(VisualSettings.Toggle.BLOCKS); break;
            case "toggleOverlayRendering": renderToggle(VisualSettings.Toggle.OVERLAY, true); break;
            case "toggleOverlayOutlineRendering": renderToggle(VisualSettings.Toggle.OVERLAY_OUTLINES, true); break;
            case "toggleOverlaySideRendering": renderToggle(VisualSettings.Toggle.OVERLAY_SIDES, true); break;
            case "toggleTranslucentRendering": renderToggle(VisualSettings.Toggle.TRANSLUCENT, false); break;
            case "toggleAreaSelectionBoxesRendering": VisualSettings.toggle(VisualSettings.Toggle.AREA_BOXES); break;
            case "togglePlacementBoxesRendering": VisualSettings.toggle(VisualSettings.Toggle.PLACEMENT_BOXES); break;
            case "toggleInfoOverlayRendering": VisualSettings.toggle(VisualSettings.Toggle.INFO_OVERLAY); break;
            case "toggleVerifierOverlayRendering": VisualSettings.toggle(VisualSettings.Toggle.VERIFIER_OVERLAY); break;
            case "executeOperation":
                if (!com.github.lunatrius.schematica.handler.ConfigurationHandler.executeRequireTool || ToolManager.toolActive()) ToolHandler.onExecute(minecraft.thePlayer);
                else return false;
                break;
            default: return ToolManager.hotkey(key.id);
        }
        return true;
    }

    /** Litematica's RenderToggle: rebuilds schematic geometry when turned on, or on every change for geometry baked into chunks. */
    private void renderToggle(VisualSettings.Toggle toggle, boolean baked) {
        if (VisualSettings.toggle(toggle) || baked) com.github.lunatrius.schematica.client.renderer.RendererSchematicGlobal.INSTANCE.refresh();
    }

    private double layerCoordinate() {
        net.minecraft.entity.Entity camera = minecraft.renderViewEntity;
        if (camera == null) return 0;
        return RenderLayerSettings.RANGE.axis() == RenderLayerRange.Axis.X ? camera.posX
            : RenderLayerSettings.RANGE.axis() == RenderLayerRange.Axis.Y ? camera.posY - camera.yOffset : camera.posZ;
    }

    private void cycleLayer(int direction) {
        RenderLayerRange.Mode[] modes = RenderLayerRange.Mode.values();
        RenderLayerSettings.RANGE.setMode(modes[Math.floorMod(RenderLayerSettings.RANGE.mode().ordinal() + direction, modes.length)]);
    }

    private void moveLayer(int amount) {
        RenderLayerRange range = RenderLayerSettings.RANGE;
        if (range.mode() != RenderLayerRange.Mode.ALL) {
            net.minecraft.entity.Entity camera = minecraft.renderViewEntity;
            if (camera != null) {
                double coordinate = range.axis() == RenderLayerRange.Axis.X
                    ? camera.posX : range.axis() == RenderLayerRange.Axis.Y
                    ? camera.posY - camera.yOffset : camera.posZ;
                range.move(amount, coordinate);
            }
        } else {
            SchematicWorld schematic = ClientProxy.schematic;
            if (schematic != null && schematic.isRenderingLayer) {
                schematic.renderingLayer = MathHelper.clamp_int(schematic.renderingLayer + amount, 0, schematic.getHeight() - 1);
            }
        }
    }

    private boolean pickBlock() {
        if (!com.github.lunatrius.schematica.handler.ConfigurationHandler.pickBlockEnabled) return false;
        try {
            SchematicWorld schematic = ClientProxy.schematic;
            return schematic != null && schematic.isRenderingEnabled() && VisualSettings.schematicVisible() && minecraft.thePlayer != null
                && pickBlock(schematic, RenderTickHandler.INSTANCE.rayTrace(schematic, 1));
        } catch (Exception error) {
            Reference.logger.error("Could not pick block!", error);
            return false;
        }
    }

    private boolean pickBlock(final SchematicWorld schematic, final MovingObjectPosition hit) {
        net.minecraft.entity.EntityLivingBase camera = this.minecraft.renderViewEntity;
        if (!PickBlockInput.schematicFirst(hit, this.minecraft.objectMouseOver, camera == null ? null : camera.getPosition(1),
            schematic.position.x, schematic.position.y, schematic.position.z)
            || !schematic.isBlockRendered(hit.blockX, hit.blockY, hit.blockZ)) return false;
        final EntityClientPlayerMP player = this.minecraft.thePlayer;
        if (!ForgeHooks.onPickBlock(hit, player, schematic)) return false;
        if (player.capabilities.isCreativeMode) {
            final Block block = schematic.getBlock(hit.blockX, hit.blockY, hit.blockZ);
            final int metadata = schematic.getBlockMetadata(hit.blockX, hit.blockY, hit.blockZ);
            if (block == Blocks.double_stone_slab || block == Blocks.double_wooden_slab || block == Blocks.snow_layer) {
                player.inventory.setInventorySlotContents(player.inventory.currentItem, new ItemStack(block, 1, metadata & 0xF));
            }
            final int slot = player.inventoryContainer.inventorySlots.size() - 9 + player.inventory.currentItem;
            this.minecraft.playerController.sendSlotPacket(player.inventory.getStackInSlot(player.inventory.currentItem), slot);
        }
        return true;
    }
}

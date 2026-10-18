package com.github.lunatrius.schematica.handler.client;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import com.github.lunatrius.schematica.client.gui.GuiModConfig;
import com.github.lunatrius.schematica.client.gui.GuiSchematicMainMenu;
import com.github.lunatrius.schematica.client.gui.GuiSchematicVerifier;
import com.github.lunatrius.schematica.client.gui.control.GuiSchematicMaterials;
import com.github.lunatrius.schematica.client.gui.material.MaterialList;
import com.github.lunatrius.schematica.client.gui.material.MaterialLists;
import com.github.lunatrius.schematica.client.gui.placement.GuiPlacementConfiguration;
import com.github.lunatrius.schematica.client.gui.placement.GuiPlacementGridSettings;
import com.github.lunatrius.schematica.client.gui.placement.GuiSchematicLoadedList;
import com.github.lunatrius.schematica.client.gui.placement.GuiSchematicPlacementsList;
import com.github.lunatrius.schematica.client.gui.projects.GuiVersionPrompt;
import com.github.lunatrius.schematica.client.gui.save.GuiAreaSelectionEditor;
import com.github.lunatrius.schematica.client.gui.save.GuiAreaSelectionManager;
import com.github.lunatrius.schematica.client.gui.save.GuiSchematicSave;
import com.github.lunatrius.schematica.client.input.Hotkey;
import com.github.lunatrius.schematica.client.input.Hotkeys;
import com.github.lunatrius.schematica.client.printer.EasyPlace;
import com.github.lunatrius.schematica.client.printer.PickBlockSlots;
import com.github.lunatrius.schematica.client.printer.SchematicPrinter;
import com.github.lunatrius.schematica.client.projects.SchematicProject;
import com.github.lunatrius.schematica.client.projects.SchematicProjects;
import com.github.lunatrius.schematica.client.renderer.RendererSchematicGlobal;
import com.github.lunatrius.schematica.client.selection.AreaSelections;
import com.github.lunatrius.schematica.client.util.InfoUtils;
import com.github.lunatrius.schematica.client.world.Placements;
import com.github.lunatrius.schematica.client.world.RenderLayerRange;
import com.github.lunatrius.schematica.client.world.RenderLayerSettings;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.handler.VisualSettings;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.tool.PlacementActions;
import com.github.lunatrius.schematica.tool.SchematicRebuild;
import com.github.lunatrius.schematica.tool.SchematicTargets;
import com.github.lunatrius.schematica.tool.ToolHandler;
import com.github.lunatrius.schematica.tool.ToolManager;
import com.github.lunatrius.schematica.util.MessageType;

public class InputHandler {

    public static final InputHandler INSTANCE = new InputHandler();

    private final Minecraft minecraft = Minecraft.getMinecraft();

    private InputHandler() {}

    public boolean onHotkey(Hotkey key,
                            Hotkey.Action action) {
        GuiScreen parent = minecraft.currentScreen;
        SchematicWorld placement = Placements.selected;
        switch (key.id) {
            case "openGuiMainMenu": minecraft.displayGuiScreen(new GuiSchematicMainMenu(parent)); return true;
            case "openGuiSettings": minecraft.displayGuiScreen(new GuiModConfig(parent)); return true;
            default: break;
        }
        if (minecraft.thePlayer == null || minecraft.theWorld == null) return false;
        if (key.toggleGroup != null) {
            VisualSettings.Toggle toggle = VisualSettings.Toggle.byUpstream(key.id);
            if (toggle == null) return false;
            VisualSettings.toggle(toggle);
            if (toggle != VisualSettings.Toggle.ENTITY_HITBOXES) RendererSchematicGlobal.INSTANCE.refresh();
            return true;
        }
        switch (key.id) {
            case "openGuiLoadedSchematics": minecraft.displayGuiScreen(new GuiSchematicLoadedList(parent)); break;
            case "openGuiSchematicPlacements": minecraft.displayGuiScreen(new GuiSchematicPlacementsList(parent)); break;
            case "openGuiSelectionManager":
                if (!SchematicProjects.hasProjectOpen()) minecraft.displayGuiScreen(new GuiAreaSelectionManager(parent));
                else InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica.gui.button.hover.schematic_projects.area_browser_disabled_currently_in_projects_mode");
                break;
            case "openGuiSchematicProjects": return SchematicProjects.openGui(parent);
            case "openGuiAreaSettings": minecraft.displayGuiScreen(new GuiAreaSelectionEditor(parent)); break;
            case "openGuiPlacementSettings":
                if (placement != null) minecraft.displayGuiScreen(new GuiPlacementConfiguration(parent, placement));
                break;
            case "openPlacementGridSettingsScreen":
                if (placement != null) minecraft.displayGuiScreen(new GuiPlacementGridSettings(parent, placement));
                else InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_placement_selected");
                break;
            case "openGuiMaterialList":
                openMaterialList(minecraft, parent, placement);
                break;
            case "openGuiSchematicVerifier":
                if (placement != null) minecraft.displayGuiScreen(new GuiSchematicVerifier(parent, placement));
                break;
            case "saveAreaAsSchematicToFile": return saveSchematic(parent);
            case "schematicVersionCycleNext": if (SchematicProjects.hasProjectOpen()) SchematicProjects.cycleVersion(1); break;
            case "schematicVersionCyclePrevious": if (SchematicProjects.hasProjectOpen()) SchematicProjects.cycleVersion(-1); break;
            case "schematicVCSDeleteBlockByPlacement": return SchematicProjects.deleteBlocksByPlacement();
            case "layerNext": moveLayer(1); break;
            case "layerPrevious": moveLayer(-1); break;
            case "layerModeNext": cycleLayer(1); break;
            case "layerModePrevious": cycleLayer(-1); break;
            case "layerSetHere": RenderLayerSettings.RANGE.setValue(false, MathHelper.floor_double(layerCoordinate())); break;
            case "rerenderSchematic": RendererSchematicGlobal.INSTANCE.refresh(); break;
            case "unloadCurrentSchematic": if (Placements.selected != null) Placements.remove(Placements.selected); break;
            case "pickBlockFirst": return pickBlock(true);
            case "pickBlockLast":
                if (!Hotkeys.boundOnlyTo("pickBlockLast", minecraft.gameSettings.keyBindUseItem.getKeyCode())) pickBlock(false);
                return false;
            case "pickBlockToggle": ToolManager.toggleConfig("pickBlockEnabled", true); break;
            case "schematicEditReplaceSelection": return SchematicRebuild.replaceSelection();
            case "refreshMaterialList": {
                MaterialList list = MaterialLists.current();
                if (list == null && placement != null) list = placement.materialList;
                if (list == null) return false;
                list.refresh();
                InfoUtils.printActionbarMessage("schematica.message.material_list.refreshed");
                return true;
            }
            case "printerWorkingSwitch": SchematicPrinter.INSTANCE.toggleWithMessage(); return true;
            case "easyPlaceUseKey": return EasyPlace.handle();
            case "easyPlaceFirst": ToolManager.toggleConfig("easyPlaceFirst", true); break;
            case "easyPlaceToggle": ToolManager.toggleConfig("easyPlaceMode", true); break;
            case "togglePlacementRestriction": ToolManager.toggleConfig("placementRestriction", true); break;
            case "toggleSignTextPaste": ToolManager.toggleConfig("signTextPaste", true); break;
            case "cloneSelection": return PlacementActions.cloneSelection();
            case "saveAreaAsInMemorySchematic": return PlacementActions.saveInMemory();
            case "schematicPlacementRotation": return PlacementActions.rotate();
            case "schematicPlacementMirror": return PlacementActions.mirror();
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
                return (!ConfigurationHandler.executeRequireTool || ToolManager.toolActive())
                    && ToolHandler.onExecute(minecraft.thePlayer);
            default: return ToolManager.hotkey(key.id);
        }
        return true;
    }

    /** SchematicUtils.saveSchematic: a new project version while a project is open, else the save screen. */
    public boolean saveSchematic(GuiScreen parent) {
        if (AreaSelections.library().selected() == null) return false;
        SchematicProject project = SchematicProjects.current();
        if (project != null) minecraft.displayGuiScreen(new GuiVersionPrompt(parent, project));
        else minecraft.displayGuiScreen(new GuiSchematicSave(parent));
        return true;
    }

    /** Litematica's RenderToggle: rebuilds schematic geometry when turned on, or on every change for geometry baked into chunks. */
    private void renderToggle(VisualSettings.Toggle toggle, boolean baked) {
        if (VisualSettings.toggle(toggle) || baked) RendererSchematicGlobal.INSTANCE.refresh();
    }

    private double layerCoordinate() {
        Entity camera = minecraft.renderViewEntity;
        if (camera == null) return 0;
        return RenderLayerSettings.RANGE.axis() == RenderLayerRange.Axis.X ? camera.posX
            : RenderLayerSettings.RANGE.axis() == RenderLayerRange.Axis.Y ? camera.posY - camera.yOffset : camera.posZ;
    }

    private void cycleLayer(int direction) {
        RenderLayerRange.Mode[] modes = RenderLayerRange.Mode.values();
        RenderLayerSettings.RANGE.setMode(modes[Math.floorMod(RenderLayerSettings.RANGE.mode().ordinal() + direction, modes.length)]);
    }

    private static void openMaterialList(Minecraft minecraft, GuiScreen parent, SchematicWorld placement) {
        MaterialList list = MaterialLists.current();
        // No last-viewed material list currently stored, try to get one for the currently selected placement, if any
        if (list == null) {
            if (placement == null) {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_placement_selected");
                return;
            }
            list = MaterialList.placement(placement);
            list.refresh();
        }
        minecraft.displayGuiScreen(new GuiSchematicMaterials(parent, list));
    }

    private void moveLayer(int amount) {
        RenderLayerRange range = RenderLayerSettings.RANGE;
        if (range.mode() != RenderLayerRange.Mode.ALL) {
            Entity camera = minecraft.renderViewEntity;
            if (camera != null) {
                double coordinate = range.axis() == RenderLayerRange.Axis.X
                    ? camera.posX : range.axis() == RenderLayerRange.Axis.Y
                    ? camera.posY - camera.yOffset : camera.posZ;
                range.move(amount, coordinate);
            }
        } else {
            SchematicWorld schematic = Placements.selected;
            if (schematic != null && schematic.isRenderingLayer) {
                schematic.renderingLayer = MathHelper.clamp_int(schematic.renderingLayer + amount, 0, schematic.getHeight() - 1);
            }
        }
    }

    /** Litematica's EntityUtils.shouldPickBlock. */
    public static boolean shouldPickBlock() {
        return ConfigurationHandler.pickBlockEnabled
            && (!ConfigurationHandler.toolItemEnabled || !ToolManager.isHoldingToolItem())
            && VisualSettings.rendering && VisualSettings.schematic;
    }

    /** Litematica's doSchematicWorldPickBlock: the closest schematic block, or the farthest one before the targeted real block. */
    public boolean pickBlock(boolean closest) {
        if (minecraft.thePlayer == null || !shouldPickBlock()) return false;
        try {
            double range = SchematicTargets.validBlockRange();
            SchematicTargets.Hit hit = closest ? SchematicTargets.closest(range, false) : SchematicTargets.furthestBeforeVanilla(range);
            return hit != null && pickBlock(hit);
        } catch (Exception error) {
            Reference.logger.error("Could not pick block!", error);
            return false;
        }
    }

    private boolean pickBlock(SchematicTargets.Hit target) {
        SchematicWorld schematic = target.world;
        int x = target.localX(), y = target.localY(), z = target.localZ();
        MovingObjectPosition hit = new MovingObjectPosition(x, y, z, target.side, Vec3.createVectorHelper(
            target.hitX - schematic.position.x, target.hitY - schematic.position.y, target.hitZ - schematic.position.z));
        final EntityClientPlayerMP player = this.minecraft.thePlayer;
        final Block block = schematic.getBlock(x, y, z);
        if (block.isAir(schematic, x, y, z)) return false;
        ItemStack stack = block.getPickBlock(hit, schematic, x, y, z, player);
        if (stack == null || stack.getItem() == null) return false;
        if (player.capabilities.isCreativeMode && (block == Blocks.double_stone_slab || block == Blocks.double_wooden_slab || block == Blocks.snow_layer)) {
            stack = new ItemStack(block, 1, schematic.getBlockMetadata(x, y, z) & 0xF);
        }
        // InventoryUtils.schematicWorldPickBlock: hotbar first, else a pick-blockable slot
        PickBlockSlots.pickToHand(this.minecraft, stack, true);
        return true;
    }
}

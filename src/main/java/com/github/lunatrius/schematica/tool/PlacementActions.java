// SPDX-License-Identifier: LGPL-3.0-only
// Litematica placement and selection hotkey actions, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.tool;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.client.gui.GuiTextPrompt;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.gui.placement.PlacementTransform;
import com.github.lunatrius.schematica.client.printer.SchematicPrinter;
import com.github.lunatrius.schematica.client.renderer.RendererSchematicGlobal;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Area;
import com.github.lunatrius.schematica.client.selection.AreaSelections;
import com.github.lunatrius.schematica.client.world.SchematicLibrary;
import com.github.lunatrius.schematica.client.world.SchematicSourceData;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.handler.client.WorldHandler;
import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.world.schematic.SchematicFileSnapshot;
import com.github.lunatrius.schematica.world.storage.RegionSelection;

public final class PlacementActions {
    private PlacementActions() {}

    private static Minecraft mc() { return Minecraft.getMinecraft(); }

    public static void actionBar(String text) {
        if (mc().ingameGUI != null) mc().ingameGUI.func_110326_a(text, false);
    }

    private static void chat(EnumChatFormatting color, String key, Object... arguments) {
        if (mc().thePlayer == null) return;
        ChatComponentTranslation text = new ChatComponentTranslation(key, arguments);
        text.getChatStyle().setColor(color);
        mc().thePlayer.addChatMessage(text);
    }

    /** schematicPlacementRotation: rotates the selected placement one step clockwise. */
    public static boolean rotate() { return transform(true); }

    /** schematicPlacementMirror: cycles the selected placement's mirror. */
    public static boolean mirror() { return transform(false); }

    private static boolean transform(boolean rotation) {
        SchematicWorld placement = ClientProxy.schematic;
        if (placement == null) return false;
        if (placement.placementSettings().locked) {
            actionBar(UiTranslations.format("litematica.message.placement.cant_modify_is_locked"));
            return true;
        }
        PlacementTransform.Orientation orientation = PlacementTransform.orientation(placement.transformOperations);
        if (orientation == null) {
            actionBar(UiTranslations.format("schematica.ui.placement.custom"));
            return true;
        }
        String steps = rotation ? "Y" : orientation.cycleMirror(false);
        try {
            for (int i = 0; i < steps.length(); i++) {
                char op = steps.charAt(i);
                if (op == 'Y') placement.rotate(ForgeDirection.UP);
                else placement.flip(op == 'x' ? ForgeDirection.EAST : ForgeDirection.SOUTH);
            }
        } catch (RuntimeException error) {
            Reference.logger.error("Failed to transform placement", error);
            actionBar(EnumChatFormatting.RED + UiTranslations.format("schematica.ui.placement.transform_failed"));
            return true;
        } finally {
            RendererSchematicGlobal.INSTANCE.createRendererSchematicChunks(placement);
            SchematicPrinter.INSTANCE.refresh();
            WorldHandler.INSTANCE.saveSession();
        }
        PlacementTransform.Orientation next = PlacementTransform.orientation(placement.transformOperations);
        if (next != null) actionBar(UiTranslations.format(rotation ? "litematica.message.placement.rotation_set_to" : "litematica.message.placement.mirror_set_to",
            rotation ? next.rotationName() : next.mirrorName()));
        return true;
    }

    /** cloneSelection: captures the selected area into an in-memory schematic and places it for pasting. */
    public static boolean cloneSelection() {
        Area area = AreaSelections.library().selected();
        if (area == null || area.boxes().isEmpty()) {
            chat(EnumChatFormatting.RED, "litematica.message.error.no_area_selected");
            return true;
        }
        Vector3i origin = ConfigurationHandler.cloneAtOriginalPosition ? area.origin() : targetedPosition();
        String name = area.name();
        capture(area, name, source -> {
            SchematicWorld placement = ClientProxy.createPlacement(source);
            placement.moveOriginTo(origin.x, origin.y, origin.z);
            ClientProxy.selectSchematic(placement);
            if (mc().thePlayer != null && mc().thePlayer.capabilities.isCreativeMode) ToolManager.setCurrentMode(ToolMode.PASTE_SCHEMATIC);
            WorldHandler.INSTANCE.saveSession();
        });
        return true;
    }

    /** saveAreaAsInMemorySchematic: asks for a name, then captures the selected area into an in-memory schematic. */
    public static boolean saveInMemory() {
        Area area = AreaSelections.library().selected();
        if (area == null) return false;
        mc().displayGuiScreen(new GuiTextPrompt(mc().currentScreen, UiTranslations.format("litematica.gui.title.create_in_memory_schematic"), area.name(), name -> {
            if (AreaSelections.library().selected() != area || area.boxes().isEmpty()) return UiTranslations.format("litematica.message.error.no_area_selected");
            capture(area, name, source -> chat(EnumChatFormatting.GREEN, "litematica.message.in_memory_schematic_created", name));
            return null;
        }));
        return true;
    }

    interface Created { void accept(SchematicLibrary.Source<SchematicSourceData> source) throws Exception; }

    private static void capture(Area area, String name, Created created) {
        if (mc().theWorld == null || mc().thePlayer == null || !SchematicaPlus.proxy.isSaveEnabled) {
            chat(EnumChatFormatting.RED, "schematica.ui.save.disabled");
            return;
        }
        RegionSelection selection;
        try { selection = area.snapshot(); }
        catch (IllegalArgumentException | ArithmeticException error) {
            chat(EnumChatFormatting.RED, "schematica.ui.save.failed");
            return;
        }
        Object world = mc().theWorld;
        boolean queued = SchematicaPlus.proxy.captureSchematic(mc().thePlayer, name, mc().theWorld, selection, (SchematicFileSnapshot snapshot) -> {
            if (mc().theWorld != world) return;
            try { created.accept(ClientProxy.addMemorySource(name, snapshot)); }
            catch (Exception error) {
                Reference.logger.error("Could not create the in-memory schematic", error);
                chat(EnumChatFormatting.RED, "schematica.ui.save.failed");
            }
        });
        if (!queued) chat(EnumChatFormatting.RED, "schematica.ui.save.failed");
    }

    /** Litematica's RayTraceUtils.getTargetedPosition (sneak for the adjacent position), or the player position. */
    static Vector3i targetedPosition() {
        double range = SchematicTargets.validBlockRange();
        MovingObjectPosition hit = ToolManager.trace(range);
        if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            Vector3i point = new Vector3i(hit.blockX, hit.blockY, hit.blockZ);
            if (!mc().thePlayer.isSneaking()) {
                ForgeDirection side = ForgeDirection.getOrientation(hit.sideHit);
                point.add(side.offsetX, side.offsetY, side.offsetZ);
            }
            return point;
        }
        return new Vector3i(MathHelper.floor_double(mc().thePlayer.posX), MathHelper.floor_double(mc().thePlayer.boundingBox.minY),
            MathHelper.floor_double(mc().thePlayer.posZ));
    }
}

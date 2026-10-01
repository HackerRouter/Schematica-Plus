// SPDX-License-Identifier: LGPL-3.0-only
// Litematica selection actions, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.tool;

import net.minecraft.client.Minecraft;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Area;
import com.github.lunatrius.schematica.client.selection.AreaSelections;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;

final class ToolSelectionActions {
    private ToolSelectionActions() {}
    static boolean hotkey(String id) {
        if (id.equals("selectionModeCycle")) { cycleMode(); return true; }
        if (!SchematicaPlus.proxy.isSaveEnabled) return false;
        AreaSelectionLibrary library = AreaSelections.library(); Area area = library.selected();
        if (area == null) return false;
        Vector3i point = ToolManager.playerPosition();
        switch (id) {
            case "addSelectionBox": {
                if (!ToolManager.currentModeUsesAreaSelection() || library.mode() != AreaSelectionLibrary.Mode.NORMAL) return false;
                AreaSelections.capture();
                String name = area.name(); int i = 1;
                while (hasBox(area, name)) name = area.name() + " " + i++;
                AreaSelectionLibrary.Box box = library.addBox(area, name, point, point);
                library.selectCorner(area, box, AreaSelectionLibrary.Corner.FIRST);
                library.setGuide(area, true);
                PlacementActions.actionBar(UiTranslations.format("litematica.message.added_selection_box",
                    String.format("x: %d, y: %d, z: %d", point.x, point.y, point.z)));
                break;
            }
            case "deleteSelectionBox": {
                if (!ToolManager.currentModeUsesAreaSelection()) return false;
                AreaSelections.capture();
                if (area.originSelected()) {
                    library.setOrigin(area, null);
                    PlacementActions.actionBar(UiTranslations.format("litematica.message.removed_area_origin"));
                    ToolManager.saveArea();
                    return false;
                }
                AreaSelectionLibrary.Box box = area.selectedBox();
                if (box == null || library.mode() != AreaSelectionLibrary.Mode.NORMAL) return false;
                library.removeBox(area, box);
                PlacementActions.actionBar(UiTranslations.format("litematica.message.removed_selection_box", box.name()));
                break;
            }
            case "setAreaOrigin": AreaSelections.capture(); library.setOrigin(area, point); break;
            case "setSelectionBoxPosition1":
            case "setSelectionBoxPosition2":
                if (area.selectedBox() == null) return true;
                AreaSelections.capture(); library.setPoints(area, id.endsWith("1") ? point : area.first(), id.endsWith("2") ? point : area.second());
                break;
            case "moveEntireSelection":
                if (ToolManager.getCurrentMode() == ToolMode.MOVE) { WorldMoveController.moveTo(point); return true; }
                AreaSelections.capture(); Vector3i old = area.origin();
                library.moveEntire(area, point.x - old.x, point.y - old.y, point.z - old.z); break;
            case "selectionGrow":
            case "selectionShrink":
                if (!ToolManager.currentModeUsesAreaSelection() || area.selectedBox() == null) return false;
                AreaSelections.capture(); fit(library, area, id.equals("selectionGrow")); break;
            default: return false;
        }
        ToolManager.saveArea(); return true;
    }
    /** Litematica's selectionModeCycle: Delete target, paste replace behavior, or the area corner mode. */
    private static void cycleMode() {
        ToolMode mode = ToolManager.getCurrentMode();
        if (mode == ToolMode.DELETE) ToolMode.deleteUsesPlacement = !ToolMode.deleteUsesPlacement;
        else if (mode == ToolMode.PASTE_SCHEMATIC) {
            ConfigurationHandler.setPasteReplaceBehavior(ConfigurationHandler.pasteReplaceBehavior.cycle(false));
        }
        else if (mode.getUsesAreaSelection()) {
            AreaSelectionLibrary library = AreaSelections.library();
            library.setCornerMode(library.cornerMode() == AreaSelectionLibrary.CornerMode.CORNERS
                ? AreaSelectionLibrary.CornerMode.EXPAND : AreaSelectionLibrary.CornerMode.CORNERS);
            AreaSelections.saveCurrent();
        }
    }

    private static boolean hasBox(Area area, String name) {
        for (AreaSelectionLibrary.Box box : area.boxes()) if (box.name().equalsIgnoreCase(name)) return true;
        return false;
    }
    private static void fit(AreaSelectionLibrary library, Area area, boolean grow) {
        Vector3i a = area.first(), b = area.second();
        int[] min = {Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z)};
        int[] max = {Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z)};
        int[] budget = {2_000_000};
        for (int step = 0; step < 256; step++) {
            if (grow) for (int axis = 0; axis < 3; axis++) { min[axis]--; max[axis]++; }
            if (min[1] < 0 || max[1] > 255) return;
            int empty = 0;
            for (int axis = 0; axis < 3; axis++) {
                if (emptySlice(min, max, axis, min[axis], budget)) { min[axis]++; empty++; }
                if (emptySlice(min, max, axis, max[axis], budget)) { max[axis]--; empty++; }
                if (min[axis] > max[axis]) return;
            }
            if (grow ? empty == 6 : empty == 0) break;
        }
        library.setPoints(area, new Vector3i(min[0], min[1], min[2]), new Vector3i(max[0], max[1], max[2]));
    }
    private static boolean emptySlice(int[] min, int[] max, int axis, int coordinate, int[] budget) {
        int[] start = min.clone(), end = max.clone(); start[axis] = end[axis] = coordinate;
        net.minecraft.world.World world = Minecraft.getMinecraft().theWorld;
        for (int y = start[1]; y <= end[1]; y++) for (int z = start[2]; z <= end[2]; z++) for (int x = start[0]; x <= end[0]; x++) {
            if (--budget[0] < 0 || !world.blockExists(x, y, z)) throw new IllegalArgumentException("Selection scan exceeds loaded bounds");
            if (!world.isAirBlock(x, y, z)) return false;
        }
        return true;
    }
}

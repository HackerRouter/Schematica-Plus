// SPDX-License-Identifier: LGPL-3.0-only
// Litematica MaterialListBase / MaterialListPlacement / MaterialListSchematic / MaterialListAreaAnalyzer, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.material;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.api.SchematicRegion;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Area;
import com.github.lunatrius.schematica.client.selection.AreaSelections;
import com.github.lunatrius.schematica.client.world.RenderLayerSettings;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.google.gson.JsonObject;

/** A material list that outlives its screen: the last viewed one is reopened by the hotkey and drawn by the info HUD. */
public final class MaterialList {
    public enum Kind { PLACEMENT, SCHEMATIC, AREA }

    public final Kind kind;
    private SchematicWorld schematic;
    private final Area area;
    private final AreaSelectionLibrary library;
    private final String fileName;
    private final List<SchematicRegion> fileRegions;
    private final WorldClient openedWorld;
    private final MaterialListModel<MaterialItemKey> model = new MaterialListModel<>();
    private boolean renderLayers;
    private boolean hud;
    private MaterialScanner scan;
    private boolean hasResult;
    private int skipped, unverified;
    private String scanError = "";
    private int[] geometry;
    private long layerRevision;
    private Object source;
    private boolean placementEnabled;
    private List<String> transforms = Collections.emptyList();
    private int revision;
    private int ticks;
    private long lastTick = -1;

    private MaterialList(Kind kind, SchematicWorld schematic, Area area, String fileName, List<SchematicRegion> fileRegions) {
        this.kind = kind;
        this.schematic = schematic;
        this.area = area;
        library = area == null ? null : AreaSelections.library();
        this.fileName = fileName;
        this.fileRegions = fileRegions == null ? null : new ArrayList<>(fileRegions);
        openedWorld = Minecraft.getMinecraft().theWorld;
        model.restoreSort(ConfigurationHandler.sortType);
    }

    /** The material list of a placement, kept with it like SchematicPlacement.getMaterialList(). */
    public static MaterialList placement(SchematicWorld placement) {
        if (placement.materialList == null || placement.materialList.openedWorld != Minecraft.getMinecraft().theWorld) {
            MaterialList list = new MaterialList(Kind.PLACEMENT, placement, null, null, null);
            list.fromJson(placement.materialList != null ? placement.materialList.toJson() : placement.materialListData);
            placement.materialList = list;
        }
        return placement.materialList;
    }

    /** MaterialListSchematic: the items of a schematic file that is not placed. */
    public static MaterialList schematic(SchematicWorld schematic, String name, List<SchematicRegion> regions) {
        return new MaterialList(Kind.SCHEMATIC, schematic, null, name, regions);
    }

    public static MaterialList area(Area area) { return new MaterialList(Kind.AREA, null, area, null, null); }

    /** Moves a placement's list to the placement that replaced it after a reload. */
    public void rebind(SchematicWorld placement) {
        if (kind == Kind.PLACEMENT) schematic = placement;
    }

    public MaterialListModel<MaterialItemKey> model() { return model; }
    public SchematicWorld schematic() { return schematic; }
    public Area area() { return area; }
    public String fileName() { return fileName; }
    public boolean renderLayers() { return renderLayers; }
    public boolean supportsRenderLayers() { return kind != Kind.SCHEMATIC; }
    public boolean hud() { return hud; }
    public void setHud(boolean hud) { this.hud = hud; }
    public boolean scanning() { return scan != null; }
    public int scanPercent() { return scan == null ? 100 : scan.percent(); }
    public boolean hasResult() { return hasResult; }
    public int skipped() { return skipped; }
    public int unverified() { return unverified; }
    public String scanError() { return scanError; }
    /** Changes whenever the entries or their available counts change. */
    public int revision() { return revision; }

    public void setRenderLayers(boolean renderLayers) {
        if (!supportsRenderLayers()) return;
        this.renderLayers = renderLayers;
        refresh();
    }

    public String name() {
        if (kind == Kind.AREA) return area.name();
        return kind == Kind.SCHEMATIC ? fileName : schematic == null ? "-" : schematic.name;
    }

    public String title() {
        switch (kind) {
            case AREA: return UiTranslations.format("litematica.gui.title.material_list.area_analyzer", area.name());
            case SCHEMATIC:
                int regions = Math.max(1, schematic.getSchematic().getRegions().size());
                return UiTranslations.format("litematica.gui.title.material_list.schematic", fileName,
                    fileRegions.isEmpty() ? regions : fileRegions.size(), regions);
            default: return UiTranslations.format("litematica.gui.title.material_list.placement", name());
        }
    }

    public boolean validContext() {
        Minecraft mc = Minecraft.getMinecraft();
        if (openedWorld == null || mc.theWorld != openedWorld || mc.thePlayer == null) return false;
        switch (kind) {
            case SCHEMATIC: return true;
            case AREA: return AreaSelections.available(library) && library.contains(area) && SchematicaPlus.proxy.isSaveEnabled;
            default: return schematic != null && ClientProxy.loadedSchematics.contains(schematic);
        }
    }

    private int[] geometry() {
        if (area != null) {
            int[] bounds = new int[area.boxes().size() * 6];
            int i = 0;
            for (SchematicRegion region : area.regions()) {
                bounds[i++] = region.minX; bounds[i++] = region.minY; bounds[i++] = region.minZ;
                bounds[i++] = region.maxX; bounds[i++] = region.maxY; bounds[i++] = region.maxZ;
            }
            return bounds;
        }
        return new int[] {schematic.position.x, schematic.position.y, schematic.position.z,
            schematic.getWidth(), schematic.getHeight(), schematic.getLength(),
            renderLayers && schematic.isRenderingLayer ? schematic.renderingLayer : -1};
    }

    public boolean geometryChanged() {
        if (kind == Kind.SCHEMATIC) return false;
        if (renderLayers && layerRevision != RenderLayerSettings.RANGE.revision()) return true;
        if (area != null) return !Arrays.equals(geometry, geometry());
        return placementEnabled != schematic.isEnabled() || !Arrays.equals(geometry, geometry()) || source != schematic.getSchematic()
            || !transforms.equals(schematic.transformOperations);
    }

    /** reCreateMaterialList: starts a new count; the old entries are dropped. */
    public void refresh() {
        Minecraft mc = Minecraft.getMinecraft();
        scan = null;
        hasResult = false;
        skipped = 0; unverified = 0; scanError = "";
        model.setEntries(Collections.<MaterialListModel.Entry<MaterialItemKey>>emptyList());
        revision++;
        if (!validContext()) return;
        if (kind == Kind.SCHEMATIC) {
            scan = new SchematicFileMaterialScan(schematic, fileRegions, mc.thePlayer);
            return;
        }
        geometry = geometry();
        layerRevision = RenderLayerSettings.RANGE.revision();
        if (area != null) {
            try { scan = new AreaMaterialScan(area.regions(), openedWorld, mc.thePlayer, renderLayers); }
            catch (IllegalArgumentException error) { scanError = UiTranslations.format("schematica.ui.area.analysis_invalid"); }
        } else {
            source = schematic.getSchematic();
            placementEnabled = schematic.isEnabled();
            transforms = new ArrayList<>(schematic.transformOperations);
            scan = new MaterialScan(schematic, openedWorld, mc.thePlayer, renderLayers);
        }
    }

    /** Advances the count and the available item counts; runs at most once per client tick. */
    public void tick() {
        if (lastTick == MaterialLists.tickCount()) return;
        lastTick = MaterialLists.tickCount();
        if (!validContext()) {
            if (scan != null || hasResult) refresh();
            return;
        }
        if (scan == null && !hasResult && scanError.isEmpty() || geometryChanged()) refresh();
        if (scan != null) {
            scan.step();
            if (scan.done()) {
                model.setEntries(scan.result());
                skipped = scan.skipped();
                unverified = scan.unverified();
                scan = null;
                hasResult = true;
                revision++;
            }
        } else if (++ticks % 20 == 0 && MaterialScan.updateAvailable(model.entries(), Minecraft.getMinecraft().thePlayer)) revision++;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", renderLayers ? "render_layers" : "all");
        json.addProperty("sort_criteria", model.sort() == MaterialListModel.Sort.NAME ? "NAME" : "COUNT_" + model.sort().name());
        json.addProperty("sort_reverse", !model.descending());
        json.addProperty("hide_available", model.hideAvailable());
        json.addProperty("multiplier", model.multiplier());
        return json;
    }

    public void fromJson(JsonObject json) {
        if (json == null) return;
        try {
            if (json.has("type")) renderLayers = "render_layers".equals(json.get("type").getAsString()) && supportsRenderLayers();
            if (json.has("sort_criteria")) {
                String name = json.get("sort_criteria").getAsString().replace("COUNT_", "");
                MaterialListModel.Sort sort = MaterialListModel.Sort.TOTAL;
                for (MaterialListModel.Sort value : MaterialListModel.Sort.values()) if (value.name().equalsIgnoreCase(name)) sort = value;
                model.setSort(sort, !(json.has("sort_reverse") && json.get("sort_reverse").getAsBoolean()));
            }
            if (json.has("hide_available")) model.setHideAvailable(json.get("hide_available").getAsBoolean());
            if (json.has("multiplier")) model.setMultiplier(json.get("multiplier").getAsInt());
        } catch (RuntimeException ignored) {}
    }
}

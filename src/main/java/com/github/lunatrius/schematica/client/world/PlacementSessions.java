package com.github.lunatrius.schematica.client.world;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.lunatrius.schematica.api.SchematicOrigin;
import com.github.lunatrius.schematica.client.renderer.RendererSchematicGlobal;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.util.FileUtils;
import com.github.lunatrius.schematica.util.PlusDataFiles;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

public final class PlacementSessions {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting()
        .create();
    private static final Type loadedSchematicsDataType = new TypeToken<Map<String, List<LoadedSchematicEntry>>>() {}.getType();

    private PlacementSessions() {}

    /** Persistence entry for saving/restoring loaded schematics across sessions. */
    private static class LoadedSchematicEntry {
        public String displayName;
        public String filename;
        public String directory;
        public int X, Y, Z;
        public int RotationX, RotationY, RotationZ;
        public int FlipX, FlipY, FlipZ;
        public boolean isActive;
        public List<String> transforms;
        public Boolean visible, entities, blockNBT, sourceOnly, layerMode;
        public Integer layer;
        public int[] origin;
        public JsonObject subregions;
        public JsonObject placementSettings;
        public JsonObject grid;
        public JsonObject materialList;
        public Integer bb_color;
        public JsonObject fingerprint;

        LoadedSchematicEntry() {}
    }

    /** Saves the list of currently loaded schematics for the given world/server. */
    public static void save(String worldServerName) {
        if (worldServerName == null || worldServerName.isEmpty()) return;
        try {
            File file = PlusDataFiles.file("LoadedSchematics.json");
            Map<String, List<LoadedSchematicEntry>> allData;
            if (file.exists()) {
                try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                    allData = gson.fromJson(reader, loadedSchematicsDataType);
                } catch (Exception e) {
                    throw new IOException("Existing schematic settings are unreadable; preserving the file", e);
                }
            } else {
                allData = new HashMap<>();
            }
            if (allData == null) allData = new HashMap<>();

            List<LoadedSchematicEntry> entries = new ArrayList<>();
            for (SchematicWorld sw : Placements.loaded) {
                if (Placements.LIBRARY.sourceOf(sw).memory() || sw.projectVersion) continue;
                LoadedSchematicEntry entry = new LoadedSchematicEntry();
                entry.displayName = sw.name;
                entry.filename = sw.sourceFilename;
                entry.directory = sw.sourceDirectory != null ? sw.sourceDirectory.getAbsolutePath() : "";
                entry.X = sw.position.x;
                entry.Y = sw.position.y;
                entry.Z = sw.position.z;
                entry.origin = sw.originPosition().coordinates();
                entry.subregions = sw.subregions() == null ? null : sw.subregions().toJson();
                entry.placementSettings = sw.placementSettings().toJson();
                entry.grid = sw.grid.isEnabled() || !sw.grid.isAtDefaultValues() ? sw.grid.toJson() : null;
                entry.materialList = sw.materialList != null ? sw.materialList.toJson() : sw.materialListData;
                entry.bb_color = sw.boxColor;
                entry.fingerprint = sw.fingerprint == null ? null : sw.fingerprint.toJson();
                entry.RotationX = sw.rotationStateX;
                entry.RotationY = sw.rotationStateY;
                entry.RotationZ = sw.rotationStateZ;
                entry.FlipX = sw.flipStateX;
                entry.FlipY = sw.flipStateY;
                entry.FlipZ = sw.flipStateZ;
                entry.isActive = (sw == Placements.selected);
                entry.transforms = new ArrayList<>(sw.transformOperations);
                entry.visible = sw.isRendering;
                entry.entities = sw.isRenderingEntities;
                entry.blockNBT = sw.isPastingBlockNBT;
                entry.layerMode = sw.isRenderingLayer;
                entry.layer = sw.renderingLayer;
                entries.add(entry);
            }
            for (SchematicLibrary.Source<SchematicSourceData> source : Placements.LIBRARY.sources()) {
                boolean placed = false;
                for (SchematicWorld world : Placements.loaded) if (Placements.LIBRARY.sourceOf(world) == source) placed = true;
                if (placed || source.memory()) continue;
                LoadedSchematicEntry entry = new LoadedSchematicEntry();
                entry.filename = source.file().getName();
                entry.directory = source.file().getParentFile().getAbsolutePath();
                entry.sourceOnly = true;
                entries.add(entry);
            }
            allData.put(worldServerName, entries);

            FileUtils.writeUtf8Atomically(file,
                gson.toJson(allData, loadedSchematicsDataType));
            Reference.logger.info("Saved {} loaded schematics for '{}'", entries.size(), worldServerName);
        } catch (Exception e) {
            Reference.logger.error("Failed to save loaded schematics", e);
        }
    }

    /** Restores previously loaded schematics for the given world/server. */
    public static void restore(String worldServerName) {
        if (worldServerName == null || worldServerName.isEmpty()) return;
        try {
            File file = PlusDataFiles.file("LoadedSchematics.json");
            if (!file.exists()) return;

            Map<String, List<LoadedSchematicEntry>> allData;
            try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                allData = gson.fromJson(reader, loadedSchematicsDataType);
            }
            if (allData == null || !allData.containsKey(worldServerName)) return;

            List<LoadedSchematicEntry> entries = allData.get(worldServerName);
            if (entries == null || entries.isEmpty()) return;

            SchematicWorld activeWorld = null;
            for (LoadedSchematicEntry entry : entries) {
                try {
                    if (entry == null || entry.filename == null || entry.filename.isEmpty()) continue;
                    File dir = entry.directory != null && !entry.directory.isEmpty() ? new File(entry.directory) : ConfigurationHandler.schematicDirectory;
                    SchematicLibrary.Source<SchematicSourceData> source = Placements.loadSource(new File(dir, entry.filename));
                    if (Boolean.TRUE.equals(entry.sourceOnly)) continue;
                    SchematicWorld world = Placements.LIBRARY.create(source, (data, previous) -> {
                        SchematicWorld restored = Placements.instantiate(source, data);
                        if (entry.displayName != null && !entry.displayName.trim().isEmpty()) restored.name = entry.displayName;
                        List<String> operations = entry.transforms;
                        if (operations == null) {
                            operations = new ArrayList<>();
                            int[] values = {entry.RotationX, entry.RotationY, entry.RotationZ, entry.FlipX, entry.FlipY, entry.FlipZ};
                            String[] axes = {"X", "Y", "Z", "x", "y", "z"};
                            for (int i = 0; i < values.length; i++) {
                                for (int j = 0; j < Math.floorMod(values[i], i < 3 ? 4 : 2); j++) operations.add(axes[i]);
                            }
                        }
                        PlacementState.applyTransforms(restored, operations);
                        restored.restoreSubregions(entry.subregions);
                        SchematicOrigin minimum = restored.getSchematic().getOrigin()
                            .restoredMinimum(entry.X, entry.Y, entry.Z, entry.origin);
                        restored.position.set(minimum.x, minimum.y, minimum.z);
                        if (entry.visible != null) restored.isRendering = entry.visible;
                        if (entry.entities != null) restored.isRenderingEntities = entry.entities;
                        if (entry.blockNBT != null) restored.isPastingBlockNBT = entry.blockNBT;
                        restored.isRenderingLayer = Boolean.TRUE.equals(entry.layerMode);
                        if (entry.layer != null) restored.renderingLayer = Math.max(0, Math.min(entry.layer, restored.getHeight() - 1));
                        restored.setPlacementSettings(PlacementSettings.fromJson(entry.placementSettings));
                        restored.grid.fromJson(entry.grid);
                        restored.materialListData = entry.materialList;
                        restored.boxColor = entry.bb_color;
                        restored.fingerprint = WorldFingerprint.fromJson(entry.fingerprint);
                        return restored;
                    });
                    RendererSchematicGlobal.INSTANCE.createRendererSchematicChunks(world);
                    if (entry.isActive) activeWorld = world;
                } catch (Exception e) {
                    Reference.logger.warn("Could not restore schematic session entry", e);
                }
            }
            Placements.select(activeWorld);

            Reference.logger.info("Restored {} schematics for '{}'", Placements.loaded.size(), worldServerName);
        } catch (Exception e) {
            Reference.logger.error("Failed to restore loaded schematics", e);
        }
    }
}

// SPDX-License-Identifier: LGPL-3.0-only
// Litematica SchematicProject, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.projects;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.lunatrius.schematica.api.SchematicRegion;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Area;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;
import com.github.lunatrius.schematica.util.FileUtils;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** A versioned area: its own area selections, a project origin and saved schematic versions in one directory. */
public final class SchematicProject {
    static final String FILE_EXTENSION = ".schemplus";

    private final List<SchematicVersion> versions = new ArrayList<>();
    private final File directory;
    private File projectFile;
    private Vector3i origin = new Vector3i();
    private String name = "unnamed";
    private AreaSelectionLibrary selections;
    private List<SchematicRegion> lastSeenArea = new ArrayList<>();
    private int currentVersionId = -1;
    private int lastCheckedOutVersion = -1;
    private int lastPastedVersion = -1;
    private boolean saveInProgress;
    private boolean dirty = true;
    private SchematicWorld currentPlacement;

    SchematicProject(File directory, File projectFile) {
        this.directory = directory;
        this.projectFile = projectFile;
        this.selections = new AreaSelectionLibrary();
    }

    /** A new project named after its file, with one selection box at the origin. */
    static SchematicProject create(File directory, String name, Vector3i origin) {
        SchematicProject project = new SchematicProject(directory, new File(directory, name + ".json"));
        project.name = name;
        project.origin = origin.clone();
        Area area = project.selections.create(name, origin, origin);
        project.selections.select(area);
        Area simple = project.selections.simpleSelection();
        project.selections.rename(simple, name);
        project.selections.setPoints(simple, origin, origin);
        return project;
    }

    public File directory() { return directory; }
    public File projectFile() { return projectFile; }
    public String name() { return name; }
    public Vector3i origin() { return origin.clone(); }
    public AreaSelectionLibrary selections() { return selections; }
    public int versionCount() { return versions.size(); }
    public int currentVersionId() { return currentVersionId; }
    public List<SchematicVersion> versions() { return Collections.unmodifiableList(new ArrayList<>(versions)); }
    public SchematicWorld currentPlacement() { return currentPlacement; }
    boolean saveInProgress() { return saveInProgress; }

    public SchematicVersion currentVersion() {
        return currentVersionId >= 0 && currentVersionId < versions.size() ? versions.get(currentVersionId) : null;
    }

    public String currentVersionName() {
        SchematicVersion version = currentVersion();
        Area area = selections.selected();
        return version != null ? version.name : area != null ? area.name() : name;
    }

    public String currentVersionDescription() {
        SchematicVersion version = currentVersion();
        return version != null ? version.description : "";
    }

    /** Renames the project and moves its file; returns an error key and argument, or null. */
    String[] setName(String value) {
        File next = new File(directory, value + ".json");
        if (next.exists()) return new String[] {"litematica.error.schematic_projects.failed_to_rename_project_file_exists", value};
        try {
            if (projectFile.exists()) Files.move(projectFile.toPath(), next.toPath(), StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException | RuntimeException e) {
            return new String[] {"litematica.error.schematic_projects.failed_to_rename_project_file_exception", next.getAbsolutePath()};
        }
        name = value;
        projectFile = next;
        Area normal = selections.normalSelection();
        if (normal != null) selections.rename(normal, value);
        selections.rename(selections.simpleSelection(), value);
        dirty = true;
        return null;
    }

    /** Moves the project origin, its selections and the current version placement together. */
    void setOrigin(Vector3i value) {
        int dx = value.x - origin.x, dy = value.y - origin.y, dz = value.z - origin.z;
        for (Area area : new Area[] {selections.normalSelection(), selections.simpleSelection()}) {
            try {
                if (area != null) selections.moveEntire(area, dx, dy, dz);
            } catch (IllegalArgumentException | ArithmeticException ignored) {
                // A selection that would leave the world keeps its position.
            }
        }
        lastSeenArea = new ArrayList<>();
        origin = value.clone();
        lastPastedVersion = -1;
        SchematicVersion version = currentVersion();
        if (version != null && currentPlacement != null) SchematicProjects.moveTo(currentPlacement, placementOrigin(version));
        dirty = true;
    }

    Vector3i placementOrigin(SchematicVersion version) {
        Vector3i offset = version.areaOffset();
        return new Vector3i(origin.x + offset.x, origin.y + offset.y, origin.z + offset.z);
    }

    void markDirty() { dirty = true; }

    boolean cycleVersion(int amount) {
        return currentVersionId >= 0 && switchVersion(currentVersionId + amount, true);
    }

    boolean switchVersion(int version, boolean createPlacement) {
        if (version == currentVersionId || version < 0 || version >= versions.size()) return false;
        currentVersionId = version;
        dirty = true;
        if (createPlacement) createAndAddPlacement();
        return true;
    }

    boolean switchVersion(SchematicVersion version, boolean createPlacement) {
        int index = versions.indexOf(version);
        return index >= 0 && version != currentVersion() && switchVersion(index, createPlacement);
    }

    private void createAndAddPlacement() {
        SchematicVersion version = currentVersion();
        if (version == null || currentVersionId == lastCheckedOutVersion) return;
        removeCurrentPlacement();
        String fileName = version.fileName;
        if (!com.github.lunatrius.schematica.client.gui.browser.SchematicBrowserModel.supported(fileName)) fileName += FILE_EXTENSION;
        currentPlacement = SchematicProjects.place(new File(directory, fileName), version.name, placementOrigin(version));
        lastCheckedOutVersion = currentVersionId;
    }

    void removeCurrentPlacement() {
        if (currentPlacement != null) SchematicProjects.remove(currentPlacement);
        currentPlacement = null;
        lastCheckedOutVersion = -1;
    }

    /** AreaSelection.fromPlacement: the world boxes of the current placement's enabled regions. */
    void cacheCurrentAreaFromPlacement() {
        if (currentPlacement == null) return;
        lastSeenArea = SchematicProjects.boxes(currentPlacement);
        dirty = true;
    }

    List<SchematicRegion> lastSeenArea() { return Collections.unmodifiableList(lastSeenArea); }
    int lastPastedVersion() { return lastPastedVersion; }

    void pasted() {
        lastPastedVersion = currentVersionId;
        dirty = true;
    }

    String nextFileName() {
        for (int version = 1; version < 10_000_000; version++) {
            String file = name + "_" + String.format("%05d", version) + FILE_EXTENSION;
            if (!new File(directory, file).exists()) return file;
        }
        return name + "_error";
    }

    /** checkCanSaveOrPrintError: null when a new version can be saved, else the error key. */
    String saveError() {
        if (saveInProgress) return "litematica.error.schematic_projects.save_already_in_progress";
        if (directory == null || !directory.isDirectory()) return "litematica.error.schematic_projects.invalid_project_directory";
        Area area = selections.selected();
        if (area == null || area.boxes().isEmpty()) return "litematica.error.schematic_projects.empty_selection";
        return null;
    }

    void beginSave() {
        saveInProgress = true;
        dirty = true;
    }

    void saveFailed() { saveInProgress = false; }

    /** Records a saved version, checks it out and remembers its area. */
    SchematicVersion addVersion(String versionName, String fileName, String description, Vector3i areaOffset, long time, boolean createPlacement) {
        SchematicVersion version = new SchematicVersion(versionName, fileName, description, areaOffset, versions.size() + 1, time);
        versions.add(version);
        switchVersion(versions.size() - 1, createPlacement);
        cacheCurrentAreaFromPlacement();
        saveInProgress = false;
        return version;
    }

    public boolean saveToFile() {
        if (!dirty) return true;
        try {
            String json = new GsonBuilder().setPrettyPrinting().create().toJson(toJson());
            FileUtils.writeUtf8Atomically(projectFile, json);
            dirty = false;
            return true;
        } catch (IOException | RuntimeException e) {
            com.github.lunatrius.schematica.reference.Reference.logger.error("Could not save the schematic project {}", projectFile, e);
            return false;
        }
    }

    JsonObject toJson() {
        JsonObject data = new JsonObject();
        data.addProperty("name", name);
        data.add("origin", pointJson(origin));
        data.addProperty("current_version_id", currentVersionId);
        data.addProperty("last_pasted_version", lastPastedVersion);
        data.add("selections", selections.toJson());
        JsonArray seen = new JsonArray();
        for (SchematicRegion box : lastSeenArea) {
            JsonObject entry = new JsonObject();
            entry.addProperty("name", box.name);
            entry.add("min", pointJson(new Vector3i(box.minX, box.minY, box.minZ)));
            entry.add("max", pointJson(new Vector3i(box.maxX, box.maxY, box.maxZ)));
            seen.add(entry);
        }
        data.add("last_seen_area", seen);
        JsonArray list = new JsonArray();
        for (SchematicVersion version : versions) list.add(version.toJson());
        if (list.size() > 0) data.add("versions", list);
        return data;
    }

    static SchematicProject fromJson(JsonObject data, File projectFile, boolean createPlacement) {
        Vector3i origin = point(data.get("origin"));
        if (origin == null || !SchematicVersion.string(data, "name") || !SchematicVersion.number(data, "current_version_id")) return null;
        File file = projectFile.getAbsoluteFile();
        try { file = projectFile.getCanonicalFile(); } catch (IOException ignored) {}
        SchematicProject project = new SchematicProject(file.getParentFile(), file);
        project.name = data.get("name").getAsString();
        project.origin = origin;
        JsonElement selections = data.get("selections");
        if (selections != null && selections.isJsonObject()) project.selections = AreaSelectionLibrary.fromJson(selections.getAsJsonObject());
        else project.selections = create(file.getParentFile(), project.name, origin).selections;
        JsonElement seen = data.get("last_seen_area");
        if (seen != null && seen.isJsonArray()) {
            for (JsonElement element : seen.getAsJsonArray()) {
                if (!element.isJsonObject()) continue;
                JsonObject box = element.getAsJsonObject();
                Vector3i min = point(box.get("min")), max = point(box.get("max"));
                if (min == null || max == null) continue;
                String boxName = SchematicVersion.string(box, "name") ? box.get("name").getAsString() : project.name;
                project.lastSeenArea.add(new SchematicRegion(boxName, min.x, min.y, min.z, max.x, max.y, max.z));
            }
        }
        JsonElement list = data.get("versions");
        if (list != null && list.isJsonArray()) {
            for (JsonElement element : list.getAsJsonArray()) {
                SchematicVersion version = element.isJsonObject() ? SchematicVersion.fromJson(element.getAsJsonObject()) : null;
                if (version != null) project.versions.add(version);
            }
        }
        if (SchematicVersion.number(data, "last_pasted_version")) project.lastPastedVersion = data.get("last_pasted_version").getAsInt();
        int id = project.versions.size() - 1;
        int stored = data.get("current_version_id").getAsInt();
        if (stored >= 0 && stored < project.versions.size()) id = stored;
        project.switchVersion(id, createPlacement);
        project.dirty = false;
        return project;
    }

    static JsonObject pointJson(Vector3i point) {
        JsonObject data = new JsonObject();
        data.addProperty("x", point.x);
        data.addProperty("y", point.y);
        data.addProperty("z", point.z);
        return data;
    }

    static Vector3i point(JsonElement element) {
        if (element == null || !element.isJsonObject()) return null;
        JsonObject data = element.getAsJsonObject();
        if (!SchematicVersion.number(data, "x") || !SchematicVersion.number(data, "y") || !SchematicVersion.number(data, "z")) return null;
        return new Vector3i(data.get("x").getAsInt(), data.get("y").getAsInt(), data.get("z").getAsInt());
    }
}

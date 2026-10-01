// SPDX-License-Identifier: LGPL-3.0-only
// Litematica SchematicProjectsManager, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.projects;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.api.SchematicRegion;
import com.github.lunatrius.schematica.client.gui.projects.GuiSchematicProjectManager;
import com.github.lunatrius.schematica.client.gui.projects.GuiSchematicProjectsBrowser;
import com.github.lunatrius.schematica.client.printer.SchematicPrinter;
import com.github.lunatrius.schematica.client.renderer.RendererSchematicGlobal;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Area;
import com.github.lunatrius.schematica.client.selection.AreaSelections;
import com.github.lunatrius.schematica.client.world.SchematicLibrary;
import com.github.lunatrius.schematica.client.world.SchematicSourceData;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.client.world.SubRegionPlacements;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.tool.PlacementDeletionMode;
import com.github.lunatrius.schematica.tool.ToolHandler;
import com.github.lunatrius.schematica.util.FileUtils;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** The open schematic project (Schematic VCS). Its selections replace the world's while it is open. */
public final class SchematicProjects {
    private static final long MAX_FILE_SIZE = 16L * 1024 * 1024;
    private static SchematicProject current;
    private static SchematicLibrary.Source<SchematicSourceData> ownedSource;

    private SchematicProjects() {}

    private static Minecraft mc() { return Minecraft.getMinecraft(); }

    /** The project, unless none is open or unhideSchematicVCS is off. */
    public static SchematicProject current() { return hasProjectOpen() ? current : null; }

    public static boolean hasProjectOpen() { return current != null && ConfigurationHandler.unhideSchematicVCS; }

    public static void message(EnumChatFormatting color, String key, Object... arguments) {
        if (mc().thePlayer == null) return;
        ChatComponentTranslation text = new ChatComponentTranslation(key, arguments);
        text.getChatStyle().setColor(color);
        mc().thePlayer.addChatMessage(text);
    }

    /** openSchematicProjectsGui: the manager for an open project, else the project browser. */
    public static boolean openGui(GuiScreen parent) {
        if (!ConfigurationHandler.unhideSchematicVCS) {
            message(EnumChatFormatting.GOLD, "litematica.message.warning.schematic_projects_hidden");
            return true;
        }
        mc().displayGuiScreen(current != null ? new GuiSchematicProjectManager(parent, current) : new GuiSchematicProjectsBrowser(parent));
        return true;
    }

    /** Keeps the active area selections in step with the open project and the unhideSchematicVCS option. */
    public static void syncSelections() {
        SchematicProject project = current();
        AreaSelections.setOverride(project == null ? null : project.selections(), project == null ? null : () -> {
            project.markDirty();
            return project.saveToFile();
        });
    }

    public static void create(File directory, String name) {
        close();
        net.minecraft.entity.player.EntityPlayer player = mc().thePlayer;
        Vector3i origin = player == null ? new Vector3i() : new Vector3i(MathHelper.floor_double(player.posX),
            MathHelper.floor_double(player.boundingBox.minY), MathHelper.floor_double(player.posZ));
        current = SchematicProject.create(directory, name, origin);
        current.saveToFile();
        syncSelections();
    }

    public static boolean open(File file) {
        close();
        current = load(file, true);
        if (current == null) {
            message(EnumChatFormatting.RED, "litematica.error.schematic_projects.failed_to_load_project");
            return false;
        }
        syncSelections();
        return true;
    }

    /** loadProjectFromFile: a project file, optionally checking out its current version as a placement. */
    public static SchematicProject load(File file, boolean createPlacement) {
        if (file == null || !file.isFile() || !file.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".json") || file.length() > MAX_FILE_SIZE) return null;
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            JsonElement data = new JsonParser().parse(reader);
            return data != null && data.isJsonObject() ? SchematicProject.fromJson(data.getAsJsonObject(), file, createPlacement) : null;
        } catch (IOException | RuntimeException e) {
            Reference.logger.warn("Could not read the schematic project {}", file, e);
            return null;
        }
    }

    public static void close() {
        if (current == null) return;
        current.saveToFile();
        current.removeCurrentPlacement();
        current = null;
        syncSelections();
    }

    /** Forgets the project without touching placements, as the world's state is being discarded. */
    public static void clear() {
        current = null;
        ownedSource = null;
    }

    public static void save() {
        if (current != null) current.saveToFile();
    }

    public static boolean cycleVersion(int amount) {
        if (current == null) {
            message(EnumChatFormatting.RED, "litematica.error.schematic_projects.no_project_open");
            return false;
        }
        boolean changed = current.cycleVersion(amount);
        if (changed) current.saveToFile();
        return changed;
    }

    public static boolean switchVersion(SchematicVersion version) {
        boolean changed = current != null && current.switchVersion(version, true);
        if (changed) current.saveToFile();
        return changed;
    }

    public static void setOrigin(Vector3i origin) {
        if (current == null) return;
        current.setOrigin(origin);
        AreaSelections.apply();
        current.saveToFile();
    }

    public static String[] rename(String name) {
        String[] error = current == null ? null : current.setName(name);
        if (current != null && error == null) {
            AreaSelections.apply();
            current.saveToFile();
        }
        return error;
    }

    /** commitNewVersion: saves the project's current selection as the next numbered .schemplus version. */
    public static boolean commitNewVersion(String name, String description) {
        SchematicProject project = current;
        if (project == null) {
            message(EnumChatFormatting.RED, "litematica.error.schematic_projects.no_project_open");
            return false;
        }
        String error = mc().thePlayer == null ? "litematica.error.schematic_projects.null_player"
            : !SchematicaPlus.proxy.isSaveEnabled ? "schematica.ui.save.disabled" : project.saveError();
        if (error != null) {
            message(EnumChatFormatting.RED, error);
            return false;
        }
        AreaSelections.capture();
        Area area = project.selections().selected();
        String fileName = project.nextFileName();
        Vector3i areaOrigin = area.origin(), origin = project.origin();
        Vector3i offset = new Vector3i(areaOrigin.x - origin.x, areaOrigin.y - origin.y, areaOrigin.z - origin.z);
        boolean queued;
        try {
            queued = SchematicaPlus.proxy.saveSchematic(mc().thePlayer, project.directory(), fileName, mc().theWorld, area.snapshot(), file -> {
                if (project != current) return;
                if (file == null) {
                    project.saveFailed();
                    message(EnumChatFormatting.RED, "litematica.message.error.schematic_save_failed", fileName);
                    return;
                }
                SchematicVersion version = project.addVersion(name, file.getName(), description, offset, System.currentTimeMillis(), true);
                project.saveToFile();
                message(EnumChatFormatting.GREEN, "litematica.message.schematic_projects.version_saved", version.version, version.name);
                if (mc().currentScreen instanceof GuiSchematicProjectManager) ((GuiSchematicProjectManager) mc().currentScreen).versionsChanged();
            });
        } catch (IllegalArgumentException | ArithmeticException e) {
            queued = false;
        }
        if (!queued) {
            message(EnumChatFormatting.RED, "schematica.ui.save.failed");
            return false;
        }
        project.beginSave();
        project.saveToFile();
        return true;
    }

    /** pasteToWorld: ENTIRE_VOLUME first clears the version's area; other modes paste over the world directly. */
    public static boolean pasteCurrentVersionToWorld() {
        SchematicProject project = current();
        if (project == null || project.currentPlacement() == null) return false;
        if (mc().thePlayer == null || !mc().thePlayer.capabilities.isCreativeMode) {
            message(EnumChatFormatting.RED, "litematica.error.generic.creative_mode_only");
            return true;
        }
        project.cacheCurrentAreaFromPlacement();
        if (ConfigurationHandler.schematicVcsDeleteMode == PlacementDeletionMode.ENTIRE_VOLUME) {
            ToolHandler.deleteBoxes(mc().thePlayer, project.lastSeenArea(), () -> pasteCurrentPlacement(project));
        } else {
            if (project.lastPastedVersion() < 0 || project.lastPastedVersion() >= project.versionCount()) {
                mc().thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(EnumChatFormatting.RED + "No previous pasted version known, skipping delete"));
            }
            pasteCurrentPlacement(project);
        }
        return true;
    }

    private static void pasteCurrentPlacement(SchematicProject project) {
        if (project != current || project.currentPlacement() == null) return;
        project.pasted();
        project.saveToFile();
        ToolHandler.paste(mc().thePlayer, project.currentPlacement(), null);
    }

    public static boolean deleteLastSeenArea() {
        SchematicProject project = current();
        if (project == null) return false;
        if (project.lastSeenArea().isEmpty()) {
            message(EnumChatFormatting.RED, "litematica.message.error.empty_area_selection");
            return true;
        }
        ToolHandler.deleteBoxes(mc().thePlayer, project.lastSeenArea(), null);
        return true;
    }

    public static boolean deleteBlocksByPlacement() {
        SchematicProject project = current();
        if (project == null) return false;
        if (project.currentPlacement() != null) ToolHandler.deleteByPlacement(mc().thePlayer, project.currentPlacement(), ConfigurationHandler.schematicVcsDeleteMode);
        return true;
    }

    // --- Version placements: transient placements that are never saved in the world session.

    static SchematicWorld place(File file, String name, Vector3i origin) {
        try {
            boolean loaded = false;
            for (SchematicLibrary.Source<SchematicSourceData> source : ClientProxy.SCHEMATICS.sources()) {
                if (source.file().getCanonicalFile().equals(file.getCanonicalFile())) loaded = true;
            }
            SchematicLibrary.Source<SchematicSourceData> source = ClientProxy.loadSource(file);
            ownedSource = loaded ? null : source;
            SchematicWorld world = ClientProxy.createPlacement(source);
            world.name = name;
            world.projectVersion = true;
            world.moveOriginTo(origin.x, origin.y, origin.z);
            RendererSchematicGlobal.INSTANCE.createRendererSchematicChunks(world);
            SchematicPrinter.INSTANCE.refresh();
            return world;
        } catch (IOException | RuntimeException e) {
            Reference.logger.warn("Could not load the schematic project version {}", file, e);
            message(EnumChatFormatting.RED, "litematica.error.schematic_projects.failed_to_load_schematic");
            return null;
        }
    }

    static void remove(SchematicWorld world) {
        if (!ClientProxy.loadedSchematics.contains(world)) return;
        SchematicLibrary.Source<SchematicSourceData> source = ClientProxy.SCHEMATICS.sourceOf(world);
        ClientProxy.removePlacement(world);
        boolean used = false;
        for (SchematicWorld other : ClientProxy.loadedSchematics) if (ClientProxy.SCHEMATICS.sourceOf(other) == source) used = true;
        if (source != null && source == ownedSource && !used) ClientProxy.unloadSource(source);
        if (source == ownedSource) ownedSource = null;
    }

    static void moveTo(SchematicWorld world, Vector3i origin) {
        world.moveOriginTo(origin.x, origin.y, origin.z);
        SchematicPrinter.INSTANCE.refresh();
    }

    /** AreaSelection.fromPlacement: the world boxes of a placement's enabled sub-regions. */
    static List<SchematicRegion> boxes(SchematicWorld world) {
        List<SchematicRegion> boxes = new ArrayList<>();
        if (world.subregions() != null) {
            for (SubRegionPlacements.Region region : world.subregions().regions()) {
                if (region.enabled) boxes.add(world.subregionBounds(region.name()));
            }
        } else {
            boxes.add(new SchematicRegion(world.name, world.position.x, world.position.y, world.position.z,
                world.position.x + world.getWidth() - 1, world.position.y + world.getHeight() - 1, world.position.z + world.getLength() - 1));
        }
        return boxes;
    }

    // --- Session: the open project per world, like the per-world schematic_projects_manager data.

    private static File sessionFile() { return new File(ConfigurationHandler.schematicDirectory, "SchematicProjects.json"); }

    public static void saveSession(String key) {
        if (key == null || key.isEmpty()) return;
        try {
            JsonObject sessions = readSessions();
            if (current != null) sessions.addProperty(key, current.projectFile().getAbsolutePath());
            else sessions.remove(key);
            FileUtils.writeUtf8Atomically(sessionFile(), new GsonBuilder().setPrettyPrinting().create().toJson(sessions));
        } catch (IOException | RuntimeException e) {
            Reference.logger.error("Could not save the open schematic project", e);
        }
        save();
    }

    public static void restoreSession(String key) {
        clear();
        if (key == null || key.isEmpty()) return;
        try {
            JsonElement path = readSessions().get(key);
            if (path != null && path.isJsonPrimitive()) current = load(new File(path.getAsString()), true);
        } catch (IOException | RuntimeException e) {
            Reference.logger.error("Could not restore the open schematic project", e);
        }
        syncSelections();
    }

    private static JsonObject readSessions() throws IOException {
        File file = sessionFile();
        if (!file.exists()) return new JsonObject();
        if (file.length() > MAX_FILE_SIZE) throw new IOException("Schematic project sessions exceed 16 MiB");
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            JsonElement data = new JsonParser().parse(reader);
            if (data == null || !data.isJsonObject()) throw new IOException("Schematic project sessions are unreadable");
            return data.getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IOException("Schematic project sessions are unreadable; preserving the file", e);
        }
    }
}

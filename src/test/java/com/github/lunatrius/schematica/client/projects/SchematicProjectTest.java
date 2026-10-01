package com.github.lunatrius.schematica.client.projects;

import java.io.File;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

import com.github.lunatrius.schematica.api.SchematicRegion;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary;
import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;
import com.github.lunatrius.schematica.tool.PlacementDeletionMode;
import com.google.gson.JsonObject;

public class SchematicProjectTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    private SchematicProject project() {
        return SchematicProject.create(temporary.getRoot(), "house", new Vector3i(10, 64, -5));
    }

    @Test public void newProjectsSelectOneBoxAtTheOriginInBothModes() {
        SchematicProject project = project();
        AreaSelectionLibrary selections = project.selections();
        assertEquals("house", selections.selected().name());
        assertEquals(1, selections.selected().boxes().size());
        assertEquals(new Vector3i(10, 64, -5), selections.selected().first());
        assertEquals("house", selections.simpleSelection().name());
        assertEquals(new Vector3i(10, 64, -5), selections.simpleSelection().first());
        assertNull(project.currentVersion());
        assertEquals("house", project.currentVersionName());
        assertNull(project.saveError());
    }

    @Test public void versionFilesAreNumberedAndSkipExistingNames() throws Exception {
        SchematicProject project = project();
        assertEquals("house_00001.schemplus", project.nextFileName());
        Files.write(new File(temporary.getRoot(), "house_00001.schemplus").toPath(), new byte[0]);
        assertEquals("house_00002.schemplus", project.nextFileName());
    }

    @Test public void committedVersionsAreCheckedOutAndBlockConcurrentSaves() {
        SchematicProject project = project();
        project.beginSave();
        assertEquals("litematica.error.schematic_projects.save_already_in_progress", project.saveError());
        SchematicVersion first = project.addVersion("v1", "house_00001.schemplus", "first", new Vector3i(1, 0, 2), 100L, false);
        assertNull(project.saveError());
        assertEquals(1, first.version);
        assertSame(first, project.currentVersion());
        project.addVersion("v2", "house_00002.schemplus", "", new Vector3i(), 200L, false);
        assertEquals(1, project.currentVersionId());
        assertTrue(project.switchVersion(0, false));
        assertFalse(project.switchVersion(0, false));
        assertFalse(project.switchVersion(5, false));
        assertEquals(new Vector3i(11, 64, -3), project.placementOrigin(first));
    }

    @Test public void jsonRoundTripKeepsVersionsSelectionsAndPasteState() {
        SchematicProject project = project();
        project.selections().setPoints(project.selections().selected(), new Vector3i(10, 64, -5), new Vector3i(14, 70, 0));
        project.addVersion("v1", "house_00001.schemplus", "line one\nline two", new Vector3i(0, 0, 0), 100L, false);
        project.addVersion("v2", "house_00002.schemplus", "", new Vector3i(2, 0, 0), 200L, false);
        project.switchVersion(0, false);
        project.pasted();
        JsonObject data = project.toJson();
        SchematicProject loaded = SchematicProject.fromJson(data, new File(temporary.getRoot(), "house.json"), false);
        assertNotNull(loaded);
        assertEquals("house", loaded.name());
        assertEquals(new Vector3i(10, 64, -5), loaded.origin());
        assertEquals(2, loaded.versionCount());
        assertEquals(0, loaded.currentVersionId());
        assertEquals(0, loaded.lastPastedVersion());
        assertEquals("line one\nline two", loaded.currentVersion().description);
        assertEquals(new Vector3i(14, 70, 0), loaded.selections().selected().second());
        assertNull(SchematicProject.fromJson(new JsonObject(), new File(temporary.getRoot(), "bad.json"), false));
    }

    @Test public void movingTheOriginMovesBothSelectionsAndForgetsThePastedVersion() {
        SchematicProject project = project();
        project.pasted();
        project.setOrigin(new Vector3i(20, 60, -5));
        assertEquals(new Vector3i(20, 60, -5), project.origin());
        assertEquals(new Vector3i(20, 60, -5), project.selections().selected().first());
        assertEquals(new Vector3i(20, 60, -5), project.selections().simpleSelection().first());
        assertEquals(-1, project.lastPastedVersion());
        assertTrue(project.lastSeenArea().isEmpty());
    }

    @Test public void renamingMovesTheProjectFileAndRefusesExistingNames() throws Exception {
        SchematicProject project = project();
        assertTrue(project.saveToFile());
        assertTrue(new File(temporary.getRoot(), "house.json").isFile());
        assertNull(project.setName("barn"));
        assertEquals("barn", project.name());
        assertTrue(new File(temporary.getRoot(), "barn.json").isFile());
        assertFalse(new File(temporary.getRoot(), "house.json").exists());
        Files.write(new File(temporary.getRoot(), "shed.json").toPath(), new byte[0]);
        assertEquals("litematica.error.schematic_projects.failed_to_rename_project_file_exists", project.setName("shed")[0]);
        assertEquals("barn", project.selections().selected().name());
    }

    @Test public void lastSeenAreaSurvivesSaving() {
        SchematicProject project = project();
        JsonObject data = project.toJson();
        com.google.gson.JsonArray seen = new com.google.gson.JsonArray();
        JsonObject box = new JsonObject();
        box.addProperty("name", "main");
        box.add("min", SchematicProject.pointJson(new Vector3i(0, 1, 2)));
        box.add("max", SchematicProject.pointJson(new Vector3i(3, 4, 5)));
        seen.add(box);
        data.add("last_seen_area", seen);
        SchematicProject loaded = SchematicProject.fromJson(data, new File(temporary.getRoot(), "house.json"), false);
        SchematicRegion region = loaded.lastSeenArea().get(0);
        assertEquals("main", region.name);
        assertEquals(3, region.maxX);
        assertEquals(2, region.minZ);
    }

    @Test public void deletionModesFollowTheUpstreamChecks() {
        assertTrue(PlacementDeletionMode.MATCHING_BLOCK.deletes(false, true));
        assertFalse(PlacementDeletionMode.MATCHING_BLOCK.deletes(false, false));
        assertTrue(PlacementDeletionMode.NON_MATCHING_BLOCK.deletes(false, false));
        assertFalse(PlacementDeletionMode.NON_MATCHING_BLOCK.deletes(true, false));
        assertTrue(PlacementDeletionMode.ANY_SCHEMATIC_BLOCK.deletes(false, false));
        assertTrue(PlacementDeletionMode.NO_SCHEMATIC_BLOCK.deletes(true, false));
        assertFalse(PlacementDeletionMode.NO_SCHEMATIC_BLOCK.deletes(false, true));
        assertTrue(PlacementDeletionMode.ENTIRE_VOLUME.deletes(true, true));
        assertEquals(PlacementDeletionMode.MATCHING_BLOCK, PlacementDeletionMode.parse("bogus"));
    }
}

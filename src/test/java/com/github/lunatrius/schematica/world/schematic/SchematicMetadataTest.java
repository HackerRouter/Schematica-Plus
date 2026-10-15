package com.github.lunatrius.schematica.world.schematic;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.client.world.SchematicSourceData;
import com.github.lunatrius.schematica.client.world.SourceEditor;
import com.github.lunatrius.schematica.client.world.CellState;
import com.github.lunatrius.schematica.world.storage.Schematic;
import com.github.lunatrius.schematica.world.storage.SchematicCopies;

import cpw.mods.fml.common.registry.GameData;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SchematicMetadataTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private static Block block;
    private static final String[] EXTENSIONS = {".schematic", ".schemplus", ".litematic", ".nbt"};

    @BeforeClass public static void setup() throws Exception {
        SchematicFilesTest.register();
        block = GameData.getBlockRegistry().getObject("test:files_block");
    }

    private Schematic sample() {
        Schematic schematic = new Schematic(null, 2, 1, 1);
        schematic.setBlock(0, 0, 0, block, 5);
        schematic.setBlock(1, 0, 0, block, 0);
        return schematic;
    }

    private File save(String name, ISchematic schematic) throws Exception {
        File target = new File(temporary.getRoot(), name);
        LitematicExport.Document document = SchematicFormat.encode(target, schematic, null, true, true, "Builder");
        File file = name.endsWith(".litematic") ? SchematicFileWriter.write(target, document) : SchematicFileWriter.write(target, document.root);
        assertNotNull(file);
        return file;
    }

    private static int[] preview() {
        int[] pixels = new int[120 * 120];
        Arrays.fill(pixels, 0xFF123456);
        return pixels;
    }

    private void decorate(File file) throws Exception {
        SchematicFiles.editMetadata(file, metadata -> {
            metadata.setString("Name", "A shared build");
            metadata.setString("Author", "Original author");
            metadata.setString("Description", "第一行\nSecond line");
            metadata.setLong("TimeCreated", 123456L);
            metadata.setIntArray("PreviewImageData", preview());
        });
    }

    private void assertDescription(File file) throws Exception {
        SchematicFiles.Info info = SchematicFiles.info(file);
        assertNotNull(info);
        assertEquals("A shared build", info.name);
        assertEquals("Original author", info.author);
        assertEquals("第一行\nSecond line", info.description);
        assertEquals(123456L, info.created);
        assertArrayEquals(preview(), info.preview);
        ISchematic schematic = SchematicFormat.readFromFile(file);
        assertNotNull(schematic);
        assertEquals(block, schematic.getBlock(0, 0, 0));
        assertEquals(5, schematic.getBlockMetadata(0, 0, 0));
    }

    @Test public void newSavesFillAuthorNameTimeAndDimensionsInEveryWritableFormat() throws Exception {
        for (String ext : EXTENSIONS) {
            File file = save("fresh" + ext, sample());
            assertEquals("fresh" + ext, file.getName());
            SchematicFiles.Info info = SchematicFiles.info(file);
            assertEquals("fresh", info.name);
            assertEquals("Builder", info.author);
            assertEquals("", info.description);
            assertEquals(2, info.sizeX);
            assertEquals(2, info.volume);
            assertTrue(info.created > 0);
            assertEquals(info.created, info.modified);
            assertNull(info.preview);
        }
    }

    @Test public void allWritableFormatPairsKeepDescriptionAndOriginalAttribution() throws Exception {
        int index = 0;
        for (String from : EXTENSIONS) {
            File source = save("source" + from, sample());
            decorate(source);
            for (String to : EXTENSIONS) {
                assertDescription(save("converted" + index++ + to, SchematicFormat.readFromFile(source)));
            }
        }
    }

    @Test public void classicMetadataEditsPreserveEncodingUnknownTagsAndBlockArrays() throws Exception {
        File file = save("classic.schematic", sample());
        NBTTagCompound before = LitematicaNBTReader.readFromFile(file);
        before.setString("OtherTool", "retain");
        before.getCompoundTag("ExtendedMetadata").setString("OtherExtension", "retain too");
        SchematicFileWriter.write(file, before);
        decorate(file);
        NBTTagCompound after = LitematicaNBTReader.readFromFile(file);
        assertEquals("nibble12", after.getString(SchematicBlockIds.ENCODING));
        assertArrayEquals(before.getByteArray("Blocks"), after.getByteArray("Blocks"));
        assertArrayEquals(before.getByteArray("AddBlocks"), after.getByteArray("AddBlocks"));
        assertArrayEquals(before.getByteArray("Data"), after.getByteArray("Data"));
        assertEquals("retain", after.getString("OtherTool"));
        assertEquals("retain too", after.getCompoundTag("ExtendedMetadata").getString("OtherExtension"));
        after.removeTag("ExtendedMetadata");
        ISchematic legacy = new SchematicAlpha().readFromNBT(after);
        assertEquals(block, legacy.getBlock(0, 0, 0));
        assertEquals(5, legacy.getBlockMetadata(0, 0, 0));
        assertFalse(new File(temporary.getRoot(), "classic.schemplus").exists());
    }

    @Test public void structureEditsKeepTheAuthorChangedByAnotherEditor() throws Exception {
        File file = save("native-author.nbt", sample());
        NBTTagCompound root = LitematicaNBTReader.readFromFile(file);
        root.setString("author", "Updated native author");
        SchematicFileWriter.write(file, root);
        SchematicFiles.editMetadata(file, metadata -> metadata.setString("Description", "Keep attribution"));
        assertEquals("Updated native author", SchematicFiles.info(file).author);
        root = LitematicaNBTReader.readFromFile(file);
        assertEquals("Updated native author", root.getCompoundTag("ExtendedMetadata").getCompoundTag(SchematicMetadata.EXTENSION).getString("Author"));
        assertEquals("Keep attribution", SchematicFiles.info(file).description);
    }

    @Test public void loadedSourcesRetainMetadataAfterRebuildAndSnapshotRefresh() throws Exception {
        for (String ext : EXTENSIONS) {
            File file = save("loaded" + ext, sample());
            SchematicSourceData source = SchematicSourceData.read(file);
            Schematic contents = source.editable();
            String region = SchematicCopies.independent(contents) ? contents.getRegions().get(0).name : null;
            SourceEditor.apply(source, java.util.Collections.singletonMap(new SourceEditor.Cell(region, 1, 0, 0), new CellState(block, 3, null)));
            decorate(file);
            source.updateMetadata(SchematicFormat.readFromFile(file).getSchematicMetadata());
            File saved = new File(temporary.getRoot(), "edited-" + ext.substring(1) + ".schemplus");
            source.save(saved, false, "Editor");
            assertDescription(saved);
            assertEquals(ext, 3, SchematicFormat.readFromFile(saved).getBlockMetadata(1, 0, 0));
            assertFalse(source.unsaved());
        }
    }

    @Test public void loadedSnapshotMetadataEditsKeepUnknownLongArrays() throws Exception {
        File file = save("unknown.litematic", sample());
        NBTTagCompound root = LitematicaNBTReader.readFromFile(file);
        NBTTagCompound metadata = root.getCompoundTag("Metadata");
        long[] unknown = {1L, Long.MAX_VALUE, Long.MIN_VALUE};
        LitematicaNBTReader.longArrays().put(metadata, java.util.Collections.singletonMap("OtherToolData", unknown));
        SchematicFileWriter.write(file, new LitematicExport.Document(root, LitematicaNBTReader.longArrays()));
        LitematicaNBTReader.clearLongArrayStore();
        SchematicSourceData source = SchematicSourceData.read(file);
        decorate(file);
        source.updateMetadata(SchematicFormat.readFromFile(file).getSchematicMetadata());
        File saved = new File(temporary.getRoot(), "unknown-copy.litematic");
        source.save(saved, false);
        assertDescription(saved);
        NBTTagCompound result = LitematicaNBTReader.readFromFile(saved);
        assertArrayEquals(unknown, LitematicaNBTReader.getLongArray(result.getCompoundTag("Metadata"), "OtherToolData"));
        LitematicaNBTReader.clearLongArrayStore();
    }

    @Test public void metadataCopiesDoNotMutateTheOriginalAndUneditedSaveKeepsBytes() throws Exception {
        File file = save("original.litematic", sample());
        decorate(file);
        SchematicSourceData source = SchematicSourceData.read(file);
        ISchematic first = source.instantiate();
        NBTTagCompound metadata = first.getSchematicMetadata();
        metadata.setString("Author", "Mutation");
        assertEquals("Original author", first.getSchematicMetadata().getString("Author"));
        Schematic copy = SchematicCopies.copy(first);
        copy.setSchematicMetadata(metadata);
        assertEquals("Original author", first.getSchematicMetadata().getString("Author"));
        File target = new File(temporary.getRoot(), "copy.litematic");
        source.save(target, false);
        assertArrayEquals(Files.readAllBytes(file.toPath()), Files.readAllBytes(target.toPath()));
    }

    @Test public void flatDownloadPayloadRetainsMetadataWithoutInventingRegions() throws Exception {
        File file = save("download.schematic", sample());
        decorate(file);
        ISchematic original = SchematicFormat.readFromFile(file);
        byte[] payload = SchematicAlpha.regionPayload(original);
        assertNotNull(payload);
        ISchematic received = SchematicAlpha.withRegionPayload(sample(), payload);
        assertEquals(original.getSchematicMetadata(), received.getSchematicMetadata());
        assertFalse(SchematicCopies.independent(received));
        assertDescription(save("received.litematic", received));
    }

    @Test public void invalidPreviewIsNotUploadedAndRemovalKeepsOtherMetadata() throws Exception {
        File file = save("preview.schemplus", sample());
        decorate(file);
        SchematicFiles.editMetadata(file, metadata -> metadata.setIntArray("PreviewImageData", new int[7]));
        assertNull(SchematicFiles.info(file).preview);
        SchematicFiles.editMetadata(file, metadata -> metadata.removeTag("PreviewImageData"));
        assertNull(SchematicFiles.info(file).preview);
        assertEquals("Original author", SchematicFiles.info(file).author);
        assertFalse(SchematicMetadata.validPreview(new int[513 * 513]));
    }
}

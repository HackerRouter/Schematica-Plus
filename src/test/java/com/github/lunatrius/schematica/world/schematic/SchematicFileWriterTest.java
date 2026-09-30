package com.github.lunatrius.schematica.world.schematic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class SchematicFileWriterTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    private NBTTagCompound tag(byte[] low, byte[] high, boolean extended) {
        NBTTagCompound tag = new NBTTagCompound();
        SchematicBlockIds.write(tag, low, high, extended);
        tag.setByteArray("Data", new byte[low.length]);
        return tag;
    }

    @Test public void savesLargeIdsInAnExtendedFileWithTileStateIntact() throws IOException {
        NBTTagCompound tag = tag(new byte[] {0, -1, 0, -1, 0, -1}, new byte[] {0, 15, 16, 127, -128, -1}, false);
        NBTTagCompound tile = new NBTTagCompound();
        tile.setString("id", "test:pipe");
        tile.setByte("connections", (byte) 37);
        NBTTagList tiles = new NBTTagList();
        tiles.appendTag(tile);
        tag.setTag("TileEntities", tiles);
        NBTTagCompound mapping = new NBTTagCompound();
        mapping.setShort("test:pipe", (short) 65535);
        tag.setTag("SchematicaMapping", mapping);
        File requested = new File(temporary.getRoot(), "建筑.schematic");
        File saved = SchematicFileWriter.write(requested, tag);
        assertEquals("建筑.schemplus", saved.getName());
        assertFalse(requested.exists());
        NBTTagCompound restored = LitematicaNBTReader.readFromFile(saved);
        assertEquals(tag, restored);
        assertArrayEquals(new int[] {0, 4095, 4096, 32767, 32768, 65535}, SchematicBlockIds.read(restored, 6));
    }

    @Test public void keepsStandardFilesForIdsUpTo4095() throws IOException {
        File requested = new File(temporary.getRoot(), "small.schematic");
        NBTTagCompound tag = tag(new byte[] {35, 86, -1}, new byte[] {1, 4, 15}, false);
        assertEquals(requested, SchematicFileWriter.write(requested, tag));
        NBTTagCompound restored = LitematicaNBTReader.readFromFile(requested);
        assertEquals(SchematicBlockIds.STANDARD, restored.getString(SchematicBlockIds.ENCODING));
        assertArrayEquals(new int[] {0x123, 0x456, 4095}, SchematicBlockIds.read(restored, 3));
    }

    @Test public void automaticUpgradeNeverOverwritesExistingNames() throws IOException {
        File requested = temporary.newFile("test.SCHEMATIC");
        File extended = temporary.newFile("test.schemplus");
        File suffixed = temporary.newFile("test-1.schemplus");
        for (File file : new File[] {requested, extended, suffixed}) Files.write(file.toPath(), new byte[] {42});
        File saved = SchematicFileWriter.write(requested, tag(new byte[] {0}, new byte[] {16}, false));
        assertEquals("test-2.schemplus", saved.getName());
        for (File file : new File[] {requested, extended, suffixed}) {
            assertArrayEquals(new byte[] {42}, Files.readAllBytes(file.toPath()));
        }
        assertEquals(4, temporary.getRoot().list().length);
    }

    @Test public void explicitExtendedTargetKeepsItsNameAndReplacementBehavior() throws IOException {
        File requested = temporary.newFile("explicit.SCHEMPLUS");
        Files.write(requested.toPath(), new byte[] {42});
        NBTTagCompound tag = tag(new byte[] {1}, new byte[] {0}, true);
        assertEquals(requested, SchematicFileWriter.write(requested, tag));
        assertEquals(tag, LitematicaNBTReader.readFromFile(requested));
        assertEquals(1, temporary.getRoot().list().length);
    }

    @Test public void failedMovePreservesDestinationAndCleansTemporaryFile() throws IOException {
        File directory = temporary.newFolder("blocked.schematic");
        File existing = new File(directory, "existing");
        Files.write(existing.toPath(), new byte[] {42});
        NBTTagCompound tag = tag(new byte[] {1}, new byte[] {0}, false);
        assertThrows(IOException.class, () -> SchematicFileWriter.write(directory, tag));
        assertArrayEquals(new byte[] {42}, Files.readAllBytes(existing.toPath()));
        assertEquals(1, temporary.getRoot().list().length);
    }
}

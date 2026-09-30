package com.github.lunatrius.schematica.world.schematic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

public class SchematicFileSnapshotTest {

    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void cachedSourceSurvivesDiskDeletionAndEveryReadIsIndependent() throws IOException {
        File file = temporary.newFile("source.schemplus");
        NBTTagCompound root = new NBTTagCompound();
        NBTTagCompound tile = new NBTTagCompound();
        tile.setByte("connections", (byte) 37);
        tile.setString("unknownModState", "preserve me");
        root.setTag("tile", tile);
        root.setByteArray("Blocks", new byte[] {1, 2, 3});
        try (java.io.OutputStream out = Files.newOutputStream(file.toPath())) { CompressedStreamTools.writeCompressed(root, out); }
        byte[] original = Files.readAllBytes(file.toPath());
        SchematicFileSnapshot snapshot = SchematicFileSnapshot.read(file);
        Files.delete(file.toPath());
        NBTTagCompound first = snapshot.readNBT();
        first.getCompoundTag("tile").setByte("connections", (byte) 0);
        first.getByteArray("Blocks")[0] = 99;
        assertEquals(root, snapshot.readNBT());
        File saved = new File(temporary.getRoot(), "copy.schemplus");
        snapshot.write(saved, false);
        assertArrayEquals(original, Files.readAllBytes(saved.toPath()));
        assertEquals(original.length, snapshot.size());
    }

    @Test public void saveDoesNotOverwriteWithoutExplicitReplacementOrRelabelFormats() throws IOException {
        File file = temporary.newFile("source.LITEMATIC");
        Files.write(file.toPath(), new byte[] {1, 2, 3});
        SchematicFileSnapshot snapshot = SchematicFileSnapshot.read(file);
        File existing = temporary.newFile("target.litematic");
        Files.write(existing.toPath(), new byte[] {9});
        assertThrows(IOException.class, () -> snapshot.write(existing, false));
        assertArrayEquals(new byte[] {9}, Files.readAllBytes(existing.toPath()));
        assertThrows(IOException.class, () -> snapshot.write(new File(temporary.getRoot(), "wrong.schematic"), false));
        snapshot.write(existing, true);
        assertArrayEquals(new byte[] {1, 2, 3}, Files.readAllBytes(existing.toPath()));
        assertEquals(2, temporary.getRoot().list().length);
    }

    @Test public void modernLongArraysAreRecreatedForEachInstance() throws IOException {
        File file = temporary.newFile("source.litematic");
        try (java.io.DataOutputStream out = new java.io.DataOutputStream(Files.newOutputStream(file.toPath()))) {
            out.writeByte(10); out.writeUTF("");
            out.writeByte(12); out.writeUTF("BlockStates"); out.writeInt(2);
            out.writeLong(123456789L); out.writeLong(-1L);
            out.writeByte(0);
        }
        SchematicFileSnapshot snapshot = SchematicFileSnapshot.read(file);
        try {
            NBTTagCompound first = snapshot.readNBT();
            LitematicaNBTReader.getLongArray(first, "BlockStates")[0] = 0;
            NBTTagCompound second = snapshot.readNBT();
            assertArrayEquals(new long[] {123456789L, -1L}, LitematicaNBTReader.getLongArray(second, "BlockStates"));
        } finally {
            LitematicaNBTReader.clearLongArrayStore();
        }
    }
}

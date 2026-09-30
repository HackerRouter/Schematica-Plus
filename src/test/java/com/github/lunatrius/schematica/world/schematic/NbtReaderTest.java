package com.github.lunatrius.schematica.world.schematic;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class NbtReaderTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void readsCompressedModernAndUncompressedLegacyNbt() throws IOException {
        for (boolean compressed : new boolean[] {false, true}) {
            File file = temporary.newFile();
            try (java.io.OutputStream raw = new FileOutputStream(file);
                 DataOutputStream out = new DataOutputStream(compressed ? new java.util.zip.GZIPOutputStream(raw) : raw)) {
                out.writeByte(10); out.writeUTF("");
                out.writeByte(8); out.writeUTF("Name"); out.writeUTF("建筑");
                out.writeByte(12); out.writeUTF("BlockStates"); out.writeInt(1); out.writeLong(123);
                out.writeByte(0);
            }
            net.minecraft.nbt.NBTTagCompound tag = LitematicaNBTReader.readFromFile(file);
            assertEquals("建筑", tag.getString("Name"));
            assertEquals(123, LitematicaNBTReader.getLongArray(tag, "BlockStates")[0]);
            LitematicaNBTReader.clearLongArrayStore();
        }
    }

    @Test public void rejectsOversizedArrayBeforeReadingItsPayload() throws IOException {
        for (int length : new int[] {-1, Integer.MAX_VALUE}) {
            File file = temporary.newFile();
            try (DataOutputStream out = new DataOutputStream(new FileOutputStream(file))) {
                out.writeByte(10); out.writeUTF("");
                out.writeByte(7); out.writeUTF("Blocks"); out.writeInt(length);
            }
            assertThrows(IOException.class, () -> LitematicaNBTReader.readFromFile(file));
        }
    }

    @Test public void rejectsDeepCompounds() throws IOException {
        File file = temporary.newFile();
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(file))) {
            for (int i = 0; i < 70; i++) { out.writeByte(10); out.writeUTF(""); }
        }
        assertThrows(IOException.class, () -> LitematicaNBTReader.readFromFile(file));
    }

    @Test public void checksPackedArrayLengthAndCrossWordValues() {
        assertThrows(IllegalArgumentException.class, () -> new LitematicBitArray(5, 13, new long[1]));
        LitematicBitArray bits = new LitematicBitArray(5, 13, new long[] {15L << 60, 1});
        assertEquals(31, bits.getAt(12));
    }
}

package com.github.lunatrius.schematica.handler;

import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPOutputStream;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class RemoteEditsTest {
    @Test public void uploadsDecodeAsGzipNbt() throws Exception {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("version", 1);
        tag.setString("kind", "FILL");
        assertEquals(tag, RemoteEdits.read(CompressedStreamTools.compress(tag)));
    }

    @Test public void uploadsMustBeCompressed() throws Exception {
        assertThrows(java.io.IOException.class, () -> RemoteEdits.read(new byte[] {1, 2, 3, 4}));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) { gzip.write(new byte[] {99}); }
        assertThrows(Exception.class, () -> RemoteEdits.read(bytes.toByteArray()));
    }
}

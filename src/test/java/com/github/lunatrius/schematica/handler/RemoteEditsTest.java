package com.github.lunatrius.schematica.handler;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

import com.github.lunatrius.schematica.network.message.MessageEditUpload;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

    private RemoteEdits edits() throws Exception {
        Constructor<RemoteEdits> constructor = RemoteEdits.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private Object accept(RemoteEdits edits, UUID owner, int dimension, long id, int messageDimension, int index) throws Exception {
        Method method = RemoteEdits.class.getDeclaredMethod("accept", UUID.class, int.class, MessageEditUpload.class);
        method.setAccessible(true);
        return method.invoke(edits, owner, dimension, new MessageEditUpload(id, messageDimension, index, 2, new byte[] {1}));
    }

    private Object field(Object received, String name) throws Exception {
        Field field = received.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(received);
    }

    @Test public void changingDimensionDuringUploadReleasesTheOwnerAndLateSlicesCannotRemoveTheNewUpload() throws Exception {
        RemoteEdits edits = edits();
        UUID owner = UUID.randomUUID();
        assertNull(field(accept(edits, owner, 0, 1, 0, 0), "error"));
        edits.forget(owner);
        assertNull(field(accept(edits, owner, 1, 2, 1, 0), "error"));
        assertEquals("schematica.message.edit.remote_invalid", field(accept(edits, owner, 1, 1, 0, 1), "error"));
        Object completed = accept(edits, owner, 1, 2, 1, 1);
        assertNull(field(completed, "error"));
        assertNotNull(field(completed, "complete"));
        edits.clear();
    }

    @Test public void aLateFirstSliceFromThePreviousDimensionCannotClaimTheOwnerAgain() throws Exception {
        RemoteEdits edits = edits();
        UUID owner = UUID.randomUUID();
        assertEquals("schematica.message.edit.remote_invalid", field(accept(edits, owner, 1, 1, 0, 0), "error"));
        assertNull(field(accept(edits, owner, 1, 2, 1, 0), "error"));
        assertNotNull(field(accept(edits, owner, 1, 2, 1, 1), "complete"));
        edits.clear();
    }

    @Test public void busyRejectionAndStaleSameDimensionSlicesLeaveTheCurrentUploadIntact() throws Exception {
        RemoteEdits edits = edits();
        UUID owner = UUID.randomUUID();
        assertNull(field(accept(edits, owner, 0, 1, 0, 0), "error"));
        assertEquals("schematica.message.edit.busy", field(accept(edits, owner, 0, 2, 0, 0), "error"));
        assertEquals("schematica.message.edit.remote_invalid", field(accept(edits, owner, 0, 2, 0, 1), "error"));
        assertNotNull(field(accept(edits, owner, 0, 1, 0, 1), "complete"));
        edits.clear();
    }
}

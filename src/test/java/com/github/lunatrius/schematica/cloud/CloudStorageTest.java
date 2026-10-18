package com.github.lunatrius.schematica.cloud;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

import com.github.lunatrius.schematica.util.MessageException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

public class CloudStorageTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private final UUID alice = UUID.randomUUID(), bob = UUID.randomUUID();
    private CloudStorage storage;

    @Before public void create() {
        storage = new CloudStorage(temporary.getRoot(), snapshot -> {
            if (snapshot.size() < 4) throw new IOException("Not a schematic");
        });
    }

    private static byte[] bytes(int size) {
        byte[] data = new byte[size];
        for (int i = 0; i < size; i++) data[i] = (byte) i;
        return data;
    }

    private interface Action { void run() throws IOException; }

    private static String failure(Action action) throws IOException {
        try {
            action.run();
        } catch (MessageException e) {
            return e.key();
        }
        return null;
    }

    @Test public void storesPrivateAndPublicFilesWithOwnersAndQuota() throws IOException {
        storage.write(CloudPath.parse("private/a.schematic"), alice, "Alice", bytes(100), 1000);
        storage.write(CloudPath.parse("public/Shared/b.litematic"), alice, "Alice", bytes(200), 1000);
        assertTrue(new File(temporary.getRoot(), alice + "/private/a.schematic").isFile());
        assertTrue(new File(temporary.getRoot(), "public/Shared/b.litematic").isFile());
        assertEquals(300, storage.used(alice));
        assertEquals(0, storage.used(bob));
        assertEquals("schematica.message.cloud.quota",
            failure(() -> storage.write(CloudPath.parse("private/c.schematic"), alice, "Alice", bytes(800), 1000)));
        assertEquals("schematica.message.cloud.exists",
            failure(() -> storage.write(CloudPath.parse("private/a.schematic"), alice, "Alice", bytes(10), -1)));
        assertEquals("schematica.message.cloud.invalid_file",
            failure(() -> storage.write(CloudPath.parse("private/d.schematic"), alice, "Alice", bytes(2), -1)));
        assertFalse(new File(temporary.getRoot(), alice + "/private/d.schematic").exists());

        CloudCatalog catalog = storage.catalog(bob, -1, true, true, false);
        assertEquals(1, catalog.files.size());
        assertEquals("Alice", catalog.files.get(0).owner);
        assertEquals(CloudStorage.hash(bytes(200)), catalog.files.get(0).hash);
        assertTrue(catalog.directories.contains(CloudPath.parse("public/Shared")));

        CloudCatalog read = CloudCatalog.read(catalog.write());
        assertEquals(catalog.files.get(0).path, read.files.get(0).path);
        assertEquals(catalog.directories, read.directories);
        assertTrue(read.publish);
    }

    @Test public void onlyOwnersOrManagersDeletePublicFiles() throws IOException {
        storage.write(CloudPath.parse("public/b.litematic"), alice, "Alice", bytes(50), -1);
        CloudPath path = CloudPath.parse("public/b.litematic");
        assertEquals("schematica.message.cloud.no_permission", failure(() -> storage.delete(path, bob, false)));
        storage.delete(path, bob, true);
        assertFalse(storage.file(path, bob).exists());
        storage.createDirectory(CloudPath.parse("public/Folder"), alice, "Alice");
        storage.write(CloudPath.parse("public/Folder/c.litematic"), alice, "Alice", bytes(50), -1);
        assertEquals("schematica.message.cloud.not_empty", failure(() -> storage.delete(CloudPath.parse("public/Folder"), alice, false)));
    }

    @Test public void publishesPrivateCopiesAndReadsWithinLimits() throws IOException {
        storage.write(CloudPath.parse("private/a.schematic"), alice, "Alice", bytes(64), -1);
        CloudPath published = storage.publish(CloudPath.parse("private/a.schematic"), CloudPath.root(CloudPath.PUBLIC), alice, "Alice");
        assertEquals(CloudPath.parse("public/a.schematic"), published);
        assertTrue(storage.owns(published, alice));
        assertFalse(storage.owns(published, bob));
        assertArrayEquals(bytes(64), storage.read(published, bob, 1000));
        assertEquals("schematica.message.cloud.too_large", failure(() -> storage.read(published, bob, 10)));
        assertEquals("schematica.message.cloud.not_found", failure(() -> storage.read(CloudPath.parse("private/a.schematic"), bob, 1000)));
        assertEquals("schematica.message.cloud.exists",
            failure(() -> storage.publish(CloudPath.parse("private/a.schematic"), CloudPath.root(CloudPath.PUBLIC), alice, "Alice")));
        assertTrue(Files.exists(new File(new File(temporary.getRoot(), "public"), CloudStorage.OWNERS).toPath()));
    }
}

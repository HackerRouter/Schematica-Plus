package com.github.lunatrius.schematica.cloud;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import com.github.lunatrius.schematica.util.MessageException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

public class SharedPlacementTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private static final String HASH = CloudStorage.hash(new byte[] {1, 2, 3, 4});

    private static SharedPlacement placement(String subregions, String... transforms) {
        return new SharedPlacement(UUID.randomUUID(), "Tower", "tower.litematic", HASH, 4, -1, new int[] {10, 64, -20},
            Arrays.asList(transforms), subregions, "", "", "", "", 0);
    }

    @Test public void survivesNetworkAndSaveFormats() {
        UUID alice = UUID.randomUUID();
        SharedPlacement shared = placement("{\"regions\":[]}", "Y", "x").owned(alice, "Alice");
        SharedPlacement read = SharedPlacement.read(shared.write());
        assertEquals(shared.id, read.id);
        assertArrayEquals(shared.origin, read.origin);
        assertEquals(Arrays.asList("Y", "x"), read.transforms);
        assertEquals("Alice", read.ownerName);
        assertEquals("Alice", read.modifierName);
        assertEquals(CloudPath.parse("shared/" + HASH + ".litematic"), read.file());
        SharedPlacement loaded = SharedPlacement.fromJson(shared.toJson());
        assertEquals(shared.subregions, loaded.subregions);
        assertEquals(alice.toString(), loaded.ownerId);
    }

    @Test public void modificationsKeepIdentityAndCountVersions() {
        UUID alice = UUID.randomUUID(), bob = UUID.randomUUID();
        SharedPlacement shared = placement("").owned(alice, "Alice");
        SharedPlacement change = new SharedPlacement(UUID.randomUUID(), "Other", "other.schematic", HASH, 4, 0, new int[] {1, 2, 3},
            Collections.singletonList("Z"), "", bob.toString(), "Bob", "", "", 7);
        SharedPlacement next = shared.modified(change, bob, "Bob");
        assertEquals(shared.id, next.id);
        assertEquals("Tower", next.name);
        assertEquals(-1, next.dimension);
        assertArrayEquals(new int[] {1, 2, 3}, next.origin);
        assertEquals("Alice", next.ownerName);
        assertEquals("Bob", next.modifierName);
        assertEquals(1, next.version);
    }

    @Test public void rejectsInvalidData() {
        assertThrows(IllegalArgumentException.class, () -> placement("", "Q").validate());
        assertThrows(RuntimeException.class, () -> placement("not json").validate());
        assertThrows(IllegalArgumentException.class, () -> new SharedPlacement(UUID.randomUUID(), "A", "a.txt", HASH, 1, 0, new int[] {0, 0, 0},
            Collections.<String>emptyList(), "", "", "", "", "", 0).validate());
        assertThrows(IllegalArgumentException.class, () -> new SharedPlacement(UUID.randomUUID(), "A", "a.litematic", "abc", 1, 0, new int[] {0, 0, 0},
            Collections.<String>emptyList(), "", "", "", "", "", 0).validate());
        assertThrows(IllegalArgumentException.class, () -> new SharedPlacement(UUID.randomUUID(), "A", "a.litematic", HASH, 1, 0, new int[] {0, 0, 40000000},
            Collections.<String>emptyList(), "", "", "", "", "", 0).validate());
    }

    @Test public void storesSharedFilesByTheirContent() throws IOException {
        CloudStorage storage = new CloudStorage(temporary.getRoot(), snapshot -> {});
        byte[] data = {1, 2, 3, 4};
        CloudPath path = placement("").file();
        File written = storage.writeShared(path, data);
        assertEquals(new File(new File(temporary.getRoot(), "shared"), HASH + ".litematic"), written);
        assertEquals(written, storage.writeShared(path, data));
        assertThrows(MessageException.class, () -> storage.writeShared(path, new byte[] {9}));
        assertThrows(MessageException.class, () -> storage.write(path, UUID.randomUUID(), "A", data, -1));
        assertThrows(MessageException.class, () -> storage.delete(path, UUID.randomUUID(), true));
    }
}

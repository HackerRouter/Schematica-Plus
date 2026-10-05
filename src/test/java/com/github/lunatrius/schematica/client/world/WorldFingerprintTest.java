package com.github.lunatrius.schematica.client.world;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.*;

public class WorldFingerprintTest {
    private static boolean[] bedrock(long seed) {
        Random random = new Random(seed);
        boolean[] bits = new boolean[1024];
        for (int layer = 0; layer < 4; layer++) for (int i = 0; i < 256; i++) bits[layer * 256 + i] = 1 + layer <= random.nextInt(5);
        return bits;
    }

    @Test public void generatedBedrockIsNaturalAndFlatOrEmptyIsNot() {
        assertTrue(WorldFingerprint.natural(bedrock(1)));
        assertFalse(WorldFingerprint.natural(new boolean[1024]));
        boolean[] full = new boolean[1024];
        Arrays.fill(full, true);
        assertFalse(WorldFingerprint.natural(full));
        String a = WorldFingerprint.hash(0, false, bedrock(1));
        assertTrue(a.matches("[0-9a-f]{16}"));
        assertEquals(a, WorldFingerprint.hash(0, false, bedrock(1)));
        assertNotEquals(a, WorldFingerprint.hash(0, false, bedrock(2)));
        assertNotEquals(a, WorldFingerprint.hash(-1, false, bedrock(1)));
    }

    @Test public void judgesByTheLoadedSamplesAndSurvivesJson() {
        WorldFingerprint fingerprint = new WorldFingerprint();
        fingerprint.samples.add(new WorldFingerprint.Sample(0, 0, "0123456789abcdef"));
        fingerprint.samples.add(new WorldFingerprint.Sample(1, 0, "fedcba9876543210"));
        String loose = WorldFingerprint.NOT_LOADED, none = WorldFingerprint.NO_BEDROCK;
        assertEquals(WorldFingerprint.State.UNKNOWN, fingerprint.judge(Arrays.asList(loose, loose)));
        assertEquals(WorldFingerprint.State.MATCH, fingerprint.judge(Arrays.asList("1111111111111111", "fedcba9876543210")));
        assertEquals(WorldFingerprint.State.OTHER_WORLD, fingerprint.judge(Arrays.asList("1111111111111111", loose)));
        assertEquals(WorldFingerprint.State.OTHER_WORLD, fingerprint.judge(Arrays.asList(none, none)));
        fingerprint.x = 5; fingerprint.y = 64; fingerprint.z = -3;
        WorldFingerprint copy = WorldFingerprint.fromJson(fingerprint.toJson());
        assertEquals(2, copy.samples.size());
        assertEquals("fedcba9876543210", copy.samples.get(1).hash);
        assertEquals(-3, copy.z);
        assertNull(WorldFingerprint.fromJson(new com.google.gson.JsonObject()));
        assertEquals(2, WorldFingerprint.chunks(new int[] {0, 0, 0, 20, 10, 5}).size());
        assertEquals(4, WorldFingerprint.chunks(new int[] {-1, 0, -1, 20, 10, 20}).size());
    }
}

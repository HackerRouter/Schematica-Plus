// Bedrock fingerprint of the world a placement was put in, after the behavior of Buildprint's world fingerprinting, by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.world;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Servers that run several worlds behind one address look like one world to the client, so a placement would show
 * in all of them. worldFingerprinting remembers the random bedrock under a placement (the four layers above the
 * floor, or below the Nether roof) of up to four chunks; where that bedrock differs the placement is hidden.
 */
public final class WorldFingerprint {
    /** Share of bedrock in the four random layers above y 0 (1.7.10 terrain: y <= rand(5)), nearest the floor first. */
    static final double[] EXPECTED = {0.8, 0.6, 0.4, 0.2};
    static final int BAND = 64, MAX_SAMPLES = 4;

    public enum State { NONE, MATCH, OTHER_WORLD, UNKNOWN }

    public static final class Sample {
        public final int cx, cz;
        public final String hash;
        Sample(int cx, int cz, String hash) { this.cx = cx; this.cz = cz; this.hash = hash; }
    }

    /** NOT_LOADED: the chunk is not there to read; NO_BEDROCK: no natural bedrock pattern. */
    static final String NOT_LOADED = "-", NO_BEDROCK = "";

    public final List<Sample> samples = new ArrayList<>();
    /** The placement position the fingerprint was taken at. */
    public int x, y, z;

    /** Whether four layers of 256 cells look like generated bedrock; a roof is read from the top down. */
    static boolean natural(boolean[] bits) {
        int[] counts = new int[4];
        for (int layer = 0; layer < 4; layer++) {
            for (int i = 0; i < 256; i++) if (bits[layer * 256 + i]) counts[layer]++;
            int expected = (int) Math.round(EXPECTED[layer] * 256);
            if (counts[layer] <= 0 || counts[layer] >= 256 || Math.abs(counts[layer] - expected) > BAND) return false;
        }
        return true;
    }

    static String hash(int dimension, boolean roof, boolean[] bits) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            sha.update(("schematica-plus-bedrock-v1|" + dimension + "|" + (roof ? "roof" : "floor") + "|").getBytes(StandardCharsets.UTF_8));
            byte[] packed = new byte[bits.length / 8];
            for (int i = 0; i < bits.length; i++) if (bits[i]) packed[i >> 3] |= (byte) (1 << (i & 7));
            sha.update(packed);
            StringBuilder hex = new StringBuilder();
            byte[] digest = sha.digest();
            for (int i = 0; i < 8; i++) hex.append(String.format("%02x", digest[i] & 255));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The bedrock hash of a chunk: its floor, else its roof (y 126 down to 123); NOT_LOADED or NO_BEDROCK. */
    static String read(World world, int cx, int cz) {
        Chunk chunk = world.getChunkFromChunkCoords(cx, cz);
        if (chunk instanceof EmptyChunk || !chunk.isChunkLoaded) return NOT_LOADED;
        boolean[] floor = new boolean[1024], roof = new boolean[1024];
        for (int layer = 0; layer < 4; layer++) {
            for (int i = 0; i < 256; i++) {
                floor[layer * 256 + i] = chunk.getBlock(i & 15, 1 + layer, i >> 4) == Blocks.bedrock;
                roof[layer * 256 + i] = chunk.getBlock(i & 15, 126 - layer, i >> 4) == Blocks.bedrock;
            }
        }
        int dimension = world.provider.dimensionId;
        if (natural(floor)) return hash(dimension, false, floor);
        if (natural(roof)) return hash(dimension, true, roof);
        return NO_BEDROCK;
    }

    /** Up to four chunks under a box {minX, minY, minZ, maxX, maxY, maxZ}: its corners. */
    static List<int[]> chunks(int[] box) {
        List<int[]> result = new ArrayList<>();
        int[][] corners = {{box[0], box[2]}, {box[3], box[5]}, {box[3], box[2]}, {box[0], box[5]}};
        for (int[] corner : corners) {
            int[] chunk = {corner[0] >> 4, corner[1] >> 4};
            boolean known = false;
            for (int[] other : result) known |= other[0] == chunk[0] && other[1] == chunk[1];
            if (!known && result.size() < MAX_SAMPLES) result.add(chunk);
        }
        return result;
    }

    /** A fingerprint of the bedrock under the box, or null while one of its chunks is not loaded or none has bedrock. */
    public static WorldFingerprint take(World world, int[] box, int x, int y, int z) {
        WorldFingerprint result = new WorldFingerprint();
        for (int[] chunk : chunks(box)) {
            String hash = read(world, chunk[0], chunk[1]);
            if (NOT_LOADED.equals(hash)) return null;
            if (!NO_BEDROCK.equals(hash)) result.samples.add(new Sample(chunk[0], chunk[1], hash));
        }
        if (result.samples.isEmpty()) return null;
        result.x = x; result.y = y; result.z = z;
        return result;
    }

    /** MATCH when a loaded sample has the same bedrock; OTHER_WORLD when loaded samples differ; else UNKNOWN. */
    public State judge(World world) {
        if (samples.isEmpty()) return State.NONE;
        List<String> readings = new ArrayList<>();
        for (Sample sample : samples) readings.add(read(world, sample.cx, sample.cz));
        return judge(readings);
    }

    State judge(List<String> readings) {
        boolean differs = false;
        int loaded = 0, noBedrock = 0;
        for (int i = 0; i < samples.size() && i < readings.size(); i++) {
            String reading = readings.get(i);
            if (NOT_LOADED.equals(reading)) continue;
            loaded++;
            if (NO_BEDROCK.equals(reading)) noBedrock++;
            else if (reading.equals(samples.get(i).hash)) return State.MATCH;
            else differs = true;
        }
        if (differs || loaded > 0 && noBedrock == loaded) return State.OTHER_WORLD;
        return State.UNKNOWN;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("x", x);
        json.addProperty("y", y);
        json.addProperty("z", z);
        JsonArray array = new JsonArray();
        for (Sample sample : samples) {
            JsonObject entry = new JsonObject();
            entry.addProperty("cx", sample.cx);
            entry.addProperty("cz", sample.cz);
            entry.addProperty("hash", sample.hash);
            array.add(entry);
        }
        json.add("samples", array);
        return json;
    }

    public static WorldFingerprint fromJson(JsonObject json) {
        if (json == null) return null;
        try {
            WorldFingerprint result = new WorldFingerprint();
            result.x = json.get("x").getAsInt();
            result.y = json.get("y").getAsInt();
            result.z = json.get("z").getAsInt();
            for (JsonElement element : json.getAsJsonArray("samples")) {
                JsonObject entry = element.getAsJsonObject();
                String hash = entry.get("hash").getAsString();
                if (hash.matches("[0-9a-f]{16}") && result.samples.size() < MAX_SAMPLES) {
                    result.samples.add(new Sample(entry.get("cx").getAsInt(), entry.get("cz").getAsInt(), hash));
                }
            }
            return result.samples.isEmpty() ? null : result;
        } catch (RuntimeException e) {
            return null;
        }
    }
}

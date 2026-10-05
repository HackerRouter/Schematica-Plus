// Finding a placement's build in the world, after the behavior of Buildprint's auto-align, by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.align;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The rarest non-terrain blocks of the schematic are anchors. Each anchor position, in each of the eight
 * orientations (rotations about Y and the X mirror), votes for every offset that puts it on a block of its kind
 * in the world; the offsets with the most votes are scored by sampling the schematic, and the best one wins if
 * enough of the sampled blocks are there.
 */
public final class AutoAlign {
    /** Rotations about Y, each also followed by an X mirror; the same steps the placement configuration applies. */
    public static final String[] ORIENTATIONS = {"", "Y", "YY", "YYY", "x", "Yx", "YYx", "YYYx"};
    static final int MAX_IDS = 4, ANCHOR_TARGET = 192, MAX_ANCHORS_PER_ID = 4096, MAX_HITS_PER_ID = 8000;
    static final int MAX_PAIRS_PER_ID = 1_000_000, PER_ORIENTATION = 8, SAMPLES = 2000, MIN_MATCHED = 8;
    static final double MIN_RATIO = 0.3;

    private AutoAlign() {}

    /** The schematic in its current orientation; block identities compare with ==, null is air. */
    public interface Source {
        int width();
        int height();
        int length();
        Object block(int x, int y, int z);
    }

    /** The world; loaded tells whether the column is there to compare with. */
    public interface Target {
        Object block(int x, int y, int z);
        boolean loaded(int x, int z);
    }

    /** The anchor kinds, their positions in the schematic and the cells that scores sample. */
    public static final class Plan {
        public final List<Object> ids;
        final List<long[]> anchors;
        final long[] samples;

        Plan(List<Object> ids, List<long[]> anchors, long[] samples) { this.ids = ids; this.anchors = anchors; this.samples = samples; }

        public boolean isEmpty() { return ids.isEmpty(); }
    }

    /** An orientation and the world position of the transformed schematic's minimum corner. */
    public static final class Candidate {
        public final String steps;
        public final int x, y, z;
        final int votes;

        Candidate(String steps, int x, int y, int z, int votes) { this.steps = steps; this.x = x; this.y = y; this.z = z; this.votes = votes; }
    }

    public static final class Result {
        public final Candidate candidate;
        public final int matched, checked;

        Result(Candidate candidate, int matched, int checked) { this.candidate = candidate; this.matched = matched; this.checked = checked; }

        public double ratio() { return checked == 0 ? 0 : (double) matched / checked; }
        public int percent() { return (int) Math.round(ratio() * 100); }
        public boolean confident() { return matched >= MIN_MATCHED && ratio() >= MIN_RATIO; }

        boolean better(Result other) {
            if (other == null) return true;
            if (confident() != other.confident()) return confident();
            if (ratio() != other.ratio()) return ratio() > other.ratio();
            return matched > other.matched;
        }
    }

    public static long pack(int x, int y, int z) { return ((long) x & 0x3FFFFFF) << 38 | ((long) y & 0xFFF) << 26 | (long) z & 0x3FFFFFF; }
    public static int x(long packed) { return (int) (packed >> 38); }
    public static int y(long packed) { return (int) (packed << 26 >> 52); }
    public static int z(long packed) { return (int) (packed << 38 >> 38); }

    /** The cell (x, z) of a width x length schematic after the steps, and the new width and length. */
    public static int[] cell(String steps, int x, int z, int width, int length) {
        int w = width, l = length;
        for (int i = 0; i < steps.length(); i++) {
            if (steps.charAt(i) == 'Y') {
                int next = l - 1 - z;
                z = x;
                x = next;
                int swap = w; w = l; l = swap;
            } else {
                x = w - 1 - x;
            }
        }
        return new int[] {x, z, w, l};
    }

    public static Plan plan(Source source, Set<Object> terrain) {
        Map<Object, Integer> counts = new IdentityHashMap<>();
        int w = source.width(), h = source.height(), l = source.length();
        long cells = 0;
        for (int y = 0; y < h; y++) for (int z = 0; z < l; z++) for (int x = 0; x < w; x++) {
            Object block = source.block(x, y, z);
            if (block == null) continue;
            counts.merge(block, 1, Integer::sum);
            cells++;
        }
        List<Map.Entry<Object, Integer>> ranked = new ArrayList<>(counts.entrySet());
        ranked.sort(Comparator.comparingInt(Map.Entry::getValue));
        List<Object> ids = new ArrayList<>();
        int total = pick(ranked, ids, terrain, false, 0);
        if (total < MIN_MATCHED * 4) pick(ranked, ids, terrain, true, total);
        if (ids.isEmpty()) return new Plan(ids, new ArrayList<>(), new long[0]);

        Map<Object, Integer> index = new IdentityHashMap<>();
        for (int i = 0; i < ids.size(); i++) index.put(ids.get(i), i);
        List<List<Long>> anchors = new ArrayList<>();
        int[] seen = new int[ids.size()], stride = new int[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            anchors.add(new ArrayList<>());
            stride[i] = Math.max(1, counts.get(ids.get(i)) / MAX_ANCHORS_PER_ID);
        }
        long sampleStride = Math.max(1, cells / SAMPLES);
        List<Long> samples = new ArrayList<>();
        long cell = 0;
        for (int y = 0; y < h; y++) for (int z = 0; z < l; z++) for (int x = 0; x < w; x++) {
            Object block = source.block(x, y, z);
            if (block == null) continue;
            if (cell++ % sampleStride == 0) samples.add(pack(x, y, z));
            Integer k = index.get(block);
            if (k != null && seen[k]++ % stride[k] == 0) anchors.get(k).add(pack(x, y, z));
        }
        List<long[]> anchorArrays = new ArrayList<>();
        for (List<Long> list : anchors) anchorArrays.add(toArray(list));
        return new Plan(ids, anchorArrays, toArray(samples));
    }

    private static int pick(List<Map.Entry<Object, Integer>> ranked, List<Object> ids, Set<Object> terrain, boolean anyBlock, int total) {
        for (Map.Entry<Object, Integer> entry : ranked) {
            if (ids.size() >= MAX_IDS || total >= ANCHOR_TARGET) break;
            if (ids.contains(entry.getKey()) || !anyBlock && terrain.contains(entry.getKey())) continue;
            ids.add(entry.getKey());
            total += Math.min(entry.getValue(), MAX_ANCHORS_PER_ID);
        }
        return total;
    }

    private static long[] toArray(List<Long> list) {
        long[] result = new long[list.size()];
        for (int i = 0; i < result.length; i++) result[i] = list.get(i);
        return result;
    }

    /** The best voted offsets per orientation; hits are the world positions of each anchor kind, in plan order. */
    public static List<Candidate> vote(int width, int length, Plan plan, List<long[]> hits) {
        List<Candidate> result = new ArrayList<>();
        for (String steps : ORIENTATIONS) {
            Map<Long, Integer> votes = new HashMap<>();
            for (int k = 0; k < plan.anchors.size(); k++) {
                long[] anchors = plan.anchors.get(k), found = hits.get(k);
                if (anchors.length == 0 || found.length == 0) continue;
                int stride = Math.max(1, (int) ((long) anchors.length * found.length / MAX_PAIRS_PER_ID) + 1);
                for (int a = 0; a < anchors.length; a += stride) {
                    int[] moved = cell(steps, x(anchors[a]), z(anchors[a]), width, length);
                    int ay = y(anchors[a]);
                    for (long hit : found) votes.merge(pack(x(hit) - moved[0], y(hit) - ay, z(hit) - moved[1]), 1, Integer::sum);
                }
            }
            List<Map.Entry<Long, Integer>> top = new ArrayList<>(votes.entrySet());
            top.sort((a, b) -> b.getValue() - a.getValue());
            for (int i = 0; i < Math.min(PER_ORIENTATION, top.size()); i++) {
                long key = top.get(i).getKey();
                result.add(new Candidate(steps, x(key), y(key), z(key), top.get(i).getValue()));
            }
        }
        result.sort((a, b) -> b.votes - a.votes);
        return result;
    }

    /** How many sampled schematic blocks the world has at the candidate's positions. */
    public static Result score(Source source, Target target, Plan plan, Candidate candidate) {
        int matched = 0, checked = 0;
        for (long sample : plan.samples) {
            int[] moved = cell(candidate.steps, x(sample), z(sample), source.width(), source.length());
            int wx = candidate.x + moved[0], wy = candidate.y + y(sample), wz = candidate.z + moved[1];
            if (wy < 0 || wy > 255 || !target.loaded(wx, wz)) continue;
            checked++;
            if (target.block(wx, wy, wz) == source.block(x(sample), y(sample), z(sample))) matched++;
        }
        return new Result(candidate, matched, checked);
    }

    public static Candidate current(int x, int y, int z) { return new Candidate("", x, y, z, 0); }
}

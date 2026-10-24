package com.github.lunatrius.schematica.client.verifier;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;

import com.github.lunatrius.schematica.util.BlockGroups;
import com.github.lunatrius.schematica.util.SchematicLimits;

public final class VerificationScan {
    public enum Type {
        ALL("litematica.gui.label.schematic_verifier_display_type.all_not_ignored", ""),
        WRONG_BLOCK("litematica.gui.label.schematic_verifier_display_type.wrong_blocks", "§c"),
        WRONG_STATE("litematica.gui.label.schematic_verifier_display_type.wrong_state", "§6"),
        DIFF_BLOCK("litematica.gui.label.schematic_verifier_display_type.diff_blocks", "§e"),
        EXTRA("litematica.gui.label.schematic_verifier_display_type.extra", "§d"),
        MISSING("litematica.gui.label.schematic_verifier_display_type.missing", "§b"),
        CORRECT("litematica.gui.label.schematic_verifier_display_type.correct_state", "§a");

        public final String key, color;
        Type(String key, String color) { this.key = key; this.color = color; }
    }

    public static final class State {
        public static final State AIR = new State("minecraft:air", 0);
        public final String block;
        public final int metadata;
        public State(String block, int metadata) { this.block = block; this.metadata = metadata; }
        public boolean air() { return equals(AIR); }
        @Override public boolean equals(Object other) {
            return other instanceof State && metadata == ((State) other).metadata && block.equals(((State) other).block);
        }
        @Override public int hashCode() { return 31 * block.hashCode() + metadata; }
        @Override public String toString() { return block + " : " + metadata; }
    }

    public static final class Pair {
        public final State expected, found;
        public Pair(State expected, State found) { this.expected = expected; this.found = found; }
        @Override public boolean equals(Object other) {
            return other instanceof Pair && expected.equals(((Pair) other).expected) && found.equals(((Pair) other).found);
        }
        @Override public int hashCode() { return 31 * expected.hashCode() + found.hashCode(); }
    }

    public static final class Group {
        public final Pair pair;
        public final Type type;
        private int count;
        private Group(Pair pair, Type type) { this.pair = pair; this.type = type; }
        public int count() { return count; }
    }

    public interface Reader {
        boolean included(int x, int y, int z);
        boolean loaded(int chunkX, int chunkZ);
        State expected(int x, int y, int z);
        State found(int worldX, int worldY, int worldZ);
        /** An existing block that is not an extra block where the schematic has air (ignoreExistingFluids, ignorable blocks). */
        default boolean ignorable(State found) { return false; }
        default void failed(int worldX, int worldY, int worldZ, RuntimeException error) {}
    }

    private final int x, y, z, width, length, minChunkX, minChunkZ, chunkColumns;
    private final int[] bounds, cells;
    private final List<Work> chunks = new ArrayList<>();
    private final ArrayDeque<Work> pending = new ArrayDeque<>();
    private final List<Group> groups = new ArrayList<>();
    private final Map<Pair, Integer> groupIds = new HashMap<>();
    private final Set<Pair> ignored = new HashSet<>();
    private Work active;
    private Batch inFlight;
    private final List<Part> captured = new ArrayList<>();
    private final Set<Work> held = new LinkedHashSet<>();
    private int skipped, checked, total;
    private long revision;

    public VerificationScan(int x, int y, int z, int width, int height, int length, int[] bounds) {
        SchematicLimits.volume(width, height, length);
        if (bounds.length != 6 || bounds[0] < 0 || bounds[1] < 0 || bounds[2] < 0
            || bounds[3] > width || bounds[4] > height || bounds[5] > length
            || bounds[3] < bounds[0] || bounds[4] < bounds[1] || bounds[5] < bounds[2]) throw new IllegalArgumentException("Invalid verifier bounds");
        this.x = x; this.y = y; this.z = z; this.width = bounds[3] - bounds[0]; this.length = bounds[5] - bounds[2];
        this.bounds = bounds.clone();
        total = (bounds[3] - bounds[0]) * (bounds[4] - bounds[1]) * (bounds[5] - bounds[2]);
        if (total > 0) SchematicLimits.worldBounds((long) x + bounds[0], (long) y + bounds[1], (long) z + bounds[2],
            (long) x + bounds[3] - 1, (long) y + bounds[4] - 1, (long) z + bounds[5] - 1);
        cells = new int[total];
        minChunkX = (x + bounds[0]) >> 4;
        minChunkZ = (z + bounds[2]) >> 4;
        chunkColumns = total == 0 ? 0 : ((z + bounds[5] - 1) >> 4) - minChunkZ + 1;
        groups.add(null);
        if (total > 0) for (int cx = minChunkX; cx <= ((x + bounds[3] - 1) >> 4); cx++) {
            for (int cz = minChunkZ; cz <= ((z + bounds[5] - 1) >> 4); cz++) {
                Work work = new Work(cx, cz);
                chunks.add(work);
                pending.add(work);
            }
        }
    }

    public static Type classify(State expected, State found) {
        if (expected.equals(found)) return Type.CORRECT;
        if (expected.air()) return Type.EXTRA;
        if (found.air()) return Type.MISSING;
        switch (BlockGroups.compare(expected.block, expected.metadata, found.block, found.metadata,
            BlockGroups.enabled)) {
            case SAME: return Type.CORRECT;
            case DIFFERENT_BLOCK: return Type.DIFF_BLOCK;
            case WRONG_STATE: return Type.WRONG_STATE;
            default: return Type.WRONG_BLOCK;
        }
    }

    public void step(Reader reader, int limit, long deadline) {
        VerificationRules rules = new VerificationRules();
        int attempts = pending.size() + 1;
        while (limit > 0 && System.nanoTime() < deadline) {
            Batch batch = capture(reader, Math.min(4096, limit), deadline, rules);
            if (batch == null) { if (--attempts <= 0) return; continue; }
            apply(batch.compare(), reader);
            limit -= batch.indices.length;
        }
    }

    static final class Batch {
        private final int[] indices, skipped;
        private final Pair[] pairs;
        private final Map<String, BlockGroups.Snapshot> descriptions;
        private final VerificationRules rules;

        private Batch(int[] indices, int[] skipped, Pair[] pairs,
            Map<String, BlockGroups.Snapshot> descriptions, VerificationRules rules) {
            this.indices = indices; this.skipped = skipped;
            this.pairs = pairs; this.descriptions = descriptions; this.rules = rules;
        }

        Result compare() {
            Map<Pair, Integer> ids = new HashMap<>();
            List<Pair> unique = new ArrayList<>();
            List<Type> types = new ArrayList<>();
            int[] cells = skipped.clone();
            for (int i = 0; i < pairs.length; i++) {
                if (Thread.currentThread().isInterrupted()) throw new CancellationException();
                Pair pair = pairs[i];
                if (pair == null) continue;
                Integer id = ids.get(pair);
                if (id == null) {
                    id = unique.size(); ids.put(pair, id); unique.add(pair);
                    types.add(rules.classify(pair.expected, pair.found,
                        descriptions.get(pair.expected.block), descriptions.get(pair.found.block)));
                }
                cells[i] = id + 1;
            }
            return new Result(this, cells, unique, types);
        }
    }

    static final class Result {
        private final Batch batch;
        private final int[] cells;
        private final List<Pair> pairs;
        private final List<Type> types;
        private Result(Batch batch, int[] cells, List<Pair> pairs, List<Type> types) {
            this.batch = batch; this.cells = cells; this.pairs = pairs; this.types = types;
        }
    }

    Batch capture(Reader reader, int limit, long deadline, VerificationRules rules) {
        if (inFlight != null || limit < 1 || System.nanoTime() >= deadline) return null;
        int capacity = Math.min(4096, limit), attempts = pending.size(), count = 0;
        int[] indices = new int[capacity], statuses = new int[capacity];
        Pair[] pairs = new Pair[capacity];
        Map<String, BlockGroups.Snapshot> descriptions = new HashMap<>();
        while (count < capacity && System.nanoTime() < deadline) {
            if (active == null) {
                if (attempts-- <= 0 || pending.isEmpty()) break;
                active = pending.remove();
            }
            if (active.section < 0) active.beginSection();
            Part part = new Part(active, count);
            Boolean loaded = null;
            while (count < capacity && active.cursor < active.end && System.nanoTime() < deadline) {
                int sx = active.minX + active.cursor % active.width;
                int sz = active.minZ + active.cursor / active.width % active.length;
                int sy = bounds[1] + active.cursor / (active.width * active.length);
                indices[count] = sx - bounds[0] + width * (sz - bounds[2] + length * (sy - bounds[1]));
                try {
                    if (!reader.included(sx, sy, sz)) statuses[count] = -1;
                    else {
                        if (loaded == null) loaded = reader.loaded(active.chunkX, active.chunkZ);
                        if (!loaded) break;
                        part.needsChunk = true;
                        State expected = Objects.requireNonNull(reader.expected(sx, sy, sz));
                        State found = Objects.requireNonNull(reader.found(x + sx, y + sy, z + sz));
                        if (expected.air() && (found.air() || reader.ignorable(found))) statuses[count] = -1;
                        else {
                            descriptions.computeIfAbsent(expected.block, BlockGroups::snapshot);
                            descriptions.computeIfAbsent(found.block, BlockGroups::snapshot);
                            pairs[count] = new Pair(expected, found);
                        }
                    }
                } catch (RuntimeException error) {
                    statuses[count] = -2;
                    reader.failed(x + sx, y + sy, z + sz, error);
                }
                active.cursor++; count++;
            }
            part.end = count;
            if (part.end > part.start) { captured.add(part); held.add(active); }
            if (active.cursor == active.end) {
                active.section = -1;
                if (active.sections == 0) active = null;
            } else {
                if (!held.contains(active)) pending.add(active);
                active = null;
            }
        }
        active = null;
        if (count == 0) return null;
        inFlight = new Batch(Arrays.copyOf(indices, count), Arrays.copyOf(statuses, count), Arrays.copyOf(pairs, count), descriptions, rules);
        return inFlight;
    }

    boolean apply(Result result, Reader reader) {
        if (result.batch != inFlight || inFlight == null) return false;
        Set<Work> invalid = new HashSet<>();
        for (Part part : captured) if (part.work.version != part.version
            || part.needsChunk && !reader.loaded(part.work.chunkX, part.work.chunkZ)) invalid.add(part.work);
        int[] ids = new int[result.pairs.size()];
        boolean applied = false;
        for (Part part : captured) {
            if (invalid.contains(part.work)) {
                part.work.sections |= 1 << part.section;
                part.work.section = -1;
                continue;
            }
            active = part.work;
            for (int i = part.start; i < part.end; i++) {
                int local = result.cells[i], id = local;
                if (local > 0) {
                    if (ids[local - 1] == 0) {
                        Pair pair = result.pairs.get(local - 1);
                        Integer known = groupIds.get(pair);
                        if (known == null) { known = groups.size(); groups.add(new Group(pair, result.types.get(local - 1))); groupIds.put(pair, known); }
                        ids[local - 1] = known;
                    }
                    id = ids[local - 1];
                }
                replace(inFlight.indices[i], id);
            }
            applied = true;
        }
        releaseCaptured();
        return applied;
    }

    void cancelBatch() {
        if (inFlight == null) return;
        for (Part part : captured) {
            part.work.sections |= 1 << part.section;
            part.work.section = -1;
        }
        releaseCaptured();
    }

    private void releaseCaptured() {
        for (Work work : held) {
            if (work.section >= 0 || work.sections != 0) pending.add(work);
            else work.queued = false;
        }
        active = null; inFlight = null; captured.clear(); held.clear();
    }

    private final class Part {
        final Work work;
        final int start, section;
        final long version;
        int end;
        boolean needsChunk;
        Part(Work work, int start) { this.work = work; this.start = start; section = work.section; version = work.version; }
    }

    private void replace(int index, int id) {
        int old = cells[index];
        if (old == 0) checked++;
        if (old == id) return;
        if (old > 0) groups.get(old).count--;
        if (old > 0 && groups.get(old).type != Type.CORRECT) active.errors--;
        if (old == -2) skipped--;
        cells[index] = id;
        if (id > 0) groups.get(id).count++;
        if (id > 0 && groups.get(id).type != Type.CORRECT) active.errors++;
        if (id == -2) skipped++;
        revision++;
    }

    public void changed(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        if (total == 0 || minX > maxX || minY > maxY || minZ > maxZ
            || maxY < (long) y + bounds[1] || minY >= (long) y + bounds[4]) return;
        int firstX = Math.max(minX >> 4, minChunkX), firstZ = Math.max(minZ >> 4, minChunkZ);
        int lastX = Math.min(maxX >> 4, minChunkX + chunks.size() / chunkColumns - 1);
        int lastZ = Math.min(maxZ >> 4, minChunkZ + chunkColumns - 1);
        int mask = 0;
        for (int sy = Math.max(minY, y + bounds[1]) >> 4; sy <= (Math.min(maxY, y + bounds[4] - 1) >> 4); sy++) mask |= 1 << sy;
        for (int cx = firstX; cx <= lastX; cx++) for (int cz = firstZ; cz <= lastZ; cz++) {
            Work chunk = chunks.get((cx - minChunkX) * chunkColumns + cz - minChunkZ);
            chunk.sections |= mask;
            chunk.version++;
            if (!chunk.queued) { chunk.queued = true; pending.add(chunk); }
        }
    }

    public boolean done() { return inFlight == null && active == null && pending.isEmpty(); }
    public int remainingChunks() { return pending.size() + held.size() + (active != null && !held.contains(active) ? 1 : 0); }
    public int totalChunks() { return chunks.size(); }
    /** The chunk coordinates still to verify, closest to the block position first. */
    public List<int[]> pendingChunks(int blockX, int blockZ) {
        List<int[]> result = new ArrayList<>();
        for (Work work : held) result.add(new int[] {work.chunkX, work.chunkZ});
        if (active != null && !held.contains(active)) result.add(new int[] {active.chunkX, active.chunkZ});
        for (Work work : pending) if (work != active) result.add(new int[] {work.chunkX, work.chunkZ});
        result.sort(Comparator.comparingLong(c -> {
            long dx = (c[0] << 4) + 8 - blockX, dz = (c[1] << 4) + 8 - blockZ;
            return dx * dx + dz * dz;
        }));
        return result;
    }
    public int checked() { return checked; }
    public int total() { return total; }
    public int skipped() { return skipped; }
    public long revision() { return revision; }
    public Set<Pair> ignored() { return Collections.unmodifiableSet(ignored); }
    public void ignore(Group group) { if (group.type != Type.CORRECT && ignored.add(group.pair)) revision++; }
    public void addIgnored(Set<Pair> values) { ignored.addAll(values); revision++; }
    public void resetIgnored() { ignored.clear(); revision++; }
    public boolean ignored(Group group) { return ignored.contains(group.pair); }

    public List<Group> groups(Type type) {
        List<Group> result = new ArrayList<>();
        for (int i = 1; i < groups.size(); i++) {
            Group group = groups.get(i);
            if (group.count > 0 && !ignored(group) && (type == Type.ALL ? group.type != Type.CORRECT : group.type == type)) result.add(group);
        }
        return result;
    }

    public int count(Type type) {
        int count = 0;
        for (int i = 1; i < groups.size(); i++) {
            Group group = groups.get(i);
            if (!ignored(group) && (type == Type.ALL ? group.type != Type.CORRECT : group.type == type)) count += group.count;
        }
        return count;
    }
    public int expectedBlocks() {
        int count = 0;
        for (int i = 1; i < groups.size(); i++) if (!groups.get(i).pair.expected.air()) count += groups.get(i).count;
        return count;
    }

    public Group at(int worldX, int worldY, int worldZ) {
        int sx = worldX - x, sy = worldY - y, sz = worldZ - z;
        if (sx < bounds[0] || sy < bounds[1] || sz < bounds[2] || sx >= bounds[3] || sy >= bounds[4] || sz >= bounds[5]) return null;
        int id = cells[sx - bounds[0] + width * (sz - bounds[2] + length * (sy - bounds[1]))];
        return id > 0 ? groups.get(id) : null;
    }

    public static final class Marker {
        public final int x, y, z;
        public final Group group;
        private final double distance;
        private Marker(int x, int y, int z, Group group, double distance) {
            this.x = x; this.y = y; this.z = z; this.group = group; this.distance = distance;
        }
    }

    public MarkerSearch closest(VerificationSelection selection, double cx, double cy, double cz, int limit) {
        return new MarkerSearch(selection, cx, cy, cz, limit);
    }

    public final class MarkerSearch {
        private final double cx, cy, cz;
        private final int limit;
        private final BitSet selected = new BitSet();
        private final Comparator<Marker> order = Comparator.comparingDouble((Marker m) -> m.distance)
            .thenComparingInt(m -> m.x).thenComparingInt(m -> m.y).thenComparingInt(m -> m.z);
        private final PriorityQueue<Marker> nearest = new PriorityQueue<>(order.reversed());
        private final PriorityQueue<Work> remaining;
        private Work chunk;
        private int cursor;

        private MarkerSearch(VerificationSelection selection, double cx, double cy, double cz, int limit) {
            if (limit < 1 || !Double.isFinite(cx) || !Double.isFinite(cy) || !Double.isFinite(cz)) throw new IllegalArgumentException();
            this.cx = cx; this.cy = cy; this.cz = cz; this.limit = limit;
            for (int i = 1; i < groups.size(); i++) if (groups.get(i).count > 0 && selection.includes(groups.get(i)) && !ignored(groups.get(i))) selected.set(i);
            remaining = new PriorityQueue<>(Comparator.comparingDouble(this::distance));
            if (!selected.isEmpty()) for (Work work : chunks) if (work.errors > 0) remaining.add(work);
        }

        private double distance(Work work) {
            double dx = gap(cx, x + work.minX + 0.5, x + work.minX + work.width - 0.5);
            double dy = gap(cy, y + bounds[1] + 0.5, y + bounds[4] - 0.5);
            double dz = gap(cz, z + work.minZ + 0.5, z + work.minZ + work.length - 0.5);
            return dx * dx + dy * dy + dz * dz;
        }

        public boolean done() { return chunk == null && remaining.isEmpty(); }

        public void step(int budget, long deadline) {
            for (int n = 0; n < budget && System.nanoTime() < deadline; n++) {
                if (chunk == null) {
                    if (remaining.isEmpty()) return;
                    chunk = remaining.remove(); cursor = 0;
                    if (nearest.size() == limit && distance(chunk) > nearest.peek().distance) {
                        chunk = null; remaining.clear(); return;
                    }
                }
                int sx = chunk.minX + cursor % chunk.width;
                int sz = chunk.minZ + cursor / chunk.width % chunk.length;
                int sy = bounds[1] + cursor / (chunk.width * chunk.length);
                int id = cells[sx - bounds[0] + width * (sz - bounds[2] + length * (sy - bounds[1]))];
                if (id > 0 && selected.get(id)) {
                    double dx = x + sx + 0.5 - cx, dy = y + sy + 0.5 - cy, dz = z + sz + 0.5 - cz;
                    double distance = dx * dx + dy * dy + dz * dz;
                    if (nearest.size() < limit || distance <= nearest.peek().distance) {
                        Marker marker = new Marker(x + sx, y + sy, z + sz, groups.get(id), distance);
                        if (nearest.size() < limit) nearest.add(marker);
                        else if (order.compare(marker, nearest.peek()) < 0) { nearest.remove(); nearest.add(marker); }
                    }
                }
                if (++cursor == chunk.volume) chunk = null;
            }
        }

        public List<Marker> result() {
            List<Marker> result = new ArrayList<>(nearest);
            result.sort(order);
            return Collections.unmodifiableList(result);
        }
    }

    private static double gap(double value, double min, double max) { return Math.max(0, Math.max(min - value, value - max)); }

    private final class Work {
        final int chunkX, chunkZ, minX, minZ, width, length, volume;
        int cursor, end, errors, sections, section = -1;
        long version;
        boolean queued = true;
        Work(int cx, int cz) {
            chunkX = cx; chunkZ = cz;
            minX = Math.max(bounds[0], (cx << 4) - x);
            minZ = Math.max(bounds[2], (cz << 4) - z);
            width = Math.min(bounds[3], (cx << 4) + 16 - x) - minX;
            length = Math.min(bounds[5], (cz << 4) + 16 - z) - minZ;
            volume = width * length * (bounds[4] - bounds[1]);
            for (int sy = (y + bounds[1]) >> 4; sy <= ((y + bounds[4] - 1) >> 4); sy++) sections |= 1 << sy;
        }

        void beginSection() {
            if (sections == 0) return;
            section = Integer.numberOfTrailingZeros(sections);
            sections &= ~(1 << section);
            cursor = width * length * (Math.max(bounds[1], (section << 4) - y) - bounds[1]);
            end = width * length * (Math.min(bounds[4], ((section + 1) << 4) - y) - bounds[1]);
        }
    }
}

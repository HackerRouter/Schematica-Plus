package com.github.lunatrius.schematica.client.verifier;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.BitSet;
import java.util.Comparator;
import java.util.PriorityQueue;

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
        private Group(Pair pair) { this.pair = pair; type = classify(pair.expected, pair.found); }
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
        switch (com.github.lunatrius.schematica.util.BlockGroups.compare(expected.block, expected.metadata, found.block, found.metadata,
            com.github.lunatrius.schematica.util.BlockGroups.enabled)) {
            case DIFFERENT_BLOCK: return Type.DIFF_BLOCK;
            case WRONG_STATE: return Type.WRONG_STATE;
            default: return Type.WRONG_BLOCK;
        }
    }

    public void step(Reader reader, int limit, long deadline) {
        int attempts = pending.size();
        Boolean loaded = null;
        for (int work = 0; work < limit && System.nanoTime() < deadline; work++) {
            if (active == null) {
                if (attempts-- <= 0 || pending.isEmpty()) break;
                active = pending.remove();
                loaded = null;
            }
            int sx = active.minX + active.cursor % active.width;
            int sz = active.minZ + active.cursor / active.width % active.length;
            int sy = bounds[1] + active.cursor / (active.width * active.length);
            int index = sx - bounds[0] + width * (sz - bounds[2] + length * (sy - bounds[1]));
            try {
                if (!reader.included(sx, sy, sz)) replace(index, -1);
                else {
                    if (loaded == null) loaded = reader.loaded(active.chunkX, active.chunkZ);
                    if (!loaded) { pending.add(active); active = null; continue; }
                    State expected = Objects.requireNonNull(reader.expected(sx, sy, sz));
                    State found = Objects.requireNonNull(reader.found(x + sx, y + sy, z + sz));
                    if (expected.air() && (found.air() || reader.ignorable(found))) replace(index, -1);
                    else {
                        Pair pair = new Pair(expected, found);
                        Integer id = groupIds.get(pair);
                        if (id == null) { id = groups.size(); groups.add(new Group(pair)); groupIds.put(pair, id); }
                        replace(index, id);
                    }
                }
            } catch (RuntimeException error) {
                replace(index, -2);
                reader.failed(x + sx, y + sy, z + sz, error);
            }
            if (++active.cursor == active.volume) {
                active.queued = false;
                if (active.again) enqueue(active);
                active = null;
            }
        }
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
        if (total == 0 || maxY < (long) y + bounds[1] || minY >= (long) y + bounds[4]) return;
        int firstX = Math.max(minX >> 4, minChunkX), firstZ = Math.max(minZ >> 4, minChunkZ);
        int lastX = Math.min(maxX >> 4, minChunkX + chunks.size() / chunkColumns - 1);
        int lastZ = Math.min(maxZ >> 4, minChunkZ + chunkColumns - 1);
        for (int cx = firstX; cx <= lastX; cx++) for (int cz = firstZ; cz <= lastZ; cz++) {
            Work chunk = chunks.get((cx - minChunkX) * chunkColumns + cz - minChunkZ);
            if (chunk == active) chunk.again = true;
            else if (!chunk.queued) enqueue(chunk);
        }
    }

    private void enqueue(Work work) {
        work.cursor = 0; work.queued = true; work.again = false;
        pending.add(work);
    }

    public boolean done() { return active == null && pending.isEmpty(); }
    public int remainingChunks() { return pending.size() + (active == null ? 0 : 1); }
    public int totalChunks() { return chunks.size(); }
    /** The chunk coordinates still to verify, closest to the block position first. */
    public List<int[]> pendingChunks(int blockX, int blockZ) {
        List<int[]> result = new ArrayList<>();
        if (active != null) result.add(new int[] {active.chunkX, active.chunkZ});
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
        int cursor, errors;
        boolean queued = true, again;
        Work(int cx, int cz) {
            chunkX = cx; chunkZ = cz;
            minX = Math.max(bounds[0], (cx << 4) - x);
            minZ = Math.max(bounds[2], (cz << 4) - z);
            width = Math.min(bounds[3], (cx << 4) + 16 - x) - minX;
            length = Math.min(bounds[5], (cz << 4) + 16 - z) - minZ;
            volume = width * length * (bounds[4] - bounds[1]);
        }
    }
}

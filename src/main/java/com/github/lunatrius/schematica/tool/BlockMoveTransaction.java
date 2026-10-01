package com.github.lunatrius.schematica.tool;

import java.util.BitSet;

public final class BlockMoveTransaction<T> {
    public interface Access<T> {
        T read(int x, int y, int z);
        void write(int x, int y, int z, T value);
        void changed(int x, int y, int z);
    }
    public enum Phase { CAPTURE, CLEAR, PLACE, RESTORE_SOURCE, RESTORE_TARGET, UPDATES, DONE }
    private final int x, y, z, width, height, length, dx, dy, dz, volume;
    private final BitSet selected;
    private final Object[] source, target;
    private final T air;
    private Phase phase = Phase.CAPTURE;
    private int cursor;
    private boolean mutated, cancelled, rollingBack;
    private RuntimeException failure;

    public BlockMoveTransaction(int x, int y, int z, int width, int height, int length, int dx, int dy, int dz, BitSet selected, T air) {
        if (width < 1 || height < 1 || length < 1 || (long) width * height > 1_048_576L
            || (long) width * height * length > 1_048_576L) throw new IllegalArgumentException("Move volume exceeds limit");
        this.x = x; this.y = y; this.z = z; this.width = width; this.height = height; this.length = length;
        this.dx = dx; this.dy = dy; this.dz = dz; this.air = air;
        volume = width * height * length; this.selected = (BitSet) selected.clone();
        if (this.selected.length() > volume) throw new IllegalArgumentException("Invalid move mask");
        source = new Object[volume]; target = new Object[volume];
    }
    public Phase phase() { return phase; }
    public int completed() { return cursor; }
    public int volume() { return volume; }
    public int affected() { return selected.cardinality(); }
    public RuntimeException failure() { return failure; }
    public boolean successful() { return phase == Phase.DONE && !cancelled && failure == null; }
    public boolean mutated() { return mutated; }

    public boolean step(Access<T> access, boolean cancel) {
        if (phase == Phase.DONE) return true;
        if (cancel && !rollingBack) { cancelled = true; restore(); }
        if (phase == Phase.DONE) return true;
        if (cursor == volume) { advance(); return phase == Phase.DONE; }
        int index = cursor++;
        if (!selected.get(index)) return false;
        int sx = x + index % width, sy = y + index / width / length, sz = z + index / width % length;
        try {
            switch (phase) {
                case CAPTURE: source[index] = access.read(sx, sy, sz); target[index] = access.read(sx + dx, sy + dy, sz + dz); break;
                case CLEAR: mutated = true; access.write(sx, sy, sz, air); break;
                case PLACE: access.write(sx + dx, sy + dy, sz + dz, value(source, index)); break;
                case RESTORE_SOURCE: access.write(sx, sy, sz, value(source, index)); break;
                case RESTORE_TARGET: access.write(sx + dx, sy + dy, sz + dz, value(target, index)); break;
                case UPDATES: access.changed(sx, sy, sz); access.changed(sx + dx, sy + dy, sz + dz); break;
                default: break;
            }
        } catch (RuntimeException error) {
            if (failure == null) failure = error;
            if (rollingBack) { phase = Phase.DONE; throw error; }
            restore();
        }
        return phase == Phase.DONE;
    }
    @SuppressWarnings("unchecked") private T value(Object[] values, int index) { return (T) values[index]; }
    private void restore() { rollingBack = true; phase = mutated ? Phase.RESTORE_SOURCE : Phase.DONE; cursor = 0; }
    private void advance() {
        cursor = 0;
        switch (phase) {
            case CAPTURE: phase = Phase.CLEAR; break;
            case CLEAR: phase = Phase.PLACE; break;
            case PLACE: phase = Phase.UPDATES; break;
            case RESTORE_SOURCE: phase = Phase.RESTORE_TARGET; break;
            case RESTORE_TARGET: phase = Phase.UPDATES; break;
            default: phase = Phase.DONE; break;
        }
    }
}

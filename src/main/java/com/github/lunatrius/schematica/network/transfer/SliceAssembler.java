package com.github.lunatrius.schematica.network.transfer;

import java.io.ByteArrayOutputStream;

public final class SliceAssembler {
    private final byte[][] parts;
    private final int limit;
    private int received, bytes;

    private SliceAssembler(int count, int limit) {
        this.parts = new byte[count][];
        this.limit = limit;
    }

    public static SliceAssembler create(int count, int sliceSize, int limit) {
        if (count < 1 || (long) count * sliceSize > (long) limit + sliceSize) return null;
        return new SliceAssembler(count, limit);
    }

    public int count() { return parts.length; }

    public int bytes() { return bytes; }

    public boolean add(int index, int count, byte[] data) {
        if (count != parts.length || index < 0 || index >= parts.length || parts[index] != null) return false;
        if ((bytes += data.length) > limit) return false;
        parts[index] = data;
        received++;
        return true;
    }

    public boolean complete() { return received == parts.length; }

    public byte[] join() {
        ByteArrayOutputStream joined = new ByteArrayOutputStream(bytes);
        for (byte[] part : parts) joined.write(part, 0, part.length);
        return joined.toByteArray();
    }
}

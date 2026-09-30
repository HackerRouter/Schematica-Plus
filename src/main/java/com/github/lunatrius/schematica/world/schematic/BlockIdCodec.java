package com.github.lunatrius.schematica.world.schematic;

/** Unsigned 16-bit block IDs used by the schemplus format. */
public final class BlockIdCodec {
    private BlockIdCodec() {}
    public static byte low(int id) { return (byte) (id & 255); }
    public static byte high(int id) { return (byte) ((id >>> 8) & 255); }
    public static int decode(byte low, byte high) { return (low & 255) | ((high & 255) << 8); }
}

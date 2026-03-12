package com.github.lunatrius.schematica.world.schematic;

/**
 * Unpacks bit-packed long arrays used by the Litematica schematic format.
 * <p>
 * In .litematic files, block state palette indices are stored as tightly packed
 * integers within a long[] array. Each entry uses exactly {@code bitsPerEntry} bits,
 * and entries DO NOT cross long boundaries (tight packing, not spanning).
 * <p>
 * This is a direct port of Litematica's TightLongBackedIntArray logic.
 *
 * @author HackerRouter
 */
public final class LitematicBitArray {

    private final long[] data;
    private final int bitsPerEntry;
    private final long maxEntryValue;
    private final long totalEntries;

    /**
     * @param bitsPerEntry number of bits each palette index occupies
     * @param totalEntries total number of block entries (width * height * length)
     * @param data         the raw long[] from the NBT "BlockStates" tag
     */
    public LitematicBitArray(int bitsPerEntry, long totalEntries, long[] data) {
        this.bitsPerEntry = bitsPerEntry;
        this.totalEntries = totalEntries;
        this.maxEntryValue = (1L << bitsPerEntry) - 1L;
        this.data = data;
    }

    /**
     * Retrieves the palette index at the given position.
     *
     * @param index the linear block index (x + z * sizeX + y * sizeX * sizeZ)
     * @return the palette index stored at that position
     */
    public int getAt(long index) {
        long entriesPerLong = 64L / bitsPerEntry;
        long longIndex = index / entriesPerLong;
        long bitOffset = (index % entriesPerLong) * bitsPerEntry;

        if (longIndex < 0 || longIndex >= data.length) {
            return 0;
        }

        return (int) ((data[(int) longIndex] >>> bitOffset) & maxEntryValue);
    }

    /**
     * @return total number of entries this array can hold
     */
    public long size() {
        return totalEntries;
    }

    /**
     * Calculates the minimum number of bits needed to represent {@code paletteSize} entries.
     *
     * @param paletteSize number of entries in the block state palette
     * @return minimum bits per entry (at least 2)
     */
    public static int getRequiredBits(int paletteSize) {
        int bits = 0;
        int value = paletteSize - 1;
        while (value > 0) {
            value >>= 1;
            bits++;
        }
        return Math.max(2, bits);
    }
}

package com.github.lunatrius.schematica.world.schematic;

/**
 * Unpacks bit-packed long arrays used by the Litematica schematic format.
 * <p>
 * In .litematic files (all format versions 1-7+), block state palette indices are stored
 * as tightly packed integers within a long[] array. Each entry uses exactly {@code bitsPerEntry}
 * bits, and entries CAN cross long boundaries (tight packing).
 * <p>
 * Array length formula: roundUp(totalEntries * bitsPerEntry, 64) / 64
 * <p>
 * This is a direct port of Litematica's LitematicaBitArray / TightLongBackedIntArray logic.
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
     * Retrieves the palette index at the given position using tight packing.
     * Entries can span across two longs.
     *
     * @param index the linear block index (x + z * sizeX + y * sizeX * sizeZ)
     * @return the palette index stored at that position
     */
    public int getAt(long index) {
        long startOffset = index * (long) this.bitsPerEntry;
        int startArrIndex = (int) (startOffset >> 6); // startOffset / 64
        int endArrIndex = (int) (((index + 1L) * (long) this.bitsPerEntry - 1L) >> 6);
        int startBitOffset = (int) (startOffset & 0x3F); // startOffset % 64

        if (startArrIndex < 0 || startArrIndex >= data.length) {
            return 0;
        }

        if (startArrIndex == endArrIndex) {
            return (int) (this.data[startArrIndex] >>> startBitOffset & this.maxEntryValue);
        } else {
            if (endArrIndex >= data.length) {
                return 0;
            }
            int endOffset = 64 - startBitOffset;
            return (int) ((this.data[startArrIndex] >>> startBitOffset | this.data[endArrIndex] << endOffset) & this.maxEntryValue);
        }
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
        return Math.max(2, Integer.SIZE - Integer.numberOfLeadingZeros(paletteSize - 1));
    }
}

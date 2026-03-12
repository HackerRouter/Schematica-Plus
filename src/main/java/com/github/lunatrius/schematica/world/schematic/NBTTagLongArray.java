package com.github.lunatrius.schematica.world.schematic;

/**
 * Data container for TAG_Long_Array (type 12), which doesn't exist in MC 1.7.10.
 * Modern .litematic files use this tag type for the BlockStates data.
 * <p>
 * This is NOT an NBTBase subclass — it's a standalone data holder used by
 * {@link LitematicaNBTReader} and {@link SchematicLitematica}. We avoid extending
 * NBTBase because the abstract methods are package-private in net.minecraft.nbt
 * and cannot be overridden from a different package.
 *
 * @author HackerRouter
 */
public class NBTTagLongArray {

    /** NBT tag type ID for long arrays (added in MC 1.12) */
    public static final byte TAG_LONG_ARRAY = 12;

    private final long[] data;

    public NBTTagLongArray(long[] data) {
        this.data = data != null ? data : new long[0];
    }

    public long[] getLongArray() {
        return this.data;
    }

    public int size() {
        return this.data.length;
    }

    @Override
    public String toString() {
        return "[" + data.length + " longs]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof NBTTagLongArray)) return false;
        NBTTagLongArray other = (NBTTagLongArray) obj;
        return java.util.Arrays.equals(data, other.data);
    }

    @Override
    public int hashCode() {
        return java.util.Arrays.hashCode(data);
    }
}

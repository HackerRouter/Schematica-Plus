package com.github.lunatrius.schematica.world.schematic;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.nbt.NBTTagString;

import com.github.lunatrius.schematica.util.SchematicLimits;

/** Bounded legacy/modern NBT reader. Long arrays use a thread-local side channel. */
public final class LitematicaNBTReader {
    private static final ThreadLocal<Map<NBTTagCompound, Map<String, long[]>>> LONG_ARRAY_STORE =
        ThreadLocal.withInitial(IdentityHashMap::new);
    private static final ThreadLocal<Boolean> DROPPED = ThreadLocal.withInitial(() -> false);

    private LitematicaNBTReader() {}

    public static NBTTagCompound readFromFile(File file) throws IOException {
        clearLongArrayStore();
        return readFromStream(new FileInputStream(file));
    }

    public static NBTTagCompound readFromStream(InputStream source) throws IOException {
        clearLongArrayStore();
        try (BufferedInputStream stream = new BufferedInputStream(source)) {
            stream.mark(2);
            boolean gzip = stream.read() == 0x1f && stream.read() == 0x8b;
            stream.reset();
            try (DataInputStream input = new DataInputStream(gzip ? new GZIPInputStream(stream) : stream)) {
                Reader reader = new Reader(input);
                if (input.readByte() != 10) throw new IOException("NBT root must be a compound");
                reader.string();
                return (NBTTagCompound) reader.tag(10, 0);
            }
        } catch (IOException | RuntimeException e) {
            clearLongArrayStore();
            throw e;
        }
    }

    public static void clearLongArrayStore() {
        LONG_ARRAY_STORE.remove();
        DROPPED.remove();
    }

    /** The long arrays of the last read on this thread, keyed by their parent compound, for writing the tree back. */
    public static Map<NBTTagCompound, Map<String, long[]>> longArrays() { return LONG_ARRAY_STORE.get(); }

    /** Whether the last read skipped long arrays inside lists, which a rewrite would lose. */
    public static boolean droppedData() { return DROPPED.get(); }

    public static long[] getLongArray(NBTTagCompound compound, String key) {
        Map<String, long[]> arrays = LONG_ARRAY_STORE.get().get(compound);
        if (arrays != null && arrays.containsKey(key)) return arrays.get(key);
        NBTBase tag = compound.getTag(key);
        if (tag instanceof NBTTagIntArray) {
            int[] values = ((NBTTagIntArray) tag).func_150302_c();
            if ((values.length & 1) != 0) throw new IllegalArgumentException("Odd packed long array length");
            long[] result = new long[values.length / 2];
            for (int i = 0; i < result.length; i++) {
                result[i] = (values[i * 2] & 0xffffffffL) | ((long) values[i * 2 + 1] << 32);
            }
            return result;
        }
        return null;
    }

    private static final class Reader {
        private final DataInputStream input;
        private long remaining = SchematicLimits.MAX_NBT_BYTES;

        Reader(DataInputStream input) { this.input = input; }

        private void charge(long bytes) throws IOException {
            if (bytes < 0 || bytes > remaining) throw new IOException("NBT exceeds allocation limit");
            remaining -= bytes;
        }

        private String string() throws IOException {
            String value = input.readUTF();
            charge(40L + 2L * value.length());
            return value;
        }

        private int length(int bytesPerElement) throws IOException {
            int length = input.readInt();
            if (length < 0) throw new IOException("Negative NBT array/list length");
            charge(24L + (long) length * bytesPerElement);
            return length;
        }

        private long[] longArray() throws IOException {
            long[] values = new long[length(8)];
            for (int i = 0; i < values.length; i++) values[i] = input.readLong();
            return values;
        }

        private NBTBase tag(int type, int depth) throws IOException {
            if (depth > SchematicLimits.MAX_NBT_DEPTH) throw new IOException("NBT nesting is too deep");
            charge(64);
            switch (type) {
                case 1: return new NBTTagByte(input.readByte());
                case 2: return new NBTTagShort(input.readShort());
                case 3: return new NBTTagInt(input.readInt());
                case 4: return new NBTTagLong(input.readLong());
                case 5: return new NBTTagFloat(input.readFloat());
                case 6: return new NBTTagDouble(input.readDouble());
                case 7: {
                    byte[] values = new byte[length(1)];
                    input.readFully(values);
                    return new NBTTagByteArray(values);
                }
                case 8: return new NBTTagString(string());
                case 9: {
                    int elementType = input.readUnsignedByte();
                    int size = length(8);
                    if (elementType > 12 || (elementType == 0 && size != 0)) {
                        throw new IOException("Invalid NBT list type");
                    }
                    NBTTagList list = new NBTTagList();
                    for (int i = 0; i < size; i++) {
                        if (elementType == 12) {
                            longArray();
                            DROPPED.set(true);
                        }
                        else list.appendTag(tag(elementType, depth + 1));
                    }
                    return list;
                }
                case 10: {
                    NBTTagCompound compound = new NBTTagCompound();
                    int childType;
                    while ((childType = input.readUnsignedByte()) != 0) {
                        String name = string();
                        if (childType == 12) {
                            charge(64);
                            LONG_ARRAY_STORE.get().computeIfAbsent(compound, key -> new HashMap<>())
                                .put(name, longArray());
                        } else {
                            compound.setTag(name, tag(childType, depth + 1));
                        }
                    }
                    return compound;
                }
                case 11: {
                    int[] values = new int[length(4)];
                    for (int i = 0; i < values.length; i++) values[i] = input.readInt();
                    return new NBTTagIntArray(values);
                }
                default: throw new IOException("Unknown NBT tag type: " + type);
            }
        }
    }
}

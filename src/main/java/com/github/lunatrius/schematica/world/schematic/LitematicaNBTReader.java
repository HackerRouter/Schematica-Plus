package com.github.lunatrius.schematica.world.schematic;

import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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

import com.github.lunatrius.schematica.reference.Reference;

/**
 * Custom NBT reader that handles the full modern NBT specification,
 * including TAG_Long_Array (type 12) which doesn't exist in MC 1.7.10.
 * <p>
 * .litematic files are GZip-compressed NBT written by modern MC versions.
 * The standard 1.7.10 CompressedStreamTools will crash on TAG_Long_Array,
 * so we need this custom reader.
 * <p>
 * Since NBTTagLongArray cannot extend NBTBase (abstract methods are package-private
 * in net.minecraft.nbt), long arrays are stored in a side-channel map keyed by
 * compound identity + tag name. Use {@link #getLongArray(NBTTagCompound, String)}
 * to retrieve them.
 *
 * @author HackerRouter
 */
public final class LitematicaNBTReader {

    // NBT tag type IDs
    private static final byte TAG_END = 0;
    private static final byte TAG_BYTE = 1;
    private static final byte TAG_SHORT = 2;
    private static final byte TAG_INT = 3;
    private static final byte TAG_LONG = 4;
    private static final byte TAG_FLOAT = 5;
    private static final byte TAG_DOUBLE = 6;
    private static final byte TAG_BYTE_ARRAY = 7;
    private static final byte TAG_STRING = 8;
    private static final byte TAG_LIST = 9;
    private static final byte TAG_COMPOUND = 10;
    private static final byte TAG_INT_ARRAY = 11;
    private static final byte TAG_LONG_ARRAY = 12;

    /**
     * Side-channel storage for long arrays that can't be stored in NBTTagCompound.
     * Keyed by (compound identity, tag name) -> long[].
     * Uses IdentityHashMap so we key on the exact compound object instance.
     */
    private static final Map<NBTTagCompound, Map<String, long[]>> LONG_ARRAY_STORE = new IdentityHashMap<>();

    private LitematicaNBTReader() {}

    /**
     * Reads a .litematic file (GZip-compressed NBT with TAG_Long_Array support).
     *
     * @param file the .litematic file
     * @return the root NBTTagCompound
     * @throws IOException if reading fails
     */
    public static NBTTagCompound readFromFile(File file) throws IOException {
        // Clear any stale data from previous reads
        LONG_ARRAY_STORE.clear();

        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(
                    new GZIPInputStream(
                        new FileInputStream(file))))) {
            return readRootCompound(dis);
        }
    }

    /**
     * Clears the side-channel long array storage. Call after you're done reading
     * a .litematic file to free memory.
     */
    public static void clearLongArrayStore() {
        LONG_ARRAY_STORE.clear();
    }

    /**
     * Reads the root compound tag (expects TAG_Compound with name at the top level).
     */
    private static NBTTagCompound readRootCompound(DataInput input) throws IOException {
        byte tagType = input.readByte();
        if (tagType != TAG_COMPOUND) {
            throw new IOException("Root tag is not a compound (got type " + tagType + ")");
        }
        // Read root tag name (usually empty string for .litematic)
        input.readUTF();
        return readCompoundPayload(input);
    }

    /**
     * Reads the payload of a compound tag (the key-value pairs until TAG_End).
     * Long arrays are stored in the side-channel since they can't be NBTBase.
     */
    private static NBTTagCompound readCompoundPayload(DataInput input) throws IOException {
        NBTTagCompound compound = new NBTTagCompound();

        while (true) {
            byte tagType = input.readByte();
            if (tagType == TAG_END) {
                break;
            }

            String name = input.readUTF();

            if (tagType == TAG_LONG_ARRAY) {
                // Read long array and store in side-channel
                int length = input.readInt();
                long[] data = new long[length];
                for (int i = 0; i < length; i++) {
                    data[i] = input.readLong();
                }
                Map<String, long[]> compoundMap = LONG_ARRAY_STORE.get(compound);
                if (compoundMap == null) {
                    compoundMap = new java.util.HashMap<>();
                    LONG_ARRAY_STORE.put(compound, compoundMap);
                }
                compoundMap.put(name, data);
            } else {
                NBTBase tag = readTagPayload(tagType, input);
                if (tag != null) {
                    compound.setTag(name, tag);
                }
            }
        }

        return compound;
    }

    /**
     * Reads a tag payload based on its type ID.
     * Note: TAG_LONG_ARRAY is handled directly in readCompoundPayload.
     */
    private static NBTBase readTagPayload(byte tagType, DataInput input) throws IOException {
        switch (tagType) {
            case TAG_BYTE:
                return new NBTTagByte(input.readByte());

            case TAG_SHORT:
                return new NBTTagShort(input.readShort());

            case TAG_INT:
                return new NBTTagInt(input.readInt());

            case TAG_LONG:
                return new NBTTagLong(input.readLong());

            case TAG_FLOAT:
                return new NBTTagFloat(input.readFloat());

            case TAG_DOUBLE:
                return new NBTTagDouble(input.readDouble());

            case TAG_BYTE_ARRAY: {
                int length = input.readInt();
                byte[] data = new byte[length];
                input.readFully(data);
                return new NBTTagByteArray(data);
            }

            case TAG_STRING:
                return new NBTTagString(input.readUTF());

            case TAG_LIST:
                return readListPayload(input);

            case TAG_COMPOUND:
                return readCompoundPayload(input);

            case TAG_INT_ARRAY: {
                int length = input.readInt();
                int[] data = new int[length];
                for (int i = 0; i < length; i++) {
                    data[i] = input.readInt();
                }
                return new NBTTagIntArray(data);
            }

            default:
                throw new IOException("Unknown NBT tag type: " + tagType);
        }
    }

    /**
     * Reads a list tag payload.
     */
    private static NBTTagList readListPayload(DataInput input) throws IOException {
        byte elementType = input.readByte();
        int length = input.readInt();

        NBTTagList list = new NBTTagList();

        if (elementType == TAG_LONG_ARRAY) {
            // Long arrays in a list — skip them since we can't store in NBTTagList
            for (int i = 0; i < length; i++) {
                int arrLen = input.readInt();
                for (int j = 0; j < arrLen; j++) {
                    input.readLong();
                }
            }
            Reference.logger.warn("Skipped TAG_Long_Array list (unsupported in list context)");
            return list;
        }

        for (int i = 0; i < length; i++) {
            NBTBase element = readTagPayload(elementType, input);
            if (element != null) {
                list.appendTag(element);
            }
        }

        return list;
    }

    /**
     * Extracts a long[] from an NBTTagCompound key.
     * Checks the side-channel store first (for TAG_Long_Array data read by our custom reader),
     * then falls back to checking for NBTTagIntArray and converting.
     *
     * @param compound the compound to read from (must be the same object instance returned by our reader)
     * @param key      the tag key (e.g. "BlockStates")
     * @return the long array, or null if not found
     */
    public static long[] getLongArray(NBTTagCompound compound, String key) {
        // Check side-channel first
        Map<String, long[]> compoundMap = LONG_ARRAY_STORE.get(compound);
        if (compoundMap != null) {
            long[] data = compoundMap.get(key);
            if (data != null) {
                return data;
            }
        }

        // Fallback: if somehow stored as int array, convert
        NBTBase tag = compound.getTag(key);
        if (tag instanceof NBTTagIntArray) {
            Reference.logger.warn("BlockStates stored as int[] instead of long[], converting...");
            int[] intData = ((NBTTagIntArray) tag).func_150302_c();
            long[] longData = new long[intData.length / 2];
            for (int i = 0; i < longData.length; i++) {
                longData[i] = ((long) intData[i * 2] & 0xFFFFFFFFL) | ((long) intData[i * 2 + 1] << 32);
            }
            return longData;
        }
        return null;
    }
}

package com.github.lunatrius.schematica.world.schematic;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.NBTTagCompound;
import com.github.lunatrius.schematica.reference.Names;

final class SchematicBlockIds {
    static final String ENCODING = "SchematicaBlockIdEncoding";
    static final String EXTENDED = "unsigned16";
    static final String STANDARD = "nibble12";

    static int[] read(NBTTagCompound tag, int size) {
        byte[] low = tag.getByteArray(Names.NBT.BLOCKS);
        byte[] high = tag.getByteArray(Names.NBT.ADD_BLOCKS);
        if (low.length != size || tag.getByteArray(Names.NBT.DATA).length != size) {
            throw new IllegalArgumentException("Block and metadata arrays must match schematic dimensions");
        }
        String encoding = tag.getString(ENCODING);
        if (!encoding.isEmpty() && !EXTENDED.equals(encoding) && !STANDARD.equals(encoding)) {
            throw new IllegalArgumentException("Unknown block ID encoding: " + encoding);
        }
        boolean extended = EXTENDED.equals(encoding)
            || (encoding.isEmpty() && size > 2 && high.length == size);
        if (tag.hasKey(Names.NBT.ADD_BLOCKS_SCHEMATICA) && high.length == 0) {
            high = tag.getByteArray(Names.NBT.ADD_BLOCKS_SCHEMATICA);
            extended = true;
        }
        if (extended && high.length != size) {
            throw new IllegalArgumentException("Extended block ID array must match schematic dimensions");
        }
        if (!extended && high.length > (size / 2 + 1)) {
            throw new IllegalArgumentException("Invalid packed block ID array length");
        }
        boolean reversed = false;
        if (!extended && encoding.isEmpty() && tag.hasKey(Names.NBT.MAPPING_SCHEMATICA)) {
            NBTTagCompound mapping = tag.getCompoundTag(Names.NBT.MAPPING_SCHEMATICA);
            Set<Integer> ids = new HashSet<>();
            for (Object name : mapping.func_150296_c()) ids.add(mapping.getShort((String) name) & 65535);
            int normalMissing = 0;
            int reversedMissing = 0;
            for (int i = 0; i < size; i++) {
                if (!ids.contains(decode(low, high, i, false, false))) normalMissing++;
                if (!ids.contains(decode(low, high, i, false, true))) reversedMissing++;
            }
            reversed = normalMissing > 0 && reversedMissing == 0;
        }
        int[] ids = new int[size];
        for (int i = 0; i < size; i++) ids[i] = decode(low, high, i, extended, reversed);
        return ids;
    }

    private static int decode(byte[] low, byte[] high, int index, boolean extended, boolean reversed) {
        if (extended) return BlockIdCodec.decode(low[index], high[index]);
        int packed = index / 2 < high.length ? high[index / 2] & 255 : 0;
        int shift = ((index & 1) ^ (reversed ? 1 : 0)) * 4;
        return (low[index] & 255) | (((packed >>> shift) & 15) << 8);
    }

    static void write(NBTTagCompound tag, byte[] low, byte[] high, boolean extended) {
        tag.setString(ENCODING, extended ? EXTENDED : STANDARD);
        tag.setByteArray(Names.NBT.BLOCKS, low);
        if (extended) {
            tag.setByteArray(Names.NBT.ADD_BLOCKS, high);
        } else {
            byte[] packed = new byte[(high.length + 1) / 2];
            for (int i = 0; i < high.length; i++) {
                int value = high[i] & 255;
                if (value > 15) throw new IllegalArgumentException("Block IDs above 4095 require .schemplus");
                packed[i / 2] |= (byte) (value << ((i & 1) * 4));
            }
            tag.setByteArray(Names.NBT.ADD_BLOCKS, packed);
        }
    }
}

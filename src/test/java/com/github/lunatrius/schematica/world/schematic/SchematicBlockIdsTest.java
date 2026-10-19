package com.github.lunatrius.schematica.world.schematic;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class SchematicBlockIdsTest {
    private NBTTagCompound tag(byte[] low, byte[] high) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setByteArray("Blocks", low);
        tag.setByteArray("Data", new byte[low.length]);
        tag.setByteArray("AddBlocks", high);
        return tag;
    }

    private static int[] readLikeSchematica(byte[] low, byte[] packed) {
        int[] ids = new int[low.length];
        for (int i = 0; i < low.length; i++) {
            int nibble = packed[i >> 1] >> ((i & 1) == 0 ? 4 : 0) & 15;
            ids[i] = (low[i] & 255) | nibble << 8;
        }
        return ids;
    }

    @Test public void readsWorldEditNibbleOrderAndOddLength() {
        NBTTagCompound tag = tag(new byte[] {35, 86, 1}, new byte[] {0x41, 0x0f});
        assertArrayEquals(new int[] {0x123, 0x456, 0xf01}, SchematicBlockIds.read(tag, 3));
        tag.setString(SchematicBlockIds.ENCODING, SchematicBlockIds.WORLDEDIT);
        assertArrayEquals(new int[] {0x123, 0x456, 0xf01}, SchematicBlockIds.read(tag, 3));
    }

    @Test public void writesTheSchematicaNibbleOrderAndOddLength() {
        NBTTagCompound tag = tag(new byte[0], new byte[0]);
        byte[] low = {35, 86, 1};
        SchematicBlockIds.write(tag, low, new byte[] {1, 4, 15}, false);
        tag.setByteArray("Data", new byte[3]);
        assertEquals(SchematicBlockIds.STANDARD, tag.getString(SchematicBlockIds.ENCODING));
        assertArrayEquals(new byte[] {0x14, (byte) 0xf0}, tag.getByteArray("AddBlocks"));
        assertArrayEquals(new int[] {0x123, 0x456, 0xf01}, readLikeSchematica(low, tag.getByteArray("AddBlocks")));
        assertArrayEquals(new int[] {0x123, 0x456, 0xf01}, SchematicBlockIds.read(tag, 3));
    }

    private static NBTTagCompound mapping(int... ids) {
        NBTTagCompound mapping = new NBTTagCompound();
        for (int id : ids) mapping.setShort("test:" + id, (short) id);
        return mapping;
    }

    @Test public void readsSchematicaFilesByTheirNameMapping() {
        NBTTagCompound tag = tag(new byte[] {35, 86, 1}, new byte[] {0x14, (byte) 0xf0});
        tag.setTag("SchematicaMapping", mapping(0x123, 0x456, 0xf01, 0x423, 0x156, 0x001));
        assertArrayEquals(new int[] {0x123, 0x456, 0xf01}, SchematicBlockIds.read(tag, 3));
    }

    @Test public void recoversWorldEditPairsOnlyWhenTheMappingDisambiguates() {
        NBTTagCompound tag = tag(new byte[] {35, 86, 1}, new byte[] {0x41, 0x0f});
        tag.setTag("SchematicaMapping", mapping(0x123, 0x456, 0xf01));
        assertArrayEquals(new int[] {0x123, 0x456, 0xf01}, SchematicBlockIds.read(tag, 3));
    }

    @Test public void extendedIdsRemainUnsignedAndDoNotDependOnUserConfiguration() {
        NBTTagCompound tag = tag(new byte[] {0, 0, -1}, new byte[] {1, -128, -1});
        assertArrayEquals(new int[] {256, 32768, 65535}, SchematicBlockIds.read(tag, 3));
        SchematicBlockIds.write(tag, new byte[] {0}, new byte[] {-128}, true);
        tag.setByteArray("Data", new byte[1]);
        assertEquals(32768, SchematicBlockIds.read(tag, 1)[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsTruncatedExtendedIds() {
        NBTTagCompound tag = tag(new byte[] {1, 2}, new byte[] {1});
        tag.setString(SchematicBlockIds.ENCODING, SchematicBlockIds.EXTENDED);
        SchematicBlockIds.read(tag, 2);
    }

    @Test public void promotesIdsThatCannotFitStandardFormat() {
        NBTTagCompound tag = tag(new byte[] {0}, new byte[0]);
        SchematicBlockIds.write(tag, new byte[] {0}, new byte[] {16}, false);
        assertEquals(SchematicBlockIds.EXTENDED, tag.getString(SchematicBlockIds.ENCODING));
        assertArrayEquals(new int[] {4096}, SchematicBlockIds.read(tag, 1));
    }
}

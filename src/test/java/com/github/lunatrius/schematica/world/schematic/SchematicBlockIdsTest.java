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

    @Test public void readsWorldEditNibbleOrderAndOddLength() {
        NBTTagCompound tag = tag(new byte[] {35, 86, 1}, new byte[] {0x41, 0x0f});
        assertArrayEquals(new int[] {0x123, 0x456, 0xf01}, SchematicBlockIds.read(tag, 3));
        SchematicBlockIds.write(tag, new byte[] {35, 86, 1}, new byte[] {1, 4, 15}, false);
        assertArrayEquals(new byte[] {0x41, 0x0f}, tag.getByteArray("AddBlocks"));
    }

    @Test public void recoversReversedLegacyPairsOnlyWhenTheMappingDisambiguates() {
        NBTTagCompound tag = tag(new byte[] {35, 86, 1}, new byte[] {0x14, (byte) 0xf0});
        NBTTagCompound mapping = new NBTTagCompound();
        mapping.setShort("test:a", (short) 0x123);
        mapping.setShort("test:b", (short) 0x456);
        mapping.setShort("test:c", (short) 0xf01);
        tag.setTag("SchematicaMapping", mapping);
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

    @Test(expected = IllegalArgumentException.class)
    public void rejectsIdsThatCannotFitStandardFormat() {
        SchematicBlockIds.write(new NBTTagCompound(), new byte[] {0}, new byte[] {16}, false);
    }
}

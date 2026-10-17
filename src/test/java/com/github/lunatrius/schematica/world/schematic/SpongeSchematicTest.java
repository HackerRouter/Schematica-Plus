package com.github.lunatrius.schematica.world.schematic;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;
import static org.junit.Assert.*;

import com.github.lunatrius.schematica.api.ISchematic;

public class SpongeSchematicTest {
    private static NBTTagCompound blocks(int version, byte[] data) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Version", version);
        tag.setShort("Width", (short) 2);
        tag.setShort("Height", (short) 1);
        tag.setShort("Length", (short) 2);
        NBTTagCompound palette = new NBTTagCompound();
        palette.setInteger("minecraft:air", 0);
        palette.setInteger("minecraft:cave_air", 200);
        if (version == 3) {
            NBTTagCompound container = new NBTTagCompound();
            container.setTag("Palette", palette);
            container.setByteArray("Data", data);
            tag.setTag("Blocks", container);
            tag.setIntArray("Offset", new int[] {-1, 0, -2});
            NBTTagCompound root = new NBTTagCompound();
            root.setTag("Schematic", tag);
            return root;
        }
        tag.setTag("Palette", palette);
        tag.setByteArray("BlockData", data);
        NBTTagCompound metadata = new NBTTagCompound();
        metadata.setInteger("WEOffsetX", -1);
        metadata.setInteger("WEOffsetY", 0);
        metadata.setInteger("WEOffsetZ", -2);
        tag.setTag("Metadata", metadata);
        return tag;
    }

    @Test public void readsVersionsOneTwoAndThreeWithVarintIndicesAndTheCopyOrigin() {
        // index 200 needs two varint bytes
        byte[] data = {0, (byte) 0xC8, 0x01, 0, 0};
        for (int version : new int[] {1, 2, 3}) {
            NBTTagCompound tag = blocks(version, data);
            assertTrue(SpongeSchematic.isSponge(tag));
            ISchematic schematic = new SpongeSchematic().readFromNBT(tag);
            assertEquals(2, schematic.getWidth());
            assertEquals(2, schematic.getLength());
            assertEquals(1, schematic.getOrigin().x);
            assertEquals(2, schematic.getOrigin().z);
        }
    }

    @Test public void rejectsTruncatedDataAndUnknownIndices() {
        for (int version : new int[] {1, 2, 3}) {
            assertInvalid(blocks(version, new byte[] {0, 0, 0, (byte) 0x80}));
            assertInvalid(blocks(version, new byte[] {5, 0, 0, 0}));
        }
    }

    @Test public void rejectsOutOfRangePaletteIndicesBeforeAllocation() {
        for (int version : new int[] {1, 2, 3}) {
            for (int index : new int[] {-1, Integer.MIN_VALUE, 1 << 20, Integer.MAX_VALUE}) {
                NBTTagCompound tag = blocks(version, new byte[] {0, 0, 0, 0});
                palette(tag, version).setInteger("minecraft:cave_air", index);
                assertInvalid(tag);
            }
        }
    }

    private static NBTTagCompound palette(NBTTagCompound tag, int version) {
        return (version == 3 ? tag.getCompoundTag("Schematic").getCompoundTag("Blocks") : tag).getCompoundTag("Palette");
    }

    private static void assertInvalid(NBTTagCompound tag) {
        try { new SpongeSchematic().readFromNBT(tag); fail("Accepted malformed Sponge block data"); }
        catch (IllegalArgumentException expected) {}
    }
}

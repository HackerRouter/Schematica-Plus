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

    @Test public void readsVersionsTwoAndThreeWithVarintIndicesAndTheCopyOrigin() {
        // index 200 needs two varint bytes
        byte[] data = {0, (byte) 0xC8, 0x01, 0, 0};
        for (int version : new int[] {2, 3}) {
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
        try { new SpongeSchematic().readFromNBT(blocks(2, new byte[] {0, (byte) 0x80})); fail(); }
        catch (IllegalArgumentException expected) {}
        try { new SpongeSchematic().readFromNBT(blocks(2, new byte[] {5})); fail(); }
        catch (IllegalArgumentException expected) {}
    }
}

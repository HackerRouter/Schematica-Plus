package com.github.lunatrius.schematica.world.schematic;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.BitSet;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.BeforeClass;
import org.junit.Test;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.api.SchematicOrigin;
import com.github.lunatrius.schematica.api.SchematicRegion;
import com.github.lunatrius.schematica.world.storage.MultiRegionSchematic;
import com.github.lunatrius.schematica.world.storage.Schematic;

import cpw.mods.fml.common.registry.GameData;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class IndependentRegionsTest {
    private static Block red, blue;

    @BeforeClass public static void register() throws Exception {
        red = block("test:region_red", 4051);
        blue = block("test:region_blue", 4052);
    }

    private static Block block(String name, int id) throws Exception {
        if (GameData.getBlockRegistry().containsKey(name)) return GameData.getBlockRegistry().getObject(name);
        Block block = new Block(Material.rock) {};
        Method add = Block.blockRegistry.getClass().getDeclaredMethod("add", int.class, String.class, Object.class, BitSet.class);
        add.setAccessible(true);
        add.invoke(Block.blockRegistry, id, name, block, new BitSet());
        return block;
    }

    private static Schematic filled(Block block, int meta) {
        Schematic part = new Schematic(null, 2, 1, 2);
        for (int x = 0; x < 2; x++) for (int z = 0; z < 2; z++) part.setBlock(x, 0, z, block, meta);
        part.setOrigin(new SchematicOrigin(-1, 0, 0));
        return part;
    }

    @Test public void overlappingRegionsKeepTheirOwnContents() {
        MultiRegionSchematic schematic = new MultiRegionSchematic(null, 3, 1, 2);
        SchematicRegion a = new SchematicRegion("A", 0, 0, 0, 1, 0, 1), b = new SchematicRegion("B", 1, 0, 0, 2, 0, 1);
        schematic.setRegions(Arrays.asList(a, b));
        schematic.setOrigin(new SchematicOrigin(4, 0, 0));
        schematic.addRegion(a, filled(red, 1));
        schematic.addRegion(b, filled(blue, 2));
        NBTTagCompound tag = new NBTTagCompound();
        assertTrue(new SchematicAlpha().writeToNBT(tag, schematic, null, true, true, false));
        assertEquals(SchematicBlockIds.EXTENDED, tag.getString(SchematicBlockIds.ENCODING));
        ISchematic read = new SchematicAlpha().readFromNBT(tag);
        assertTrue(read instanceof MultiRegionSchematic);
        assertEquals(4, read.getOrigin().x);
        ISchematic first = read.getRegionSchematic("A"), second = read.getRegionSchematic("B");
        assertNotNull(first);
        assertSame(red, first.getBlock(1, 0, 1));
        assertEquals(1, first.getBlockMetadata(1, 0, 1));
        assertSame(blue, second.getBlock(0, 0, 0));
        assertEquals(-1, first.getOrigin().x);
        assertSame(blue, read.getBlock(1, 0, 0));
        assertSame(red, read.getBlock(0, 0, 1));
    }

    @Test public void downloadsRestoreRegionsFromTheirPayload() throws Exception {
        MultiRegionSchematic schematic = new MultiRegionSchematic(null, 3, 1, 2);
        SchematicRegion a = new SchematicRegion("A", 0, 0, 0, 1, 0, 1), b = new SchematicRegion("B", 1, 0, 0, 2, 0, 1);
        schematic.setRegions(Arrays.asList(a, b));
        schematic.addRegion(a, filled(red, 1));
        schematic.addRegion(b, filled(blue, 2));
        byte[] payload = SchematicAlpha.regionPayload(schematic);
        Schematic flat = new Schematic(null, 3, 1, 2);
        flat.setRegions(Arrays.asList(a, b));
        com.github.lunatrius.schematica.handler.DownloadHandler handler = com.github.lunatrius.schematica.handler.DownloadHandler.INSTANCE;
        handler.beginDownload(flat);
        handler.receivedChunk(0, 0, 0);
        int size = com.github.lunatrius.schematica.network.message.MessageDownloadRegions.CHUNK_SIZE;
        int count = (payload.length + size - 1) / size;
        for (int i = 0; i < count; i++) {
            handler.receivedRegions(new com.github.lunatrius.schematica.network.message.MessageDownloadRegions(i, count,
                java.util.Arrays.copyOfRange(payload, i * size, Math.min(payload.length, (i + 1) * size))));
        }
        ISchematic restored = handler.completedSchematic();
        assertSame(red, restored.getRegionSchematic("A").getBlock(1, 0, 0));
        assertSame(blue, restored.getRegionSchematic("B").getBlock(0, 0, 0));
        assertEquals(2, restored.getRegionSchematic("B").getBlockMetadata(0, 0, 0));
        handler.beginDownload(null);
        assertEquals(null, SchematicAlpha.regionPayload(flat));
    }
}

package com.github.lunatrius.schematica.world.schematic;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.BitSet;
import java.util.Collections;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.lunatrius.schematica.api.ISchematic;

import cpw.mods.fml.common.registry.GameData;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

public class SchematicFilesTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private static Block block;

    @BeforeClass public static void register() throws Exception {
        String name = "test:files_block";
        if (GameData.getBlockRegistry().containsKey(name)) {
            block = GameData.getBlockRegistry().getObject(name);
            return;
        }
        block = new Block(Material.rock) {};
        Method add = Block.blockRegistry.getClass().getDeclaredMethod("add", int.class, String.class, Object.class, BitSet.class);
        add.setAccessible(true);
        add.invoke(Block.blockRegistry, 4061, name, block, new BitSet());
    }

    private File litematic(String name) throws Exception {
        LitematicExport.Document document = new LitematicExport.Document();
        NBTTagCompound metadata = new NBTTagCompound(), regions = new NBTTagCompound(), region = new NBTTagCompound();
        metadata.setString("Name", "Old");
        metadata.setString("Author", "Alex");
        metadata.setLong("TimeCreated", 10L);
        metadata.setLong("TimeModified", 10L);
        metadata.setInteger("RegionCount", 1);
        metadata.setLong("TotalVolume", 8L);
        metadata.setTag("EnclosingSize", LitematicExport.vector(2, 2, 2));
        region.setTag("Position", LitematicExport.vector(0, 0, 0));
        region.setTag("Size", LitematicExport.vector(2, 2, 2));
        NBTTagList palette = new NBTTagList();
        palette.appendTag(LegacyBlockStates.tag("minecraft:air"));
        region.setTag("BlockStatePalette", palette);
        regions.setTag("Main", region);
        document.root.setInteger("Version", 7);
        document.root.setInteger("MinecraftDataVersion", 4790);
        document.root.setTag("Metadata", metadata);
        document.root.setTag("Regions", regions);
        document.longArrays.put(region, Collections.singletonMap("BlockStates", new long[] {0L, 42L}));
        File file = temporary.newFile(name);
        try (FileOutputStream output = new FileOutputStream(file)) {
            LitematicaNBTWriter.writeCompressed(document.root, document.longArrays, output);
        }
        return file;
    }

    @Test public void metadataEditsKeepModernBlockData() throws Exception {
        File file = litematic("modern.litematic");
        SchematicFiles.Info before = SchematicFiles.info(file);
        assertEquals("Old", before.name);
        assertEquals("Alex", before.author);
        assertEquals(7, before.version);
        assertEquals(4790, before.dataVersion);
        assertEquals("26.1.2", DataVersions.name(before.dataVersion));
        int[] pixels = new int[4];
        java.util.Arrays.fill(pixels, 0xFF102030);
        SchematicFiles.editLitematicMetadata(file, metadata -> {
            metadata.setString("Name", "New");
            metadata.setIntArray("PreviewImageData", pixels);
        });
        SchematicFiles.Info after = SchematicFiles.info(file);
        assertEquals("New", after.name);
        assertEquals("Alex", after.author);
        assertTrue(after.hasBeenModified());
        assertArrayEquals(pixels, after.preview);
        NBTTagCompound root = LitematicaNBTReader.readFromFile(file);
        try {
            assertEquals(4790, root.getInteger("MinecraftDataVersion"));
            assertArrayEquals(new long[] {0L, 42L},
                LitematicaNBTReader.getLongArray(root.getCompoundTag("Regions").getCompoundTag("Main"), "BlockStates"));
        } finally {
            LitematicaNBTReader.clearLongArrayStore();
        }
        assertNull(SchematicFiles.info(temporary.newFile("other.schematic")));
        assertThrows(java.io.IOException.class, () -> SchematicFiles.editLitematicMetadata(temporary.newFile("x.nbt"), metadata -> {}));
    }

    @Test public void structuresUseThe112Layout() throws Exception {
        ISchematic grid = new LitematicExportTestGrid(block);
        NBTTagCompound tag = VanillaStructure.encode(grid, null, true, "Steve");
        assertTrue(VanillaStructure.isStructure(tag));
        assertEquals(1343, tag.getInteger("DataVersion"));
        assertEquals("Steve", tag.getString("author"));
        NBTTagList palette = tag.getTagList("palette", 10), blocks = tag.getTagList("blocks", 10);
        assertEquals("minecraft:air", palette.getCompoundTagAt(0).getString("Name"));
        assertEquals("test:files_block", palette.getCompoundTagAt(1).getString("Name"));
        assertEquals(5, palette.getCompoundTagAt(1).getInteger(LitematicExport.META_KEY));
        assertEquals(2, blocks.tagCount());
        assertEquals(1, blocks.getCompoundTagAt(0).getInteger("state"));
        assertEquals(0, blocks.getCompoundTagAt(1).getInteger("state"));
        assertSame(block, SchematicLitematica.legacyMapping(palette.getCompoundTagAt(1), null).block);
    }
}

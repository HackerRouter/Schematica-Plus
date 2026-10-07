package com.github.lunatrius.schematica.world.schematic;

import java.lang.reflect.Method;
import java.util.BitSet;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.BeforeClass;
import org.junit.Test;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.world.storage.Schematic;

import cpw.mods.fml.common.registry.GameData;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** EndlessIDs extended metadata (GTNH 2.9 frames keep the material id, such as 316, as metadata). */
public class ExtendedMetadataTest {
    private static Block frame;

    @BeforeClass public static void register() throws Exception {
        String name = "test:extended_frame";
        if (GameData.getBlockRegistry().containsKey(name)) { frame = GameData.getBlockRegistry().getObject(name); return; }
        frame = new Block(Material.iron) {};
        Method add = Block.blockRegistry.getClass().getDeclaredMethod("add", int.class, String.class, Object.class, BitSet.class);
        add.setAccessible(true);
        add.invoke(Block.blockRegistry, 4061, name, frame, new BitSet());
    }

    @Test public void schemplusKeepsMetadataAbove255() {
        Schematic schematic = new Schematic(null, 3, 1, 1);
        schematic.setBlock(0, 0, 0, frame, 316);
        schematic.setBlock(1, 0, 0, frame, 60);
        schematic.setBlock(2, 0, 0, frame, 65535);
        NBTTagCompound tag = new NBTTagCompound();
        assertTrue(new SchematicAlpha().writeToNBT(tag, schematic, null, true, true, false));
        assertEquals(SchematicBlockIds.EXTENDED, tag.getString(SchematicBlockIds.ENCODING));
        assertTrue(tag.hasKey(SchematicAlpha.DATA_HIGH));
        ISchematic read = new SchematicAlpha().readFromNBT(tag);
        assertSame(frame, read.getBlock(0, 0, 0));
        assertEquals(316, read.getBlockMetadata(0, 0, 0));
        assertEquals(60, read.getBlockMetadata(1, 0, 0));
        assertEquals(65535, read.getBlockMetadata(2, 0, 0));
    }

    @Test public void metadataUpTo255StaysInTheStandardLayout() {
        Schematic schematic = new Schematic(null, 1, 1, 1);
        schematic.setBlock(0, 0, 0, frame, 200);
        NBTTagCompound tag = new NBTTagCompound();
        assertTrue(new SchematicAlpha().writeToNBT(tag, schematic, null, true, true, false));
        assertFalse(tag.hasKey(SchematicAlpha.DATA_HIGH));
        assertEquals(200, new SchematicAlpha().readFromNBT(tag).getBlockMetadata(0, 0, 0));
    }

    @Test public void litematicPaletteKeepsExtendedMetadata() {
        NBTTagCompound entry = LitematicExport.paletteEntry(frame, 316);
        assertEquals(316, entry.getInteger(LitematicExport.META_KEY));
    }

    @Test public void downloadedChunksCarryTheHighByteAndStillReadOldPayloads() {
        Schematic schematic = new Schematic(null, 16, 16, 16);
        schematic.setBlock(1, 2, 3, frame, 316);
        com.github.lunatrius.schematica.network.message.MessageDownloadChunk message =
            new com.github.lunatrius.schematica.network.message.MessageDownloadChunk(schematic, 0, 0, 0);
        ByteBuf buffer = Unpooled.buffer();
        message.toBytes(buffer);
        com.github.lunatrius.schematica.network.message.MessageDownloadChunk read =
            new com.github.lunatrius.schematica.network.message.MessageDownloadChunk();
        read.fromBytes(buffer.copy());
        assertEquals(316, read.metadata[1][2][3] & 0xffff);

        // an older sender writes no trailer: the low byte is all there is
        ByteBuf old = buffer.copy(0, buffer.writerIndex() - 1 - 4096);
        com.github.lunatrius.schematica.network.message.MessageDownloadChunk legacy =
            new com.github.lunatrius.schematica.network.message.MessageDownloadChunk();
        legacy.fromBytes(old);
        assertEquals(60, legacy.metadata[1][2][3] & 0xffff);
    }
}

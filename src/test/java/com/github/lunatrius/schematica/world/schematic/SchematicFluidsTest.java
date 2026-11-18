package com.github.lunatrius.schematica.world.schematic;

import java.lang.reflect.Method;
import java.util.BitSet;
import java.util.Collections;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.api.SchematicRegion;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.network.message.MessageDownloadChunk;
import com.github.lunatrius.schematica.tool.WorldEditJob;
import com.github.lunatrius.schematica.world.storage.Schematic;
import com.github.lunatrius.schematica.world.storage.SchematicCopies;
import com.github.lunatrius.schematica.world.storage.SchematicFluids;

import cpw.mods.fml.common.registry.GameData;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SchematicFluidsTest {
    private static Block stone, glass;

    @BeforeClass public static void register() throws Exception {
        stone = block("test:fluid_stone", 4062);
        glass = block("test:fluid_glass", 4063);
    }

    private static Block block(String name, int id) throws Exception {
        if (GameData.getBlockRegistry().containsKey(name)) return GameData.getBlockRegistry().getObject(name);
        Block block = new Block(Material.rock) {};
        Method add = Block.blockRegistry.getClass().getDeclaredMethod("add", int.class, String.class, Object.class, BitSet.class);
        add.setAccessible(true);
        add.invoke(Block.blockRegistry, id, name, block, new BitSet());
        return block;
    }

    private static Schematic filled(int width, int height, int length) {
        Schematic schematic = new Schematic(null, width, height, length);
        for (int x = 0; x < width; x++) for (int y = 0; y < height; y++) for (int z = 0; z < length; z++) schematic.setBlock(x, y, z, stone);
        return schematic;
    }

    private static Schematic sample() {
        Schematic schematic = filled(4, 3, 2);
        schematic.setBlock(1, 2, 0, glass);
        schematic.setStoredFluid(1, 2, 0, "water");
        schematic.setStoredFluid(3, 0, 1, "lava");
        return schematic;
    }

    @Test public void storedFluidsAreSparseAndBoundsChecked() {
        Schematic schematic = sample();
        assertEquals("water", schematic.getStoredFluid(1, 2, 0));
        assertNull(schematic.getStoredFluid(0, 0, 0));
        assertFalse(schematic.setStoredFluid(4, 0, 0, "water"));
        assertTrue(schematic.setStoredFluid(1, 2, 0, null));
        assertNull(schematic.getStoredFluid(1, 2, 0));
        assertEquals(1, schematic.getStoredFluids().size());
    }

    @Test public void filesKeepStoredFluids() {
        NBTTagCompound tag = new NBTTagCompound();
        assertTrue(new SchematicAlpha().writeToNBT(tag, sample(), null, true, true, true));
        ISchematic read = new SchematicAlpha().readFromNBT(tag);
        assertEquals("water", read.getStoredFluid(1, 2, 0));
        assertEquals("lava", read.getStoredFluid(3, 0, 1));
        assertEquals(2, read.getStoredFluids().size());
        NBTTagCompound empty = new NBTTagCompound();
        assertTrue(new SchematicAlpha().writeToNBT(empty, filled(1, 1, 1), null, true, true, true));
        assertFalse(empty.hasKey(SchematicFluids.NBT));
    }

    @Test public void vanillaStructurePreservesStoredFluids() {
        Schematic source = sample();
        NBTTagCompound tag = VanillaStructure.encode(source, null, false, false, "");
        assertEquals(source.getStoredFluids(), new VanillaStructure().readFromNBT(tag).getStoredFluids());
    }

    @Test public void litematicStoresRegionLocalFluidCoordinates() throws Exception {
        Schematic source = sample();
        source.setRegions(Collections.singletonList(new SchematicRegion("Part", 1, 1, 0, 3, 2, 1)));
        LitematicExport.Document document = LitematicExport.encode(source, null, false, false, "Test", "", 1L);
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        LitematicaNBTWriter.writeCompressed(document.root, document.longArrays, bytes);
        ISchematic read = new SchematicLitematica().readFromNBT(LitematicaNBTReader.readFromStream(
            new java.io.ByteArrayInputStream(bytes.toByteArray())));
        assertEquals("water", read.getRegionSchematic("Part").getStoredFluid(0, 1, 0));
        assertEquals(1, read.getStoredFluids().size());
    }

    @Test public void regionsDropFluidsOutsideThem() {
        Schematic schematic = sample();
        schematic.setRegions(Collections.singletonList(new SchematicRegion("Box", 0, 0, 0, 1, 2, 1)));
        assertEquals("water", schematic.getStoredFluid(1, 2, 0));
        assertNull(schematic.getStoredFluid(3, 0, 1));
    }

    @Test public void copiesExtractsAndOverlaysCarryFluids() {
        Schematic source = sample();
        assertEquals("lava", SchematicCopies.copy(source).getStoredFluid(3, 0, 1));
        Schematic part = SchematicCopies.extract(source, new SchematicRegion("Part", 1, 1, 0, 3, 2, 1));
        assertEquals("water", part.getStoredFluid(0, 1, 0));
        assertEquals(1, part.getStoredFluids().size());
        Schematic target = new Schematic(null, 6, 3, 2);
        target.setStoredFluid(2, 0, 0, "oil");
        target.setStoredFluid(5, 0, 0, "oil");
        SchematicCopies.overlay(source, target, 1, 0, 0, true);
        assertNull(target.getStoredFluid(2, 0, 0));
        assertEquals("oil", target.getStoredFluid(5, 0, 0));
        assertEquals("water", target.getStoredFluid(2, 2, 0));
        assertEquals("lava", target.getStoredFluid(4, 0, 1));
    }

    @Test public void placementRotationsAndMirrorsMoveFluidsWithTheirBlocks() {
        SchematicWorld world = new SchematicWorld(sample());
        world.rotate(ForgeDirection.UP);
        assertEquals("water", world.getSchematic().getStoredFluid(1, 2, 1));
        assertEquals("lava", world.getSchematic().getStoredFluid(0, 0, 3));
        assertEquals(glass, world.getSchematic().getBlock(1, 2, 1));
        world.flip(ForgeDirection.EAST);
        assertEquals("water", world.getSchematic().getStoredFluid(0, 2, 1));
        assertEquals(glass, world.getSchematic().getBlock(0, 2, 1));
        world.flip(ForgeDirection.UP);
        assertEquals("water", world.getSchematic().getStoredFluid(0, 0, 1));
        assertEquals(glass, world.getSchematic().getBlock(0, 0, 1));
        world.rotate(ForgeDirection.EAST);
        assertEquals(glass, world.getSchematic().getBlock(0, 1, 2));
        assertEquals("water", world.getSchematic().getStoredFluid(0, 1, 2));
        world.rotate(ForgeDirection.SOUTH);
        int[] at = find(world.getSchematic(), "water");
        assertEquals(glass, world.getSchematic().getBlock(at[0], at[1], at[2]));
    }

    private static int[] find(ISchematic schematic, String fluid) {
        for (java.util.Map.Entry<Long, String> entry : schematic.getStoredFluids().entrySet()) {
            if (entry.getValue().equals(fluid)) {
                long key = entry.getKey();
                return new int[] {SchematicFluids.x(key), SchematicFluids.y(key), SchematicFluids.z(key)};
            }
        }
        throw new AssertionError("Missing " + fluid);
    }

    @Test public void downloadedChunksCarryFluids() {
        MessageDownloadChunk message = new MessageDownloadChunk(sample(), 0, 0, 0);
        ByteBuf buffer = Unpooled.buffer();
        message.toBytes(buffer);
        MessageDownloadChunk read = new MessageDownloadChunk();
        read.fromBytes(buffer.copy());
        assertEquals(2, read.fluids.tagCount());
        Schematic empty = filled(1, 1, 1);
        ByteBuf none = Unpooled.buffer();
        new MessageDownloadChunk(empty, 0, 0, 0).toBytes(none);
        MessageDownloadChunk readNone = new MessageDownloadChunk();
        readNone.fromBytes(none.copy());
        assertEquals(0, readNone.fluids.tagCount());
    }

    @Test public void editUploadsCarryFluidsInsideThePastedBounds() {
        WorldEditJob job = new WorldEditJob(UUID.randomUUID(), 0, WorldEditJob.Kind.PASTE, 0, 64, 0, 4, 3, 2, null, 0, null, 0);
        job.capture(sample(), false, false, new int[] {0, 1, 0, 4, 3, 2}, false);
        assertEquals(1, job.storedFluidCount());
        WorldEditJob read = WorldEditJob.read(job.write(), job.player, 0);
        assertEquals(1, read.storedFluidCount());
        assertEquals(job.write(), read.write());
        NBTTagCompound fill = new WorldEditJob(UUID.randomUUID(), 0, WorldEditJob.Kind.FILL, 0, 64, 0, 1, 1, 1,
            stone, 0, null, 0).write();
        fill.setTag("fluids", job.write().getTag("fluids"));
        org.junit.Assert.assertThrows(IllegalArgumentException.class, () -> WorldEditJob.read(fill, UUID.randomUUID(), 0));
    }
}

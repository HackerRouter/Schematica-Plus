package com.github.lunatrius.schematica.world.schematic;

import java.lang.reflect.Method;
import java.util.BitSet;

import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntityChest;

import org.junit.BeforeClass;
import org.junit.Test;

import com.github.lunatrius.schematica.api.ISchematic;

import static org.junit.Assert.*;

public class SchematicImportTest {
    @BeforeClass public static void register() throws Exception {
        Method add = Block.blockRegistry.getClass().getDeclaredMethod("add", int.class, String.class, Object.class, BitSet.class);
        add.setAccessible(true);
        add.invoke(Block.blockRegistry, 3986, "test:migration_chest", new BlockChest(0) {}, new BitSet());
        add.invoke(Block.blockRegistry, 3987, "test:migration_unavailable_api", new BlockChest(0) {
            @Override public boolean hasTileEntity(int metadata) { throw new NoClassDefFoundError("Optional mod API"); }
        }, new BitSet());
    }

    private static NBTTagCompound tile(String id, int x) {
        NBTTagCompound tile = new NBTTagCompound();
        tile.setString("id", id); tile.setInteger("x", x); tile.setInteger("y", 0); tile.setInteger("z", 0);
        tile.setString("CustomName", "Retained chest");
        return tile;
    }

    private static NBTTagCompound source(String block, String tileId) {
        NBTTagCompound root = new NBTTagCompound(), names = new NBTTagCompound();
        root.setShort("Width", (short) 2); root.setShort("Height", (short) 1); root.setShort("Length", (short) 1);
        root.setByteArray("Blocks", new byte[]{(byte) ("test:migration_chest".equals(block) ? 2 : 1), 2}); root.setByteArray("Data", new byte[]{0, 0});
        names.setShort(block, (short) 1); names.setShort("test:migration_chest", (short) 2); root.setTag("SchematicaMapping", names);
        NBTTagList tiles = new NBTTagList(); tiles.appendTag(tile(tileId, 0)); tiles.appendTag(tile("Chest", 1)); root.setTag("TileEntities", tiles);
        return root;
    }

    private static ISchematic load(NBTTagCompound root) {
        ImportReport.begin();
        try { return new SchematicAlpha().readFromNBT(root); }
        finally { ImportReport.end(); }
    }

    @Test public void missingBlocksCannotRetainOrphanedTileData() {
        ISchematic data = load(source("missingmod:chest", "Chest"));
        assertNull(data.getTileEntity(0, 0, 0));
        assertNotNull(data.getTileEntity(1, 0, 0));
        assertTrue(ImportReport.lastItems().omittedData.contains("block entity: Chest"));
    }

    @Test public void missingTileTypesAreReportedWithoutLosingSurvivingCells() {
        ISchematic data = load(source("test:migration_chest", "missingmod:tile"));
        assertNull(data.getTileEntity(0, 0, 0));
        assertNotNull(data.getTileEntity(1, 0, 0));
        assertTrue(ImportReport.lastItems().omittedData.contains("block entity: missingmod:tile"));
    }

    @Test public void missingEntityTypesAreReported() {
        NBTTagCompound root = source("test:migration_chest", "Chest"), entity = new NBTTagCompound();
        entity.setString("id", "missingmod:cart"); NBTTagList entities = new NBTTagList(); entities.appendTag(entity); root.setTag("Entities", entities);
        assertTrue(load(root).getEntities().isEmpty());
        assertTrue(ImportReport.lastItems().omittedData.contains("entity: missingmod:cart"));
    }

    @Test public void unavailableOptionalApisDoNotAbortOtherCells() {
        ISchematic data = load(source("test:migration_unavailable_api", "Chest"));
        assertNull(data.getTileEntity(0, 0, 0));
        assertNotNull(data.getTileEntity(1, 0, 0));
        assertTrue(ImportReport.lastItems().omittedData.contains("block entity: Chest"));
    }

    @Test public void supportedTileDataAndInputTagsSurviveImport() {
        NBTTagCompound root = source("test:migration_chest", "Chest"), before = (NBTTagCompound) root.copy();
        ISchematic data = load(root);
        assertEquals("Retained chest", ((TileEntityChest) data.getTileEntity(1, 0, 0)).getInventoryName());
        assertTrue(ImportReport.lastItems().omittedData.isEmpty());
        assertEquals(before.getTagList("TileEntities", 10), root.getTagList("TileEntities", 10));
    }
}

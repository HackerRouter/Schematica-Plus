package com.github.lunatrius.schematica.client.gui.browser;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.BitSet;

import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockFurnace;
import net.minecraft.block.BlockSign;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

import com.github.lunatrius.schematica.world.storage.Schematic;

import cpw.mods.fml.common.registry.GameData;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class SchematicPreview3DTest {
    private static Block chestBlock;
    private static Block signBlock;
    private static Block furnaceBlock;

    @BeforeClass public static void registerBlocks() throws Exception {
        Constructor<BlockChest> chestCtor = BlockChest.class.getDeclaredConstructor(int.class);
        chestCtor.setAccessible(true);
        Constructor<BlockSign> signCtor = BlockSign.class.getDeclaredConstructor(Class.class, boolean.class);
        signCtor.setAccessible(true);
        Constructor<BlockFurnace> furnaceCtor = BlockFurnace.class.getDeclaredConstructor(boolean.class);
        furnaceCtor.setAccessible(true);
        chestBlock = register("test:preview3d_chest", 3980, chestCtor.newInstance(0));
        signBlock = register("test:preview3d_sign", 3981, signCtor.newInstance(TileEntitySign.class, true));
        furnaceBlock = register("test:preview3d_furnace", 3982, furnaceCtor.newInstance(false));
    }

    private static Block register(String name, int id, Block block) throws Exception {
        if (GameData.getBlockRegistry().containsKey(name)) return GameData.getBlockRegistry().getObject(name);
        Method add = Block.blockRegistry.getClass().getDeclaredMethod("add", int.class, String.class, Object.class, BitSet.class);
        add.setAccessible(true);
        add.invoke(Block.blockRegistry, id, name, block, new BitSet());
        return block;
    }

    @Test public void collectsSpecialRendererTileEntitiesAndLinksAdjacentChests() {
        Schematic schematic = new Schematic(null, 4, 2, 2);
        schematic.setBlock(0, 0, 0, chestBlock, 3);
        schematic.setBlock(1, 0, 0, chestBlock, 3);
        schematic.setBlock(2, 0, 0, signBlock, 0);
        schematic.setBlock(3, 0, 0, furnaceBlock, 3);

        TileEntityChest left = new TileEntityChest();
        left.xCoord = 0; left.yCoord = 0; left.zCoord = 0;
        TileEntityChest right = new TileEntityChest();
        right.xCoord = 1; right.yCoord = 0; right.zCoord = 0;
        TileEntitySign sign = new TileEntitySign();
        sign.xCoord = 2; sign.yCoord = 0; sign.zCoord = 0;
        TileEntityFurnace furnace = new TileEntityFurnace();
        furnace.xCoord = 3; furnace.yCoord = 0; furnace.zCoord = 0;

        schematic.setTileEntity(0, 0, 0, left);
        schematic.setTileEntity(1, 0, 0, right);
        schematic.setTileEntity(2, 0, 0, sign);
        schematic.setTileEntity(3, 0, 0, furnace);

        SchematicPreview3D preview = new SchematicPreview3D(schematic);
        SchematicPreview3D.PreviewWorld world = preview.initWorld();

        assertEquals(3, preview.tileEntities().size());
        assertTrue(preview.tileEntities().contains(left));
        assertTrue(preview.tileEntities().contains(right));
        assertTrue(preview.tileEntities().contains(sign));
        assertFalse(preview.tileEntities().contains(furnace));
        assertSame(right, left.adjacentChestXPos);
        assertSame(left, right.adjacentChestXNeg);
        assertEquals(15 << 20 | 15 << 4, world.getLightBrightnessForSkyBlocks(0, 0, 0, 0));
        assertEquals(15, world.getSkyBlockTypeBrightness(EnumSkyBlock.Sky, 0, 0, 0));
        assertEquals(1.0f, world.getLightBrightness(0, 0, 0), 0.0001f);
        assertSame(Blocks.air, world.getBlock(-1, 0, 0));
        assertNull(world.getTileEntity(-1, 0, 0));
    }

    @Test public void synthesizesFallbackTileEntityForVanillaChestWithoutTileNbt() {
        Schematic schematic = new Schematic(null, 2, 1, 1);
        schematic.setBlock(0, 0, 0, chestBlock, 3);
        schematic.setBlock(1, 0, 0, furnaceBlock, 3);

        SchematicPreview3D preview = new SchematicPreview3D(schematic);
        SchematicPreview3D.PreviewWorld world = preview.initWorld();

        assertTrue(preview.tileEntities().isEmpty());
        assertTrue(preview.ensureVanillaTileEntity(0, 0, 0));
        assertFalse(preview.ensureVanillaTileEntity(1, 0, 0));
        assertEquals(1, preview.tileEntities().size());
        assertTrue(preview.tileEntities().get(0) instanceof TileEntityChest);
        assertSame(preview.tileEntities().get(0), world.getTileEntity(0, 0, 0));
    }

    @Test public void previewTileContextRestoresDispatcherFields() throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        Object unsafe = theUnsafe.get(null);
        java.lang.reflect.Method allocate = unsafeClass.getMethod("allocateInstance", Class.class);

        Minecraft mc = (Minecraft) allocate.invoke(unsafe, Minecraft.class);
        TextureManager previewTextures = (TextureManager) allocate.invoke(unsafe, TextureManager.class);
        FontRenderer previewFont = (FontRenderer) allocate.invoke(unsafe, FontRenderer.class);
        Field renderEngine = Minecraft.class.getDeclaredField("renderEngine");
        renderEngine.setAccessible(true);
        renderEngine.set(mc, previewTextures);
        mc.fontRenderer = previewFont;

        TileEntityRendererDispatcher dispatcher = TileEntityRendererDispatcher.instance;
        World originalWorld = dispatcher.field_147550_f;
        TextureManager originalTextures = dispatcher.field_147553_e;
        FontRenderer originalFont = dispatcher.getFontRenderer();
        float originalYaw = dispatcher.field_147562_h;
        float originalPitch = dispatcher.field_147563_i;
        double originalCamX = dispatcher.field_147560_j;
        double originalCamY = dispatcher.field_147561_k;
        double originalCamZ = dispatcher.field_147558_l;
        double originalStaticX = TileEntityRendererDispatcher.staticPlayerX;
        double originalStaticY = TileEntityRendererDispatcher.staticPlayerY;
        double originalStaticZ = TileEntityRendererDispatcher.staticPlayerZ;

        dispatcher.field_147560_j = 11.0;
        dispatcher.field_147561_k = 22.0;
        dispatcher.field_147558_l = 33.0;
        TileEntityRendererDispatcher.staticPlayerX = 44.0;
        TileEntityRendererDispatcher.staticPlayerY = 55.0;
        TileEntityRendererDispatcher.staticPlayerZ = 66.0;

        Schematic schematic = new Schematic(null, 2, 2, 2);
        SchematicPreview3D preview = new SchematicPreview3D(schematic);
        SchematicPreview3D.PreviewWorld world = preview.initWorld();
        double[] eye = {1.5, 2.5, 3.5};

        try {
            try (SchematicPreview3D.PreviewTileContext ignored = new SchematicPreview3D.PreviewTileContext(mc, world, eye, preview.camera)) {
                assertSame(world, dispatcher.field_147550_f);
                assertSame(previewTextures, dispatcher.field_147553_e);
                assertSame(previewFont, dispatcher.getFontRenderer());
                assertEquals(1.5, dispatcher.field_147560_j, 0.0001);
                assertEquals(2.5, dispatcher.field_147561_k, 0.0001);
                assertEquals(3.5, dispatcher.field_147558_l, 0.0001);
                assertEquals(0.0, TileEntityRendererDispatcher.staticPlayerX, 0.0001);
                assertEquals(0.0, TileEntityRendererDispatcher.staticPlayerY, 0.0001);
                assertEquals(0.0, TileEntityRendererDispatcher.staticPlayerZ, 0.0001);
            }
            assertSame(originalWorld, dispatcher.field_147550_f);
            assertSame(originalTextures, dispatcher.field_147553_e);
            assertSame(originalFont, dispatcher.getFontRenderer());
            assertEquals(originalYaw, dispatcher.field_147562_h, 0.0001f);
            assertEquals(originalPitch, dispatcher.field_147563_i, 0.0001f);
            assertEquals(11.0, dispatcher.field_147560_j, 0.0001);
            assertEquals(22.0, dispatcher.field_147561_k, 0.0001);
            assertEquals(33.0, dispatcher.field_147558_l, 0.0001);
            assertEquals(44.0, TileEntityRendererDispatcher.staticPlayerX, 0.0001);
            assertEquals(55.0, TileEntityRendererDispatcher.staticPlayerY, 0.0001);
            assertEquals(66.0, TileEntityRendererDispatcher.staticPlayerZ, 0.0001);
        } finally {
            dispatcher.field_147560_j = originalCamX;
            dispatcher.field_147561_k = originalCamY;
            dispatcher.field_147558_l = originalCamZ;
            TileEntityRendererDispatcher.staticPlayerX = originalStaticX;
            TileEntityRendererDispatcher.staticPlayerY = originalStaticY;
            TileEntityRendererDispatcher.staticPlayerZ = originalStaticZ;
        }
    }
}

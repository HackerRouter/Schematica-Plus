package com.github.lunatrius.schematica.world.schematic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;

import org.junit.BeforeClass;
import org.junit.Test;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.api.SchematicOrigin;
import com.github.lunatrius.schematica.api.SchematicRegion;
import com.github.lunatrius.schematica.compat.BlockMapping;

import cpw.mods.fml.common.registry.GameData;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class LitematicExportTest {
    private static Block first, second;

    @BeforeClass public static void register() throws Exception {
        first = block("test:litematic_first", 4071);
        second = block("test:litematic_second", 4072);
    }

    private static Block block(String name, int id) throws Exception {
        if (GameData.getBlockRegistry().containsKey(name)) return GameData.getBlockRegistry().getObject(name);
        Block block = new Block(Material.rock) {};
        Method add = Block.blockRegistry.getClass().getDeclaredMethod("add", int.class, String.class, Object.class, BitSet.class);
        add.setAccessible(true);
        add.invoke(Block.blockRegistry, id, name, block, new BitSet());
        return block;
    }

    /** A 3x2x2 schematic of test blocks with two regions and an origin offset. */
    private static final class Grid implements ISchematic {
        final Block[] blocks = new Block[12];
        final int[] meta = new int[12];
        final List<SchematicRegion> regions = new ArrayList<>();

        int index(int x, int y, int z) { return x + 3 * (z + 2 * y); }
        @Override public SchematicOrigin getOrigin() { return new SchematicOrigin(1, 0, 0); }
        @Override public List<SchematicRegion> getRegions() { return regions; }
        @Override public Block getBlock(int x, int y, int z) { return blocks[index(x, y, z)]; }
        @Override public boolean setBlock(int x, int y, int z, Block block) { return setBlock(x, y, z, block, 0); }
        @Override public boolean setBlock(int x, int y, int z, Block block, int metadata) {
            blocks[index(x, y, z)] = block; meta[index(x, y, z)] = metadata; return true;
        }
        @Override public TileEntity getTileEntity(int x, int y, int z) { return null; }
        @Override public List<TileEntity> getTileEntities() { return Collections.emptyList(); }
        @Override public void setTileEntity(int x, int y, int z, TileEntity tileEntity) {}
        @Override public void removeTileEntity(int x, int y, int z) {}
        @Override public int getBlockMetadata(int x, int y, int z) { return meta[index(x, y, z)]; }
        @Override public boolean setBlockMetadata(int x, int y, int z, int metadata) { meta[index(x, y, z)] = metadata; return true; }
        @Override public List<Entity> getEntities() { return Collections.emptyList(); }
        @Override public void addEntity(Entity entity) {}
        @Override public void removeEntity(Entity entity) {}
        @Override public ItemStack getIcon() { return null; }
        @Override public void setIcon(ItemStack icon) {}
        @Override public int getWidth() { return 3; }
        @Override public int getLength() { return 2; }
        @Override public int getHeight() { return 2; }
    }

    @Test public void packedValuesCrossLongBoundariesLikeLitematica() {
        Random random = new Random(7);
        for (int bits : new int[] {2, 3, 5, 13, 32}) {
            int[] values = new int[1000];
            for (int i = 0; i < values.length; i++) values[i] = bits == 32 ? random.nextInt() : random.nextInt(1 << bits);
            long[] packed = LitematicBitArray.pack(values, bits);
            assertEquals((values.length * (long) bits + 63) / 64, packed.length);
            LitematicBitArray array = new LitematicBitArray(bits, values.length, packed);
            for (int i = 0; i < values.length; i++) assertEquals(values[i], array.getAt(i));
        }
    }

    @Test public void longArraysSurviveWritingAndReading() throws Exception {
        NBTTagCompound root = new NBTTagCompound(), regions = new NBTTagCompound(), region = new NBTTagCompound();
        region.setString("Name", "A");
        NBTTagList list = new NBTTagList();
        list.appendTag(new NBTTagCompound());
        region.setTag("TileEntities", list);
        regions.setTag("A", region);
        root.setTag("Regions", regions);
        root.setInteger("Version", 4);
        java.util.Map<NBTTagCompound, java.util.Map<String, long[]>> arrays = new java.util.IdentityHashMap<>();
        arrays.put(region, Collections.singletonMap("BlockStates", new long[] {1L, -1L, Long.MIN_VALUE}));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        LitematicaNBTWriter.writeCompressed(root, arrays, bytes);
        NBTTagCompound read = LitematicaNBTReader.readFromStream(new ByteArrayInputStream(bytes.toByteArray()));
        try {
            NBTTagCompound readRegion = read.getCompoundTag("Regions").getCompoundTag("A");
            assertEquals(4, read.getInteger("Version"));
            assertEquals("A", readRegion.getString("Name"));
            assertEquals(1, readRegion.getTagList("TileEntities", 10).tagCount());
            assertArrayEquals(new long[] {1L, -1L, Long.MIN_VALUE}, LitematicaNBTReader.getLongArray(readRegion, "BlockStates"));
        } finally {
            LitematicaNBTReader.clearLongArrayStore();
        }
    }

    @Test public void vanillaBlocksUseTheExact112StatesOfTheConversionTable() {
        assertEquals("minecraft:air", LegacyBlockStates.state("minecraft:air", 0));
        assertEquals("minecraft:stone[variant=stone]", LegacyBlockStates.state("minecraft:stone", 0));
        assertEquals("minecraft:log[axis=x,variant=spruce]", LegacyBlockStates.state("minecraft:log", 5));
        assertEquals("minecraft:grass[snowy=false]", LegacyBlockStates.state("minecraft:grass", 0));
        assertEquals("minecraft:oak_stairs[facing=west,half=top,shape=straight]", LegacyBlockStates.state("minecraft:oak_stairs", 5));
        assertEquals("minecraft:fence[east=false,north=false,south=false,west=false]", LegacyBlockStates.state("minecraft:fence", 0));
        assertNull(LegacyBlockStates.state("minecraft:stone", 9));
        assertNull(LegacyBlockStates.state("gregtech:gt.blockmachines", 0));
        LegacyBlockStates.Entry snowy = LegacyBlockStates.entry("minecraft:grass[snowy=true]");
        assertEquals(2 << 4, snowy.idMeta);
        assertEquals("minecraft:grass", snowy.name);
        assertEquals("minecraft:grass_block[snowy=false]", snowy.modern);
        String state = "minecraft:log[axis=x,variant=spruce]";
        assertEquals(state, LegacyBlockStates.state(LegacyBlockStates.tag(state)));
        assertEquals(17 << 4 | 5, LegacyBlockStates.entry(state).idMeta);
    }

    @Test public void documentsUseTheVersion4LayoutOf1122Litematica() throws Exception {
        Grid grid = new Grid();
        for (int i = 0; i < 12; i++) grid.blocks[i] = null;
        grid.setBlock(0, 0, 0, first, 0);
        grid.setBlock(2, 1, 1, second, 7);
        grid.setBlock(1, 0, 0, first, 3);
        grid.regions.add(new SchematicRegion("Low", 0, 0, 0, 2, 0, 1));
        grid.regions.add(new SchematicRegion("High", 0, 1, 0, 2, 1, 1));
        LitematicExport.Document document = LitematicExport.encode(grid, null, true, true, "Test", "Steve", 1234L);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        LitematicaNBTWriter.writeCompressed(document.root, document.longArrays, bytes);
        NBTTagCompound root = LitematicaNBTReader.readFromStream(new ByteArrayInputStream(bytes.toByteArray()));
        try {
            assertEquals(4, root.getInteger("Version"));
            assertEquals(1343, root.getInteger("MinecraftDataVersion"));
            assertTrue(SchematicLitematica.isLegacy(root));
            NBTTagCompound metadata = root.getCompoundTag("Metadata");
            assertEquals("Test", metadata.getString("Name"));
            assertEquals("Steve", metadata.getString("Author"));
            assertEquals(2, metadata.getInteger("RegionCount"));
            assertEquals(12, metadata.getLong("TotalVolume"));
            assertEquals(3, metadata.getLong("TotalBlocks"));
            assertEquals(1234L, metadata.getLong("TimeModified"));
            assertEquals(3, metadata.getCompoundTag("EnclosingSize").getInteger("x"));
            LitematicRegions regions = new LitematicRegions(root);
            assertEquals(2, regions.regions.size());
            LitematicRegions.Region high = regions.regions.get(0), low = regions.regions.get(1);
            assertEquals("High", high.box.name);
            assertEquals(-1, low.box.minX);
            assertEquals(1, high.box.minY);
            assertEquals("minecraft:air", low.palette.getCompoundTagAt(0).getString("Name"));
            NBTTagCompound firstEntry = low.palette.getCompoundTagAt(low.paletteIndex(0, 0, 0));
            assertEquals("test:litematic_first", firstEntry.getString("Name"));
            assertFalse(firstEntry.hasKey(LitematicExport.META_KEY));
            NBTTagCompound third = low.palette.getCompoundTagAt(low.paletteIndex(1, 0, 0));
            assertEquals(3, third.getInteger(LitematicExport.META_KEY));
            assertEquals(0, low.paletteIndex(2, 0, 1));
            NBTTagCompound top = high.palette.getCompoundTagAt(high.paletteIndex(2, 0, 1));
            BlockMapping mapping = SchematicLitematica.legacyMapping(top, null);
            assertSame(second, mapping.block);
            assertEquals(7, mapping.metadata);
        } finally {
            LitematicaNBTReader.clearLongArrayStore();
        }
    }

    @Test public void legacyNbtFollowsTheVanillaFixers() {
        assertEquals("{\"text\":\"Hello \\\"you\\\"\"}", LegacyNbt.json("Hello \"you\""));
        assertEquals("Hello \"you\"", LegacyNbt.plain(LegacyNbt.json("Hello \"you\"")));
        assertEquals("ab", LegacyNbt.plain("{\"text\":\"a\",\"extra\":[{\"text\":\"b\"}]}"));
        assertEquals("not json at all", LegacyNbt.plain("not json at all"));

        NBTTagCompound sign = new NBTTagCompound();
        sign.setString("id", "Sign");
        sign.setString("Text1", "Line");
        NBTTagCompound modern = LegacyNbt.tileTo112(sign);
        assertEquals("minecraft:sign", modern.getString("id"));
        assertEquals("{\"text\":\"Line\"}", modern.getString("Text1"));
        NBTTagCompound back = LegacyNbt.tileFrom112(modern);
        assertEquals("Sign", back.getString("id"));
        assertEquals("Line", back.getString("Text1"));

        NBTTagCompound spawner = new NBTTagCompound();
        spawner.setString("id", "MobSpawner");
        spawner.setString("EntityId", "Skeleton");
        NBTTagCompound spawner112 = LegacyNbt.tileTo112(spawner);
        assertEquals("minecraft:skeleton", spawner112.getCompoundTag("SpawnData").getString("id"));
        assertFalse(spawner112.hasKey("EntityId"));
        assertEquals("Skeleton", LegacyNbt.tileFrom112(spawner112).getString("EntityId"));

        NBTTagCompound wither = new NBTTagCompound();
        wither.setString("id", "Skeleton");
        wither.setByte("SkeletonType", (byte) 1);
        NBTTagList equipment = new NBTTagList();
        for (int i = 0; i < 5; i++) {
            NBTTagCompound slot = new NBTTagCompound();
            if (i == 4) slot.setString("Marker", "helmet");
            equipment.appendTag(slot);
        }
        wither.setTag("Equipment", equipment);
        NBTTagCompound wither112 = LegacyNbt.entityTo112(wither);
        assertEquals("minecraft:wither_skeleton", wither112.getString("id"));
        assertEquals(2, wither112.getTagList("HandItems", 10).tagCount());
        assertEquals("helmet", wither112.getTagList("ArmorItems", 10).getCompoundTagAt(3).getString("Marker"));
        NBTTagCompound witherBack = LegacyNbt.entityFrom112(wither112);
        assertEquals("Skeleton", witherBack.getString("id"));
        assertEquals(1, witherBack.getByte("SkeletonType"));
        assertEquals("helmet", witherBack.getTagList("Equipment", 10).getCompoundTagAt(4).getString("Marker"));

        NBTTagCompound mule = new NBTTagCompound();
        mule.setString("id", "EntityHorse");
        mule.setInteger("Type", 2);
        assertEquals("minecraft:mule", LegacyNbt.entityTo112(mule).getString("id"));
        assertEquals(2, LegacyNbt.entityFrom112(LegacyNbt.entityTo112(mule)).getInteger("Type"));
        assertEquals("EntityHorse", LegacyNbt.entityFrom112(LegacyNbt.entityTo112(mule)).getString("id"));
        assertTrue(Arrays.asList("Pig", "minecraft:pig").contains(LegacyNbt.entityTo112(pig()).getString("id")));
        assertEquals("Pig", LegacyNbt.entityFrom112(LegacyNbt.entityTo112(pig())).getString("id"));
    }

    private static NBTTagCompound pig() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", "Pig");
        return tag;
    }
}

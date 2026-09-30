package com.github.lunatrius.schematica.world.schematic;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagList;

import org.junit.Test;
import static org.junit.Assert.*;

public class LitematicRegionsTest {
    private NBTTagCompound xyz(int x, int y, int z) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("x", x); tag.setInteger("y", y); tag.setInteger("z", z);
        return tag;
    }

    private NBTTagList list(NBTTagCompound... tags) {
        NBTTagList list = new NBTTagList();
        for (NBTTagCompound tag : tags) list.appendTag(tag);
        return list;
    }

    private NBTTagCompound entity(double x, double y, double z) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", "minecraft:item_frame");
        NBTTagList pos = new NBTTagList();
        for (double value : new double[] {x, y, z}) pos.appendTag(new NBTTagDouble(value));
        tag.setTag("Pos", pos);
        return tag;
    }

    private NBTTagCompound region(int sx, int sy, int sz, String block, int... indices) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("Position", xyz(10, 20, 30));
        tag.setTag("Size", xyz(sx, sy, sz));
        NBTTagCompound air = new NBTTagCompound(), solid = new NBTTagCompound();
        air.setString("Name", "minecraft:air"); solid.setString("Name", block);
        tag.setTag("BlockStatePalette", list(air, solid));
        int volume = Math.abs(sx * sy * sz);
        int[] packed = new int[((volume * 2 + 63) / 64) * 2];
        for (int i = 0; i < indices.length; i++) packed[i / 16] |= indices[i] << ((i % 16) * 2);
        tag.setIntArray("BlockStates", packed);
        return tag;
    }

    private NBTTagCompound root(NBTTagCompound... regions) {
        NBTTagCompound root = new NBTTagCompound(), entries = new NBTTagCompound();
        root.setInteger("Version", 7);
        for (int i = regions.length - 1; i >= 0; i--) entries.setTag(String.valueOf((char) ('A' + i)), regions[i]);
        root.setTag("Regions", entries);
        return root;
    }

    @Test public void overlappingRegionsRetainTheirOwnPaletteAirAndTileDataInStableOrder() {
        NBTTagCompound a = region(2, 1, 1, "minecraft:chest", 1, 0);
        NBTTagCompound b = region(2, 1, 1, "minecraft:furnace", 0, 1);
        NBTTagCompound ta = xyz(0, 0, 0), tb = xyz(1, 0, 0);
        ta.setString("CustomName", "first"); tb.setString("CustomName", "second");
        a.setTag("TileEntities", list(ta)); b.setTag("TileEntities", list(tb));
        NBTTagCompound source = root(a, b), original = (NBTTagCompound) source.copy();
        LitematicRegions parsed = new LitematicRegions(source);
        assertEquals(2, parsed.width);
        LitematicRegions.Region first = parsed.regions.get(0), second = parsed.regions.get(1);
        assertEquals("A", first.box.name); assertEquals("B", second.box.name);
        assertEquals(1, first.paletteIndex(0, 0, 0)); assertEquals(0, second.paletteIndex(0, 0, 0));
        assertEquals(0, first.paletteIndex(1, 0, 0)); assertEquals(1, second.paletteIndex(1, 0, 0));
        assertEquals("minecraft:chest", first.palette.getCompoundTagAt(1).getString("Name"));
        assertEquals("minecraft:furnace", second.palette.getCompoundTagAt(1).getString("Name"));
        first.tileEntities().getCompoundTagAt(0).setString("CustomName", "changed");
        assertEquals("first", first.tileEntities().getCompoundTagAt(0).getString("CustomName"));
        assertEquals("second", second.tileEntities().getCompoundTagAt(0).getString("CustomName"));
        assertEquals(original, source);
    }

    @Test public void signedSizesNormalizeBlocksTilesAndEntitiesFromTheirDifferentCoordinateBases() {
        for (int sx : new int[] {-3, 3}) for (int sy : new int[] {-2, 2}) for (int sz : new int[] {-4, 4}) {
            NBTTagCompound region = region(sx, sy, sz, "minecraft:stone", 1);
            region.setTag("TileEntities", list(xyz(2, 1, 3)));
            NBTTagCompound frame = entity(-0.5, 0.25, 0.5);
            frame.setInteger("TileX", -1); frame.setInteger("TileY", 0); frame.setInteger("TileZ", 1);
            frame.setTag("Passengers", list(entity(0, 0, 0)));
            region.setTag("Entities", list(frame));
            NBTTagCompound source = root(region), original = (NBTTagCompound) source.copy();
            LitematicRegions document = new LitematicRegions(source);
            LitematicRegions.Region part = document.regions.get(0);
            int px = sx < 0 ? 2 : 0, py = sy < 0 ? 1 : 0, pz = sz < 0 ? 3 : 0;
            assertArrayEquals(new int[] {px, py, pz}, part.origin.coordinates());
            assertArrayEquals(new int[] {10 - px, 20 - py, 30 - pz}, document.minimum.coordinates());
            assertEquals(2, part.tileEntities().getCompoundTagAt(0).getInteger("x"));
            NBTTagCompound normalized = part.entities().getCompoundTagAt(0);
            NBTTagList pos = normalized.getTagList("Pos", 6);
            assertEquals(px - 0.5, pos.func_150309_d(0), 0);
            assertEquals(py + 0.25, pos.func_150309_d(1), 0);
            assertEquals(pz + 0.5, pos.func_150309_d(2), 0);
            assertEquals(px - 1, normalized.getInteger("TileX"));
            assertEquals(py, normalized.getInteger("TileY"));
            assertEquals(pz + 1, normalized.getInteger("TileZ"));
            assertEquals(px, normalized.getTagList("Passengers", 10).getCompoundTagAt(0).getTagList("Pos", 6).func_150309_d(0), 0);
            normalized.setString("id", "mutated");
            assertEquals("minecraft:item_frame", part.entities().getCompoundTagAt(0).getString("id"));
            assertEquals(original, source);
        }
    }

    @Test public void handlesModernHangingAnchorsAndVersionOneWrappers() {
        NBTTagCompound data = region(-3, 1, -4, "minecraft:stone");
        NBTTagCompound modern = entity(0, 0, 0), compound = entity(0, 0, 0);
        modern.setIntArray("block_pos", new int[] {-1, 0, -2});
        NBTTagCompound anchor = new NBTTagCompound();
        anchor.setInteger("X", -1); anchor.setInteger("Y", 0); anchor.setInteger("Z", -2);
        compound.setTag("block_pos", anchor);
        data.setTag("Entities", list(modern, compound));
        NBTTagList normalized = new LitematicRegions(root(data)).regions.get(0).entities();
        for (int i = 0; i < 2; i++) {
            assertEquals(1, normalized.getCompoundTagAt(i).getInteger("TileX"));
            assertEquals(1, normalized.getCompoundTagAt(i).getInteger("TileZ"));
            assertFalse(normalized.getCompoundTagAt(i).hasKey("block_pos"));
        }
        NBTTagCompound tile = new NBTTagCompound(), wrappedTile = xyz(1, 0, 2), wrappedEntity = new NBTTagCompound();
        tile.setString("id", "Chest"); wrappedTile.setTag("TileNBT", tile);
        wrappedEntity.setTag("EntityData", modern);
        wrappedEntity.setDouble("x", -0.25); wrappedEntity.setDouble("y", 0.5); wrappedEntity.setDouble("z", -0.75);
        data.setTag("TileEntities", list(wrappedTile)); data.setTag("Entities", list(wrappedEntity));
        NBTTagCompound root = root(data); root.setInteger("Version", 1);
        LitematicRegions.Region part = new LitematicRegions(root).regions.get(0);
        assertEquals(1, part.tileEntities().getCompoundTagAt(0).getInteger("x"));
        assertEquals("Chest", part.tileEntities().getCompoundTagAt(0).getString("id"));
        assertEquals(1.75, part.entities().getCompoundTagAt(0).getTagList("Pos", 6).func_150309_d(0), 0);
        assertFalse(tile.hasKey("x"));
    }

    @Test public void malformedRegionsFailInsteadOfSilentlyDroppingTheirContents() {
        NBTTagCompound data = region(1, 1, 1, "minecraft:stone", 3);
        LitematicRegions.Region invalidIndex = new LitematicRegions(root(data)).regions.get(0);
        assertThrows(IllegalArgumentException.class, () -> invalidIndex.paletteIndex(0, 0, 0));
        data.setIntArray("BlockStates", new int[0]);
        assertThrows(IllegalArgumentException.class, () -> new LitematicRegions(root(data)));
        data.setIntArray("BlockStates", new int[2]);
        data.setTag("TileEntities", list(xyz(1, 0, 0)));
        assertThrows(IllegalArgumentException.class, () -> new LitematicRegions(root(data)).regions.get(0).tileEntities());
        data.setTag("Entities", list(entity(Double.NaN, 0, 0)));
        assertThrows(IllegalArgumentException.class, () -> new LitematicRegions(root(data)).regions.get(0).entities());
        data.getCompoundTag("Position").removeTag("x");
        assertThrows(IllegalArgumentException.class, () -> new LitematicRegions(root(data)));
        for (int version : new int[] {0, 8}) {
            NBTTagCompound root = root(region(1, 1, 1, "minecraft:stone")); root.setInteger("Version", version);
            assertThrows(IllegalArgumentException.class, () -> new LitematicRegions(root));
        }
    }

    @Test public void boundsAndSumOfOverlappingContentsAreBothLimitedBeforeDenseAllocation() {
        NBTTagCompound huge = region(256, 128, 256, "minecraft:stone");
        assertThrows(IllegalArgumentException.class, () -> new LitematicRegions(root(huge, huge, huge)));
        NBTTagCompound far = region(1, 1, 1, "minecraft:stone");
        far.setTag("Position", xyz(40000, 20, 30));
        assertThrows(IllegalArgumentException.class, () -> new LitematicRegions(root(region(1, 1, 1, "minecraft:stone"), far)));
        NBTTagCompound overflow = region(-2, 1, 1, "minecraft:stone");
        overflow.setTag("Position", xyz(Integer.MIN_VALUE, 0, 0));
        assertThrows(ArithmeticException.class, () -> new LitematicRegions(root(overflow)));
        NBTTagCompound zero = region(1, 1, 1, "minecraft:stone"); zero.setTag("Size", xyz(0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new LitematicRegions(root(zero)));
    }
}

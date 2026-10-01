package com.github.lunatrius.schematica.client.gui.material;

import java.util.Arrays;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import org.junit.Test;
import static org.junit.Assert.*;

public class MaterialCacheTest {
    private static final Item ITEM = new Item();

    private static final class CountingBlock extends Block {
        int picks;
        final boolean tile;
        CountingBlock(boolean tile) { super(Material.rock); this.tile = tile; }
        @Override public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z, EntityPlayer player) {
            picks++;
            return new ItemStack(ITEM, 5, picks);
        }
        @Override public boolean hasTileEntity(int metadata) { return tile; }
    }

    @Test public void cachesPlainBlocksPerMetadataButNotTileEntityBlocks() {
        MaterialCache cache = MaterialCache.INSTANCE;
        cache.clear();
        CountingBlock plain = new CountingBlock(false);
        MaterialCache.BuildItems first = cache.items(null, 0, 0, 0, plain, 3, null);
        assertEquals(1, first.size());
        assertEquals(1, first.count(0)); // one item per block, whatever the picked stack size
        assertSame(first, cache.items(null, 1, 2, 3, plain, 3, null));
        assertEquals(1, plain.picks);
        cache.items(null, 0, 0, 0, plain, 4, null);
        assertEquals(2, plain.picks);
        cache.clear();
        cache.items(null, 0, 0, 0, plain, 3, null);
        assertEquals(3, plain.picks);
        CountingBlock machine = new CountingBlock(true);
        MaterialCache.BuildItems a = cache.items(null, 0, 0, 0, machine, 0, null);
        MaterialCache.BuildItems b = cache.items(null, 0, 0, 0, machine, 0, null);
        assertEquals(2, machine.picks);
        assertNotEquals(a, b);
    }

    @Test public void buildItemsMergeEqualStacks() {
        MaterialCache.BuildItems items = MaterialCache.BuildItems.of(new ItemStack(ITEM, 1, 0), null, new ItemStack(ITEM, 2, 0), new ItemStack(ITEM, 1, 1));
        assertEquals(2, items.size());
        assertEquals(3, items.count(0));
        assertEquals(items, MaterialCache.BuildItems.of(new ItemStack(ITEM, 3, 0), new ItemStack(ITEM, 1, 1)));
        assertTrue(MaterialCache.BuildItems.of().isEmpty());
        assertSame(MaterialCache.BuildItems.NONE, MaterialCache.BuildItems.of((ItemStack) null));
    }

    @Test public void hudListsTheMissingItemsBeyondTheInventory() {
        MaterialListModel<String> model = new MaterialListModel<>();
        MaterialListModel.Entry<String> stone = new MaterialListModel.Entry<>("stone", "Stone", "minecraft:stone", 100, 40, 0, 0);
        MaterialListModel.Entry<String> dirt = new MaterialListModel.Entry<>("dirt", "Dirt", "minecraft:dirt", 50, 10, 0, 0);
        MaterialListModel.Entry<String> glass = new MaterialListModel.Entry<>("glass", "Glass", "minecraft:glass", 20, 20, 0, 0);
        stone.available = 15;
        dirt.available = 10;
        model.setEntries(Arrays.asList(stone, dirt, glass));
        model.setSort(MaterialListModel.Sort.MISSING, true);
        assertEquals(Arrays.asList(stone, glass), model.missingOnly());
        assertEquals(25, model.hudCount(stone));
        model.ignore("glass");
        assertEquals(Arrays.asList(stone), model.missingOnly());
        model.setMultiplier(3);
        assertEquals(300, model.hudCount(stone));
        assertEquals(Arrays.asList(stone, dirt), model.missingOnly());
    }
}

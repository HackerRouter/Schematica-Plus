// SPDX-License-Identifier: LGPL-3.0-only
// Litematica MaterialCache, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.material;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockFlowerPot;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockSlab;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFlowerPot;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.fluids.BlockFluidClassic;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;

/**
 * The items needed to build a block. Results are cached per block and metadata, except for blocks with a tile entity,
 * whose item usually depends on that tile entity in 1.7.10 mods (GregTech machines, multiparts, flower pots).
 */
public final class MaterialCache {
    public static final MaterialCache INSTANCE = new MaterialCache();

    /** The items of one block, with their counts; empty when the block needs no item (door tops, portals, flowing fluids). */
    public static final class BuildItems {
        public static final BuildItems NONE = new BuildItems(Collections.<MaterialItemKey>emptyList(), new int[0]);
        private final List<MaterialItemKey> keys;
        private final int[] counts;
        private final int hash;

        BuildItems(List<MaterialItemKey> keys, int[] counts) {
            this.keys = Collections.unmodifiableList(new ArrayList<>(keys));
            this.counts = counts.clone();
            hash = this.keys.hashCode() * 31 + Arrays.hashCode(this.counts);
        }

        static BuildItems of(ItemStack... stacks) {
            List<MaterialItemKey> keys = new ArrayList<>();
            List<Integer> counts = new ArrayList<>();
            for (ItemStack stack : stacks) {
                if (stack == null || stack.getItem() == null || stack.stackSize <= 0) continue;
                MaterialItemKey key = new MaterialItemKey(stack);
                int index = keys.indexOf(key);
                if (index >= 0) counts.set(index, counts.get(index) + stack.stackSize);
                else { keys.add(key); counts.add(stack.stackSize); }
            }
            int[] values = new int[counts.size()];
            for (int i = 0; i < values.length; i++) values[i] = counts.get(i);
            return keys.isEmpty() ? NONE : new BuildItems(keys, values);
        }

        public boolean isEmpty() { return keys.isEmpty(); }
        public int size() { return keys.size(); }
        public MaterialItemKey key(int index) { return keys.get(index); }
        public int count(int index) { return counts[index]; }

        @Override public boolean equals(Object object) {
            return object instanceof BuildItems && keys.equals(((BuildItems) object).keys) && Arrays.equals(counts, ((BuildItems) object).counts);
        }
        @Override public int hashCode() { return hash; }
    }

    private final Map<Block, BuildItems[]> cache = new HashMap<>();

    private MaterialCache() {}

    public synchronized void clear() { cache.clear(); }

    /** Returns null when the block has no item to build it with. */
    public BuildItems items(World world, int x, int y, int z, Block block, int meta, EntityPlayer player) {
        boolean cacheable = !block.hasTileEntity(meta) && meta >= 0 && meta < 16;
        if (cacheable) {
            synchronized (this) {
                BuildItems[] metas = cache.get(block);
                if (metas != null && metas[meta] != null) return metas[meta] == UNKNOWN ? null : metas[meta];
            }
        }
        BuildItems items = resolve(world, x, y, z, block, meta, player);
        if (cacheable) {
            synchronized (this) { cache.computeIfAbsent(block, ignored -> new BuildItems[16])[meta] = items == null ? UNKNOWN : items; }
        }
        return items;
    }

    private static final BuildItems UNKNOWN = new BuildItems(Collections.<MaterialItemKey>emptyList(), new int[] {-1});

    private static BuildItems resolve(World world, int x, int y, int z, Block block, int meta, EntityPlayer player) {
        TileEntity tile = world == null ? null : world.getTileEntity(x, y, z);
        if (com.github.lunatrius.schematica.compat.MultipartItems.supports(tile)) {
            try {
                ItemStack[] parts = com.github.lunatrius.schematica.compat.MultipartItems.items(tile);
                return parts == null ? null : BuildItems.of(parts);
            } catch (ReflectiveOperationException e) {
                return null;
            }
        }
        if (block == Blocks.piston_head || block == Blocks.piston_extension || block == Blocks.portal || block == Blocks.end_portal
            || block == Blocks.fire) return BuildItems.NONE;
        if (block instanceof BlockDoor && (meta & 8) != 0) return BuildItems.NONE;
        if (block instanceof BlockBed && BlockBed.isBlockHeadOfBed(meta)) return BuildItems.NONE;
        if (block instanceof BlockDoublePlant && BlockDoublePlant.func_149887_c(meta)) return BuildItems.NONE;
        if (block == Blocks.farmland) return BuildItems.of(new ItemStack(Blocks.dirt));
        if (block instanceof BlockLiquid) {
            if (meta != 0) return BuildItems.NONE;
            return BuildItems.of(new ItemStack(block.getMaterial() == net.minecraft.block.material.Material.lava ? Items.lava_bucket
                : Items.water_bucket));
        }
        if (block instanceof IFluidBlock) return fluid((IFluidBlock) block, world, x, y, z, meta);
        if (block instanceof BlockFlowerPot) {
            ItemStack plant = tile instanceof TileEntityFlowerPot && ((TileEntityFlowerPot) tile).getFlowerPotItem() != null
                ? new ItemStack(((TileEntityFlowerPot) tile).getFlowerPotItem(), 1, ((TileEntityFlowerPot) tile).getFlowerPotData()) : null;
            return BuildItems.of(new ItemStack(Items.flower_pot), plant);
        }
        ItemStack stack = block.getPickBlock(new MovingObjectPosition(x, y, z, 1, Vec3.createVectorHelper(x + 0.5, y + 0.5, z + 0.5)),
            world, x, y, z, player);
        if (stack == null || stack.getItem() == null) return null;
        stack = stack.copy();
        stack.stackSize = 1;
        if (block instanceof BlockSlab && block.isOpaqueCube()) stack.stackSize = 2;
        else if (block == Blocks.snow_layer) stack.stackSize = (meta & 7) + 1;
        return BuildItems.of(stack);
    }

    private static BuildItems fluid(IFluidBlock block, World world, int x, int y, int z, int meta) {
        boolean source = block instanceof BlockFluidClassic ? meta == 0 : block.canDrain(world, x, y, z);
        if (!source || block.getFluid() == null) return BuildItems.NONE;
        ItemStack bucket = FluidContainerRegistry.fillFluidContainer(new FluidStack(block.getFluid(), FluidContainerRegistry.BUCKET_VOLUME),
            new ItemStack(Items.bucket));
        return bucket == null ? null : BuildItems.of(bucket);
    }

}

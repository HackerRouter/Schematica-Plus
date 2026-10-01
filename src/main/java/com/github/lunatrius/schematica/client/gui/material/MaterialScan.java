package com.github.lunatrius.schematica.client.gui.material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.chunk.Chunk;

import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.reference.Reference;

import cpw.mods.fml.common.registry.GameData;

public final class MaterialScan implements MaterialScanner {
    private final SchematicWorld schematic;
    private final WorldClient world;
    private final EntityPlayer player;
    private final Map<MaterialItemKey, int[]> counts = new LinkedHashMap<>();
    private final int[] bounds;
    private final int width, length;
    private final long volume;
    private final boolean ignoreState = com.github.lunatrius.schematica.handler.ConfigurationHandler.materialListIgnoreState;
    private long cursor;
    private int skipped;

    public MaterialScan(SchematicWorld schematic, WorldClient world, EntityPlayer player, boolean renderLayers) {
        this.schematic = schematic;
        this.world = world;
        this.player = player;
        bounds = renderLayers ? schematic.renderBounds()
            : new int[] {0, 0, 0, schematic.getWidth(), schematic.getHeight(), schematic.getLength()};
        width = bounds[3] - bounds[0];
        length = bounds[5] - bounds[2];
        volume = schematic.isEnabled() ? (long) width * length * (bounds[4] - bounds[1]) : 0;
    }

    public boolean done() { return cursor >= volume; }

    public int percent() { return volume == 0 ? 100 : (int) (cursor * 100 / volume); }

    public int skipped() { return skipped; }

    public void step() {
        long deadline = System.nanoTime() + 4_000_000L;
        int processed = 0;
        while (!done() && processed++ < 4096) {
            long index = cursor++;
            int z = (int) (index % length) + bounds[2];
            int x = (int) (index / length % width) + bounds[0];
            int y = (int) (index / ((long) length * width)) + bounds[1];
            collect(x, y, z);
            if (System.nanoTime() >= deadline) break;
        }
    }

    private void collect(int x, int y, int z) {
        try {
            if (schematic.isAirBlock(x, y, z)) return;
            Block block = schematic.getBlock(x, y, z);
            int meta = schematic.getBlockMetadata(x, y, z);
            MaterialCache.BuildItems items = MaterialCache.INSTANCE.items(schematic, x, y, z, block, meta, player);
            if (items == null) { skipped++; return; }
            if (items.isEmpty()) return;
            int wx = schematic.position.x + x;
            int wy = schematic.position.y + y;
            int wz = schematic.position.z + z;
            Chunk chunk = world.getChunkFromChunkCoords(wx >> 4, wz >> 4);
            boolean unknown = !verifiedChunk(chunk, wy);
            Block real = unknown ? null : world.getBlock(wx, wy, wz);
            boolean missing = unknown || real != block || !ignoreState && world.getBlockMetadata(wx, wy, wz) != meta;
            boolean mismatched = missing && !unknown && !world.isAirBlock(wx, wy, wz);
            for (int i = 0; i < items.size(); i++) {
                int amount = items.count(i);
                int[] count = counts.computeIfAbsent(items.key(i), ignored -> new int[4]);
                count[0] += amount;
                if (missing) count[1] += amount;
                if (mismatched) count[2] += amount;
                if (unknown) count[3] += amount;
            }
        } catch (Exception e) {
            skipped++;
            if (skipped <= 3) Reference.logger.debug("Could not count schematic material at {}, {}, {}", x, y, z, e);
        }
    }

    public List<MaterialListModel.Entry<MaterialItemKey>> result() {
        if (!done()) throw new IllegalStateException("Material scan is incomplete");
        List<MaterialListModel.Entry<MaterialItemKey>> result = new ArrayList<>();
        for (Map.Entry<MaterialItemKey, int[]> counted : counts.entrySet()) {
            MaterialItemKey key = counted.getKey();
            ItemStack stack = key.stack();
            String registry = String.valueOf(GameData.getItemRegistry().getNameForObject(stack.getItem()));
            String name;
            try { name = stack.getDisplayName(); }
            catch (Exception e) { name = registry; }
            int[] c = counted.getValue();
            result.add(new MaterialListModel.Entry<>(key, name, registry, c[0], c[1], c[2], c[3]));
        }
        updateAvailable(result, player);
        return result;
    }

    static boolean verifiedChunk(Chunk chunk, int y) {
        return y >= 0 && y < 256 && chunk != null && chunk.isChunkLoaded && !chunk.isEmpty();
    }

    public static boolean updateAvailable(List<MaterialListModel.Entry<MaterialItemKey>> entries, EntityPlayer player) {
        Map<MaterialItemKey, Long> inventory = new HashMap<>();
        if (player != null) {
            for (ItemStack stack : player.inventory.mainInventory) {
                if (stack != null && stack.getItem() != null && stack.stackSize > 0) {
                    MaterialItemKey key = new MaterialItemKey(stack);
                    inventory.put(key, inventory.getOrDefault(key, 0L) + stack.stackSize);
                }
            }
        }
        boolean changed = false;
        for (MaterialListModel.Entry<MaterialItemKey> entry : entries) {
            long available = inventory.getOrDefault(entry.key, 0L);
            changed |= entry.available != available;
            entry.available = available;
        }
        return changed;
    }
}

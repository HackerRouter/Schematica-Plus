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
import net.minecraft.util.MovingObjectPosition;

import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.reference.Reference;

import cpw.mods.fml.common.registry.GameData;

public final class MaterialScan {
    private final SchematicWorld schematic;
    private final WorldClient world;
    private final EntityPlayer player;
    private final Map<MaterialItemKey, int[]> counts = new LinkedHashMap<>();
    private final int startY;
    private final long volume;
    private long cursor;
    private int skipped;

    public MaterialScan(SchematicWorld schematic, WorldClient world, EntityPlayer player, boolean renderLayers) {
        this.schematic = schematic;
        this.world = world;
        this.player = player;
        boolean single = renderLayers && schematic.isRenderingLayer;
        startY = single ? schematic.renderingLayer : 0;
        volume = (long) schematic.getWidth() * schematic.getLength() * (single ? 1 : schematic.getHeight());
    }

    public boolean done() { return cursor >= volume; }

    public int percent() { return volume == 0 ? 100 : (int) (cursor * 100 / volume); }

    public int skipped() { return skipped; }

    public void step() {
        long deadline = System.nanoTime() + 4_000_000L;
        int processed = 0;
        while (!done() && processed++ < 4096) {
            long index = cursor++;
            int z = (int) (index % schematic.getLength());
            int x = (int) (index / schematic.getLength() % schematic.getWidth());
            int y = (int) (index / ((long) schematic.getLength() * schematic.getWidth())) + startY;
            collect(x, y, z);
            if (System.nanoTime() >= deadline) break;
        }
    }

    private void collect(int x, int y, int z) {
        try {
            if (schematic.isAirBlock(x, y, z)) return;
            Block block = schematic.getBlock(x, y, z);
            MovingObjectPosition target = new MovingObjectPosition(x, y, z, 1,
                net.minecraft.util.Vec3.createVectorHelper(x + 0.5, y + 0.5, z + 0.5));
            ItemStack stack = block.getPickBlock(target, schematic, x, y, z, player);
            if (stack == null || stack.getItem() == null) { skipped++; return; }
            MaterialItemKey key = new MaterialItemKey(stack);
            int wx = schematic.position.x + x;
            int wy = schematic.position.y + y;
            int wz = schematic.position.z + z;
            boolean unknown = !world.blockExists(wx, wy, wz);
            boolean missing = unknown || world.getBlock(wx, wy, wz) != block
                || world.getBlockMetadata(wx, wy, wz) != schematic.getBlockMetadata(x, y, z);
            boolean mismatched = missing && !unknown && !world.isAirBlock(wx, wy, wz);
            int[] count = counts.computeIfAbsent(key, ignored -> new int[4]);
            count[0]++;
            if (missing) count[1]++;
            if (mismatched) count[2]++;
            if (unknown) count[3]++;
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

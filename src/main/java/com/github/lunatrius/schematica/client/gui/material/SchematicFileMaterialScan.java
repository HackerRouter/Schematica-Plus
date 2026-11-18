// SPDX-License-Identifier: LGPL-3.0-only
// Litematica MaterialListSchematic / MaterialListUtils.createMaterialListFor, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.material;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.api.SchematicRegion;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.reference.Reference;

import cpw.mods.fml.common.registry.GameData;

/** Counts the items of a schematic that is not placed; everything counts as missing, as upstream does. */
public final class SchematicFileMaterialScan implements MaterialScanner {
    private final EntityPlayer player;
    private final List<Part> parts = new ArrayList<>();
    private final Map<MaterialItemKey, int[]> counts = new LinkedHashMap<>();
    private final long volume;
    private int part;
    private long cursor, offset;
    private int skipped;

    private static final class Part {
        final SchematicWorld world;
        final List<SchematicRegion> regions;
        final int width, length;
        final long volume;

        Part(SchematicWorld world, List<SchematicRegion> regions) {
            this.world = world;
            this.regions = regions;
            width = world.getWidth();
            length = world.getLength();
            volume = (long) width * length * world.getHeight();
        }

        boolean selected(int x, int y, int z) {
            if (!world.getSchematic().containsBlock(x, y, z)) return false;
            if (regions.isEmpty()) return true;
            for (SchematicRegion region : regions) if (region.contains(x, y, z)) return true;
            return false;
        }
    }

    /** regions: the sub-regions to count, or all of the schematic when empty; regions with own contents are read alone. */
    public SchematicFileMaterialScan(SchematicWorld schematic, Collection<SchematicRegion> regions, EntityPlayer player) {
        this.player = player;
        List<SchematicRegion> composed = new ArrayList<>();
        for (SchematicRegion region : regions) {
            ISchematic contents = schematic.getSchematic().getRegionSchematic(region.name);
            if (contents != null) parts.add(new Part(new SchematicWorld(contents), new ArrayList<>()));
            else composed.add(region);
        }
        if (regions.isEmpty() || !composed.isEmpty()) parts.add(0, new Part(schematic, composed));
        long total = 0;
        for (Part counted : parts) total += counted.volume;
        volume = total;
    }

    @Override public boolean done() { return offset + cursor >= volume; }

    @Override public int percent() { return volume == 0 ? 100 : (int) ((offset + cursor) * 100 / volume); }

    @Override public int skipped() { return skipped; }

    @Override
    public void step() {
        long deadline = System.nanoTime() + 4_000_000L;
        int processed = 0;
        while (!done() && processed++ < 4096) {
            Part current = parts.get(part);
            if (cursor >= current.volume) {
                offset += current.volume;
                cursor = 0;
                part++;
                continue;
            }
            long index = cursor++;
            int x = (int) (index % current.width), z = (int) (index / current.width % current.length);
            int y = (int) (index / ((long) current.width * current.length));
            if (current.selected(x, y, z)) collect(current.world, x, y, z);
            if (System.nanoTime() >= deadline) break;
        }
    }

    private void collect(SchematicWorld schematic, int x, int y, int z) {
        try {
            if (schematic.isAirBlock(x, y, z)) return;
            Block block = schematic.getBlock(x, y, z);
            MaterialCache.BuildItems items = MaterialCache.INSTANCE.items(schematic, x, y, z, block, schematic.getBlockMetadata(x, y, z), player);
            if (items == null) { skipped++; return; }
            for (int i = 0; i < items.size(); i++) counts.computeIfAbsent(items.key(i), ignored -> new int[1])[0] += items.count(i);
        } catch (Exception e) {
            skipped++;
            if (skipped <= 3) Reference.logger.debug("Could not count schematic material at {}, {}, {}", x, y, z, e);
        }
    }

    @Override
    public List<MaterialListModel.Entry<MaterialItemKey>> result() {
        if (!done()) throw new IllegalStateException("Material scan is incomplete");
        List<MaterialListModel.Entry<MaterialItemKey>> result = new ArrayList<>();
        for (Map.Entry<MaterialItemKey, int[]> counted : counts.entrySet()) {
            ItemStack stack = counted.getKey().stack();
            String registry = String.valueOf(GameData.getItemRegistry().getNameForObject(stack.getItem()));
            String name;
            try { name = stack.getDisplayName(); }
            catch (Exception e) { name = registry; }
            int total = counted.getValue()[0];
            result.add(new MaterialListModel.Entry<>(counted.getKey(), name, registry, total, total, 0, 0));
        }
        MaterialScan.updateAvailable(result, player);
        return result;
    }
}

package com.github.lunatrius.schematica.client.selection;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.github.lunatrius.schematica.api.SchematicRegion;
import com.github.lunatrius.schematica.util.SchematicLimits;
import com.github.lunatrius.schematica.world.storage.RegionSelection;

public final class AreaBlockCounter<T> {
    public interface Reader<T> {
        boolean loaded(int x, int y, int z);
        T read(int x, int y, int z);
    }

    private final RegionSelection selection;
    private final com.github.lunatrius.schematica.client.world.RenderLayerRange layers = new com.github.lunatrius.schematica.client.world.RenderLayerRange();
    private final Map<T, Integer> counts = new LinkedHashMap<>();
    private final int width, length, volume;
    private int cursor, selected, unverified, skipped;

    public AreaBlockCounter(List<SchematicRegion> regions) { this(regions, null); }

    public AreaBlockCounter(List<SchematicRegion> regions, com.github.lunatrius.schematica.client.world.RenderLayerRange range) {
        if (range != null) layers.load(range.toJson());
        selection = new RegionSelection(regions);
        width = selection.maxX - selection.minX + 1;
        length = selection.maxZ - selection.minZ + 1;
        volume = SchematicLimits.volume(width, selection.maxY - selection.minY + 1, length);
    }

    public boolean done() { return cursor == volume; }
    public int percent() { return (int) ((long) cursor * 100 / volume); }
    public int selected() { return selected; }
    public int unverified() { return unverified; }
    public int skipped() { return skipped; }
    public Map<T, Integer> counts() { return Collections.unmodifiableMap(counts); }

    public void step(Reader<T> reader, int budget, long deadline) {
        for (int processed = 0; !done() && processed < budget; processed++) {
            int index = cursor++;
            int z = index % length, x = index / length % width, y = index / (length * width);
            for (SchematicRegion region : selection.localRegions) {
                if (region.contains(x, y, z)) {
                    collect(reader, x + selection.minX, y + selection.minY, z + selection.minZ);
                    break;
                }
            }
            if (System.nanoTime() >= deadline) break;
        }
    }

    private void collect(Reader<T> reader, int x, int y, int z) {
        if (!layers.contains(x, y, z)) return;
        selected++;
        try {
            if (!reader.loaded(x, y, z)) { unverified++; return; }
            T key = reader.read(x, y, z);
            if (key != null) counts.put(key, counts.getOrDefault(key, 0) + 1);
        } catch (RuntimeException error) { skipped++; }
    }
}

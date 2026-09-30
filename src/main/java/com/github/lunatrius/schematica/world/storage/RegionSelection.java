package com.github.lunatrius.schematica.world.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.github.lunatrius.schematica.util.SchematicLimits;

public final class RegionSelection {
    public final int minX, minY, minZ, maxX, maxY, maxZ;
    public final List<SchematicRegion> localRegions;

    public RegionSelection(List<SchematicRegion> regions) {
        if (regions.isEmpty() || regions.size() > 256) throw new IllegalArgumentException("Select between 1 and 256 subregions");
        int ax = Integer.MAX_VALUE, ay = ax, az = ax, bx = Integer.MIN_VALUE, by = bx, bz = bx;
        for (SchematicRegion region : regions) {
            ax = Math.min(ax, region.minX); ay = Math.min(ay, region.minY); az = Math.min(az, region.minZ);
            bx = Math.max(bx, region.maxX); by = Math.max(by, region.maxY); bz = Math.max(bz, region.maxZ);
        }
        SchematicLimits.worldBounds(ax, ay, az, bx, by, bz);
        minX = ax; minY = ay; minZ = az; maxX = bx; maxY = by; maxZ = bz;
        List<SchematicRegion> local = new ArrayList<>();
        for (SchematicRegion region : regions) local.add(region.offset(-minX, -minY, -minZ));
        localRegions = Collections.unmodifiableList(local);
    }
}

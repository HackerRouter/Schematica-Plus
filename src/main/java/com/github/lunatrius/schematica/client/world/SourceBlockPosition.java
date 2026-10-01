package com.github.lunatrius.schematica.client.world;

import java.util.ArrayList;
import java.util.List;

import com.github.lunatrius.schematica.api.SchematicOrigin;
import com.github.lunatrius.schematica.api.SchematicRegion;

public final class SourceBlockPosition {
    public final SubRegionPlacements.Region region;
    public final SchematicOrigin local;

    private SourceBlockPosition(SubRegionPlacements.Region region, SchematicOrigin local) { this.region = region; this.local = local; }

    public String name() { return region.name(); }

    /** The last enabled region covering the point owns it in the composed placement, as in {@link RegionComposer}. */
    public static SourceBlockPosition resolve(SchematicOrigin world, SchematicOrigin origin, List<String> transforms, SubRegionPlacements regions) {
        SchematicOrigin point = SubRegionPlacements.vector(new SchematicOrigin(world.x - origin.x, world.y - origin.y, world.z - origin.z), transforms, true);
        List<SubRegionPlacements.Region> values = regions.regions();
        for (int i = values.size() - 1; i >= 0; i--) {
            SubRegionPlacements.Region region = values.get(i);
            if (!region.enabled || !region.bounds().contains(point.x, point.y, point.z)) continue;
            SchematicOrigin local = SubRegionPlacements.vector(new SchematicOrigin(point.x - region.position.x, point.y - region.position.y, point.z - region.position.z),
                region.operations(), true).atMinimum(region.pivot.x, region.pivot.y, region.pivot.z);
            return new SourceBlockPosition(region, local);
        }
        return null;
    }

    /** Maps a position inside a region's source box (relative to its minimum corner) to world coordinates. */
    public static SchematicOrigin world(SubRegionPlacements.Region region, SchematicOrigin local, SchematicOrigin origin, List<String> transforms) {
        SchematicOrigin relative = SubRegionPlacements.vector(new SchematicOrigin(local.x - region.pivot.x, local.y - region.pivot.y, local.z - region.pivot.z),
            region.operations(), false).atMinimum(region.position.x, region.position.y, region.position.z);
        return SubRegionPlacements.vector(relative, transforms, false).atMinimum(origin.x, origin.y, origin.z);
    }

    public static boolean inside(SchematicRegion box, SchematicOrigin local) {
        return local.x >= 0 && local.y >= 0 && local.z >= 0 && local.x <= box.maxX - box.minX
            && local.y <= box.maxY - box.minY && local.z <= box.maxZ - box.minZ;
    }

    public static List<String> inverse(List<String> operations) {
        List<String> result = new ArrayList<>();
        for (int i = operations.size() - 1; i >= 0; i--) {
            String operation = operations.get(i);
            int count = Character.isUpperCase(operation.charAt(0)) ? 3 : 1;
            for (int j = 0; j < count; j++) result.add(operation);
        }
        return result;
    }

    public static List<String> combined(SubRegionPlacements.Region region, List<String> global) {
        List<String> result = new ArrayList<>(region.operations());
        result.addAll(global);
        return result;
    }
}

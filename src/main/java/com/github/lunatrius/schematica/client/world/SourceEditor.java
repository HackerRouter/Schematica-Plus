package com.github.lunatrius.schematica.client.world;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.api.SchematicOrigin;
import com.github.lunatrius.schematica.api.SchematicRegion;
import com.github.lunatrius.schematica.world.storage.Schematic;

/** Applies rebuild edits to a source's working copy and to the composed placements that show it. */
public final class SourceEditor {
    /** Larger edits recompose every placement of the source instead of patching individual cells. */
    public static final int PATCH_LIMIT = 4096;

    private SourceEditor() {}

    /** A source cell: coordinates inside an independent region payload, or flat source coordinates when region is null. */
    public static final class Cell {
        public final String region;
        public final int x, y, z;

        public Cell(String region, int x, int y, int z) { this.region = region; this.x = x; this.y = y; this.z = z; }

        @Override public boolean equals(Object other) {
            if (!(other instanceof Cell)) return false;
            Cell cell = (Cell) other;
            return x == cell.x && y == cell.y && z == cell.z && java.util.Objects.equals(region, cell.region);
        }

        @Override public int hashCode() { return java.util.Objects.hash(region, x, y, z); }
    }

    /** The source cell shown at a resolved placement position. */
    public static Cell cell(ISchematic source, SourceBlockPosition position) {
        if (source.getRegionSchematic(position.name()) != null) return new Cell(position.name(), position.local.x, position.local.y, position.local.z);
        SchematicRegion box = position.region.box;
        return new Cell(null, box.minX + position.local.x, box.minY + position.local.y, box.minZ + position.local.z);
    }

    public static ISchematic container(ISchematic source, Cell cell) {
        return cell.region == null ? source : source.getRegionSchematic(cell.region);
    }

    public static List<Cell> apply(SchematicSourceData data, Map<Cell, CellState> changes) throws IOException {
        Schematic edited = data.editable();
        List<Cell> applied = new ArrayList<>();
        for (Map.Entry<Cell, CellState> entry : changes.entrySet()) {
            Cell cell = entry.getKey();
            CellState state = entry.getValue();
            ISchematic target = container(edited, cell);
            if (target == null || !target.containsBlock(cell.x, cell.y, cell.z)) continue;
            if (state.tile == null && state.matches(target, cell.x, cell.y, cell.z)) continue;
            state.write(target, cell.x, cell.y, cell.z);
            if (cell.region != null) {
                SchematicRegion box = region(edited, cell.region);
                int x = box.minX + cell.x, y = box.minY + cell.y, z = box.minZ + cell.z;
                if (owner(edited.getRegions(), x, y, z) == box) state.write(edited, x, y, z);
            }
            applied.add(cell);
        }
        if (!applied.isEmpty()) data.changed();
        return applied;
    }

    /** Patches the composed cells of one placement; returns the touched world bounds or null. */
    public static int[] patch(SchematicWorld world, ISchematic edited, Collection<Cell> cells, Map<String, CellState> cache) {
        SubRegionPlacements regions = world.subregions();
        if (regions == null) return null;
        SchematicOrigin origin = world.originPosition();
        int[] bounds = null;
        for (Cell cell : cells) {
            CellState state = null;
            for (SubRegionPlacements.Region region : regions.regions()) {
                if (!region.enabled) continue;
                SchematicOrigin local;
                if (cell.region != null) {
                    if (!region.name().equals(cell.region)) continue;
                    local = new SchematicOrigin(cell.x, cell.y, cell.z);
                } else {
                    if (edited.getRegionSchematic(region.name()) != null || !region.box.contains(cell.x, cell.y, cell.z)) continue;
                    local = new SchematicOrigin(cell.x - region.box.minX, cell.y - region.box.minY, cell.z - region.box.minZ);
                }
                SchematicOrigin target = SourceBlockPosition.world(region, local, origin, world.transformOperations);
                SourceBlockPosition owner = SourceBlockPosition.resolve(target, origin, world.transformOperations, regions);
                if (owner == null || !owner.name().equals(region.name()) || owner.local.x != local.x || owner.local.y != local.y || owner.local.z != local.z) continue;
                if (state == null) state = CellState.read(container(edited, cell), cell.x, cell.y, cell.z);
                world.writeCell(target.x - world.position.x, target.y - world.position.y, target.z - world.position.z,
                    state.transform(SourceBlockPosition.combined(region, world.transformOperations), cache));
                bounds = include(bounds, target);
            }
        }
        if (bounds != null) world.refreshChests();
        return bounds;
    }

    static SchematicRegion owner(List<SchematicRegion> regions, int x, int y, int z) {
        for (int i = regions.size() - 1; i >= 0; i--) if (regions.get(i).contains(x, y, z)) return regions.get(i);
        return null;
    }

    private static SchematicRegion region(ISchematic source, String name) {
        for (SchematicRegion region : source.getRegions()) if (region.name.equals(name)) return region;
        throw new IllegalArgumentException("Unknown region: " + name);
    }

    private static int[] include(int[] bounds, SchematicOrigin point) {
        if (bounds == null) return new int[] {point.x, point.y, point.z, point.x, point.y, point.z};
        bounds[0] = Math.min(bounds[0], point.x); bounds[1] = Math.min(bounds[1], point.y); bounds[2] = Math.min(bounds[2], point.z);
        bounds[3] = Math.max(bounds[3], point.x); bounds[4] = Math.max(bounds[4], point.y); bounds[5] = Math.max(bounds[5], point.z);
        return bounds;
    }
}

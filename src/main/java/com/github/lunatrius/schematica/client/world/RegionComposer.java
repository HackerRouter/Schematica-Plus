package com.github.lunatrius.schematica.client.world;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.api.SchematicOrigin;
import com.github.lunatrius.schematica.api.SchematicRegion;
import com.github.lunatrius.schematica.client.gui.placement.PlacementTransform;
import com.github.lunatrius.schematica.world.storage.SchematicCopies;
import com.github.lunatrius.schematica.util.SchematicLimits;
import com.github.lunatrius.schematica.world.storage.Schematic;

final class RegionComposer {
    final SchematicWorld world;
    final BitSet visible;

    private RegionComposer(SchematicWorld world, BitSet visible) {
        this.world = world;
        this.visible = visible;
    }

    static RegionComposer build(ISchematic source, SubRegionPlacements placements, List<String> operations) {
        SubRegionPlacements.Layout layout = placements.layout();
        int[] size = PlacementTransform.transformedSize(layout.width, layout.height, layout.length, String.join("", operations));
        SchematicLimits.volume(size[0], size[1], size[2]);
        Schematic result = new Schematic(source.getIcon(), layout.width, layout.height, layout.length);
        result.setOrigin(new SchematicOrigin(-layout.minimum.x, -layout.minimum.y, -layout.minimum.z));
        List<SchematicRegion> bounds = new ArrayList<>();
        for (SchematicRegion box : layout.bounds) bounds.add(box.offset(-layout.minimum.x, -layout.minimum.y, -layout.minimum.z));
        result.setRegions(bounds);
        BitSet visible = new BitSet();
        for (SubRegionPlacements.Region region : placements.regions()) {
            if (!region.enabled) continue;
            SchematicWorld part = new SchematicWorld(extract(source, placements, region));
            PlacementState.applyTransforms(part, region.operations());
            SchematicRegion box = region.bounds();
            int dx = box.minX - layout.minimum.x, dy = box.minY - layout.minimum.y, dz = box.minZ - layout.minimum.z;
            SchematicCopies.overlay(part.getSchematic(), result, dx, dy, dz, true);
            for (int y = 0; y < part.getHeight(); y++) {
                for (int z = 0; z < part.getLength(); z++) {
                    for (int x = 0; x < part.getWidth(); x++) {
                        visible.set(x + dx + layout.width * (z + dz + layout.length * (y + dy)), region.rendering);
                    }
                }
            }

        }
        int width = layout.width, height = layout.height, length = layout.length;
        for (String operation : operations) {
            BitSet transformed = new BitSet();
            int[] nextSize = PlacementTransform.transformedSize(width, height, length, operation);
            for (int index = visible.nextSetBit(0); index >= 0; index = visible.nextSetBit(index + 1)) {
                SchematicOrigin point = new SchematicOrigin(index % width, index / width / length, index / width % length)
                    .transform(operation.charAt(0), width, height, length);
                transformed.set(point.x + nextSize[0] * (point.z + nextSize[2] * point.y));
            }
            visible = transformed;
            width = nextSize[0]; height = nextSize[1]; length = nextSize[2];
        }
        SchematicWorld world = new SchematicWorld(result);
        PlacementState.applyTransforms(world, operations);
        if (!placements.hasEnabled()) ((Schematic) world.getSchematic()).setEmpty();
        return new RegionComposer(world, visible);
    }

    private static Schematic extract(ISchematic source, SubRegionPlacements placements, SubRegionPlacements.Region region) {
        SchematicRegion box = region.box;
        Schematic result = new Schematic(source.getIcon(), box.maxX - box.minX + 1, box.maxY - box.minY + 1, box.maxZ - box.minZ + 1);
        result.setOrigin(region.pivot);
        ISchematic independent = source.getRegionSchematic(region.name());
        if (independent != null) {
            SchematicCopies.overlay(independent, result, 0, 0, 0, !region.ignoreEntities);
            return result;
        }
        for (int y = box.minY; y <= box.maxY; y++) {
            for (int z = box.minZ; z <= box.maxZ; z++) {
                for (int x = box.minX; x <= box.maxX; x++) {
                    result.setBlock(x - box.minX, y - box.minY, z - box.minZ, source.getBlock(x, y, z), source.getBlockMetadata(x, y, z));
                }
            }
        }
        for (TileEntity tile : source.getTileEntities()) if (box.contains(tile.xCoord, tile.yCoord, tile.zCoord)) {
            TileEntity copy = SchematicCopies.tile(tile, box.minX, box.minY, box.minZ);
            result.setTileEntity(copy.xCoord, copy.yCoord, copy.zCoord, copy);
        }
        if (!region.ignoreEntities) for (Entity entity : source.getEntities()) {
            int x = (int) Math.floor(entity.posX), y = (int) Math.floor(entity.posY), z = (int) Math.floor(entity.posZ);
            for (SubRegionPlacements.Region owner : placements.regions()) if (owner.box.contains(x, y, z)) {
                if (owner == region) result.addEntity(SchematicCopies.entity(entity, box.minX, box.minY, box.minZ));
                break;
            }
        }
        return result;
    }

}

package com.github.lunatrius.schematica.client.world;

import java.io.IOException;
import java.util.UUID;

import com.github.lunatrius.schematica.api.SchematicOrigin;
import com.github.lunatrius.schematica.client.renderer.RendererSchematicGlobal;
import com.github.lunatrius.schematica.cloud.SharedPlacement;

public final class SharedWorlds {
    private SharedWorlds() {}

    public static SchematicWorld create(SchematicLibrary.Source<SchematicSourceData> source, SharedPlacement placement) throws IOException {
        SchematicWorld world = Placements.LIBRARY.create(source, (data, previous) -> {
            SchematicWorld created = Placements.instantiate(source, data);
            created.name = placement.name;
            created.sharedId = placement.id;
            PlacementState.applyTransforms(created, placement.transforms);
            created.restoreSubregions(placement.subregionData());
            SchematicOrigin minimum = created.getSchematic().getOrigin().restoredMinimum(placement.origin);
            created.position.set(minimum.x, minimum.y, minimum.z);
            created.setPlacementSettings(created.placementSettings().locked(true));
            return created;
        });
        RendererSchematicGlobal.INSTANCE.createRendererSchematicChunks(world);
        if (Placements.selected == null) Placements.select(world);
        return world;
    }

    public static SharedPlacement capture(SchematicWorld world, UUID id, String name, String fileName, String hash, long size, int dimension) {
        return new SharedPlacement(id, name, fileName, hash, size, dimension, world.originPosition().coordinates(),
            world.transformOperations, world.subregions() == null ? "" : world.subregions().toJson().toString(), "", "", "", "", 0);
    }

    public static SharedPlacement capture(SchematicWorld world, SharedPlacement shared) {
        return new SharedPlacement(shared.id, shared.name, shared.fileName, shared.hash, shared.size, shared.dimension,
            world.originPosition().coordinates(), world.transformOperations,
            world.subregions() == null ? "" : world.subregions().toJson().toString(), "", "", "", "", shared.version);
    }
}

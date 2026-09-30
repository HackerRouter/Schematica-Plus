package com.github.lunatrius.schematica.client.renderer;

import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.world.World;
import com.github.lunatrius.schematica.client.world.SchematicWorld;

final class SchematicTileRenderContext implements AutoCloseable {
    private final TileEntityRendererDispatcher dispatcher = TileEntityRendererDispatcher.instance;
    private final World world = dispatcher.field_147550_f;
    private final double cameraX = dispatcher.field_147560_j;
    private final double cameraY = dispatcher.field_147561_k;
    private final double cameraZ = dispatcher.field_147558_l;
    private final double renderX = TileEntityRendererDispatcher.staticPlayerX;
    private final double renderY = TileEntityRendererDispatcher.staticPlayerY;
    private final double renderZ = TileEntityRendererDispatcher.staticPlayerZ;

    SchematicTileRenderContext(SchematicWorld schematic) {
        dispatcher.field_147550_f = schematic;
        dispatcher.field_147560_j -= schematic.position.x;
        dispatcher.field_147561_k -= schematic.position.y;
        dispatcher.field_147558_l -= schematic.position.z;
        TileEntityRendererDispatcher.staticPlayerX -= schematic.position.x;
        TileEntityRendererDispatcher.staticPlayerY -= schematic.position.y;
        TileEntityRendererDispatcher.staticPlayerZ -= schematic.position.z;
    }

    @Override public void close() {
        dispatcher.field_147550_f = world;
        dispatcher.field_147560_j = cameraX;
        dispatcher.field_147561_k = cameraY;
        dispatcher.field_147558_l = cameraZ;
        TileEntityRendererDispatcher.staticPlayerX = renderX;
        TileEntityRendererDispatcher.staticPlayerY = renderY;
        TileEntityRendererDispatcher.staticPlayerZ = renderZ;
    }
}

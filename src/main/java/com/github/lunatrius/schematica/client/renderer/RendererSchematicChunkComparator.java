package com.github.lunatrius.schematica.client.renderer;

import java.util.Comparator;

import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3d;
import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;

public class RendererSchematicChunkComparator implements Comparator<RendererSchematicChunk> {

    private final Vector3d position = new Vector3d();
    private final Vector3d schematicPosition = new Vector3d();

    @Override
    public int compare(RendererSchematicChunk rendererSchematicChunk1, RendererSchematicChunk rendererSchematicChunk2) {
        if (rendererSchematicChunk1.isInFrustrum && !rendererSchematicChunk2.isInFrustrum) {
            return -1;
        } else if (!rendererSchematicChunk1.isInFrustrum && rendererSchematicChunk2.isInFrustrum) {
            return 1;
        } else {
            final double dist1 = this.position.lengthSquaredTo(rendererSchematicChunk1.centerPosition);
            final double dist2 = this.position.lengthSquaredTo(rendererSchematicChunk2.centerPosition);
            return Double.compare(dist1, dist2);
        }
    }

    public void setPosition(Vector3i position, Vector3d cameraPosition) {
        this.position.set(cameraPosition)
            .sub(position.toVector3d(this.schematicPosition));
    }
}

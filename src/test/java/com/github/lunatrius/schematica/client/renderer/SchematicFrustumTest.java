package com.github.lunatrius.schematica.client.renderer;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SchematicFrustumTest {
    private float[] identity() {
        return new float[] {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
    }

    @Test public void retainsIntersectingBoxesAndPlaneBoundaries() {
        SchematicFrustum frustum = new SchematicFrustum();
        frustum.update(identity(), identity());
        assertTrue(frustum.isVisible(-2, -2, -2, 2, 2, 2));
        assertTrue(frustum.isVisible(1, 0, 0, 2, 1, 1));
        assertFalse(frustum.isVisible(1.01, 0, 0, 2, 1, 1));
        assertFalse(frustum.isVisible(-1, -1, -3, 1, 1, -2));
    }

    @Test public void perspectiveKeepsDistantChunksAndRejectsBehindCamera() {
        SchematicFrustum frustum = new SchematicFrustum();
        float[] projection = {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, -513f / 511, -1, 0, 0, -1024f / 511, 0};
        frustum.update(projection, identity());
        assertTrue(frustum.isVisible(-8, -8, -256, 8, 8, -240));
        assertFalse(frustum.isVisible(-8, -8, 240, 8, 8, 256));
        assertFalse(frustum.isVisible(-8, -8, -544, 8, 8, -528));
        assertTrue(frustum.isVisible(15, -8, -32, 31, 8, -16));
        assertTrue(frustum.isVisible(16, -8, -32, 32, 8, -16));
    }

    @Test public void capturesChangedCameraTransformWithoutStalePlanes() {
        SchematicFrustum frustum = new SchematicFrustum();
        float[] modelView = identity();
        modelView[12] = -16;
        frustum.update(identity(), modelView);
        assertTrue(frustum.isVisible(15, 0, 0, 16, 1, 1));
        assertFalse(frustum.isVisible(0, 0, 0, 1, 1, 1));
        frustum.update(identity(), identity());
        assertFalse(frustum.isVisible(15, 0, 0, 16, 1, 1));
        assertTrue(frustum.isVisible(0, 0, 0, 1, 1, 1));
    }
}

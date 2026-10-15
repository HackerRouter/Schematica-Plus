package com.github.lunatrius.schematica.client.gui.browser;

import org.junit.Test;

import static org.junit.Assert.*;

public class PreviewCameraTest {
    @Test public void resetFitsAllCornersInLandscapeAndPortraitViews() {
        PreviewCamera camera = new PreviewCamera((float) Math.sqrt(17 * 17 + 9 * 9 + 17 * 17));
        for (int width : new int[] {60, 200}) {
            camera.viewport(width, 120);
            camera.reset();
            double yaw = Math.toRadians(camera.yaw), pitch = Math.toRadians(camera.pitch);
            for (int sx : new int[] {-1, 1}) for (int sy : new int[] {-1, 1}) for (int sz : new int[] {-1, 1}) {
                double x = sx * 8.5, y = sy * 4.5, z = sz * 8.5;
                double rx = x * Math.cos(yaw) + z * Math.sin(yaw), rz = -x * Math.sin(yaw) + z * Math.cos(yaw);
                double ry = y * Math.cos(pitch) - rz * Math.sin(pitch), depth = y * Math.sin(pitch) + rz * Math.cos(pitch);
                double halfHeight = (camera.distance() - depth) * PreviewCamera.TAN_HALF_FOV;
                assertTrue("vertical corner outside viewport", Math.abs(ry) < halfHeight);
                assertTrue("horizontal corner outside viewport", Math.abs(rx) < halfHeight * width / 120);
            }
        }
    }

    @Test public void navigationUsesViewDirectionsAndPausesRotation() {
        PreviewCamera camera = new PreviewCamera(20);
        camera.advance(0.1f, 1, 0, 0);
        assertEquals(-1, camera.x, 0.0001);
        camera.advance(0.1f, 0, 1, 0);
        assertEquals(-1, camera.y, 0.0001);
        camera.advance(0.1f, 0, 0, 1);
        assertEquals(1, camera.z, 0.0001);
        assertEquals(45, camera.yaw, 0.0001);
        assertFalse(camera.rotating);
        camera.advance(0.1f, -1, 0, 0);
        camera.advance(0.1f, 0, -1, 0);
        camera.advance(0.1f, 0, 0, -1);
        assertEquals(0, camera.x + camera.y + camera.z, 0.0001);
    }

    @Test public void diagonalMovementIsNormalizedAndLongFramesAreBounded() {
        PreviewCamera camera = new PreviewCamera(20);
        camera.advance(10, 1, 1, 1);
        assertEquals(1, Math.sqrt(camera.x * camera.x + camera.y * camera.y + camera.z * camera.z), 0.0001);
        float before = camera.z;
        camera.advance(-1, 0, 0, 1);
        assertEquals(before, camera.z, 0);
    }

    @Test public void manualViewAndResetPreserveThePausedState() {
        PreviewCamera camera = new PreviewCamera(20);
        camera.turn(30, 1000);
        assertEquals(90, camera.pitch, 0);
        camera.pan(50, 20, 120);
        camera.zoom(1);
        camera.reset();
        assertEquals(45, camera.yaw, 0);
        assertEquals(30, camera.pitch, 0);
        assertEquals(1, camera.zoom, 0);
        assertEquals(0, camera.x + camera.y + camera.z, 0);
        camera.advance(0.1f, 0, 0, 0);
        assertEquals(45, camera.yaw, 0);
        camera.rotating = true;
        camera.advance(0.1f, 0, 0, 0);
        assertEquals(47, camera.yaw, 0);
    }

    @Test public void panFollowsTheCursorAndScalesWithTheViewport() {
        PreviewCamera small = new PreviewCamera(20), large = new PreviewCamera(20);
        small.pan(10, 20, 100);
        large.pan(20, 40, 200);
        assertTrue(small.x > 0);
        assertTrue(small.y < 0);
        assertEquals(small.x, large.x, 0.0001);
        assertEquals(small.y, large.y, 0.0001);
        small.z = small.distance();
        small.pan(10, 10, 0);
        assertFalse(Float.isInfinite(small.x));
    }

    @Test public void zoomAndTravelRemainBoundedAndResetRecoversTheModel() {
        PreviewCamera camera = new PreviewCamera(0);
        for (int i = 0; i < 1000; i++) camera.zoom(1);
        assertEquals(32, camera.zoom, 0);
        assertTrue(camera.distance() > 0);
        for (int i = 0; i < 1000; i++) camera.zoom(-1);
        assertEquals(0.125, camera.zoom, 0);
        for (int i = 0; i < 10000; i++) camera.advance(0.1f, 0, 0, 1);
        assertTrue(camera.z <= 100);
        camera.reset();
        assertEquals(0, camera.z, 0);
    }
}

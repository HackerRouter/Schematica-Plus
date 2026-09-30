package com.github.lunatrius.schematica.client.printer;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class PlacementAimTest {
    @Test public void aimsAtCardinalFaces() {
        assertEquals(0, new PlacementAim(0, 0, 1).yaw, 0.0001);
        assertEquals(-90, new PlacementAim(1, 0, 0).yaw, 0.0001);
        assertEquals(-180, new PlacementAim(0, 0, -1).yaw, 0.0001);
        assertEquals(90, new PlacementAim(-1, 0, 0).yaw, 0.0001);
    }

    @Test public void aimsAboveAndBelowWithoutNaNAtVerticalFaces() {
        assertEquals(-90, new PlacementAim(0, 1, 0).pitch, 0.0001);
        assertEquals(90, new PlacementAim(0, -1, 0).pitch, 0.0001);
        assertEquals(-45, new PlacementAim(3, 5, 4).pitch, 0.0001);
    }

    @Test public void reconstructsTheTargetDirectionInAllOctants() {
        for (int x : new int[] {-3, 3}) {
            for (int y : new int[] {-4, 4}) {
                for (int z : new int[] {-5, 5}) {
                    PlacementAim aim = new PlacementAim(x, y, z);
                    double yaw = Math.toRadians(aim.yaw);
                    double pitch = Math.toRadians(aim.pitch);
                    double length = Math.sqrt(x * x + y * y + z * z);
                    assertEquals(x / length, -Math.sin(yaw) * Math.cos(pitch), 1.0e-6);
                    assertEquals(y / length, -Math.sin(pitch), 1.0e-6);
                    assertEquals(z / length, Math.cos(yaw) * Math.cos(pitch), 1.0e-6);
                }
            }
        }
    }
}

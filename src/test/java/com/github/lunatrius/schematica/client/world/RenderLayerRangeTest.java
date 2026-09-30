package com.github.lunatrius.schematica.client.world;

import com.google.gson.JsonObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class RenderLayerRangeTest {
    @Test public void modesUseInclusiveWorldCoordinatesOnEveryAxis() {
        for (RenderLayerRange.Axis axis : RenderLayerRange.Axis.values()) {
            RenderLayerRange range = new RenderLayerRange();
            range.setAxis(axis);
            for (RenderLayerRange.Mode mode : RenderLayerRange.Mode.values()) {
                range.setMode(mode);
                range.setHere(-16);
                for (int coordinate = -17; coordinate <= -15; coordinate++) {
                    long[] p = {999, 999, 999};
                    p[axis.ordinal()] = coordinate;
                    boolean expected = mode == RenderLayerRange.Mode.ALL
                        || (mode == RenderLayerRange.Mode.ALL_ABOVE ? coordinate >= -16
                            : mode == RenderLayerRange.Mode.ALL_BELOW ? coordinate <= -16 : coordinate == -16);
                    assertEquals(expected, range.contains(p[0], p[1], p[2]));
                }
            }
        }
    }

    @Test public void clippedScanBoundsMatchPreviewIncludingLegacyIntersection() {
        RenderLayerRange range = new RenderLayerRange();
        for (RenderLayerRange.Axis axis : RenderLayerRange.Axis.values()) {
            range.setAxis(axis);
            for (RenderLayerRange.Mode mode : RenderLayerRange.Mode.values()) {
                range.setMode(mode);
                range.setHere(-3);
                range.setValue(true, 2);
                for (boolean legacy : new boolean[] {false, true}) {
                    for (int origin = -10; origin < 10; origin++) {
                        int[] b = range.localBounds(origin, origin, origin, 5, 4, 3, legacy, 2);
                        for (int x = 0; x < 5; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 3; z++) {
                            boolean visible = range.contains(origin + x, origin + y, origin + z) && (!legacy || y == 2);
                            boolean scanned = x >= b[0] && x < b[3] && y >= b[1] && y < b[4] && z >= b[2] && z < b[5];
                            assertEquals(visible, scanned);
                        }
                    }
                }
            }
        }
    }

    @Test public void largeCoordinatesCannotWrapOrTurnEmptySlicesIntoFullScans() {
        RenderLayerRange range = new RenderLayerRange();
        range.setAxis(RenderLayerRange.Axis.X);
        range.setMode(RenderLayerRange.Mode.SINGLE_LAYER);
        range.setHere(Integer.MAX_VALUE);
        assertArrayEquals(new int[] {1, 0, 0, 2, 3, 4},
            range.localBounds(Integer.MAX_VALUE - 1, 0, 0, 4, 3, 4, false, 0));
        range.setValue(false, Long.MIN_VALUE);
        int[] bounds = range.localBounds(Integer.MAX_VALUE - 1, 0, 0, 4, 3, 4, false, 0);
        assertEquals(bounds[0], bounds[3]);
        range.move(-1, 0);
        assertEquals(Integer.MIN_VALUE, range.value(false));
        assertFalse(range.contains((long) Integer.MAX_VALUE + 1, 0, 0));
    }

    @Test public void rangeHotkeysSelectNearestOrExplicitBoundariesAndKeepWidthAtLimits() {
        RenderLayerRange range = new RenderLayerRange();
        range.setMode(RenderLayerRange.Mode.LAYER_RANGE);
        range.setHere(10);
        range.setValue(true, 20);
        range.move(1, 11);
        assertEquals(11, range.value(false));
        range.move(-1, 20);
        assertEquals(19, range.value(true));
        range.setMoveMin(true);
        range.move(1, 1000);
        assertEquals(12, range.value(false));
        range.setMoveMax(true);
        range.move(Integer.MAX_VALUE, 0);
        assertEquals(Integer.MAX_VALUE, range.value(true));
        assertEquals(7, range.value(true) - range.value(false));
        range.move(1, 0);
        assertEquals(7, range.value(true) - range.value(false));
        range.setMoveMin(false);
        range.move(Integer.MIN_VALUE, 0);
        assertEquals(range.value(false), range.value(true));
    }

    @Test public void modesRememberTheirValuesAndRangeCannotInvert() {
        RenderLayerRange range = new RenderLayerRange();
        range.setMode(RenderLayerRange.Mode.SINGLE_LAYER);
        range.setHere(64);
        range.setMode(RenderLayerRange.Mode.LAYER_RANGE);
        range.setHere(20);
        range.setValue(false, 30);
        range.setValue(true, 10);
        assertEquals(20, range.value(false));
        assertEquals(20, range.value(true));
        range.setMode(RenderLayerRange.Mode.SINGLE_LAYER);
        assertEquals(64, range.value(false));
    }

    @Test public void jsonRoundTripResetAndMalformedLoadAreAtomic() {
        RenderLayerRange range = new RenderLayerRange();
        range.setAxis(RenderLayerRange.Axis.Z);
        range.setMode(RenderLayerRange.Mode.LAYER_RANGE);
        range.setHere(-45);
        range.setValue(true, -12);
        range.setMoveMin(true);
        JsonObject snapshot = range.toJson();
        RenderLayerRange restored = new RenderLayerRange();
        restored.load(snapshot);
        assertEquals(snapshot, restored.toJson());
        JsonObject invalid = range.toJson();
        invalid.addProperty("layer_range_max", "2147483648");
        assertThrows(ArithmeticException.class, () -> restored.load(invalid));
        assertEquals(snapshot, restored.toJson());
        restored.load(null);
        assertEquals(RenderLayerRange.Mode.ALL, restored.mode());
        assertEquals(RenderLayerRange.Axis.Y, restored.axis());
        assertFalse(restored.moveMin());
        assertTrue(restored.revision() > 1);
    }
}

package com.github.lunatrius.schematica.client.gui.placement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;

public class PlacementTransformTest {
    private List<String> operations(String value) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < value.length(); i++) result.add(value.substring(i, i + 1));
        return result;
    }

    @Test public void rotatedNonSquareSchematicKeepsItsOriginalCornerAsAnchor() {
        assertArrayEquals(new int[] {4, 0, 0}, PlacementTransform.anchorOffset(5, 3, 2, operations("Y")));
        assertArrayEquals(new int[] {1, 0, 4}, PlacementTransform.anchorOffset(2, 3, 5, operations("YY")));
        assertArrayEquals(new int[] {0, 0, 1}, PlacementTransform.anchorOffset(5, 3, 2, operations("YYY")));
        assertArrayEquals(new int[] {0, 0, 0}, PlacementTransform.anchorOffset(2, 3, 5, operations("YYYY")));
    }

    @Test public void mixedThreeDimensionalTransformsRecoverTheOriginalAnchor() {
        assertArrayEquals(new int[] {0, 0, 2}, PlacementTransform.anchorOffset(2, 5, 3, operations("X")));
        assertArrayEquals(new int[] {0, 4, 0}, PlacementTransform.anchorOffset(3, 5, 2, operations("zYZ")));
        assertArrayEquals(new int[] {0, 0, 0}, PlacementTransform.anchorOffset(2, 3, 5, operations("XYYYYXXX")));
    }

    @Test public void mirrorCyclesRespectRotatedLocalAxesAndKeepTheirLabels() {
        for (int r = 0; r < 4; r++) {
            for (boolean reverse : new boolean[] {false, true}) {
                List<String> history = operations("YYYY".substring(0, r));
                for (int step = 1; step <= 3; step++) {
                    PlacementTransform.Orientation before = PlacementTransform.orientation(history);
                    assertNotNull(before);
                    history.addAll(operations(before.cycleMirror(reverse)));
                    PlacementTransform.Orientation after = PlacementTransform.orientation(history);
                    assertEquals(r, after.rotation);
                    assertEquals(Math.floorMod(step * (reverse ? -1 : 1), 3), after.mirror);
                }
                assertEquals(0, PlacementTransform.orientation(history).mirror);
            }
        }
    }

    @Test public void rotationAfterMirroringIsNotReconstructedFromUnorderedCounters() {
        assertEquals(1, PlacementTransform.orientation(operations("zY")).mirror);
        assertEquals(2, PlacementTransform.orientation(operations("Yz")).mirror);
        assertEquals(2, PlacementTransform.orientation(operations("xz")).rotation);
        assertEquals(0, PlacementTransform.orientation(operations("xz")).mirror);
        assertNull(PlacementTransform.orientation(operations("X")));
        assertNull(PlacementTransform.orientation(operations("y")));
        assertNotNull(PlacementTransform.orientation(operations("XXXX")));
    }

    @Test public void anchorAndBoundsAgreeForEveryCornerAfterMixedTransforms() {
        for (String history : Arrays.asList("YxzYYz", "XYZxyzXYZ", "XXzzYYY", "ZZXXYY", "xyxy", "")) {
            int[] size = PlacementTransform.transformedSize(2, 3, 5, history);
            int[] anchor = PlacementTransform.anchorOffset(size[0], size[1], size[2], operations(history));
            for (int i = 0; i < 3; i++) assertTrue(anchor[i] == 0 || anchor[i] == size[i] - 1);
            int[] min = {-45 - anchor[0], 68 - anchor[1], 12 - anchor[2]};
            assertArrayEquals(new int[] {-45, 68, 12}, new int[] {min[0] + anchor[0], min[1] + anchor[1], min[2] + anchor[2]});
        }
    }

    @Test(expected = IllegalArgumentException.class) public void invalidHistoryIsRejected() {
        PlacementTransform.anchorOffset(2, 3, 5, Arrays.asList("Y", "invalid"));
    }

    @Test public void reloadWithDifferentDimensionsPreservesWorldOriginAfterRotationAndMirror() {
        assertArrayEquals(new int[] {96, 64, 200}, PlacementTransform.reloadedMinimum(new int[] {100, 64, 200},
            new int[] {5, 3, 2}, new int[] {9, 3, 2}, operations("Y")));
        for (String history : Arrays.asList("YxzYYz", "XYZxyzXYZ", "XXzzYYY", "ZZXXYY", "xyxy", "")) {
            int[] previousSize = PlacementTransform.transformedSize(2, 3, 5, history);
            int[] nextSize = PlacementTransform.transformedSize(7, 11, 9, history);
            int[] oldOffset = PlacementTransform.anchorOffset(previousSize[0], previousSize[1], previousSize[2], operations(history));
            int[] nextOffset = PlacementTransform.anchorOffset(nextSize[0], nextSize[1], nextSize[2], operations(history));
            int[] next = PlacementTransform.reloadedMinimum(new int[] {-120, 75, 32}, previousSize, nextSize, operations(history));
            assertArrayEquals(new int[] {-120 + oldOffset[0], 75 + oldOffset[1], 32 + oldOffset[2]},
                new int[] {next[0] + nextOffset[0], next[1] + nextOffset[1], next[2] + nextOffset[2]});
        }
        assertThrows(ArithmeticException.class, () -> PlacementTransform.reloadedMinimum(new int[] {Integer.MIN_VALUE, 0, 0},
            new int[] {5, 3, 2}, new int[] {9, 3, 2}, operations("Y")));
    }

    @Test public void alignsTheFarEdgeToTheNearestMapBorder() {
        assertEquals(-64, PlacementTransform.mapAlignedStart(-64, 128));
        assertEquals(-64, PlacementTransform.mapAlignedStart(-60, 128));
        assertEquals(64, PlacementTransform.mapAlignedStart(10, 128));
        assertEquals(-65, PlacementTransform.mapAlignedStart(-70, 129));
        assertEquals(-1089, PlacementTransform.mapAlignedStart(-1150, 129));
        assertEquals(60, PlacementTransform.mapAlignedStart(30, 4));
    }
}

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
}

package com.github.lunatrius.schematica.client.renderer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.Test;
import static org.junit.Assert.*;

public class RenderSectionIndexTest {
    private List<Integer> sections(int count) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < count; i++) result.add(i);
        return result;
    }

    @Test public void includesBothSidesOfASectionBoundaryAndItsHalo() {
        RenderSectionIndex<Integer> index = new RenderSectionIndex<>(2, 2, 2, sections(8));
        List<Integer> found = new ArrayList<>();
        index.visit(15, 15, 15, 16, 16, 16, found::add);
        assertEquals(sections(8), found);
        found.clear();
        index.visit(16, 16, 16, 16, 16, 16, found::add);
        assertEquals(Collections.singletonList(7), found);
    }

    @Test public void ignoresDisjointAndInvertedRangesWithoutCoordinateWrapping() {
        RenderSectionIndex<Integer> index = new RenderSectionIndex<>(2, 2, 2, sections(8));
        List<Integer> found = new ArrayList<>();
        index.visit(-30, 0, 0, -1, 20, 20, found::add);
        index.visit(32, 0, 0, 40, 20, 20, found::add);
        index.visit(0, 16, 0, 20, 15, 20, found::add);
        index.visit(4294967295L, 0, 0, 4294967296L, 20, 20, found::add);
        assertTrue(found.isEmpty());
        index.visit(Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE,
            Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, found::add);
        assertEquals(sections(8), found);
    }

    @Test public void keepsCoordinateOrderWhenDrawOrderIsSorted() {
        List<Integer> drawOrder = sections(8);
        RenderSectionIndex<Integer> index = new RenderSectionIndex<>(2, 2, 2, drawOrder);
        Collections.reverse(drawOrder);
        List<Integer> found = new ArrayList<>();
        index.visit(16, 0, 0, 31, 0, 0, found::add);
        assertEquals(Collections.singletonList(1), found);
    }

    @Test public void matchesInclusiveBlockIntersectionAcrossRandomWorldOrigins() {
        RenderSectionIndex<Integer> index = new RenderSectionIndex<>(3, 4, 5, sections(60));
        Random random = new Random(1949);
        for (int sample = 0; sample < 1000; sample++) {
            long[] min = new long[3], max = new long[3];
            int[] sizes = {3, 4, 5};
            for (int axis = 0; axis < 3; axis++) {
                int origin = random.nextInt();
                long worldMin = (long) origin + random.nextInt(140) - 40;
                min[axis] = worldMin - origin;
                max[axis] = worldMin + random.nextInt(40) - origin;
            }
            List<Integer> expected = new ArrayList<>(), actual = new ArrayList<>();
            for (int y = 0; y < sizes[1]; y++) for (int z = 0; z < sizes[2]; z++) for (int x = 0; x < sizes[0]; x++) {
                if (min[0] <= x * 16 + 15 && max[0] >= x * 16 && min[1] <= y * 16 + 15 && max[1] >= y * 16
                    && min[2] <= z * 16 + 15 && max[2] >= z * 16) expected.add((y * sizes[2] + z) * sizes[0] + x);
            }
            index.visit(min[0], min[1], min[2], max[0], max[1], max[2], actual::add);
            assertEquals(Arrays.toString(min), expected, actual);
        }
    }

    @Test public void rejectsInvalidDimensions() {
        assertThrows(IllegalArgumentException.class, () -> new RenderSectionIndex<>(0, 1, 1, sections(1)));
        assertThrows(IllegalArgumentException.class, () -> new RenderSectionIndex<>(2, 2, 2, sections(7)));
    }
}

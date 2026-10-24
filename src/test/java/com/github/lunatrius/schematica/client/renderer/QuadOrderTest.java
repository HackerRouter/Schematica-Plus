package com.github.lunatrius.schematica.client.renderer;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class QuadOrderTest {
    private int[] quads(float... x) {
        int[] vertices = new int[x.length * 32];
        for (int i = 0; i < x.length; i++) for (int v = 0; v < 4; v++) {
            int base = i * 32 + v * 8;
            vertices[base] = Float.floatToIntBits(x[i]);
            for (int attribute = 3; attribute < 8; attribute++) vertices[base + attribute] = i * 100 + v * 10 + attribute;
        }
        return vertices;
    }

    @Test public void sortsBackToFrontAndRetainsEveryVertexAttributeWithoutMutatingInput() {
        int[] vertices = quads(0, 10, 5), original = vertices.clone();
        QuadOrder.Result forward = QuadOrder.sort(vertices, -1, 0, 0);
        assertArrayEquals(new int[] {1, 2, 0}, forward.order);
        assertArrayEquals(new int[] {0, 2, 1}, QuadOrder.sort(vertices, 11, 0, 0).order);
        assertArrayEquals(original, vertices);
        for (int i = 0; i < 3; i++) assertArrayEquals(Arrays.copyOfRange(original, forward.order[i] * 32, (forward.order[i] + 1) * 32),
            Arrays.copyOfRange(forward.vertices, i * 32, (i + 1) * 32));
    }

    @Test public void breaksEqualDistanceTiesByOriginalIndexAndSupportsEmptyGeometry() {
        assertArrayEquals(new int[] {0, 1, 2, 3}, QuadOrder.sort(quads(-1, 1, -1, 1), 0, 0, 0).order);
        assertEquals(0, QuadOrder.sort(new int[0], 0, 0, 0).vertices.length);
        assertThrows(IllegalArgumentException.class, () -> QuadOrder.sort(new int[31], 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> QuadOrder.sort(quads(1), Double.NaN, 0, 0));
    }
}

package com.github.lunatrius.schematica.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class HudAlignmentTest {
    @Test public void rightAndBottomAnchorsStayOnScreenAfterFontScalingAndResize() {
        for (int width : new int[] {320, 854, 1920}) {
            for (double scale : new double[] {0.5, 1, 1.5}) {
                int x = HudAlignment.TOP_RIGHT.x(width, 100, scale, 4);
                double rightEdge = (x + 100 + 2) * scale;
                assertEquals(width - 4 * scale, rightEdge, scale);
                assertEquals(6, HudAlignment.TOP_RIGHT.y(240, 31, scale, 4));
                int y = HudAlignment.BOTTOM_RIGHT.y(240, 31, scale, 4);
                assertEquals(240 - 4 * scale, (y + 31) * scale, scale);
                assertEquals(6, HudAlignment.BOTTOM_LEFT.x(width, 100, scale, 4));
            }
        }
    }

    @Test public void centerUsesTheWholeTextHeightAndPreservesConfiguredOffsets() {
        assertEquals(166, HudAlignment.CENTER.x(854, 100, 2, -3));
        assertEquals(71, HudAlignment.CENTER.y(480, 110, 2, 6));
        assertEquals(HudAlignment.TOP_RIGHT, HudAlignment.parse("unknown"));
        assertEquals(HudAlignment.BOTTOM_LEFT, HudAlignment.parse("BOTTOM_LEFT"));
    }
}

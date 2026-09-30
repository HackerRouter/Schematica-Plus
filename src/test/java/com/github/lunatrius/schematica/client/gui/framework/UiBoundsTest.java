package com.github.lunatrius.schematica.client.gui.framework;

import org.junit.Test;

import static org.junit.Assert.*;

public class UiBoundsTest {

    @Test public void nestedClipsNeverExpandAndUseExclusiveRightAndBottom() {
        UiBounds clip = new UiBounds(10, 10, 20, 20).intersect(new UiBounds(20, 0, 30, 20));
        assertEquals(20, clip.x);
        assertEquals(10, clip.y);
        assertEquals(10, clip.width);
        assertEquals(10, clip.height);
        assertFalse(clip.contains(30, 19));
        assertTrue(clip.intersect(new UiBounds(0, 0, 10, 10)).isEmpty());
    }

    @Test public void scissorUsesScaleFactorAndFramebufferHeightWithRoundedGuiSize() {
        UiBounds clip = new UiBounds(1, 1, 10, 10).toScissor(3, 1001, 701);
        assertEquals(3, clip.x);
        assertEquals(668, clip.y);
        assertEquals(30, clip.width);
        assertEquals(30, clip.height);
        UiBounds full = new UiBounds(0, 0, 334, 234).toScissor(3, 1001, 701);
        assertEquals(0, full.x);
        assertEquals(0, full.y);
        assertEquals(1001, full.width);
        assertEquals(701, full.height);
    }
}

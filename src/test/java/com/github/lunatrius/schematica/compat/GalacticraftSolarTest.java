package com.github.lunatrius.schematica.compat;

import org.junit.Test;

import static org.junit.Assert.*;

public class GalacticraftSolarTest {
    @Test public void panelsSettleTowardTheSun() {
        // celestial 0 is noon: shifted by -0.7845 it is 0.2155 * 360 = 77.6 degrees, between 30 and 150
        assertEquals(77.57F, GalacticraftVisualAdapter.solarAngle(0.0F, true, false), 0.01F);
        assertEquals(257.5F, GalacticraftVisualAdapter.solarAngle(0.5F, false, false), 0);
        assertEquals(77.5F, GalacticraftVisualAdapter.solarAngle(0.3F, true, true), 0);
        assertEquals(257.5F, GalacticraftVisualAdapter.solarAngle(0.0F, false, true), 0);
    }
}

package com.github.lunatrius.schematica.compat;

import org.junit.Test;

import thaumcraft.common.tiles.TileEssentiaCrystalizer;
import thaumcraft.common.tiles.TileEtherealBloom;
import thaumcraft.common.tiles.TileInfusionMatrix;
import thaumcraft.common.tiles.TileTubeValve;

import static org.junit.Assert.*;

public class ThaumcraftVisualAdapterTest {
    private final ThaumcraftVisualAdapter adapter = new ThaumcraftVisualAdapter();

    @Test public void tickedDisplayStateIsSettled() throws Exception {
        TileTubeValve valve = new TileTubeValve();
        valve.allowFlow = false;
        adapter.beforeRender(valve, 0);
        assertEquals(360.0F, valve.rotation, 0);
        valve.allowFlow = true;
        adapter.beforeRender(valve, 0);
        assertEquals(0.0F, valve.rotation, 0);

        TileInfusionMatrix matrix = new TileInfusionMatrix();
        matrix.active = true;
        adapter.beforeRender(matrix, 0);
        assertEquals(1.0F, matrix.startUp, 0);

        TileEssentiaCrystalizer crystallizer = new TileEssentiaCrystalizer();
        crystallizer.aspect = new TileEssentiaCrystalizer.Color(0xDC0000);
        adapter.beforeRender(crystallizer, 0);
        assertEquals(1.0F, crystallizer.cr, 1e-6);
        assertEquals(0.0F, crystallizer.cg, 1e-6);
        crystallizer.aspect = null;
        adapter.beforeRender(crystallizer, 0);
        assertEquals(1.0F, crystallizer.cg, 1e-6);

        TileEtherealBloom bloom = new TileEtherealBloom();
        adapter.beforeRender(bloom, 0);
        assertEquals(100, bloom.growthCounter);
        assertFalse(adapter.supports(new net.minecraft.tileentity.TileEntityChest()));
    }
}

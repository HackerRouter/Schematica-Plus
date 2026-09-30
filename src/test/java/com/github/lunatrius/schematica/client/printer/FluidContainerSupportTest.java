package com.github.lunatrius.schematica.client.printer;

import forestry.core.items.ItemLiquidContainer;
import forestry.core.items.ItemLiquidContainer.Type;
import org.junit.Test;
import static org.junit.Assert.*;

public class FluidContainerSupportTest {
    private static class Flask extends gregtech.common.items.ItemVolumetricFlask {}

    @Test public void onlyAcceptsPlaceableForestryContainers() {
        assertEquals(FluidContainerSupport.Use.FORESTRY_BUCKET,
            FluidContainerSupport.use(new ItemLiquidContainer(Type.BUCKET, false)));
        assertNull(FluidContainerSupport.use(new ItemLiquidContainer(Type.CAN, false)));
        assertNull(FluidContainerSupport.use(new ItemLiquidContainer(Type.CAPSULE, false)));
        assertNull(FluidContainerSupport.use(new ItemLiquidContainer(Type.BUCKET, true)));
        assertNull(FluidContainerSupport.use(new Object()));
    }

    @Test public void routesFlasksAndUniversalCellsThroughBlockUse() {
        assertEquals(FluidContainerSupport.Use.BLOCK, FluidContainerSupport.use(new Flask()));
        assertEquals(FluidContainerSupport.Use.BLOCK, FluidContainerSupport.use(new ic2.core.item.ItemFluidCell()));
        assertTrue(FluidContainerSupport.Use.BLOCK.onBlock);
        assertTrue(FluidContainerSupport.Use.BLOCK.stopOnLiquids);
        assertFalse(FluidContainerSupport.Use.BUCKET.stopOnLiquids);
        assertFalse(FluidContainerSupport.Use.FORESTRY_BUCKET.onBlock);
    }
}

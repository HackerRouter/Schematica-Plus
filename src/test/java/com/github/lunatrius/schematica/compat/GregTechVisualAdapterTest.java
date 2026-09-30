package com.github.lunatrius.schematica.compat;

import org.junit.Test;
import static org.junit.Assert.*;

public class GregTechVisualAdapterTest {
    @Test public void usesClientTurbineFlagsInsteadOfServerStructureState() throws Exception {
        Object turbine = new gregtech.common.tileentities.machines.multi.turbines.MTELargeTurbineBase();
        assertEquals(0, GregTechVisualAdapter.updateData(turbine, false));
        assertEquals(3, GregTechVisualAdapter.updateData(turbine, true));
    }
}

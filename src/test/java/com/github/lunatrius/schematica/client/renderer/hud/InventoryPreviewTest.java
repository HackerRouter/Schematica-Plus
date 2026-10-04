package com.github.lunatrius.schematica.client.renderer.hud;

import net.minecraft.inventory.InventoryBasic;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.tileentity.TileEntityHopper;
import org.junit.Test;
import static org.junit.Assert.*;

public class InventoryPreviewTest {
    @Test public void slotsPerRowFollowTheContainerKind() {
        assertEquals(3, InventoryPreview.perRow(new TileEntityDispenser(), 9));
        assertEquals(5, InventoryPreview.perRow(new TileEntityHopper(), 5));
        assertEquals(9, InventoryPreview.perRow(new InventoryBasic("chest", false, 54), 54));
        assertEquals(3, InventoryPreview.perRow(new InventoryBasic("furnace", false, 3), 3));
        assertNull(InventoryPreview.inventory(null, 0, 0, 0));
    }
}

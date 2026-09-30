package com.github.lunatrius.schematica.compat;

import org.junit.Test;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.ForgeDirection;
import static org.junit.Assert.*;

public class GregTechVisualAdapterTest {
    @Test public void movesCoversWithPipeConnectionsWithoutChangingCoverData() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList covers = new NBTTagList();
        NBTTagCompound cover = new NBTTagCompound();
        cover.setByte("s", (byte) ForgeDirection.NORTH.ordinal());
        cover.setInteger("id", 12345);
        cover.setString("custom", "retained");
        covers.appendTag(cover);
        tag.setTag("gt.covers", covers);
        GregTechVisualAdapter.transformCovers(tag, 'Y');
        assertEquals(ForgeDirection.EAST.ordinal(), cover.getByte("s"));
        GregTechVisualAdapter.transformCovers(tag, 'x');
        assertEquals(ForgeDirection.WEST.ordinal(), cover.getByte("s"));
        assertEquals(12345, cover.getInteger("id"));
        assertEquals("retained", cover.getString("custom"));
    }

    @Test public void usesClientTurbineFlagsInsteadOfServerStructureState() throws Exception {
        Object turbine = new gregtech.common.tileentities.machines.multi.turbines.MTELargeTurbineBase();
        assertEquals(0, GregTechVisualAdapter.updateData(turbine, false));
        assertEquals(3, GregTechVisualAdapter.updateData(turbine, true));
    }
}

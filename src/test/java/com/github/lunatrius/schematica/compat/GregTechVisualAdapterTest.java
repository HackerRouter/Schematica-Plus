package com.github.lunatrius.schematica.compat;

import org.junit.Test;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.ForgeDirection;
import static org.junit.Assert.*;

public class GregTechVisualAdapterTest {
    @Test public void keepsClientConnectionStateAndMetaPipeStateInSync() throws Exception {
        gregtech.api.metatileentity.BaseMetaPipeEntity pipe = new gregtech.api.metatileentity.BaseMetaPipeEntity();
        pipe.mConnections = 40;
        pipe.meta.mConnections = 0;
        GregTechVisualAdapter.transformPipe(pipe, 'Y');
        assertEquals(24, pipe.mConnections);
        assertEquals(24, pipe.meta.mConnections);
        for (int i = 0; i < 3; i++) GregTechVisualAdapter.transformPipe(pipe, 'Y');
        assertEquals(40, pipe.mConnections);
        assertEquals(40, pipe.meta.mConnections);
    }

    @Test public void rotatesSavedPipeInputsWithoutChangingMachineTypeOrInventory() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("mID", 5132);
        tag.setString("Inventory", "retained");
        tag.setByte("mConnections", (byte) 40);
        tag.setByte("mDisableInput", (byte) 4);
        tag.setByte("mLastReceivedFrom", (byte) 8);
        tag.setByte("mStrongRedstone", (byte) 16);
        tag.setByteArray("mRedstoneSided", new byte[] {0, 0, 0, 0, 12, 0});
        NBTTagCompound original = (NBTTagCompound) tag.copy();
        GregTechVisualAdapter.transformPipeNBT(tag, 'Y', true, false);
        assertEquals(24, tag.getByte("mConnections"));
        assertEquals(32, tag.getByte("mDisableInput"));
        assertEquals(16, tag.getByte("mLastReceivedFrom"));
        assertEquals(4, tag.getByte("mStrongRedstone"));
        assertEquals(12, tag.getByteArray("mRedstoneSided")[2]);
        assertEquals(5132, tag.getInteger("mID"));
        assertEquals("retained", tag.getString("Inventory"));
        for (int i = 0; i < 3; i++) GregTechVisualAdapter.transformPipeNBT(tag, 'Y', true, false);
        assertEquals(original, tag);
    }

    @Test public void itemInputIsAnOrdinalRatherThanAConnectionMask() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setByte("mLastReceivedFrom", (byte) 2);
        GregTechVisualAdapter.transformPipeNBT(tag, 'Y', false, true);
        assertEquals(5, tag.getByte("mLastReceivedFrom"));
        tag.setByte("mLastReceivedFrom", (byte) 6);
        GregTechVisualAdapter.transformPipeNBT(tag, 'Y', false, true);
        assertEquals(6, tag.getByte("mLastReceivedFrom"));
    }

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

    @Test public void machinesTurnFrontOutputSidesAndCovers() throws Exception {
        net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
        tag.setShort("mFacing", (short) 2);
        tag.setInteger("mMainFacing", 4);
        tag.setByte("mStrongRedstone", (byte) (1 << 2));
        tag.setByteArray("mItemsPerSide", new byte[] {0, 0, 7, 0, 0, 0});
        tag.setByte("mCurrentSide", (byte) 2);
        GregTechVisualAdapter.transformMachineNBT(tag, 'Y', false);
        assertEquals(5, tag.getShort("mFacing"));
        assertEquals(2, tag.getInteger("mMainFacing"));
        assertEquals(1 << 5, tag.getByte("mStrongRedstone"));
        assertEquals(7, tag.getByteArray("mItemsPerSide")[5]);
        assertEquals(5, tag.getByte("mCurrentSide"));
        GregTechVisualAdapter.transformMachineNBT(tag, 'x', false);
        assertEquals(4, tag.getShort("mFacing"));
        tag.setInteger("mMainFacing", 6);
        GregTechVisualAdapter.transformMachineNBT(tag, 'Y', false);
        assertEquals(6, tag.getInteger("mMainFacing"));
    }
}

package com.github.lunatrius.schematica.nbt;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ClientVisualStateTest {
    private static class Pipe extends gregtech.api.metatileentity.BaseMetaPipeEntity {}

    @Test public void keepsSynchronizedConnectionsAfterMetaPipeOverwritesTheNbtTag() {
        Pipe pipe = new Pipe();
        pipe.mConnections = (byte) 0xa5;
        NBTTagCompound tag = new NBTTagCompound();
        tag.setByte("mConnections", (byte) 0);
        tag.setInteger("mID", 123);
        ClientVisualState.capturePipeConnections(pipe, tag);
        assertEquals(0xa5, tag.getByte("mConnections") & 255);
        assertEquals(123, tag.getInteger("mID"));
    }

    @Test public void leavesUnrelatedModsWithTheSameTagAlone() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setByte("mConnections", (byte) 19);
        ClientVisualState.capturePipeConnections(new Object(), tag);
        assertEquals(19, tag.getByte("mConnections"));
    }
}

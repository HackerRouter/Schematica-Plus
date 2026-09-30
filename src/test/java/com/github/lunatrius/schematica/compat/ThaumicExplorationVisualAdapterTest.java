package com.github.lunatrius.schematica.compat;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;
import static org.junit.Assert.*;

public class ThaumicExplorationVisualAdapterTest {
    public static class Storage {
        public int clientColor = 12, accessTicks = 40, numUsingPlayers = 1;
        public float lidAngle = 0.4f, prevLidAngle = 0.3f;
        int serverReads;
        public int getSealColor() { serverReads++; return 3; }
        public void setColor(int color) { throw new AssertionError("Must not touch shared storage"); }
    }

    @Test public void capturesClientSealWithoutReadingOrUpdatingSharedWorldData() throws Exception {
        Storage source = new Storage();
        NBTTagCompound tag = ThaumicExplorationVisualAdapter.captureState(source, true);
        Storage preview = new Storage();
        preview.clientColor = preview.accessTicks = 0;
        preview.lidAngle = 0;
        ThaumicExplorationVisualAdapter.restoreState(preview, tag);
        assertEquals(12, preview.clientColor);
        assertEquals(40, preview.accessTicks);
        assertEquals(0.4f, preview.lidAngle, 0);
        assertEquals(0, source.serverReads);
        assertEquals(0, preview.serverReads);
        assertEquals(12, source.clientColor);
    }

    @Test public void serverCaptureUsesAuthoritativeSealColor() throws Exception {
        Storage source = new Storage();
        assertEquals(3, ThaumicExplorationVisualAdapter.captureState(source, false).getInteger("SealColor"));
        assertEquals(1, source.serverReads);
        assertEquals(12, source.clientColor);
    }
}

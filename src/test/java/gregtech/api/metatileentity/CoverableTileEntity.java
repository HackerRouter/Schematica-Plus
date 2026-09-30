package gregtech.api.metatileentity;

import java.util.Collections;
import java.util.List;
import net.minecraft.nbt.NBTTagCompound;

public class CoverableTileEntity {
    protected final Object[] covers = new Object[6];
    private byte validCoversMask;
    private byte mStrongRedstone;
    protected final byte[] mSidedRedstone = new byte[6];

    protected void writeCoverNBT(NBTTagCompound tag, boolean drop) {}

    public static List<Object> readCoversNBT(NBTTagCompound tag, CoverableTileEntity tile) {
        return Collections.emptyList();
    }
}

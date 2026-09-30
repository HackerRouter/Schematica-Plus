package com.github.lunatrius.schematica.compat;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import com.github.lunatrius.schematica.api.ISchematicVisualAdapter;
import com.github.lunatrius.schematica.util.SchematicTransform;

final class GregTechVisualAdapter implements ISchematicVisualAdapter {
    @Override public String id() { return "gregtech:custom_data"; }
    @Override public String protocol() { return StreamVisualAdapter.version("gregtech"); }
    @Override public boolean replacesDescriptionPacket(TileEntity tile) { return false; }
    @Override public boolean supports(TileEntity tile) {
        return Reflect.is(tile, "gregtech.api.metatileentity.CommonBaseMetaTileEntity");
    }

    @Override public NBTTagCompound capture(TileEntity tile) throws Exception {
        Object meta = Reflect.call(tile, "getMetaTileEntity");
        if (meta == null) return null;
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Class", meta.getClass().getName());
        tag.setByte("Value", updateData(meta, tile.hasWorldObj() && tile.getWorldObj().isRemote));
        return tag;
    }

    static byte updateData(Object meta, boolean client) throws ReflectiveOperationException {
        byte value = (Byte) Reflect.call(meta, "getUpdateData");
        if (client && (Reflect.is(meta, "gregtech.common.tileentities.machines.multi.turbines.MTELargeTurbineBase")
            || Reflect.is(meta, "gregtech.common.tileentities.machines.multi.MTELargeTurbineLegacy")
            || Reflect.is(meta, "goodgenerator.blocks.tileEntity.base.MTELargeTurbineBaseLegacy"))) {
            value = (byte) (((Boolean) Reflect.get(meta, "mHasTurbine") ? 1 : 0)
                | ((Boolean) Reflect.get(meta, "mFormed") ? 2 : 0));
        } else if (client && Reflect.is(meta, "gregtech.common.tileentities.machines.multi.MTEAirFilterBase")) {
            value = (byte) ((Boolean) Reflect.get(meta, "mFormed") ? 1 : 0);
        }
        return value;
    }

    @Override public void restore(TileEntity tile, NBTTagCompound tag) throws Exception {
        Object meta = Reflect.call(tile, "getMetaTileEntity");
        if (meta != null && tag.getString("Class").equals(meta.getClass().getName()) && tag.hasKey("Value", 1)) {
            Reflect.call(meta, "onValueUpdate", new Class<?>[] {byte.class}, tag.getByte("Value"));
        }
    }

    @Override public void transformPreview(TileEntity tile, char operation) throws Exception {
        if (!Reflect.is(tile, "gregtech.api.metatileentity.BaseMetaPipeEntity")) return;
        transformPipe(tile, operation);
    }

    @Override public boolean transformsNBT(TileEntity tile) {
        return Reflect.is(tile, "gregtech.api.metatileentity.BaseMetaPipeEntity");
    }

    @Override public void transformNBT(TileEntity tile, NBTTagCompound data, char operation) throws Exception {
        Object meta = Reflect.call(tile, "getMetaTileEntity");
        data.setByte("mConnections", (Byte) Reflect.get(tile, "mConnections"));
        transformPipeNBT(data, operation, Reflect.is(meta, "gregtech.api.metatileentity.implementations.MTEFluidPipe"),
            Reflect.is(meta, "gregtech.api.metatileentity.implementations.MTEItemPipe"));
    }

    static void transformPipeNBT(NBTTagCompound tag, char operation, boolean fluid, boolean item) {
        for (String key : new String[] {"mConnections", "mStrongRedstone"}) mask(tag, key, operation);
        if (tag.hasKey("mRedstoneSided", 7) && tag.getByteArray("mRedstoneSided").length == 6) {
            byte[] sides = tag.getByteArray("mRedstoneSided").clone();
            SchematicTransform.sides(operation, sides);
            tag.setByteArray("mRedstoneSided", sides);
        }
        if (fluid) {
            mask(tag, "mDisableInput", operation);
            mask(tag, "mLastReceivedFrom", operation);
        } else if (item && tag.hasKey("mLastReceivedFrom", 1)) {
            tag.setByte("mLastReceivedFrom", (byte) SchematicTransform.direction(operation,
                ForgeDirection.getOrientation(tag.getByte("mLastReceivedFrom"))).ordinal());
        }
        transformCovers(tag, operation);
    }

    static void transformPipe(Object tile, char operation) throws Exception {
        Object meta = Reflect.call(tile, "getMetaTileEntity");
        mask(tile, "mConnections", operation);
        mask(tile, "mStrongRedstone", operation);
        SchematicTransform.sides(operation, Reflect.get(tile, "mSidedRedstone"));
        if (meta != null) {
            Reflect.field(meta.getClass(), "mConnections").setByte(meta, (Byte) Reflect.get(tile, "mConnections"));
            if (Reflect.is(meta, "gregtech.api.metatileentity.implementations.MTEFluidPipe")) {
                mask(meta, "mDisableInput", operation);
                mask(meta, "mLastReceivedFrom", operation);
            } else if (Reflect.is(meta, "gregtech.api.metatileentity.implementations.MTEItemPipe")) {
                java.lang.reflect.Field field = Reflect.field(meta.getClass(), "mLastReceivedFrom");
                field.set(meta, SchematicTransform.direction(operation, (ForgeDirection) field.get(meta)));
            }
        }
        Object[] covers = (Object[]) Reflect.get(tile, "covers");
        Object[] rotatedCovers = covers.clone();
        Class<?> coverable = Reflect.type(tile.getClass(), "gregtech.api.metatileentity.CoverableTileEntity");
        java.lang.reflect.Method write = coverable.getDeclaredMethod("writeCoverNBT", NBTTagCompound.class, boolean.class);
        write.setAccessible(true);
        NBTTagCompound coverTag = new NBTTagCompound();
        write.invoke(tile, coverTag, false);
        transformCovers(coverTag, operation);
        SchematicTransform.sides(operation, rotatedCovers);
        for (Object cover : (Iterable<?>) coverable.getMethod("readCoversNBT", NBTTagCompound.class, coverable).invoke(null, coverTag, tile)) {
            ForgeDirection side = (ForgeDirection) Reflect.call(cover, "getSide");
            if (side != ForgeDirection.UNKNOWN) rotatedCovers[side.ordinal()] = cover;
            else throw new IllegalArgumentException("Could not reconstruct transformed GT cover");
        }
        System.arraycopy(rotatedCovers, 0, covers, 0, 6);
        mask(tile, "validCoversMask", operation);
    }

    static void transformCovers(NBTTagCompound tag, char operation) {
        NBTTagList covers = tag.getTagList("gt.covers", 10);
        for (int i = 0; i < covers.tagCount(); i++) {
            NBTTagCompound cover = covers.getCompoundTagAt(i);
            ForgeDirection side = ForgeDirection.getOrientation(cover.getByte("s"));
            cover.setByte("s", (byte) SchematicTransform.direction(operation, side).ordinal());
        }
    }

    private static void mask(Object target, String name, char operation) throws ReflectiveOperationException {
        java.lang.reflect.Field field = Reflect.field(target.getClass(), name);
        field.setByte(target, (byte) SchematicTransform.sideMask(operation, field.getByte(target)));
    }

    private static void mask(NBTTagCompound tag, String name, char operation) {
        if (tag.hasKey(name, 1)) tag.setByte(name, (byte) SchematicTransform.sideMask(operation, tag.getByte(name)));
    }
}

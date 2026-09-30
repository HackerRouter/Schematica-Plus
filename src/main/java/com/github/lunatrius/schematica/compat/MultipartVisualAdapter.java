package com.github.lunatrius.schematica.compat;

import java.util.List;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import com.github.lunatrius.schematica.api.ISchematicVisualAdapter;
import com.github.lunatrius.schematica.reference.Reference;

final class MultipartVisualAdapter implements ISchematicVisualAdapter {
    @Override public String id() { return "forgemultipart:parts"; }
    @Override public String protocol() { return StreamVisualAdapter.version("ForgeMultipart"); }
    @Override public boolean supports(TileEntity tile) { return Reflect.is(tile, "codechicken.multipart.TileMultipart"); }

    @Override public NBTTagCompound capture(TileEntity tile) throws Exception {
        NBTTagList list = new NBTTagList();
        for (Object part : (List<?>) Reflect.call(tile, "jPartList")) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("Type", (String) Reflect.call(part, "getType"));
            tag.setString("Class", part.getClass().getName());
            try {
                if (Reflect.is(part, "appeng.api.parts.IPartHost") && tile.getWorldObj().isRemote) {
                    tag.setTag("AE2", AE2VisualAdapter.captureObject(part));
                } else {
                    tag.setString("Protocol", VisualAdapters.environmentProtocol());
                    tag.setByteArray("Data", VisualStreams.writeMultipart(part));
                }
            } catch (Exception | LinkageError e) {
                Reference.logger.warn("Could not capture multipart {}", tag.getString("Type"), e);
            }
            list.appendTag(tag);
        }
        NBTTagCompound data = new NBTTagCompound();
        data.setTag("Parts", list);
        return data;
    }

    @Override public void restore(TileEntity tile, NBTTagCompound data) throws Exception {
        List<?> parts = (List<?>) Reflect.call(tile, "jPartList");
        NBTTagList list = data.getTagList("Parts", 10);
        if (parts.size() != list.tagCount()) return;
        for (int i = 0; i < parts.size(); i++) {
            Object part = parts.get(i);
            NBTTagCompound tag = list.getCompoundTagAt(i);
            if (!tag.getString("Type").equals(Reflect.call(part, "getType"))
                || !tag.getString("Class").equals(part.getClass().getName())) continue;
            try {
                if (tag.hasKey("AE2", 10)) AE2VisualAdapter.restoreObject(part, tag.getCompoundTag("AE2"));
                else if (tag.hasKey("Data", 7) && tag.getString("Protocol").equals(VisualAdapters.environmentProtocol())) {
                    VisualStreams.readMultipart(part, tag.getByteArray("Data"));
                }
            } catch (Exception | LinkageError e) {
                Reference.logger.warn("Could not restore multipart {}", tag.getString("Type"), e);
            }
        }
        Reflect.call(tile, "updateRenderCache");
    }
}

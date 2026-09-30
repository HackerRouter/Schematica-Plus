package com.github.lunatrius.schematica.compat;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import com.github.lunatrius.schematica.api.ISchematicVisualAdapter;

final class StreamVisualAdapter implements ISchematicVisualAdapter {
    private final String id, baseClass, modId, write, read, output, input;

    StreamVisualAdapter(String id, String baseClass, String modId, String write, String read,
        String output, String input) {
        this.id = id;
        this.baseClass = baseClass;
        this.modId = modId;
        this.write = write;
        this.read = read;
        this.output = output;
        this.input = input;
    }

    @Override public String id() { return id; }
    @Override public boolean supports(TileEntity tile) { return Reflect.is(tile, baseClass); }
    @Override public String protocol() { return version(modId); }

    static String version(String modId) {
        ModContainer mod = Loader.instance().getIndexedModList().get(modId);
        return mod == null ? "unavailable" : mod.getVersion();
    }

    @Override public NBTTagCompound capture(TileEntity tile) throws Exception {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setByteArray("Data", output == null ? VisualStreams.writeBuffer(tile, write)
            : VisualStreams.writeStream(tile, write, output));
        return tag;
    }

    @Override public void restore(TileEntity tile, NBTTagCompound tag) throws Exception {
        if (!tag.hasKey("Data", 7)) throw new IllegalArgumentException("Missing visual stream");
        if (input == null) VisualStreams.readBuffer(tile, read, tag.getByteArray("Data"));
        else VisualStreams.readStream(tile, read, input, tag.getByteArray("Data"));
    }
}

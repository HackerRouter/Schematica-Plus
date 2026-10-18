package com.github.lunatrius.schematica.network.transfer;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;

public final class NbtPayload {
    private NbtPayload() {}

    public static NBTTagCompound read(byte[] compressed, long maxBytes) throws IOException {
        try (InputStream input = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            return CompressedStreamTools.func_152456_a(new DataInputStream(input), new NBTSizeTracker(maxBytes * 8));
        }
    }
}

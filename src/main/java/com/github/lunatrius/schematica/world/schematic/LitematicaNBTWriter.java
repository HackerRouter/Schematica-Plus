// SPDX-License-Identifier: LGPL-3.0-only
// Gzip NBT output with TAG_Long_Array support for .litematic files, by HackerRouter, 2026.
package com.github.lunatrius.schematica.world.schematic;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Writes a compound like CompressedStreamTools.writeCompressed, adding the long arrays that 1.7.10 NBT cannot hold
 * (type 12) from a side table keyed by their parent compound, as LitematicaNBTReader stores them.
 */
public final class LitematicaNBTWriter {
    private LitematicaNBTWriter() {}

    public static void writeCompressed(NBTTagCompound root, Map<NBTTagCompound, Map<String, long[]>> longArrays,
        OutputStream output) throws IOException {
        GZIPOutputStream gzip = new GZIPOutputStream(output);
        DataOutputStream data = new DataOutputStream(gzip);
        data.writeByte(10);
        data.writeUTF("");
        compound(root, longArrays, data);
        data.flush();
        gzip.finish();
    }

    private static void compound(NBTTagCompound tag, Map<NBTTagCompound, Map<String, long[]>> longArrays,
        DataOutputStream output) throws IOException {
        for (Object key : tag.func_150296_c()) {
            String name = (String) key;
            NBTBase child = tag.getTag(name);
            if (child instanceof NBTTagCompound) {
                output.writeByte(10);
                output.writeUTF(name);
                compound((NBTTagCompound) child, longArrays, output);
            } else {
                vanilla(name, child, output);
            }
        }
        for (Map.Entry<String, long[]> entry : longArrays.getOrDefault(tag, Collections.emptyMap()).entrySet()) {
            if (tag.hasKey(entry.getKey())) continue;
            output.writeByte(NBTTagLongArray.TAG_LONG_ARRAY);
            output.writeUTF(entry.getKey());
            output.writeInt(entry.getValue().length);
            for (long value : entry.getValue()) output.writeLong(value);
        }
        output.writeByte(0);
    }

    /** Lets vanilla encode one named child: the entry bytes inside a single-entry root compound. */
    private static void vanilla(String name, NBTBase child, DataOutputStream output) throws IOException {
        NBTTagCompound wrapper = new NBTTagCompound();
        wrapper.setTag(name, child);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CompressedStreamTools.write(wrapper, new DataOutputStream(bytes));
        byte[] encoded = bytes.toByteArray();
        output.write(encoded, 3, encoded.length - 4);
    }
}

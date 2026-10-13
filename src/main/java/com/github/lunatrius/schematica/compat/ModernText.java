// Vanilla text component import, by HackerRouter, 2026.
package com.github.lunatrius.schematica.compat;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants.NBT;

/** Plain text for the names, signs and book pages understood by 1.7.10. */
public final class ModernText {
    public static final int NBT_COMPONENT_VERSION = 4325;

    private ModernText() {}

    public static String plain(NBTBase value, int dataVersion) {
        return plain(value, dataVersion, 0);
    }

    private static String plain(NBTBase value, int dataVersion, int depth) {
        if (value == null || depth > 64) return "";
        if (value instanceof NBTTagString) {
            String text = ((NBTTagString) value).func_150285_a_();
            return dataVersion >= NBT_COMPONENT_VERSION ? text : SignTextConverter.convertJsonTextTo1710(text);
        }
        StringBuilder text = new StringBuilder();
        if (value instanceof NBTTagCompound) {
            NBTTagCompound tag = (NBTTagCompound) value;
            if (tag.hasKey("")) return plain(tag.getTag(""), dataVersion, depth + 1);
            if (tag.hasKey("text", NBT.TAG_STRING)) text.append(tag.getString("text"));
            else if (tag.hasKey("fallback", NBT.TAG_STRING)) text.append(tag.getString("fallback"));
            else if (tag.hasKey("translate", NBT.TAG_STRING)) text.append(tag.getString("translate"));
            text.append(plain(tag.getTag("extra"), NBT_COMPONENT_VERSION, depth + 1));
        } else if (value instanceof NBTTagList) {
            NBTTagList list = (NBTTagList) value;
            for (int i = 0; i < list.tagCount(); i++) text.append(plain(element(list, i), dataVersion, depth + 1));
        }
        return text.toString();
    }

    public static NBTBase element(NBTTagList list, int index) {
        if (list.func_150303_d() == NBT.TAG_STRING) return new NBTTagString(list.getStringTagAt(index));
        if (list.func_150303_d() == NBT.TAG_COMPOUND) return list.getCompoundTagAt(index);
        return null;
    }

    public static void name(NBTTagCompound tag, int dataVersion) {
        NBTBase name = tag.hasKey("custom_name") ? tag.getTag("custom_name") : tag.getTag("CustomName");
        if (name != null) tag.setString("CustomName", plain(name, dataVersion));
        tag.removeTag("custom_name");
    }
}

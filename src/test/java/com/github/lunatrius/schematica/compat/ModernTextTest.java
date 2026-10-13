package com.github.lunatrius.schematica.compat;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import org.junit.Test;
import static org.junit.Assert.*;

public class ModernTextTest {
    @Test public void dataVersionDistinguishesJsonFromLiteralNbtStrings() {
        NBTTagString value = new NBTTagString("{\"text\":\"literal\"}");
        assertEquals("literal", ModernText.plain(value, 3955));
        assertEquals("{\"text\":\"literal\"}", ModernText.plain(value, 4325));
    }
    @Test public void compoundTextSupportsMixedListWrappersAndExtras() {
        NBTTagCompound value = new NBTTagCompound(), extra = new NBTTagCompound(), wrapped = new NBTTagCompound();
        value.setString("text", "中文"); extra.setString("text", " text"); wrapped.setString("", "!");
        NBTTagList list = new NBTTagList(); list.appendTag(extra); list.appendTag(wrapped); value.setTag("extra", list);
        assertEquals("中文 text!", ModernText.plain(value, 4325));
    }
    @Test public void modernSignFrontTextAndNamesBecomePlainStrings() {
        NBTTagCompound sign = new NBTTagCompound(), front = new NBTTagCompound(), line = new NBTTagCompound();
        line.setString("text", "First line"); NBTTagList messages = new NBTTagList(); messages.appendTag(line); front.setTag("messages", messages);
        sign.setTag("front_text", front); SignTextConverter.convertSign(sign, 4325);
        assertEquals("First line", sign.getString("Text1")); assertEquals("", sign.getString("Text4")); assertFalse(sign.hasKey("front_text"));
        NBTTagCompound chest = new NBTTagCompound(); chest.setTag("CustomName", line); ModernText.name(chest, 4325); assertEquals("First line", chest.getString("CustomName"));
    }
}

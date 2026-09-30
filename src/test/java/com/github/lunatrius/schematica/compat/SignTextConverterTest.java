package com.github.lunatrius.schematica.compat;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class SignTextConverterTest {
    @Test public void convertsJsonAndLeavesLegacyTextIntact() {
        assertEquals("Hello world", SignTextConverter.convertJsonTextTo1710(
            "{\"text\":\"Hello\",\"extra\":[{\"text\":\" world\",\"color\":\"#ffffff\"}]}"));
        assertEquals("中文", SignTextConverter.convertJsonTextTo1710("\"中文\""));
        assertEquals("Plain text", SignTextConverter.convertJsonTextTo1710("Plain text"));
        assertEquals("AB", SignTextConverter.convertJsonTextTo1710("[{\"text\":\"A\"},{\"text\":\"B\"}]"));
    }

    @Test public void writesPlainLimitedLinesAndEmptyDefaults() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Text1", "{\"text\":\"12345678901234567890\"}");
        SignTextConverter.convertSign(tag);
        assertEquals("123456789012345", tag.getString("Text1"));
        assertEquals("", tag.getString("Text2"));
    }
}

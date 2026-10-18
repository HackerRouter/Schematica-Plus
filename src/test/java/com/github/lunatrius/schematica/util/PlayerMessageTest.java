package com.github.lunatrius.schematica.util;

import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class PlayerMessageTest {
    @Test public void chatKeepsKeyColorAndNestedTranslations() {
        PlayerMessage detail = new PlayerMessage(MessageType.ERROR, "schematica.message.edit.entity_failed", "Pig");
        PlayerMessage message = new PlayerMessage(MessageType.WARNING, "schematica.message.edit.paste_errors", 1, 2, detail);
        ChatComponentTranslation chat = (ChatComponentTranslation) message.chat();
        assertEquals("schematica.message.edit.paste_errors", chat.getKey());
        assertEquals(EnumChatFormatting.GOLD, chat.getChatStyle().getColor());
        ChatComponentTranslation nested = (ChatComponentTranslation) chat.getFormatArgs()[2];
        assertEquals("schematica.message.edit.entity_failed", nested.getKey());
        assertArrayEquals(new Object[] {"Pig"}, nested.getFormatArgs());
        assertArrayEquals(new Object[] {1, 2, detail}, message.arguments());
    }
}

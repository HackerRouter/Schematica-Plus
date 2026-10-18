// SPDX-License-Identifier: LGPL-3.0-only
// MaLiLib Message.MessageType, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.util;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

public enum MessageType {
    INFO("malilib.message.formatting_code.info", EnumChatFormatting.WHITE),
    SUCCESS("malilib.message.formatting_code.success", EnumChatFormatting.GREEN),
    WARNING("malilib.message.formatting_code.warning", EnumChatFormatting.GOLD),
    ERROR("malilib.message.formatting_code.error", EnumChatFormatting.RED);

    private final String translationKey;
    private final EnumChatFormatting chatColor;

    MessageType(String translationKey, EnumChatFormatting chatColor) {
        this.translationKey = translationKey;
        this.chatColor = chatColor;
    }

    public String formatting() {
        return StatCollector.translateToLocal(translationKey);
    }

    public EnumChatFormatting chatColor() {
        return chatColor;
    }
}

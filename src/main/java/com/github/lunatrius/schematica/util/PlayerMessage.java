package com.github.lunatrius.schematica.util;

import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;

public final class PlayerMessage {
    public final MessageType type;
    public final String key;
    private final Object[] arguments;

    public PlayerMessage(MessageType type, String key, Object... arguments) {
        this.type = type;
        this.key = key;
        this.arguments = arguments.clone();
    }

    public Object[] arguments() {
        return arguments.clone();
    }

    public IChatComponent chat() {
        ChatComponentTranslation text = new ChatComponentTranslation(key, chatArguments());
        text.getChatStyle().setColor(type.chatColor());
        return text;
    }

    private Object[] chatArguments() {
        Object[] result = arguments.clone();
        for (int i = 0; i < result.length; i++) {
            if (result[i] instanceof PlayerMessage) {
                PlayerMessage nested = (PlayerMessage) result[i];
                result[i] = new ChatComponentTranslation(nested.key, nested.chatArguments());
            }
        }
        return result;
    }
}

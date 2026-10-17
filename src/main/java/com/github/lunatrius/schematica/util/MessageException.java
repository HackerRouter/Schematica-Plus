package com.github.lunatrius.schematica.util;

import java.util.Arrays;

public final class MessageException extends IllegalArgumentException {
    private final String key;
    private final Object[] arguments;

    public MessageException(String key, Object... arguments) {
        super(key + " " + Arrays.toString(arguments));
        this.key = key;
        this.arguments = arguments.clone();
    }

    public String key() { return key; }
    public Object[] arguments() { return arguments.clone(); }
}

package com.github.lunatrius.schematica.util;

import java.util.Locale;
import java.util.regex.Pattern;

public final class ColorValue {
    public static final Pattern FORMAT = Pattern.compile("(?:#|0[xX])?(?:[0-9a-fA-F]{6}|[0-9a-fA-F]{8})");

    private ColorValue() {}

    public static int parse(String value) {
        if (value == null || !FORMAT.matcher(value).matches()) throw new IllegalArgumentException("Expected RRGGBB or AARRGGBB");
        String hex = value.startsWith("#") ? value.substring(1)
            : value.startsWith("0x") || value.startsWith("0X") ? value.substring(2) : value;
        return (int) Long.parseLong(hex, 16) | (hex.length() == 6 ? 0xFF000000 : 0);
    }

    public static String format(int color) { return String.format(Locale.ROOT, "#%08X", color); }
    public static float red(int color) { return ((color >>> 16) & 255) / 255f; }
    public static float green(int color) { return ((color >>> 8) & 255) / 255f; }
    public static float blue(int color) { return (color & 255) / 255f; }
    public static float alpha(int color) { return (color >>> 24) / 255f; }
}

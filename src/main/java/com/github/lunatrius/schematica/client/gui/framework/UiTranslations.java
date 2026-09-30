package com.github.lunatrius.schematica.client.gui.framework;

import java.util.IllegalFormatException;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.StatCollector;

public final class UiTranslations {

    private UiTranslations() {}

    public static String format(String key, Object... arguments) {
        if (!key.startsWith("litematica.") && !key.startsWith("malilib.")) return I18n.format(key, arguments);
        return formatTemplate(StatCollector.translateToLocal(key), arguments);
    }

    static String formatTemplate(String template, Object... arguments) {
        StringBuilder decoded = new StringBuilder(template.length());
        for (int i = 0; i < template.length(); i++) {
            char c = template.charAt(i);
            if (c == '\\' && i + 1 < template.length()) {
                char next = template.charAt(i + 1);
                if (next == 'n' || next == 'r' || next == 't' || next == '\\') {
                    c = next == 'n' ? '\n' : next == 'r' ? '\r' : next == 't' ? '\t' : '\\';
                    i++;
                }
            }
            decoded.append(c);
        }
        String text = decoded.toString();
        if (arguments.length == 0) return text;
        try {
            return String.format(text, arguments);
        } catch (IllegalFormatException e) {
            return "Format error: " + text;
        }
    }
}

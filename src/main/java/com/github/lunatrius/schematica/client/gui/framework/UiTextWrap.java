// Unicode dialog line wrapping, by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.framework;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.ToIntFunction;

public final class UiTextWrap {
    private UiTextWrap() {}

    public static List<String> wrap(String text, int width, ToIntFunction<String> measure) {
        List<String> lines = new ArrayList<>();
        BreakIterator breaks = BreakIterator.getLineInstance(Locale.ROOT);
        String format = "";
        for (String paragraph : text.split("\n", -1)) {
            breaks.setText(paragraph);
            int start = 0;
            do {
                int fit = start;
                int lastBreak = start;
                for (int end = start; end < paragraph.length();) {
                    if (paragraph.charAt(end) == '\u00a7' && end + 1 < paragraph.length()) {
                        end += 2;
                        if (end == paragraph.length()) fit = end;
                        continue;
                    }
                    end += Character.charCount(paragraph.codePointAt(end));
                    String candidate = display(format + paragraph.substring(start, end));
                    if (measure.applyAsInt(candidate) > Math.max(1, width) && fit > start) break;
                    fit = end;
                    if (breaks.isBoundary(end)) lastBreak = end;
                }
                if (fit == start) fit = paragraph.length();
                int end = fit == paragraph.length() || lastBreak == start ? fit : lastBreak;
                String line = format + paragraph.substring(start, end);
                lines.add(display(trimEnd(line)));
                format = formatting(line);
                start = end;
                while (start < paragraph.length() && paragraph.charAt(start) == ' ') start++;
            } while (start < paragraph.length());
        }
        return lines;
    }

    private static String display(String text) { return text.replace('\u00a0', ' '); }

    private static String trimEnd(String text) {
        int end = text.length();
        while (end > 0 && text.charAt(end - 1) == ' ') end--;
        return text.substring(0, end);
    }

    private static String formatting(String text) {
        StringBuilder format = new StringBuilder();
        for (int i = 0; i + 1 < text.length(); i++) {
            if (text.charAt(i) != '\u00a7') continue;
            char code = Character.toLowerCase(text.charAt(++i));
            if ("0123456789abcdefr".indexOf(code) >= 0) format.setLength(0);
            if ("0123456789abcdefklmno".indexOf(code) >= 0) format.append('\u00a7').append(code);
        }
        return format.toString();
    }
}

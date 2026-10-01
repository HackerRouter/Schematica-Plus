// SPDX-License-Identifier: LGPL-3.0-only
// Vanilla MultiLineEditBox/MultilineTextField editing as used by MaLiLib GuiTextFieldMultiLine, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.framework;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/** Text, cursor and selection of a multi-line editor, with soft word wrap at a pixel width. */
public final class TextAreaModel {

    /** One wrapped display line: [start, end) in the text, excluding a trailing line break. */
    public static final class Line {
        public final int start, end;
        Line(int start, int end) { this.start = start; this.end = end; }
    }

    private final int maxLength, maxLines;
    private String text = "";
    private int cursor, anchor;
    private List<Line> lines;
    private int wrapWidth = -1;
    private ToIntFunction<String> measure = String::length;

    /** maxLines limits line breaks (lines separated by Enter); 0 allows any number. */
    public TextAreaModel(int maxLength, int maxLines) {
        this.maxLength = maxLength;
        this.maxLines = maxLines;
    }

    private boolean tooManyLines(String value) {
        if (maxLines <= 0) return false;
        int count = 1;
        for (int i = 0; i < value.length(); i++) if (value.charAt(i) == '\n' && ++count > maxLines) return true;
        return false;
    }

    public String text() { return text; }
    public int cursor() { return cursor; }
    public int anchor() { return anchor; }
    public boolean hasSelection() { return cursor != anchor; }
    public int selectionStart() { return Math.min(cursor, anchor); }
    public int selectionEnd() { return Math.max(cursor, anchor); }
    public String selectedText() { return text.substring(selectionStart(), selectionEnd()); }

    public void setText(String value) {
        text = clean(value == null ? "" : value);
        if (text.length() > maxLength) text = text.substring(0, maxLength);
        while (tooManyLines(text)) text = text.substring(0, text.lastIndexOf('\n'));
        cursor = anchor = text.length();
        lines = null;
    }

    /** Sets the width used for wrapping, measured with the given text width function. */
    public void setWrap(int width, ToIntFunction<String> measure) {
        if (width == wrapWidth && measure == this.measure) return;
        wrapWidth = width;
        this.measure = measure;
        lines = null;
    }

    private static String clean(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\r') continue;
            if (c == '\n' || c >= ' ' && c != 127 && c != '\u00a7') result.append(c);
        }
        return result.toString();
    }

    public List<Line> lines() {
        if (lines == null) lines = wrap();
        return lines;
    }

    private List<Line> wrap() {
        List<Line> result = new ArrayList<>();
        int start = 0;
        while (true) {
            int newline = text.indexOf('\n', start);
            int end = newline < 0 ? text.length() : newline;
            wrapParagraph(start, end, result);
            if (newline < 0) break;
            start = newline + 1;
        }
        return result;
    }

    private void wrapParagraph(int start, int end, List<Line> result) {
        if (wrapWidth <= 0 || start == end) {
            result.add(new Line(start, end));
            return;
        }
        int lineStart = start;
        while (lineStart < end) {
            int fit = lineStart;
            while (fit < end && measure.applyAsInt(text.substring(lineStart, fit + 1)) <= wrapWidth) fit++;
            if (fit == end) {
                result.add(new Line(lineStart, end));
                return;
            }
            if (fit == lineStart) fit = lineStart + 1;
            int space = text.lastIndexOf(' ', fit);
            int lineEnd = space > lineStart && space < fit ? space + 1 : fit;
            result.add(new Line(lineStart, lineEnd));
            lineStart = lineEnd;
        }
    }

    /** The display line holding a text position; a position at a soft wrap belongs to the next line. */
    public int lineOf(int position) {
        List<Line> all = lines();
        for (int i = 0; i < all.size(); i++) {
            Line line = all.get(i);
            boolean soft = i + 1 < all.size() && all.get(i + 1).start == line.end;
            if (position >= line.start && (position < line.end || position == line.end && !soft)) return i;
        }
        return all.size() - 1;
    }

    /** The text position on a display line closest to a pixel offset. */
    public int positionAt(int lineIndex, int pixel) {
        List<Line> all = lines();
        Line line = all.get(Math.max(0, Math.min(all.size() - 1, lineIndex)));
        int best = line.start;
        for (int position = line.start; position <= line.end; position++) {
            int width = measure.applyAsInt(text.substring(line.start, position));
            if (width > pixel) {
                int previous = measure.applyAsInt(text.substring(line.start, position - 1));
                return pixel - previous < width - pixel ? position - 1 : position;
            }
            best = position;
        }
        boolean soft = lineIndex + 1 < all.size() && all.get(lineIndex + 1).start == line.end;
        return soft && best == line.end && best > line.start ? best - 1 : best;
    }

    public int pixelOf(int position) {
        Line line = lines().get(lineOf(position));
        return measure.applyAsInt(text.substring(line.start, Math.max(line.start, Math.min(position, line.end))));
    }

    public void moveTo(int position, boolean select) {
        cursor = Math.max(0, Math.min(text.length(), position));
        if (!select) anchor = cursor;
    }

    public void selectAll() {
        anchor = 0;
        cursor = text.length();
    }

    /** Replaces the selection with text, keeping within the length limit; returns whether the text changed. */
    public boolean insert(String value) {
        String clean = clean(value);
        int start = selectionStart(), end = selectionEnd();
        int room = maxLength - (text.length() - (end - start));
        if (room < clean.length()) clean = clean.substring(0, Math.max(0, room));
        if (clean.isEmpty() && start == end) return false;
        String next = text.substring(0, start) + clean + text.substring(end);
        if (tooManyLines(next)) return false;
        text = next;
        cursor = anchor = start + clean.length();
        lines = null;
        return true;
    }

    /** Backspace (direction -1) or Delete (1), by character or by word. */
    public boolean erase(int direction, boolean word) {
        if (hasSelection()) return insert("");
        int target = word ? wordFrom(cursor, direction) : cursor + direction;
        target = Math.max(0, Math.min(text.length(), target));
        if (target == cursor) return false;
        anchor = target;
        return insert("");
    }

    public int wordFrom(int position, int direction) {
        int i = position;
        if (direction < 0) {
            while (i > 0 && Character.isWhitespace(text.charAt(i - 1))) i--;
            while (i > 0 && !Character.isWhitespace(text.charAt(i - 1))) i--;
        } else {
            while (i < text.length() && !Character.isWhitespace(text.charAt(i))) i++;
            while (i < text.length() && Character.isWhitespace(text.charAt(i))) i++;
        }
        return i;
    }

    /** Moves the cursor up or down a display line, keeping its pixel column. */
    public void moveLines(int amount, boolean select) {
        int line = lineOf(cursor);
        int target = line + amount;
        if (target < 0) moveTo(0, select);
        else if (target >= lines().size()) moveTo(text.length(), select);
        else moveTo(positionAt(target, pixelOf(cursor)), select);
    }

    public void lineStart(boolean select) { moveTo(lines().get(lineOf(cursor)).start, select); }

    public void lineEnd(boolean select) {
        int index = lineOf(cursor);
        Line line = lines().get(index);
        boolean soft = index + 1 < lines().size() && lines().get(index + 1).start == line.end;
        moveTo(soft && line.end > line.start ? line.end - 1 : line.end, select);
    }
}

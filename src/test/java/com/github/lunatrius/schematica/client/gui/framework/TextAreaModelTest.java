package com.github.lunatrius.schematica.client.gui.framework;

import org.junit.Test;
import static org.junit.Assert.*;

public class TextAreaModelTest {
    private static TextAreaModel model(String text, int width) {
        TextAreaModel model = new TextAreaModel(512, 8);
        model.setWrap(width, String::length);
        model.setText(text);
        return model;
    }

    @Test public void wrapsAtSpacesAndKeepsHardBreaks() {
        TextAreaModel model = model("one two three\nfour", 8);
        assertEquals(3, model.lines().size());
        assertEquals("one two ", model.text().substring(model.lines().get(0).start, model.lines().get(0).end));
        assertEquals("three", model.text().substring(model.lines().get(1).start, model.lines().get(1).end));
        assertEquals("four", model.text().substring(model.lines().get(2).start, model.lines().get(2).end));
        assertEquals(1, model.lineOf(13));
        assertEquals(2, model.lineOf(14));
    }

    @Test public void longWordsBreakAnywhere() {
        TextAreaModel model = model("abcdefghij", 4);
        assertEquals(3, model.lines().size());
        assertEquals(4, model.lines().get(1).start);
    }

    @Test public void editingReplacesTheSelectionAndRespectsLimits() {
        TextAreaModel model = new TextAreaModel(10, 2);
        model.setText("hello");
        model.moveTo(0, false);
        model.moveTo(5, true);
        assertEquals("hello", model.selectedText());
        assertTrue(model.insert("hey"));
        assertEquals("hey", model.text());
        assertTrue(model.insert("\n"));
        assertFalse(model.insert("\n"));
        assertTrue(model.insert("0123456789"));
        assertEquals(10, model.text().length());
        model.setText("a§b\rc");
        assertEquals("abc", model.text());
    }

    @Test public void erasesCharactersAndWords() {
        TextAreaModel model = model("alpha beta", 0);
        assertTrue(model.erase(-1, true));
        assertEquals("alpha ", model.text());
        model.moveTo(0, false);
        assertTrue(model.erase(1, false));
        assertEquals("lpha ", model.text());
        assertFalse(new TextAreaModel(5, 0).erase(-1, false));
    }

    @Test public void cursorMovesBetweenDisplayLinesKeepingTheColumn() {
        TextAreaModel model = model("abc\nabcdef\nxy", 0);
        model.moveTo(2, false);
        model.moveLines(1, false);
        assertEquals(6, model.cursor());
        model.moveLines(1, false);
        assertEquals(13, model.cursor());
        model.moveLines(1, true);
        assertEquals(13, model.cursor());
        model.lineStart(false);
        assertEquals(11, model.cursor());
        model.moveLines(-1, false);
        assertEquals(4, model.cursor());
        model.lineEnd(false);
        assertEquals(10, model.cursor());
        assertEquals(1, model.lineOf(10));
    }

    @Test public void softWrapPositionsBelongToTheNextLine() {
        TextAreaModel model = model("aaaa bbbb", 5);
        assertEquals(2, model.lines().size());
        assertEquals(5, model.lines().get(1).start);
        assertEquals(1, model.lineOf(5));
        model.moveTo(5, false);
        model.lineStart(false);
        assertEquals(5, model.cursor());
        assertEquals(2, model.positionAt(0, 2));
    }
}

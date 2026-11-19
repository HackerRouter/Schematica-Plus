package com.github.lunatrius.schematica.client.gui.framework;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HeaderButtonsLayoutTest {

    private static void assertBounds(int x, int y, int width, int height, UiBounds actual) {
        assertEquals(x, actual.x);
        assertEquals(y, actual.y);
        assertEquals(width, actual.width);
        assertEquals(height, actual.height);
    }

    @Test public void alignsUpdateButtonLeftOfReportButtonAndAdjustsMargins() {
        UiButton update = new UiButton(() -> "", ignored -> {});
        UiButton report = new UiButton(() -> "", ignored -> {});

        update.setVisible(true);
        report.setVisible(true);
        assertEquals(424, UiScreen.layoutHeaderButtons(480, update, report));
        assertBounds(448, 10, 20, 20, report.bounds());
        assertBounds(424, 10, 20, 20, update.bounds());
        assertEquals(86, UiScreen.headerTitleRightMargin(update, report));

        update.setVisible(false);
        report.setVisible(true);
        assertEquals(448, UiScreen.layoutHeaderButtons(480, update, report));
        assertBounds(448, 10, 20, 20, report.bounds());
        assertEquals(62, UiScreen.headerTitleRightMargin(update, report));

        update.setVisible(true);
        report.setVisible(false);
        assertEquals(448, UiScreen.layoutHeaderButtons(480, update, report));
        assertBounds(448, 10, 20, 20, update.bounds());
        assertEquals(62, UiScreen.headerTitleRightMargin(update, report));

        update.setVisible(false);
        report.setVisible(false);
        assertEquals(474, UiScreen.layoutHeaderButtons(480, update, report));
        assertEquals(30, UiScreen.headerTitleRightMargin(update, report));
    }
}

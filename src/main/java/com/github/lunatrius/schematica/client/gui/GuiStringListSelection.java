// SPDX-License-Identifier: LGPL-3.0-only
// MaLiLib GuiStringListSelection, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.client.gui.GuiScreen;

import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiDraw;
import com.github.lunatrius.schematica.client.gui.framework.UiListModel;
import com.github.lunatrius.schematica.client.gui.framework.UiPanel;
import com.github.lunatrius.schematica.client.gui.framework.UiRowList;
import com.github.lunatrius.schematica.client.gui.framework.UiScreen;

import org.lwjgl.input.Keyboard;

/** Picks any number of strings; OK passes the selection on, Cancel returns to the parent. */
public final class GuiStringListSelection extends UiScreen {
    private final List<String> strings;
    private final Set<String> selected = new LinkedHashSet<>();
    private final Consumer<List<String>> consumer;
    private final UiListModel<String> model = new UiListModel<>(22, string -> string);
    private UiRowList<String> list;
    private UiButton ok, cancel;

    public GuiStringListSelection(GuiScreen parent, String title, List<String> strings, Consumer<List<String>> consumer) {
        super(parent, title);
        this.strings = new ArrayList<>(strings);
        this.consumer = consumer;
    }

    @Override
    protected void createWidgets() {
        list = root.add(new UiRowList<>(model, Row::new));
        model.setEntries(strings);
        ok = addButton("malilib.gui.button.ok", () -> consumer.accept(new ArrayList<>(selected)));
        cancel = addButton("malilib.gui.button.cancel", this::closeScreen);
    }

    @Override
    protected void tickScreen() { ok.setEnabled(!selected.isEmpty()); }

    @Override
    protected void layoutWidgets() {
        list.setBounds(10, 30, Math.max(0, width - 20), Math.max(0, height - 64));
        int okWidth = fontRendererObj.getStringWidth(ok.label()) + 10;
        ok.setBounds(12, height - 32, okWidth, 20);
        cancel.setBounds(14 + okWidth, height - 32, fontRendererObj.getStringWidth(cancel.label()) + 10, 20);
    }

    private final class Row extends UiPanel {
        private final String string;
        private final int index;

        Row(String string, int index) {
            this.string = string;
            this.index = index;
        }

        private void toggle() {
            if (!selected.remove(string)) selected.add(string);
        }

        @Override
        public void draw(UiDraw draw, int mouseX, int mouseY) {
            boolean chosen = selected.contains(string);
            draw.fill(bounds(), chosen || containsVisible(mouseX, mouseY) ? 0xA0707070 : index % 2 == 1 ? 0xA0101010 : 0xA0303030);
            if (chosen) draw.border(bounds(), 0xFF90D0F0);
            draw.shadowText(draw.trim(string, bounds().width - 4), bounds().x + 2, bounds().y + (bounds().height - 9) / 2 + 1, 0xFFFFFFFF);
        }

        @Override
        public boolean isFocusable() { return true; }

        @Override
        public boolean mouseDown(int x, int y, int button) {
            if (button != 0) return false;
            toggle();
            return true;
        }

        @Override
        public boolean keyTyped(char character, int keyCode) {
            if (keyCode != Keyboard.KEY_RETURN && keyCode != Keyboard.KEY_SPACE) return false;
            toggle();
            return true;
        }
    }
}

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
import com.github.lunatrius.schematica.client.gui.framework.UiCheckBox;
import com.github.lunatrius.schematica.client.gui.framework.UiScreen;

/** Picks any number of strings; OK passes the selection on, Cancel returns to the parent. */
public final class GuiStringListSelection extends UiScreen {
    private final List<String> strings;
    private final Set<String> selected = new LinkedHashSet<>();
    private final Consumer<List<String>> consumer;
    private final List<UiCheckBox> boxes = new ArrayList<>();
    private UiButton ok, cancel;

    public GuiStringListSelection(GuiScreen parent, String title, List<String> strings, Consumer<List<String>> consumer) {
        super(parent, title);
        this.strings = new ArrayList<>(strings);
        this.consumer = consumer;
    }

    @Override
    protected void createWidgets() {
        for (String string : strings) {
            boxes.add(root.add(new UiCheckBox(() -> string, () -> selected.contains(string), value -> {
                if (value) selected.add(string);
                else selected.remove(string);
            })));
        }
        ok = addButton("malilib.gui.button.ok", () -> consumer.accept(new ArrayList<>(selected)));
        cancel = addButton("malilib.gui.button.cancel", this::closeScreen);
    }

    @Override
    protected void tickScreen() { ok.setEnabled(!selected.isEmpty()); }

    @Override
    protected void layoutWidgets() {
        int rows = Math.max(1, (height - 70) / 12), column = 0, row = 0;
        for (UiCheckBox box : boxes) {
            box.setBounds(12 + column * 170, 30 + row * 12, 164, 11);
            if (++row >= rows) { row = 0; column++; }
        }
        int okWidth = fontRendererObj.getStringWidth(ok.label()) + 10;
        ok.setBounds(12, height - 30, okWidth, 20);
        cancel.setBounds(14 + okWidth, height - 30, fontRendererObj.getStringWidth(cancel.label()) + 10, 20);
    }
}

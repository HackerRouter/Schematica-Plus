// SPDX-License-Identifier: LGPL-3.0-only
// MaLiLib GuiTextInput, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui;

import java.util.function.Function;

import net.minecraft.client.gui.GuiScreen;

import com.github.lunatrius.schematica.client.gui.framework.UiScreen;

/** A stand-alone text input dialog that closes when it is confirmed or cancelled. */
public final class GuiTextPrompt extends UiScreen {
    private final String title, initial;
    private final Function<String, String> apply;
    private final GuiScreen parent;

    public GuiTextPrompt(GuiScreen parent, String title, String initial, Function<String, String> apply) {
        super(parent, title);
        this.parent = parent;
        this.title = title;
        this.initial = initial;
        this.apply = apply;
    }

    @Override protected void createWidgets() {}
    @Override protected void layoutWidgets() {}
    @Override protected void opened() { prompt(title, initial, apply); }

    @Override protected void tickScreen() {
        if (input.modalPanels().isEmpty()) mc.displayGuiScreen(parent);
    }
}

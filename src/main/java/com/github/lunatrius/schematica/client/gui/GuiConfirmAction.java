// Stand-alone action confirmation, by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui;

import net.minecraft.client.gui.GuiScreen;

import com.github.lunatrius.schematica.client.gui.framework.UiScreen;

public final class GuiConfirmAction extends UiScreen {
    private final GuiScreen parent;
    private final String title, message, acceptKey;
    private final Runnable action;

    public GuiConfirmAction(GuiScreen parent, String title, String message, String acceptKey, Runnable action) {
        super(parent, title);
        this.parent = parent;
        this.title = title;
        this.message = message;
        this.acceptKey = acceptKey;
        this.action = action;
    }

    @Override protected void createWidgets() {}
    @Override protected void layoutWidgets() {}
    @Override protected void opened() {
        confirm(title, message, acceptKey, () -> {
            mc.displayGuiScreen(parent);
            action.run();
        });
    }
    @Override protected void tickScreen() {
        if (input.modalPanels().isEmpty()) mc.displayGuiScreen(parent);
    }
}

// SPDX-License-Identifier: LGPL-3.0-only
// Litematica loaded/placement list layouts, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.placement;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import net.minecraft.client.gui.GuiScreen;

import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiLabel;
import com.github.lunatrius.schematica.client.gui.framework.UiListModel;
import com.github.lunatrius.schematica.client.gui.framework.UiPanel;
import com.github.lunatrius.schematica.client.gui.framework.UiRowList;
import com.github.lunatrius.schematica.client.gui.framework.UiScreen;
import com.github.lunatrius.schematica.client.gui.framework.UiSprite;
import com.github.lunatrius.schematica.client.gui.framework.UiTextField;
import com.github.lunatrius.schematica.client.gui.load.GuiSchematicLoad;

abstract class GuiSchematicList<T> extends UiScreen {
    private final boolean placements;
    protected final UiListModel<T> model;
    private List<T> snapshot = new ArrayList<>();
    private UiRowList<T> list;
    private UiTextField search;
    private UiButton searchButton;
    private UiButton first;
    private UiButton second;
    private UiButton menu;
    private boolean searching;

    GuiSchematicList(GuiScreen parent, boolean placements, Function<T, String> label) {
        super(parent, UiTranslations.format(placements ? "litematica.gui.title.manage_schematic_placements"
            : "litematica.gui.title.manage_loaded_schematics"));
        this.placements = placements;
        model = new UiListModel<>(22, label);
    }

    protected abstract List<T> entries();

    protected abstract UiPanel row(T entry, int index);

    private String status = "";
    private UiLabel feedback;

    protected final void message(String value) { status = value; feedback.setTooltip(value); }

    @Override
    protected void createWidgets() {
        list = root.add(new UiRowList<>(model, this::row));
        search = root.add(new UiTextField(fontRendererObj, 256, text -> { model.setQuery(text); list.sync(); }));
        search.setVisible(false);
        searchButton = root.add(new UiButton(() -> "", button -> {
            searching = !searching;
            search.setVisible(searching);
            if (searching) input.focus(search);
            else search.setText("");
        }).setSprite(UiSprite.SEARCH).setBackground(false));
        first = addButton("litematica.gui.button.change_menu." + (placements ? "show_loaded_schematics" : "load_schematics_to_memory"),
            () -> mc.displayGuiScreen(placements ? new GuiSchematicLoadedList(this) : new GuiSchematicLoad(this)))
            .setSprite(placements ? UiSprite.LOADED_SCHEMATICS : UiSprite.SCHEMATIC_BROWSER);
        if (!placements) second = addButton("litematica.gui.button.change_menu.show_schematic_placements",
            () -> mc.displayGuiScreen(new GuiSchematicPlacementsList(this))).setSprite(UiSprite.SCHEMATIC_PLACEMENTS);
        feedback = root.add(new UiLabel(() -> status));
        menu = addButton("litematica.gui.button.change_menu.to_main_menu", this::mainMenu);
    }

    @Override
    protected void opened() { refresh(); }

    protected final void refresh() {
        snapshot = new ArrayList<>(entries());
        model.setEntries(snapshot);
        list.sync();
    }

    @Override
    protected void tickScreen() {
        if (!snapshot.equals(entries())) refresh();
    }

    @Override
    protected void layoutWidgets() {
        feedback.setBounds(14, height - 40, width - 28, 12);
        searchButton.setBounds(14, 35, 12, 12);
        search.setBounds(30, 34, Math.max(0, width - 52), 14);
        list.setBounds(14, 51, width - 24, Math.max(0, height - (placements ? 89 : 93)));
        int w = fontRendererObj.getStringWidth(first.label()) + 30;
        first.setBounds(12, height - 26, w, 20);
        if (second != null) second.setBounds(16 + w, height - 26, fontRendererObj.getStringWidth(second.label()) + 30, 20);
        int menuWidth = fontRendererObj.getStringWidth(menu.label()) + 20;
        menu.setBounds(width - menuWidth - 10, height - 26, menuWidth, 20);
    }

}

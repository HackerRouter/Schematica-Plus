// SPDX-License-Identifier: LGPL-3.0-only
// Litematica GuiMainMenu layout, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui;

import net.minecraft.client.gui.GuiScreen;

import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiScreen;
import com.github.lunatrius.schematica.client.gui.framework.UiSprite;
import com.github.lunatrius.schematica.client.gui.load.GuiSchematicLoad;
import com.github.lunatrius.schematica.client.gui.browser.GuiSchematicManager;
import com.github.lunatrius.schematica.client.gui.placement.GuiSchematicLoadedList;
import com.github.lunatrius.schematica.client.gui.placement.GuiSchematicPlacementsList;
import com.github.lunatrius.schematica.client.gui.save.GuiAreaSelectionEditor;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.tool.ToolManager;

public final class GuiSchematicMainMenu extends UiScreen {
    private UiButton placements;
    private UiButton loaded;
    private UiButton load;
    private UiButton area;
    private UiButton selections;
    private UiButton selectionMode;
    private UiButton config;
    private UiButton manager;
    private UiButton tasks;
    private UiButton mode;

    public GuiSchematicMainMenu(GuiScreen parent) {
        super(parent, Reference.NAME + " v" + Reference.VERSION);
    }

    private UiButton menu(String key, UiSprite icon, Runnable action) {
        return addButton("litematica.gui.button.change_menu." + key, action).setSprite(icon);
    }

    @Override
    protected void createWidgets() {
        placements = menu("show_schematic_placements", UiSprite.SCHEMATIC_PLACEMENTS,
            () -> mc.displayGuiScreen(new GuiSchematicPlacementsList(this)));
        loaded = menu("show_loaded_schematics", UiSprite.LOADED_SCHEMATICS,
            () -> mc.displayGuiScreen(new GuiSchematicLoadedList(this)));
        load = menu("load_schematics_to_memory", UiSprite.SCHEMATIC_BROWSER,
            () -> mc.displayGuiScreen(new GuiSchematicLoad(this)));
        area = menu("area_editor", UiSprite.AREA_EDITOR, () -> mc.displayGuiScreen(new GuiAreaSelectionEditor(this)));
        selections = unavailable(menu("show_area_selections", UiSprite.AREA_SELECTION, () -> {}));
        selectionMode = unavailable(root.add(new UiButton(() -> UiTranslations.format("litematica.gui.button.area_selection_mode",
            UiTranslations.format("litematica.gui.label.area_selection.mode.simple")), button -> {})));
        config = menu("configuration_menu", UiSprite.CONFIGURATION, () -> mc.displayGuiScreen(new GuiModConfig(this)));
        manager = menu("schematic_manager", UiSprite.SCHEMATIC_MANAGER,
            () -> mc.displayGuiScreen(new GuiSchematicManager(this)));
        tasks = unavailable(menu("task_manager", UiSprite.TASK_MANAGER, () -> {}));
        mode = root.add(new UiButton(() -> UiTranslations.format("litematica.gui.button.tool_mode", ToolManager.getCurrentMode().getDisplayName()),
            button -> { ToolManager.cycleMode(button == 0); layoutWidgets(); }));
        mode.setTooltip(UiTranslations.format("schematica.ui.menu.mode_hint"));
    }

    @Override
    protected void tickScreen() {
        boolean world = mc.theWorld != null && mc.thePlayer != null;
        load.setEnabled(world && SchematicaPlus.proxy.isLoadEnabled);
        area.setEnabled(world && SchematicaPlus.proxy.isSaveEnabled);
        mode.setEnabled(world);
    }

    @Override
    protected void layoutWidgets() {
        int column = 0;
        UiButton[] buttons = {placements, loaded, load, area, selections, config, manager, tasks};
        for (UiButton button : buttons) column = Math.max(column, fontRendererObj.getStringWidth(button.label()) + 30);
        column = Math.max(column, fontRendererObj.getStringWidth(selectionMode.label()) + 10);
        column = Math.max(column, fontRendererObj.getStringWidth(UiTranslations.format("litematica.gui.button.change_menu.schematic_projects_manager")) + 30);
        column = Math.max(column, fontRendererObj.getStringWidth(UiTranslations.format("litematica.gui.button.area_selection_mode",
            UiTranslations.format("litematica.gui.label.area_selection.mode.normal"))) + 10);
        placements.setBounds(12, 30, column, 20);
        loaded.setBounds(12, 52, column, 20);
        load.setBounds(12, 74, column, 20);
        area.setBounds(12, 118, column, 20);
        selections.setBounds(12, 140, column, 20);
        selectionMode.setBounds(12, 162, column, 20);
        config.setBounds(32 + column, 30, column, 20);
        manager.setBounds(32 + column, 118, column, 20);
        tasks.setBounds(32 + column, 140, column, 20);
        mode.setBounds(12, height - 26, fontRendererObj.getStringWidth(mode.label()) + 10, 20);
    }
}

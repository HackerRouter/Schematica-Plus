package com.github.lunatrius.schematica.client.gui;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.client.gui.control.GuiSchematicControl;
import com.github.lunatrius.schematica.client.gui.control.GuiSchematicInstances;
import com.github.lunatrius.schematica.client.gui.control.GuiSchematicMaterials;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiLabel;
import com.github.lunatrius.schematica.client.gui.framework.UiScreen;
import com.github.lunatrius.schematica.client.gui.load.GuiSchematicLoad;
import com.github.lunatrius.schematica.client.gui.save.GuiSchematicSave;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.tool.ToolManager;

public final class GuiSchematicMainMenu extends UiScreen {

    private UiButton instances;
    private UiButton load;
    private UiButton control;
    private UiButton save;
    private UiButton config;
    private UiButton materials;
    private UiButton components;
    private UiButton mode;
    private UiButton done;
    private UiLabel active;

    public GuiSchematicMainMenu(GuiScreen parent) {
        super(parent, Reference.NAME + " " + Reference.VERSION);
    }

    @Override
    protected void createWidgets() {
        instances = addButton("schematica.gui.instances.title", () -> mc.displayGuiScreen(new GuiSchematicInstances(this)));
        load = addButton("schematica.ui.menu.load", () -> mc.displayGuiScreen(new GuiSchematicLoad(this)));
        control = addButton("schematica.ui.menu.control", () -> mc.displayGuiScreen(new GuiSchematicControl(this)));
        save = addButton("schematica.ui.menu.save", () -> mc.displayGuiScreen(new GuiSchematicSave(this)));
        config = addButton("schematica.ui.menu.config", () -> mc.displayGuiScreen(new GuiModConfig(this)));
        materials = addButton("schematica.gui.materials", () -> {
            if (ClientProxy.schematic != null) mc.displayGuiScreen(new GuiSchematicMaterials(this));
        });
        components = addButton("schematica.ui.menu.components", () -> mc.displayGuiScreen(new UiDemoScreen(this)));
        mode = root.add(new UiButton(() -> I18n.format("schematica.ui.menu.mode", ToolManager.getCurrentMode().getDisplayName()),
            mouseButton -> ToolManager.cycleMode(mouseButton == 0)));
        mode.setTooltip(I18n.format("schematica.ui.menu.mode_hint"));
        done = addButton("gui.back", this::closeScreen);
        active = root.add(new UiLabel(this::activeName));
    }

    private String activeName() {
        return ClientProxy.schematic == null ? I18n.format("schematica.ui.menu.no_active")
            : I18n.format("schematica.ui.menu.active", ClientProxy.schematic.name);
    }

    @Override
    protected void opened() {
        tickScreen();
    }

    @Override
    protected void tickScreen() {
        boolean world = mc.theWorld != null && mc.thePlayer != null;
        load.setEnabled(world && SchematicaPlus.proxy.isLoadEnabled);
        save.setEnabled(world && SchematicaPlus.proxy.isSaveEnabled);
        control.setEnabled(world && ClientProxy.schematic != null);
        instances.setEnabled(world && !ClientProxy.loadedSchematics.isEmpty());
        materials.setEnabled(world && ClientProxy.schematic != null);
        mode.setEnabled(world);
        active.setTooltip(activeName());
    }

    @Override
    protected void layoutWidgets() {
        int column = Math.min(220, (width - 36) / 2);
        int right = 24 + column;
        instances.setBounds(12, 32, column, 20);
        load.setBounds(12, 56, column, 20);
        control.setBounds(12, 80, column, 20);
        save.setBounds(12, 124, column, 20);
        config.setBounds(right, 32, column, 20);
        materials.setBounds(right, 56, column, 20);
        components.setBounds(right, 124, column, 20);
        active.setBounds(12, height - 64, width - 24, 20);
        mode.setBounds(12, height - 36, Math.max(1, width - 116), 20);
        done.setBounds(width - 100, height - 36, 88, 20);
    }
}

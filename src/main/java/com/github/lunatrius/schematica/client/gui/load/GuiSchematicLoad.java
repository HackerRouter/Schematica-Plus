package com.github.lunatrius.schematica.client.gui.load;

import java.io.File;
import java.io.IOException;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.client.gui.browser.GuiSchematicBrowser;
import com.github.lunatrius.schematica.client.gui.browser.SchematicBrowserModel;
import com.github.lunatrius.schematica.client.gui.control.GuiSchematicControl;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.proxy.ClientProxy;

public final class GuiSchematicLoad extends GuiSchematicBrowser {

    private UiButton load;
    private UiButton controls;

    public GuiSchematicLoad(GuiScreen parent) {
        super(parent, I18n.format("schematica.ui.load.title"), false);
    }

    @Override
    protected void createActions() {
        load = addAction("schematica.ui.load.load", () -> {
            SchematicBrowserModel.Entry entry = selection();
            if (entry != null) activateFile(entry);
        });
        controls = addAction("schematica.ui.menu.control", () -> {
            if (ClientProxy.schematic != null) mc.displayGuiScreen(new GuiSchematicControl(this));
        });
        load.setTooltip(I18n.format("schematica.ui.load.hint"));
    }

    @Override
    protected void activateFile(SchematicBrowserModel.Entry entry) {
        if (!SchematicaPlus.proxy.isLoadEnabled || mc.theWorld == null || mc.thePlayer == null) {
            setStatus(I18n.format("schematica.ui.load.disabled"));
            return;
        }
        try {
            File file = browser.readableFile(entry);
            SchematicWorld schematic = SchematicGuiLoader.load(mc, file);
            setStatus(I18n.format("schematica.ui.load.success", schematic.name,
                schematic.getWidth(), schematic.getHeight(), schematic.getLength()));
        } catch (IOException | RuntimeException e) {
            fail("schematica.ui.load.failed", e);
        }
        tickScreen();
    }

    @Override
    protected void tickScreen() {
        super.tickScreen();
        SchematicBrowserModel.Entry entry = selection();
        load.setEnabled(SchematicaPlus.proxy.isLoadEnabled && mc.theWorld != null && entry != null && !entry.directory);
        controls.setEnabled(ClientProxy.schematic != null);
    }
}

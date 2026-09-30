// SPDX-License-Identifier: LGPL-3.0-only
// Litematica load layout, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.load;

import java.io.File;
import java.io.IOException;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.client.gui.browser.GuiSchematicBrowser;
import com.github.lunatrius.schematica.client.gui.browser.SchematicBrowserModel;
import com.github.lunatrius.schematica.client.gui.placement.GuiSchematicLoadedList;
import com.github.lunatrius.schematica.client.gui.framework.UiCheckBox;
import com.github.lunatrius.schematica.client.gui.framework.UiSprite;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.world.SchematicWorld;

public final class GuiSchematicLoad extends GuiSchematicBrowser {

    private UiButton load;
    private UiCheckBox createPlacement;

    public GuiSchematicLoad(GuiScreen parent) {
        super(parent, I18n.format("litematica.gui.title.load_schematic"), false);
    }

    @Override
    protected void createActions() {
        load = addAction("litematica.gui.button.load_schematic_to_memory", () -> {
            SchematicBrowserModel.Entry entry = selection();
            if (entry != null) activateFile(entry);
        });
        unavailable(addAction("litematica.gui.button.material_list", () -> {}));
        unavailable(addAction("litematica.gui.button.rename_schematic", () -> {}));
        unavailable(addAction("litematica.gui.button.rename_file", () -> {}));
        addAction("litematica.gui.button.change_menu.show_loaded_schematics",
            () -> mc.displayGuiScreen(new GuiSchematicLoadedList(this))).setSprite(UiSprite.LOADED_SCHEMATICS);
        createPlacement = root.add(new UiCheckBox(() -> I18n.format("litematica.gui.label.schematic_load.checkbox.create_placement"),
            () -> true, value -> {}));
        createPlacement.setEnabled(false);
        createPlacement.setTooltip(I18n.format("schematica.ui.linked_instances"));
        load.setTooltip(I18n.format("schematica.ui.load.hint"));
    }

    @Override
    protected void layoutActions() {
        super.layoutActions();
        createPlacement.setBounds(12, height - 40, width - 24, 11);
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

    }
}

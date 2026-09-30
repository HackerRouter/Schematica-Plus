// SPDX-License-Identifier: LGPL-3.0-only
// Litematica load layout, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.load;

import java.io.File;
import java.io.IOException;

import net.minecraft.client.gui.GuiScreen;

import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.client.gui.browser.GuiSchematicBrowser;
import com.github.lunatrius.schematica.client.gui.browser.SchematicBrowserModel;
import com.github.lunatrius.schematica.client.gui.placement.GuiSchematicLoadedList;
import com.github.lunatrius.schematica.client.gui.framework.UiCheckBox;
import com.github.lunatrius.schematica.client.gui.framework.UiSprite;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.world.SchematicLibrary;
import com.github.lunatrius.schematica.client.world.SchematicSourceData;

public final class GuiSchematicLoad extends GuiSchematicBrowser {

    private UiButton load;
    private UiButton renameFile;
    private UiCheckBox createPlacement;
    private boolean placeOnLoad = true;

    public GuiSchematicLoad(GuiScreen parent) {
        super(parent, UiTranslations.format("litematica.gui.title.load_schematic"), false);
    }

    @Override
    protected void createActions() {
        load = addAction("litematica.gui.button.load_schematic_to_memory", () -> {
            SchematicBrowserModel.Entry entry = selection();
            if (entry != null) activateFile(entry);
        });
        unavailable(addAction("litematica.gui.button.material_list", () -> {}));
        unavailable(addAction("litematica.gui.button.rename_schematic", () -> {}));
        renameFile = addAction("litematica.gui.button.rename_file", this::renameSelectedFile);
        addAction("litematica.gui.button.change_menu.show_loaded_schematics",
            () -> mc.displayGuiScreen(new GuiSchematicLoadedList(this))).setSprite(UiSprite.LOADED_SCHEMATICS);
        createPlacement = root.add(new UiCheckBox(() -> UiTranslations.format("litematica.gui.label.schematic_load.checkbox.create_placement"),
            () -> placeOnLoad, value -> placeOnLoad = value));
        createPlacement.setTooltip(UiTranslations.format("schematica.ui.source.load_hint"));
        load.setTooltip(UiTranslations.format("schematica.ui.load.hint"));
    }

    @Override
    protected void layoutActions() {
        super.layoutActions();
        createPlacement.setBounds(12, height - 40, width - 24, 11);
    }

    @Override
    protected void activateFile(SchematicBrowserModel.Entry entry) {
        if (!SchematicaPlus.proxy.isLoadEnabled || mc.theWorld == null || mc.thePlayer == null) {
            setStatus(UiTranslations.format("schematica.ui.load.disabled"));
            return;
        }
        try {
            File file = browser.readableFile(entry);
            SchematicLibrary.Source<SchematicSourceData> source = SchematicGuiLoader.load(mc, file, placeOnLoad);
            setStatus(UiTranslations.format("schematica.ui.load.success", source.name(),
                source.data().width, source.data().height, source.data().length));
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
        renameFile.setEnabled(entry != null && !entry.directory);
    }
}

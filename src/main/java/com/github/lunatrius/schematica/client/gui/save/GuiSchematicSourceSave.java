// SPDX-License-Identifier: LGPL-3.0-only
// Litematica save browser layout, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.save;

import java.io.File;
import java.io.IOException;

import net.minecraft.client.gui.GuiScreen;

import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.gui.browser.GuiSchematicBrowser;
import com.github.lunatrius.schematica.client.gui.browser.SchematicBrowserModel;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiLabel;
import com.github.lunatrius.schematica.client.gui.framework.UiTextField;
import com.github.lunatrius.schematica.client.world.SchematicLibrary.Source;
import com.github.lunatrius.schematica.client.world.SchematicSourceData;
import com.github.lunatrius.schematica.proxy.ClientProxy;

public final class GuiSchematicSourceSave extends GuiSchematicBrowser {

    private final Source<SchematicSourceData> source;
    private UiTextField name;
    private UiButton save;
    private UiLabel format;

    public GuiSchematicSourceSave(GuiScreen parent, Source<SchematicSourceData> source) {
        super(parent, UiTranslations.format("litematica.gui.title.save_schematic_from_memory"), false);
        this.source = source;
    }

    @Override protected int browserX() { return 10; }
    @Override protected int browserY() { return 80; }
    @Override protected int browserHeight() { return height - 116; }

    @Override protected void createActions() {
        name = root.add(new UiTextField(fontRendererObj, 210, text -> {}));
        name.setText(source.name());
        save = addButton("litematica.gui.button.save_to_file", this::save);
        save.setTooltip(UiTranslations.format("schematica.ui.source.save_hint"));
        format = root.add(new UiLabel(() -> source.data().saveExtension()));
        format.setTooltip(UiTranslations.format("schematica.ui.source.save_hint"));
    }

    @Override protected void tickScreen() {
        super.tickScreen();
        save.setEnabled(browser != null && ClientProxy.SCHEMATICS.sources().contains(source));
    }

    @Override protected void selectionChanged(SchematicBrowserModel.Entry entry) {
        if (entry != null && !entry.directory) name.setText(entry.name());
    }

    private void save() {
        if (browser == null || !ClientProxy.SCHEMATICS.sources().contains(source)) return;
        try {
            File file = SchematicSaveTarget.sourceCopy(browser.root(), browser.directory(), name.text(), source.data().saveExtension());
            if (file.exists()) confirm(UiTranslations.format("schematica.ui.save.overwrite_title"),
                UiTranslations.format("schematica.ui.save.overwrite", file.getName()), () -> write(file, true));
            else write(file, false);
        } catch (IOException | IllegalArgumentException e) {
            fail("schematica.ui.source.save_failed", e);
        }
    }

    private void write(File file, boolean replace) {
        if (!ClientProxy.SCHEMATICS.sources().contains(source)) {
            setStatus(UiTranslations.format("schematica.ui.source.unloaded"));
            return;
        }
        try {
            File checked = SchematicSaveTarget.sourceCopy(browser.root(), file.getParentFile(), file.getName(), source.data().saveExtension());
            source.data().save(checked, replace);
            refreshFiles();
            setStatus(UiTranslations.format("litematica.message.schematic_saved_as", file.getName()));
        } catch (com.github.lunatrius.schematica.util.MessageException e) {
            setStatus(UiTranslations.format(e.key(), e.arguments()));
        } catch (IOException | IllegalArgumentException e) {
            fail("schematica.ui.source.save_failed", e);
        }
    }

    @Override protected void layoutActions() {
        name.setBounds(10, 32, Math.max(1, width - 140), 18);
        format.setBounds(name.bounds().right() + 6, 36, 110, 12);
        save.setBounds(10, 54, fontRendererObj.getStringWidth(save.label()) + 10, 20);
    }
}

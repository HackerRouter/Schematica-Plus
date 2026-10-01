// SPDX-License-Identifier: LGPL-3.0-only
// Litematica GuiSchematicSaveImported / GuiSchematicSaveExported, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.browser;

import java.io.File;
import java.io.IOException;

import net.minecraft.client.gui.GuiScreen;

import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiCheckBox;
import com.github.lunatrius.schematica.client.gui.framework.UiTextField;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.gui.save.SchematicSaveTarget;
import com.github.lunatrius.schematica.world.schematic.SchematicFiles;

/** Saves a converted copy of a schematic file: imports as .litematic, exports as .schematic, .litematic or .nbt. */
public final class GuiSchematicConvert extends GuiSchematicBrowser {
    private final File source;
    private final String extension, successKey;
    private boolean ignoreEntities;
    private UiTextField name;
    private UiButton save;
    private UiCheckBox entities;

    public GuiSchematicConvert(GuiScreen parent, String title, File source, String extension, String successKey) {
        super(parent, title, false);
        this.source = source;
        this.extension = extension;
        this.successKey = successKey;
    }

    @Override protected int browserX() { return 10; }
    @Override protected int browserY() { return 80; }
    @Override protected int browserHeight() { return height - 80; }
    @Override protected boolean showFooter() { return false; }

    @Override
    protected void createActions() {
        name = root.add(new UiTextField(fontRendererObj, 256, text -> {}));
        String file = source.getName();
        name.setText(file.substring(0, file.lastIndexOf('.')));
        save = addButton("litematica.gui.button.save_schematic", this::save);
        save.setTooltip(UiTranslations.format("litematica.gui.label.schematic_save.hover_info.hold_shift_to_overwrite"));
        entities = root.add(new UiCheckBox(() -> UiTranslations.format("litematica.gui.label.schematic_save.checkbox.ignore_entities"),
            () -> ignoreEntities, value -> ignoreEntities = value));
    }

    @Override
    protected void selectionChanged(SchematicBrowserModel.Entry entry) {
        if (entry != null && !entry.directory) name.setText(entry.name().substring(0, entry.name().lastIndexOf('.')));
    }

    private void save() {
        if (browser == null) return;
        if (!browser.directory().isDirectory()) {
            setStatus(UiTranslations.format("litematica.error.schematic_save.invalid_directory", browser.directory().getAbsolutePath()));
            return;
        }
        if (name.text().trim().isEmpty()) {
            setStatus(UiTranslations.format("litematica.error.schematic_save.invalid_schematic_name", name.text()));
            return;
        }
        if (!source.isFile() || !source.canRead()) {
            setStatus(UiTranslations.format("litematica.error.schematic_load.cant_read_file", source.getName()));
            return;
        }
        File target;
        try {
            target = SchematicSaveTarget.sourceCopy(browser.root(), browser.directory(), name.text(), extension);
        } catch (IOException | IllegalArgumentException e) {
            setStatus(UiTranslations.format("litematica.error.schematic_save.invalid_schematic_name", name.text()));
            return;
        }
        if (target.exists() && !isShiftKeyDown()) {
            setStatus(UiTranslations.format("litematica.error.schematic_write_to_file_failed.exists", target.getName()));
            return;
        }
        try {
            String author = mc.thePlayer != null ? mc.thePlayer.getCommandSenderName() : mc.getSession().getUsername();
            File written = SchematicFiles.convert(source, target, !ignoreEntities, author);
            refreshFiles();
            setStatus(UiTranslations.format(successKey, written.getName()));
        } catch (IOException | RuntimeException e) {
            fail("litematica.error.schematic_load.cant_read_file", e, source.getName());
        }
    }

    @Override
    protected void layoutActions() {
        name.setBounds(10, 32, Math.max(1, width - 260), 18);
        save.setBounds(10, 54, fontRendererObj.getStringWidth(save.label()) + 10, 20);
        entities.setBounds(name.bounds().right() + 4, 28, 240, 11);
    }
}

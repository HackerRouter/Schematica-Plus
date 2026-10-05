// SPDX-License-Identifier: LGPL-3.0-only
// Litematica GuiMaterialListSave / GuiMaterialListSaveBase, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.save;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;

import com.github.lunatrius.schematica.client.gui.browser.GuiSchematicBrowser;
import com.github.lunatrius.schematica.client.gui.browser.SchematicBrowserModel;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiTextField;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.gui.material.CustomMaterialListFile;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.reference.Reference;

/** Saves a material list as a custom list JSON file that the load browser opens as a material list again. */
public final class GuiMaterialListSave extends GuiSchematicBrowser {
    private final String listName;
    private final List<CustomMaterialListFile.Item> items;
    private UiTextField name;
    private UiButton save;

    public GuiMaterialListSave(GuiScreen parent, String listName, List<CustomMaterialListFile.Item> items) {
        super(parent, UiTranslations.format("litematica.gui.title.save_material_list"), false);
        this.listName = listName;
        this.items = items;
    }

    @Override protected SchematicBrowserModel createModel() throws IOException {
        return new SchematicBrowserModel(ConfigurationHandler.schematicDirectory, CustomMaterialListFile::isListFile);
    }

    @Override protected int browserX() { return 10; }
    @Override protected int browserY() { return 80; }
    @Override protected int browserHeight() { return height - 116; }

    @Override protected void createActions() {
        name = root.add(new UiTextField(fontRendererObj, 210, text -> {}));
        name.setText(listName.replaceAll("[^a-zA-Z0-9_\\-]", "_") + CustomMaterialListFile.JSON_EXTENSION);
        save = addButton("litematica.gui.button.save_material_list", this::save);
        save.setTooltip(UiTranslations.format("litematica.gui.label.schematic_save.hover_info.hold_shift_to_overwrite"));
    }

    @Override protected void selectionChanged(SchematicBrowserModel.Entry entry) {
        if (entry != null && !entry.directory) name.setText(entry.name());
    }

    private void save() {
        if (browser == null) return;
        String fileName = name.text().trim();
        if (fileName.isEmpty()) {
            setStatus(UiTranslations.format("litematica.error.material_list_save.invalid_name", fileName));
            return;
        }
        if (items.isEmpty()) {
            setStatus(UiTranslations.format("litematica.message.error.material_list_save"));
            return;
        }
        if (!fileName.toLowerCase(java.util.Locale.ROOT).endsWith(CustomMaterialListFile.JSON_EXTENSION)) fileName += CustomMaterialListFile.JSON_EXTENSION;
        try {
            SchematicBrowserModel.validateName(fileName);
            File directory = browser.directory();
            if (!directory.isDirectory()) {
                setStatus(UiTranslations.format("litematica.error.schematic_save.invalid_directory", directory.getAbsolutePath()));
                return;
            }
            File file = new File(directory, fileName);
            boolean exists = file.exists();
            if (exists && !isShiftKeyDown()) {
                setStatus(UiTranslations.format("litematica.error.material_list_write_to_file_failed.exists", fileName));
                return;
            }
            byte[] json = CustomMaterialListFile.toJson(listName, items).getBytes(StandardCharsets.UTF_8);
            if (exists) Files.write(file.toPath(), json, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            else Files.write(file.toPath(), json, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            refreshFiles();
            String message = UiTranslations.format("litematica.message.material_list_save.exported", file.getName());
            setStatus(message);
            if (mc.thePlayer != null) {
                mc.thePlayer.addChatMessage(new ChatComponentText(message).setChatStyle(new ChatStyle().setUnderlined(true)
                    .setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, file.getAbsolutePath()))));
            }
        } catch (IOException | RuntimeException e) {
            Reference.logger.warn("Could not save the material list", e);
            setStatus(UiTranslations.format("litematica.message.error.material_list.custom_export_failed", fileName));
        }
    }

    @Override protected void layoutActions() {
        name.setBounds(10, 32, Math.max(1, width - 140), 18);
        save.setBounds(10, 54, fontRendererObj.getStringWidth(save.label()) + 10, 20);
    }
}

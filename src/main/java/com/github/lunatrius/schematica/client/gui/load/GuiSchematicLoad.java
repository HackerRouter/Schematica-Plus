// SPDX-License-Identifier: LGPL-3.0-only
// Litematica load layout, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.load;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.GuiScreen;

import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.client.gui.browser.GuiSchematicBrowser;
import com.github.lunatrius.schematica.client.gui.browser.SchematicBrowserModel;
import com.github.lunatrius.schematica.client.gui.placement.GuiSchematicLoadedList;
import com.github.lunatrius.schematica.client.gui.framework.UiCheckBox;
import com.github.lunatrius.schematica.client.gui.framework.UiSprite;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.api.SchematicRegion;
import com.github.lunatrius.schematica.client.gui.GuiStringListSelection;
import com.github.lunatrius.schematica.client.gui.control.GuiSchematicMaterials;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.world.schematic.SchematicFiles;
import com.github.lunatrius.schematica.world.schematic.SchematicFormat;

public final class GuiSchematicLoad extends GuiSchematicBrowser {

    private UiButton load;
    private UiButton renameFile;
    private UiButton materialList;
    private UiButton renameSchematic;
    private UiCheckBox createPlacement;
    private boolean placeOnLoad = Boolean.parseBoolean(com.github.lunatrius.schematica.client.util.UiState.get("create_placement_on_load", "true"));

    public GuiSchematicLoad(GuiScreen parent) {
        super(parent, UiTranslations.format("litematica.gui.title.load_schematic"), false);
    }

    @Override
    protected void createActions() {
        load = addAction("litematica.gui.button.load_schematic_to_memory", () -> {
            SchematicBrowserModel.Entry entry = selection();
            if (entry != null) activateFile(entry);
        });
        materialList = addAction("litematica.gui.button.material_list", this::materialList);
        materialList.setTooltip(UiTranslations.format("litematica.gui.button.hover.material_list_shift_to_select_sub_regions"));
        renameSchematic = addAction("litematica.gui.button.rename_schematic", this::renameSchematic);
        renameFile = addAction("litematica.gui.button.rename_file", this::renameSelectedFile);
        addAction("litematica.gui.button.change_menu.show_loaded_schematics",
            () -> mc.displayGuiScreen(new GuiSchematicLoadedList(this))).setSprite(UiSprite.LOADED_SCHEMATICS);
        createPlacement = root.add(new UiCheckBox(() -> UiTranslations.format("litematica.gui.label.schematic_load.checkbox.create_placement"),
            () -> placeOnLoad, value -> {
                placeOnLoad = value;
                com.github.lunatrius.schematica.client.util.UiState.set("create_placement_on_load", Boolean.toString(value));
            }));
        createPlacement.setTooltip(UiTranslations.format("litematica.gui.label.schematic_load.hoverinfo.create_placement").split("\n"));
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
            SchematicGuiLoader.load(mc, file, placeOnLoad);
            setStatus(UiTranslations.format("litematica.message.schematic_read_from_file_success", file.getName()));
        } catch (IOException | RuntimeException e) {
            fail("litematica.error.schematic_read_from_file_failed.exception", e, entry.name());
        }
        tickScreen();
    }

    private File selectedFile() {
        SchematicBrowserModel.Entry entry = selection();
        if (entry == null || entry.directory) {
            setStatus(UiTranslations.format("litematica.error.schematic_load.no_schematic_selected"));
            return null;
        }
        try {
            return browser.readableFile(entry);
        } catch (IOException e) {
            fail("litematica.error.schematic_load.cant_read_file", e, entry.name());
            return null;
        }
    }

    /** The items of the selected file without placing it; Shift picks the sub-regions first. */
    private void materialList() {
        File file = selectedFile();
        if (file == null) return;
        ISchematic schematic = SchematicFormat.readFromFile(file);
        if (schematic == null) {
            setStatus(UiTranslations.format("litematica.error.schematic_read_from_file_failed.exception", file.getName()));
            return;
        }
        String name = file.getName().substring(0, file.getName().lastIndexOf('.'));
        SchematicWorld world = new SchematicWorld(schematic, file.getName());
        List<SchematicRegion> regions = schematic.getRegions();
        if (isShiftKeyDown() && regions.size() > 1) {
            List<String> names = new ArrayList<>();
            for (SchematicRegion region : regions) names.add(region.name);
            mc.displayGuiScreen(new GuiStringListSelection(this,
                UiTranslations.format("litematica.gui.title.material_list.select_schematic_regions", name), names, chosen -> {
                    List<SchematicRegion> selected = new ArrayList<>();
                    for (SchematicRegion region : regions) if (chosen.contains(region.name)) selected.add(region);
                    openMaterials(world, name, selected);
                }));
        } else {
            openMaterials(world, name, Collections.<SchematicRegion>emptyList());
        }
    }

    private void openMaterials(SchematicWorld world, String name, List<SchematicRegion> regions) {
        com.github.lunatrius.schematica.client.gui.material.MaterialList list = com.github.lunatrius.schematica.client.gui.material.MaterialList.schematic(world, name, regions);
        // Remember the last opened material list for the hotkey to (re-) open it
        com.github.lunatrius.schematica.client.gui.material.MaterialLists.setCurrent(list);
        mc.displayGuiScreen(new GuiSchematicMaterials(this, list));
    }

    private void renameSchematic() {
        File file = selectedFile();
        if (file == null) return;
        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".litematic")) {
            setStatus(UiTranslations.format("litematica.error.schematic_manager.schematic_edit.unsupported_type"));
            return;
        }
        SchematicFiles.Info info;
        try {
            info = SchematicFiles.info(file);
        } catch (IOException e) {
            fail("litematica.error.schematic_load.cant_read_file", e, file.getName());
            return;
        }
        prompt(UiTranslations.format("litematica.gui.title.rename_schematic"), info == null ? "" : info.name, value -> {
            try {
                SchematicFiles.editLitematicMetadata(file, metadata -> metadata.setString("Name", value));
                refreshFiles();
                return null;
            } catch (IOException e) {
                Reference.logger.warn("Could not rename the schematic", e);
                return UiTranslations.format("litematica.error.schematic_load.cant_read_file", file.getName());
            }
        });
    }

    @Override
    protected void tickScreen() {
        super.tickScreen();
        SchematicBrowserModel.Entry entry = selection();
        load.setEnabled(SchematicaPlus.proxy.isLoadEnabled && mc.theWorld != null && entry != null && !entry.directory);
        renameFile.setEnabled(entry != null && !entry.directory);
        materialList.setEnabled(entry != null && !entry.directory && mc.theWorld != null);
        renameSchematic.setEnabled(entry != null && !entry.directory);
    }
}

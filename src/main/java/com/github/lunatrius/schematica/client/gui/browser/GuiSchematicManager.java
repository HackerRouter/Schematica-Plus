// SPDX-License-Identifier: LGPL-3.0-only
// Litematica GuiSchematicManager layout, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.browser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.GuiScreen;

import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.world.schematic.SchematicFiles;

public final class GuiSchematicManager extends GuiSchematicBrowser {

    private final List<UiButton[]> groups = new ArrayList<>();
    private final int[] choices = {0, 1, 0};
    private static final String[][] OPTIONS = {
        {"edit_type.rename_schematic", "edit_type.change_author", "edit_type.set_preview"},
        {"export_type.schematic", "export_type.litematic_v4", "export_type.vanilla"},
        {"file_op_type.rename", "file_op_type.copy", "file_op_type.delete"}
    };
    private static final String[] EXPORT_EXTENSIONS = {".schematic", ".litematic", ".nbt"};

    public GuiSchematicManager(GuiScreen parent) {
        super(parent, UiTranslations.format("litematica.gui.title.schematic_manager"), false);
    }

    @Override
    protected void createActions() {
        UiButton edit = root.add(new UiButton(() -> UiTranslations.format("litematica.gui.button.schematic_manager.edit_schematic"), this::edit));
        UiButton importFile = addButton("litematica.gui.button.import", this::importFile);
        importFile.setTooltip(UiTranslations.format("litematica.gui.button.import.hover"));
        UiButton export = addButton("litematica.gui.button.schematic_manager.export_as", this::export);
        export.setTooltip(UiTranslations.format("litematica.gui.button.schematic_manager.export_as.hover"));
        UiButton operations = addButton("litematica.gui.button.schematic_manager.file_ops", () -> {
            switch (choices[2]) {
                case 0: renameSelectedFile(); break;
                case 1: copySelectedFile(); break;
                case 2: deleteSelectedFile(); break;
                default: break;
            }
        });
        operations.setTooltip(UiTranslations.format("litematica.gui.button.schematic_manager.file_ops.hover"));
        groups.add(new UiButton[] {edit, selector(0)});
        groups.add(new UiButton[] {importFile});
        groups.add(new UiButton[] {export, selector(1)});
        groups.add(new UiButton[] {operations, selector(2)});
        updateVisibility();
    }

    private File selectedFile() {
        SchematicBrowserModel.Entry entry = selection();
        if (entry == null || entry.directory) {
            setStatus(UiTranslations.format("litematica.error.schematic_load.no_schematic_selected"));
            return null;
        }
        if (!entry.file.isFile() || !entry.file.canRead()) {
            setStatus(UiTranslations.format("litematica.error.schematic_load.cant_read_file", entry.name()));
            return null;
        }
        return entry.file;
    }

    private static boolean litematic(File file) {
        return file.getName().toLowerCase(Locale.ROOT).endsWith(".litematic");
    }

    /** Schematic Edit: rename or change the author in the metadata, or set the preview image. */
    private void edit(int mouse) {
        if (choices[0] == 2 && mouse == 1) {
            if (SchematicPreview.INSTANCE.cancel()) setStatus(UiTranslations.format("litematica.message.schematic_preview_cancelled"));
            return;
        }
        if (mouse != 0) return;
        File file = selectedFile();
        if (file == null) return;
        if (!litematic(file)) {
            setStatus(UiTranslations.format("litematica.error.schematic_manager.schematic_edit.unsupported_type"));
            return;
        }
        if (choices[0] == 2) {
            if (isShiftKeyDown() && isCtrlKeyDown() && org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LMENU)
                || isShiftKeyDown() && isCtrlKeyDown() && org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RMENU)) {
                try {
                    SchematicPreview.fromThumbnail(file);
                    refreshFiles();
                    setStatus(UiTranslations.format("litematica.info.schematic_manager.preview.success"));
                } catch (IOException e) {
                    fail("litematica.error.schematic_load.cant_read_file", e, "thumb.png");
                }
                return;
            }
            SchematicPreview.INSTANCE.request(file);
            mc.displayGuiScreen(null);
            if (mc.thePlayer != null) {
                for (String line : UiTranslations.format("litematica.info.schematic_manager.preview.set_preview_by_taking_a_screenshot").split("\n")) {
                    mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(line));
                }
            }
            return;
        }
        boolean author = choices[0] == 1;
        SchematicFiles.Info info;
        try {
            info = SchematicFiles.info(file);
        } catch (IOException e) {
            fail("litematica.error.schematic_load.cant_read_file", e, file.getName());
            return;
        }
        prompt(UiTranslations.format(author ? "litematica.gui.title.schematic_change_author" : "litematica.gui.title.rename_schematic"),
            info == null ? "" : author ? info.author : info.name, value -> {
                try {
                    SchematicFiles.editLitematicMetadata(file, metadata -> metadata.setString(author ? "Author" : "Name", value));
                    refreshFiles();
                    return null;
                } catch (IOException e) {
                    Reference.logger.warn("Could not edit schematic metadata", e);
                    return UiTranslations.format("litematica.error.schematic_load.cant_read_file", file.getName());
                }
            });
    }

    private void importFile() {
        File file = selectedFile();
        if (file == null) return;
        mc.displayGuiScreen(new GuiSchematicConvert(this, UiTranslations.format("litematica.gui.title.save_imported_schematic"),
            file, ".litematic", "litematica.message.schematic_saved_as"));
    }

    private void export() {
        File file = selectedFile();
        if (file == null) return;
        if (!litematic(file)) {
            setStatus(UiTranslations.format("litematica.error.schematic_manager.schematic_export.unsupported_type"));
            return;
        }
        mc.displayGuiScreen(new GuiSchematicConvert(this, UiTranslations.format("litematica.gui.title.save_exported_schematic",
            UiTranslations.format(optionKey(1)), file.getName()), file, EXPORT_EXTENSIONS[choices[1]], "litematica.message.schematic_exported_as"));
    }

    private UiButton selector(int type) {
        UiButton button = root.add(new UiButton(() -> UiTranslations.format(optionKey(type)), mouse -> {
            choices[type] = Math.floorMod(choices[type] + (mouse == 0 ? 1 : -1), OPTIONS[type].length);
            updateVisibility();
            layoutWidgets();
        }));
        return button;
    }

    private String optionKey(int type) {
        String option = OPTIONS[type][choices[type]];
        return (option.equals("export_type.litematic_v4") ? "schematica.ui.files." : "litematica.gui.label.schematic_manager.") + option;
    }

    private void updateVisibility() {
        SchematicBrowserModel.Entry entry = selection();
        boolean selected = entry != null && !entry.directory;
        boolean litematic = selected && entry.name().toLowerCase(Locale.ROOT).endsWith(".litematic");
        for (int i = 0; i < groups.size(); i++) {
            for (UiButton button : groups.get(i)) button.setVisible(selected && (i == 1 || i == 3 || litematic));
        }
        for (int type = 0; type < 3; type++) {
            int group = type == 0 ? 0 : type == 1 ? 2 : 3;
            UiButton selector = groups.get(group)[1];
            if (type == 2) {
                String option = OPTIONS[type][choices[type]].substring("file_op_type.".length());
                selector.setTooltip(UiTranslations.format(option.equals("delete")
                    ? "litematica.gui.label.schematic_manager.file_op_type.delete.hover" : "schematica.ui.files.option." + option));
            } else {
                selector.setTooltip(UiTranslations.format(optionKey(type) + ".hover").split("\n"));
            }
        }
        String editHover = UiTranslations.format("schematica.ui.files.edit_schematic.hover");
        if (choices[0] == 2) groups.get(0)[0].setTooltip((editHover + "\n" + UiTranslations.format("litematica.info.schematic_manager.preview.right_click_to_cancel")).split("\n"));
        else groups.get(0)[0].setTooltip(editHover.split("\n"));
    }

    @Override
    protected void selectionChanged(SchematicBrowserModel.Entry entry) {
        updateVisibility();
        layoutWidgets();
    }

    @Override protected int browserX() { return 10; }

    @Override protected int browserHeight() { return height - 60 - (arrange(false) - 1) * 24; }

    @Override protected void layoutActions() { arrange(true); }

    private int arrange(boolean place) {
        int available = Math.max(40, width - fontRendererObj.getStringWidth(UiTranslations.format("litematica.gui.button.change_menu.to_main_menu")) - 44);
        int rows = 1;
        int used = 0;
        for (UiButton[] group : groups) {
            if (!group[0].isVisible()) continue;
            int groupWidth = groupWidth(group, available);
            if (used > 0 && used + groupWidth > available) { rows++; used = 0; }
            used += groupWidth + 4;
        }
        if (!place) return rows;
        int x = 10;
        int y = height - 26 - (rows - 1) * 24;
        for (UiButton[] group : groups) {
            if (!group[0].isVisible()) continue;
            int groupWidth = groupWidth(group, available);
            if (x > 10 && x - 10 + groupWidth > available) { x = 10; y += 24; }
            int natural = groupWidth(group, Integer.MAX_VALUE);
            int extra = Math.max(0, natural - groupWidth);
            for (int i = 0; i < group.length; i++) {
                UiButton button = group[i];
                int reduction = extra / (group.length - i);
                int w = fontRendererObj.getStringWidth(button.label()) + 10 - reduction;
                button.setBounds(x, y, Math.max(10, w), 20);
                x += Math.max(10, w) + 4;
                extra -= reduction;
            }
        }
        return rows;
    }

    private int groupWidth(UiButton[] group, int limit) {
        int width = -4;
        for (UiButton button : group) width += fontRendererObj.getStringWidth(button.label()) + 14;
        return Math.min(width, limit);
    }
}

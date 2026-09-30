// SPDX-License-Identifier: LGPL-3.0-only
// Litematica GuiSchematicManager layout, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.browser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.GuiScreen;

import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;

public final class GuiSchematicManager extends GuiSchematicBrowser {

    private final List<UiButton[]> groups = new ArrayList<>();
    private final int[] choices = {0, 1, 0};
    private static final String[][] OPTIONS = {
        {"edit_type.rename_schematic", "edit_type.change_author", "edit_type.set_preview"},
        {"export_type.schematic", "export_type.v6_litematic", "export_type.vanilla"},
        {"file_op_type.rename", "file_op_type.copy", "file_op_type.delete"}
    };

    public GuiSchematicManager(GuiScreen parent) {
        super(parent, UiTranslations.format("litematica.gui.title.schematic_manager"), false);
    }

    @Override
    protected void createActions() {
        UiButton edit = unavailable(addButton("litematica.gui.button.schematic_manager.edit_schematic", () -> {}));
        UiButton importFile = unavailable(addButton("litematica.gui.button.import", () -> {}));
        UiButton export = unavailable(addButton("litematica.gui.button.schematic_manager.export_as", () -> {}));
        UiButton operations = addButton("litematica.gui.button.schematic_manager.file_ops", () -> {
            switch (choices[2]) {
                case 0: renameSelectedFile(); break;
                case 1: copySelectedFile(); break;
                case 2: deleteSelectedFile(); break;
                default: break;
            }
        });
        operations.setTooltip(UiTranslations.format("litematica.gui.button.schematic_manager.file_ops.hover"),
            UiTranslations.format("schematica.ui.files.operations_hint"));
        groups.add(new UiButton[] {edit, selector(0)});
        groups.add(new UiButton[] {importFile});
        groups.add(new UiButton[] {export, selector(1)});
        groups.add(new UiButton[] {operations, selector(2)});
        updateVisibility();
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
        return "litematica.gui.label.schematic_manager." + OPTIONS[type][choices[type]];
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
                selector.setTooltip(UiTranslations.format("schematica.ui.pending"), UiTranslations.format(optionKey(type)),
                    UiTranslations.format("schematica.ui.files.selector_hint"));
            }
        }
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

// SPDX-License-Identifier: LGPL-3.0-only
// Litematica task manager layout, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui;

import java.util.Collections;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;

import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiDraw;
import com.github.lunatrius.schematica.client.gui.framework.UiLabel;
import com.github.lunatrius.schematica.client.gui.framework.UiListModel;
import com.github.lunatrius.schematica.client.gui.framework.UiPanel;
import com.github.lunatrius.schematica.client.gui.framework.UiRowList;
import com.github.lunatrius.schematica.client.gui.framework.UiScreen;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.task.TaskRegistry;
import com.github.lunatrius.schematica.task.TaskRegistry.Task;
import com.github.lunatrius.schematica.task.TaskRegistry.Progress;

public final class GuiTaskManager extends UiScreen {
    private final UiListModel<Task> model = new UiListModel<>(22, task -> UiTranslations.format(task.kind.key));
    private List<Task> snapshot = Collections.emptyList();
    private UiRowList<Task> list;
    private UiLabel empty;
    private UiButton menu;

    public GuiTaskManager(GuiScreen parent) { super(parent, UiTranslations.format("litematica.gui.title.task_manager")); }

    @Override protected void createWidgets() {
        list = root.add(new UiRowList<>(model, Entry::new));
        empty = root.add(new UiLabel(() -> UiTranslations.format("schematica.ui.task.empty"), 0xFFB0B0B0));
        menu = addButton("litematica.gui.button.change_menu.to_main_menu", this::mainMenu);
    }

    @Override protected void opened() { tickScreen(); }

    @Override protected void tickScreen() {
        List<Task> tasks = mc.thePlayer == null || mc.theWorld == null ? Collections.emptyList()
            : TaskRegistry.INSTANCE.tasks(mc.thePlayer.getUniqueID(), mc.thePlayer.dimension);
        if (!tasks.equals(snapshot)) {
            snapshot = tasks;
            model.setEntries(tasks);
            list.sync();
        }
        empty.setVisible(tasks.isEmpty());
    }

    @Override protected void layoutWidgets() {
        list.setBounds(14, 32, Math.max(0, width - 24), Math.max(0, height - 68));
        empty.setBounds(18, 38, Math.max(0, width - 36), 12);
        int w = fontRendererObj.getStringWidth(menu.label()) + 10;
        menu.setBounds(width - w - 10, height - 26, w, 20);
    }

    private static String status(Progress progress) {
        String stage = UiTranslations.format(progress.stage.key);
        if (progress.total > 0) stage = UiTranslations.format("schematica.ui.task.progress", stage, progress.completed, progress.total);
        return progress.cancelling ? UiTranslations.format("schematica.ui.task.cancelling", stage) : stage;
    }

    private final class Entry extends UiPanel {
        private final Task task;
        private final int index;
        private final UiButton remove;

        Entry(Task task, int index) {
            this.task = task;
            this.index = index;
            remove = add(new UiButton(() -> UiTranslations.format("litematica.gui.button.remove"), button -> {
                if (button == 0 && mc.thePlayer != null && mc.thePlayer.dimension == task.dimension) {
                    task.cancel(mc.thePlayer.getUniqueID());
                    tick();
                }
            }));
            tick();
        }

        @Override public void tick() {
            Progress progress = task.progress();
            remove.setEnabled(progress.cancellable);
            String hint = UiTranslations.format(task.kind == TaskRegistry.Kind.VERIFIER ? "schematica.ui.task.cancel_verifier"
                : task.kind == TaskRegistry.Kind.SAVE ? "schematica.ui.task.cancel_save"
                : task.kind == TaskRegistry.Kind.REBUILD ? "schematica.ui.task.cancel_rebuild"
                : task.backend == TaskRegistry.Backend.COMMANDS ? "schematica.ui.task.cancel_commands" : "schematica.ui.task.cancel_edit");
            remove.setTooltip(hint);
            String count = task.backend == TaskRegistry.Backend.COMMANDS
                ? UiTranslations.format("schematica.ui.task.sent", progress.affected)
                : task.kind == TaskRegistry.Kind.REBUILD ? UiTranslations.format("schematica.ui.task.schematic_changed", progress.affected)
                : UiTranslations.format("schematica.ui.task.changed", progress.affected, progress.entities);
            if (task.kind == TaskRegistry.Kind.SAVE || task.kind == TaskRegistry.Kind.VERIFIER) setTooltip(UiTranslations.format(task.kind.key), task.detail,
                UiTranslations.format(task.backend.key), status(progress), hint);
            else setTooltip(UiTranslations.format(task.kind.key), task.detail, UiTranslations.format(task.backend.key), status(progress), count, hint);
            super.tick();
        }

        @Override public void layout(UiBounds screen) {
            int w = fontRendererObj.getStringWidth(remove.label()) + 10;
            remove.setBounds(bounds().right() - w, bounds().y + 1, w, 20);
        }

        @Override public void draw(UiDraw draw, int mouseX, int mouseY) {
            draw.fill(bounds(), containsVisible(mouseX, mouseY) ? 0x70FFFFFF : index % 2 == 1 ? 0x20FFFFFF : 0x50FFFFFF);
            String name = UiTranslations.format(task.kind.key) + " - " + status(task.progress());
            draw.text(draw.trim(name, Math.max(0, remove.bounds().x - bounds().x - 8)), bounds().x + 4, bounds().y + 7, 0xFFFFFFFF);
            super.draw(draw, mouseX, mouseY);
        }
    }
}

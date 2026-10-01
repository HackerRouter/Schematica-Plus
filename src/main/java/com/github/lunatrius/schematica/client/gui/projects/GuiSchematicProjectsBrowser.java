// SPDX-License-Identifier: LGPL-3.0-only
// Litematica GuiSchematicProjectsBrowser, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.projects;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import net.minecraft.client.gui.GuiScreen;

import com.github.lunatrius.schematica.client.gui.browser.GuiSchematicBrowser;
import com.github.lunatrius.schematica.client.gui.browser.SchematicBrowserModel;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiDraw;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.projects.SchematicProject;
import com.github.lunatrius.schematica.client.projects.SchematicProjects;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;

public final class GuiSchematicProjectsBrowser extends GuiSchematicBrowser {
    private UiButton manager, create, load, delete, close;
    private SchematicProject selected;

    public GuiSchematicProjectsBrowser(GuiScreen parent) {
        super(parent, UiTranslations.format("litematica.gui.title.schematic_projects_browser"), false);
    }

    @Override
    protected SchematicBrowserModel createModel() throws IOException {
        return new SchematicBrowserModel(ConfigurationHandler.schematicDirectory, name -> name.toLowerCase(Locale.ROOT).endsWith(".json"));
    }

    @Override
    protected void createActions() {
        manager = addAction("litematica.gui.button.schematic_projects.open_manager_gui", () -> {
            SchematicProject project = SchematicProjects.current();
            if (project != null) mc.displayGuiScreen(new GuiSchematicProjectManager(this, project));
        });
        create = addAction("litematica.gui.button.schematic_projects.create_project", this::createProject);
        load = addAction("litematica.gui.button.schematic_projects.load_project", this::loadProject);
        delete = addAction("litematica.gui.button.schematic_projects.delete_project", this::deleteProject);
        close = addAction("litematica.gui.button.schematic_projects.close_project", () -> {
            SchematicProjects.close();
            layoutWidgets();
        });
    }

    private boolean projectFileSelected() {
        SchematicBrowserModel.Entry entry = selection();
        return entry != null && !entry.directory && entry.name().toLowerCase(Locale.ROOT).endsWith(".json");
    }

    @Override
    protected void selectionChanged(SchematicBrowserModel.Entry entry) {
        selected = entry != null && !entry.directory ? SchematicProjects.load(entry.file, false) : null;
        layoutWidgets();
    }

    @Override
    protected void tickScreen() {
        super.tickScreen();
        boolean open = SchematicProjects.current() != null;
        manager.setVisible(open);
        close.setVisible(open);
        load.setVisible(projectFileSelected());
        delete.setVisible(projectFileSelected());
        create.setEnabled(browser != null && browser.directory().isDirectory());
        layoutActions();
    }

    @Override
    protected void layoutActions() {
        int x = 12;
        for (UiButton button : new UiButton[] {manager, create, load, delete, close}) {
            if (!button.isVisible()) continue;
            int w = button.preferredWidth(fontRendererObj.getStringWidth(button.label()));
            button.setBounds(x, height - 26, w, 20);
            x += w + 2;
        }
    }

    private void createProject() {
        if (browser == null) return;
        File directory = browser.directory();
        prompt(UiTranslations.format("litematica.gui.title.create_schematic_project"), "", name -> {
            try {
                SchematicBrowserModel.validateName(name);
            } catch (SchematicBrowserModel.FileOperationException e) {
                return UiTranslations.format(e.translationKey, e.arguments);
            } catch (IOException e) {
                return UiTranslations.format("malilib.message.error.illegal_characters_in_file_name", name);
            }
            if (new File(directory, name + ".json").exists()) {
                return UiTranslations.format("litematica.error.schematic_projects.project_already_exists", name);
            }
            SchematicProjects.create(directory, name);
            refreshFiles();
            setStatus(UiTranslations.format("litematica.message.schematic_projects.project_created", name));
            return null;
        });
    }

    private void loadProject() {
        SchematicBrowserModel.Entry entry = selection();
        if (!projectFileSelected()) return;
        if (SchematicProjects.open(entry.file)) {
            SchematicProject project = SchematicProjects.current();
            if (project == null) return;
            mc.displayGuiScreen(new GuiSchematicProjectManager(this, project));
            SchematicProjects.message(net.minecraft.util.EnumChatFormatting.GREEN, "litematica.message.schematic_projects.project_loaded", project.name());
        } else {
            setStatus(UiTranslations.format("litematica.error.schematic_projects.failed_to_load_project"));
        }
    }

    private void deleteProject() {
        SchematicBrowserModel.Entry entry = selection();
        if (!projectFileSelected()) return;
        confirm(UiTranslations.format("litematica.gui.title.confirm_file_deletion"),
            UiTranslations.format("litematica.gui.message.confirm_file_deletion", entry.name()), () -> {
                try {
                    SchematicProject open = SchematicProjects.current();
                    if (open != null && open.projectFile().getCanonicalFile().equals(entry.file.getCanonicalFile())) SchematicProjects.close();
                    browser.delete(entry);
                    refreshFiles();
                } catch (IOException e) {
                    setStatus(UiTranslations.format("malilib.message.error.failed_to_delete_file", entry.name()));
                }
            });
    }

    @Override
    protected void drawInfo(UiDraw draw, int x, int y, int width, SchematicBrowserModel.Entry entry) {
        SchematicProject project = selected;
        if (project == null) return;
        int color = 0xFFB0B0B0;
        draw.text(UiTranslations.format("litematica.gui.label.schematic_projects.project"), x, y, color);
        draw.text(draw.trim("§f" + project.name(), width - 14), x + 8, y + 12, color);
        int id = project.currentVersionId();
        draw.text(draw.trim(UiTranslations.format("litematica.gui.label.schematic_projects.version",
            "§f" + (id >= 0 ? String.valueOf(id + 1) : "N/A") + "§r", "§f" + project.versionCount() + "§r"), width - 6), x, y + 24, color);
        if (project.currentVersion() != null) {
            draw.text(UiTranslations.format("litematica.gui.label.schematic_projects.origin"), x, y + 36, color);
            Vector3i o = project.origin();
            draw.text(draw.trim(String.format("x: §f%d§r, y: §f%d§r, z: §f%d§r", o.x, o.y, o.z), width - 14), x + 8, y + 48, color);
        }
    }
}

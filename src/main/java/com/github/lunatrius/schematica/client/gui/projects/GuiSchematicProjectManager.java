// SPDX-License-Identifier: LGPL-3.0-only
// Litematica GuiSchematicProjectManager, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.projects;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.MathHelper;

import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiDraw;
import com.github.lunatrius.schematica.client.gui.framework.UiList;
import com.github.lunatrius.schematica.client.gui.framework.UiListModel;
import com.github.lunatrius.schematica.client.gui.framework.UiScreen;
import com.github.lunatrius.schematica.client.gui.framework.UiSprite;
import com.github.lunatrius.schematica.client.gui.framework.UiTextField;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.gui.framework.UiWidget;
import com.github.lunatrius.schematica.client.gui.save.GuiAreaSelectionEditor;
import com.github.lunatrius.schematica.client.projects.SchematicProject;
import com.github.lunatrius.schematica.client.projects.SchematicProjects;
import com.github.lunatrius.schematica.client.projects.SchematicVersion;
import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;

public final class GuiSchematicProjectManager extends UiScreen {
    private static final String WHITE = "§f", RESET = "§r";
    private final GuiScreen parent;
    private final SchematicProject project;
    private final UiListModel<SchematicVersion> versions = new UiListModel<>(16, version ->
        UiTranslations.format("litematica.gui.label.schematic_projects.version_entry", version.version, version.name));
    private UiList<SchematicVersion> list;
    private UiTextField search;
    private UiButton searchButton;
    private UiWidget frame, info;
    private boolean searching;
    private final List<UiButton> top = new ArrayList<>(), bottom = new ArrayList<>();
    private SchematicVersion shown;

    public GuiSchematicProjectManager(GuiScreen parent, SchematicProject project) {
        super(parent, UiTranslations.format("litematica.gui.title.schematic_project_manager"));
        this.parent = parent;
        this.project = project;
    }

    static String date(long time) { return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(time)); }

    @Override
    protected void createWidgets() {
        frame = root.add(new UiWidget() {
            @Override public void draw(UiDraw draw, int mouseX, int mouseY) {
                draw.fill(bounds(), 0xB0000000);
                draw.border(bounds(), 0xFF999999);
            }
        });
        search = root.add(new UiTextField(fontRendererObj, 256, versions::setQuery));
        searchButton = root.add(new UiButton(() -> "", button -> {
            searching = !searching;
            if (!searching) search.setText("");
            layoutWidgets();
            if (searching) input.focus(search);
        }).setSprite(UiSprite.SEARCH).setBackground(false));
        list = root.add(new UiList<>(versions, "", version -> {}));
        list.setEntryTooltip(this::hover);
        info = root.add(new UiWidget() {
            @Override public void draw(UiDraw draw, int mouseX, int mouseY) {
                draw.fill(bounds(), 0xA0000000);
                draw.border(bounds(), 0xFF999999);
                drawInfo(draw, bounds().x + 4, bounds().y + 4, bounds().width - 8);
            }
        });
        top.add(button("save_version", "save_new_version", () -> mc.displayGuiScreen(new GuiVersionPrompt(this, project))));
        top.add(button("open_area_editor", null, () -> mc.displayGuiScreen(new GuiAreaSelectionEditor(this))));
        top.add(button("move_origin_to_player", "move_origin_to_player", () -> {
            if (mc.thePlayer == null) return;
            SchematicProjects.setOrigin(new Vector3i(MathHelper.floor_double(mc.thePlayer.posX),
                MathHelper.floor_double(mc.thePlayer.boundingBox.minY), MathHelper.floor_double(mc.thePlayer.posZ)));
        }));
        top.add(button("place_to_world", "place_to_world_warning", () -> confirm(
            UiTranslations.format("litematica.gui.title.schematic_projects.confirm_place_to_world"),
            UiTranslations.format("litematica.gui.message.schematic_projects.confirm_place_to_world"), SchematicProjects::pasteCurrentVersionToWorld)));
        top.add(button("delete_area", "delete_area", () -> confirm(
            UiTranslations.format("litematica.gui.title.schematic_projects.confirm_delete_area"),
            UiTranslations.format("litematica.gui.message.schematic_projects.confirm_delete_area"), SchematicProjects::deleteLastSeenArea)));
        bottom.add(button("open_project_browser", null, () -> mc.displayGuiScreen(new GuiSchematicProjectsBrowser(this))));
        bottom.add(button("close_project", null, () -> {
            SchematicProjects.close();
            mc.displayGuiScreen(new GuiSchematicProjectsBrowser(parent));
        }));
        versionsChanged();
    }

    private UiButton button(String key, String hover, Runnable action) {
        UiButton button = addButton("litematica.gui.button.schematic_projects." + key, action);
        if (hover != null) button.setTooltip(UiTranslations.format("litematica.gui.button.hover.schematic_projects." + hover).split("\n"));
        return button;
    }

    /** onTaskCompleted: refreshes the list after a version was saved. */
    public void versionsChanged() {
        versions.setEntries(project.versions());
        selectCurrent();
    }

    private void selectCurrent() {
        shown = project.currentVersion();
        int index = versions.entries().indexOf(shown);
        if (index >= 0) versions.select(index);
    }

    private List<String> hover(SchematicVersion version) {
        List<String> lines = new ArrayList<>();
        lines.add(UiTranslations.format("litematica.gui.label.schematic_projects.version_hover.entry", version.version, version.name));
        lines.add(UiTranslations.format("litematica.gui.label.schematic_projects.version_hover.timestamp", date(version.timeStamp)));
        if (!version.description.isEmpty()) {
            lines.add(UiTranslations.format("litematica.gui.label.schematic_projects.version_hover.description"));
            for (String line : version.description.split("\n")) lines.add("  §f" + line);
        }
        return lines;
    }

    private void drawInfo(UiDraw draw, int x, int y, int width) {
        int color = 0xFFB0B0B0;
        draw.text(UiTranslations.format("litematica.gui.label.schematic_projects.project"), x, y, color);
        draw.text(draw.trim(WHITE + project.name() + RESET, width - 4), x + 4, y += 12, color);
        int id = project.currentVersionId();
        draw.text(draw.trim(UiTranslations.format("litematica.gui.label.schematic_projects.version",
            WHITE + (id >= 0 ? String.valueOf(id + 1) : "N/A") + RESET, WHITE + project.versionCount() + RESET), width), x, y += 12, color);
        SchematicVersion version = project.currentVersion();
        if (version == null) return;
        draw.text(draw.trim(UiTranslations.format("litematica.hud.schematic_projects.current_version_date", WHITE + date(version.timeStamp) + RESET), width), x, y += 12, color);
        draw.text(UiTranslations.format("litematica.gui.label.schematic_projects.version_name"), x, y += 12, color);
        draw.text(draw.trim(WHITE + version.name + RESET, width - 4), x + 4, y += 12, color);
        draw.text(UiTranslations.format("litematica.gui.label.schematic_projects.origin"), x, y += 12, color);
        Vector3i o = project.origin();
        draw.text(draw.trim(String.format("x: %s%d%s, y: %s%d%s, z: %s%d%s", WHITE, o.x, RESET, WHITE, o.y, RESET, WHITE, o.z, RESET), width), x, y += 12, color);
        if (version.description.isEmpty()) return;
        draw.text(UiTranslations.format("litematica.gui.label.schematic_projects.version_description"), x, y += 12, color);
        for (String line : fontRendererObj.listFormattedStringToWidth(version.description, Math.max(1, width - 8))) {
            if (y + 24 > info.bounds().bottom()) break;
            draw.text(WHITE + "  " + line, x, y += 12, color);
        }
    }

    @Override
    protected void tickScreen() {
        if (SchematicProjects.current() != project) {
            mc.displayGuiScreen(new GuiSchematicProjectsBrowser(parent));
            return;
        }
        SchematicVersion selected = versions.selected();
        if (selected != null && selected != shown) {
            SchematicProjects.switchVersion(selected);
            shown = project.currentVersion();
        } else if (project.currentVersion() != shown) {
            selectCurrent();
        }
        top.get(3).setEnabled(project.currentPlacement() != null);
    }

    @Override
    protected void layoutWidgets() {
        int infoWidth = 180;
        int listWidth = Math.max(40, width - 20 - infoWidth - 6);
        int listHeight = Math.max(30, height - 74);
        frame.setBounds(10, 24, listWidth, listHeight);
        searchButton.setBounds(12, 28, 12, 12);
        search.setBounds(28, 27, listWidth - 22, 14);
        search.setVisible(searching);
        list.setBounds(12, 45, listWidth - 4, listHeight - 23);
        info.setBounds(10 + listWidth + 6, 24, infoWidth, Math.min(200, listHeight));
        layoutRow(top, height - 46);
        layoutRow(bottom, height - 24);
    }

    private void layoutRow(List<UiButton> buttons, int y) {
        int x = 10;
        for (UiButton button : buttons) {
            int w = button.preferredWidth(fontRendererObj.getStringWidth(button.label()));
            button.setBounds(x, y, w, 20);
            x += w + 2;
        }
    }
}

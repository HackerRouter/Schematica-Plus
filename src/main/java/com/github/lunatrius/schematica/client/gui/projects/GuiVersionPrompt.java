// SPDX-License-Identifier: LGPL-3.0-only
// MaLiLib GuiTextInputStackedMultiLine, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.projects;

import net.minecraft.client.gui.GuiScreen;

import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiLabel;
import com.github.lunatrius.schematica.client.gui.framework.UiScreen;
import com.github.lunatrius.schematica.client.gui.framework.UiTextField;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.projects.SchematicProject;
import com.github.lunatrius.schematica.client.projects.SchematicProjects;
import com.github.lunatrius.schematica.client.projects.SchematicVersion;

/** Save new schematic version: a name and a description for the next project version. */
public final class GuiVersionPrompt extends UiScreen {
    private final SchematicProject project;
    private UiLabel nameLabel, descriptionLabel;
    private UiTextField name, description;
    private UiButton ok, reset, cancel;

    public GuiVersionPrompt(GuiScreen parent, SchematicProject project) {
        super(parent, UiTranslations.format("litematica.gui.title.schematic_projects.save_new_version"));
        this.project = project;
    }

    @Override
    protected void createWidgets() {
        nameLabel = root.add(new UiLabel(() -> UiTranslations.format("litematica.gui.label.schematic_projects.version_name")));
        name = root.add(new UiTextField(fontRendererObj, SchematicVersion.MAX_DESCRIPTION_LENGTH, value -> {}));
        descriptionLabel = root.add(new UiLabel(() -> UiTranslations.format("litematica.gui.label.schematic_projects.version_description")));
        description = root.add(new UiTextField(fontRendererObj, SchematicVersion.MAX_DESCRIPTION_LENGTH, value -> {}));
        ok = addButton("malilib.gui.button.ok", this::submit);
        reset = addButton("malilib.gui.button.reset", this::fill);
        cancel = addButton("malilib.gui.button.cancel", this::closeScreen);
        fill();
    }

    private void fill() {
        name.setText(project.currentVersionName());
        description.setText(project.currentVersionDescription());
    }

    private void submit() {
        if (SchematicProjects.current() == project && SchematicProjects.commitNewVersion(name.text(), description.text())) closeScreen();
    }

    @Override
    protected void opened() { input.focus(name); }

    @Override
    protected void layoutWidgets() {
        int w = Math.max(80, Math.min(320, width - 20));
        int x = (width - w) / 2, y = Math.max(24, height / 2 - 60);
        nameLabel.setBounds(x, y, w, 12);
        name.setBounds(x, y + 12, w, 20);
        descriptionLabel.setBounds(x, y + 38, w, 12);
        description.setBounds(x, y + 50, w, 20);
        int bx = x;
        for (UiButton button : new UiButton[] {ok, reset, cancel}) {
            int bw = Math.max(40, fontRendererObj.getStringWidth(button.label()) + 10);
            button.setBounds(bx, y + 80, bw, 20);
            bx += bw + 2;
        }
    }
}

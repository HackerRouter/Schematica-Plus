package com.github.lunatrius.schematica.client.gui.browser;

import java.io.File;
import java.io.IOException;

import net.minecraft.client.gui.GuiScreen;

import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiLabel;
import com.github.lunatrius.schematica.client.gui.framework.UiScreen;
import com.github.lunatrius.schematica.client.gui.framework.UiTextArea;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.world.schematic.SchematicFiles;
import com.github.lunatrius.schematica.world.schematic.SchematicMetadata;

public final class GuiSchematicDescription extends UiScreen {
    private final File file;
    private final String initial;
    private UiTextArea text;
    private UiButton ok, reset, cancel;
    private UiLabel error;
    private String failure = "";

    public GuiSchematicDescription(GuiScreen parent, File file, String description) {
        super(parent, UiTranslations.format("schematica.ui.files.edit_type.change_description"));
        this.file = file;
        initial = description;
    }

    @Override protected void createWidgets() {
        text = root.add(new UiTextArea(fontRendererObj, Math.max(SchematicMetadata.MAX_DESCRIPTION, initial.length()), Integer.MAX_VALUE, value -> {}));
        text.setText(initial);
        error = root.add(new UiLabel(() -> failure));
        ok = addButton("malilib.gui.button.ok", () -> {
            try {
                SchematicFileEdits.edit(file, metadata -> metadata.setString("Description", text.text()));
                closeScreen();
            } catch (IOException e) {
                failure = UiTranslations.format("litematica.error.schematic_load.cant_read_file", file.getName());
                Reference.logger.warn("Could not edit schematic description", e);
            }
        });
        reset = addButton("malilib.gui.button.reset", () -> text.setText(initial));
        cancel = addButton("malilib.gui.button.cancel", this::closeScreen);
    }

    @Override protected void opened() { input.focus(text); }

    @Override protected void layoutWidgets() {
        int w = Math.max(100, Math.min(420, width - 20)), x = (width - w) / 2;
        text.setBounds(x, 28, w, Math.max(30, height - 88));
        error.setBounds(x, height - 55, w, 18);
        int right = x + w;
        for (UiButton button : new UiButton[] {cancel, ok}) {
            int bw = Math.max(50, fontRendererObj.getStringWidth(button.label()) + 12);
            right -= bw;
            button.setBounds(right, height - 28, bw, 20);
            right -= 4;
        }
        reset.setBounds(x, height - 28, Math.max(50, fontRendererObj.getStringWidth(reset.label()) + 12), 20);
    }
}

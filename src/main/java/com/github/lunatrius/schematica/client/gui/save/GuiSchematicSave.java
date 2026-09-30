// SPDX-License-Identifier: LGPL-3.0-only
// Litematica save layout, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.save;

import java.io.File;
import java.io.IOException;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;

import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Area;
import com.github.lunatrius.schematica.client.selection.AreaSelections;
import com.github.lunatrius.schematica.client.gui.browser.GuiSchematicBrowser;
import com.github.lunatrius.schematica.client.gui.browser.SchematicBrowserModel;
import com.github.lunatrius.schematica.client.gui.framework.UiCheckBox;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiScreen;
import com.github.lunatrius.schematica.client.gui.framework.UiTextField;
import com.github.lunatrius.schematica.client.gui.framework.UiToggleButton;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.handler.QueueTickHandler;
import com.github.lunatrius.schematica.handler.client.WorldHandler;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.world.storage.RegionSelection;
import com.github.lunatrius.schematica.world.schematic.SchematicFormat;

public final class GuiSchematicSave extends GuiSchematicBrowser {

    private boolean extended = ConfigurationHandler.useSchematicplusFormat;
    private final String initialName;
    private final AreaSelectionLibrary library = AreaSelections.library();
    private final Area area = library.selected();
    private final World sourceWorld = Minecraft.getMinecraft().theWorld;
    private UiTextField name;
    private UiButton save;
    private UiButton options;
    private final UiCheckBox[] checkboxes = new UiCheckBox[4];
    private String problem = "";

    public GuiSchematicSave(GuiScreen parent) { this(parent, AreaSelections.library().selected() == null ? "" : AreaSelections.library().selected().name()); }

    public GuiSchematicSave(GuiScreen parent, String initialName) {
        super(parent, UiTranslations.format("litematica.gui.title.create_schematic_from_selection"), false);
        this.initialName = initialName;
    }

    @Override
    protected int browserX() { return 10; }
    @Override
    protected int browserY() { return 80; }
    @Override
    protected int browserHeight() { return height - 80; }
    @Override
    protected boolean showFooter() { return false; }

    @Override
    protected void createActions() {
        name = root.add(new UiTextField(fontRendererObj, 210, text -> {}));
        name.setText(initialName);
        save = addButton("litematica.gui.button.save_schematic", this::saveSelection);
        options = addButton("schematica.ui.save.options", () -> mc.displayGuiScreen(new SaveOptions()));
        String[] keys = {"ignore_entities", "save_from_schematic_world", "visible_blocks_only", "support_blocks"};
        for (int i = 0; i < keys.length; i++) {
            final int index = i;
            checkboxes[i] = root.add(new UiCheckBox(() -> UiTranslations.format("litematica.gui.label.schematic_save.checkbox." + keys[index]),
                () -> index == 0 && !SchematicFormat.saveEntities,
                value -> { if (index == 0) SchematicFormat.saveEntities = !value; }));
            if (i != 0) unavailable(checkboxes[i]);
        }
    }

    @Override
    protected void activateFile(SchematicBrowserModel.Entry entry) {
        name.setText(entry.name().substring(0, entry.name().lastIndexOf('.')));
    }

    @Override
    protected void selectionChanged(SchematicBrowserModel.Entry entry) {
        if (entry != null && !entry.directory) activateFile(entry);
    }

    private File directory() { return browser == null ? ConfigurationHandler.schematicDirectory : browser.directory(); }

    private boolean selectionContext() {
        return AreaSelections.available(library) && area != null && library.selected() == area && mc.theWorld == sourceWorld;
    }

    private String validateSelection() {
        if (!selectionContext()) return UiTranslations.format("litematica.message.error.schematic_save_no_area_selected");
        if (mc.theWorld == null || mc.thePlayer == null || !SchematicaPlus.proxy.isSaveEnabled) {
            return UiTranslations.format("schematica.ui.save.disabled");
        }
        if (!ClientProxy.isRenderingGuide) return UiTranslations.format("schematica.ui.save.enable_guide");
        try {
            area.snapshot();
        } catch (IllegalArgumentException e) {
            return UiTranslations.format("schematica.ui.save.invalid_selection");
        }
        if (name.text().trim().isEmpty()) return UiTranslations.format("schematica.ui.save.enter_name");
        try {
            SchematicSaveTarget.filename(name.text(), extended);
        } catch (IllegalArgumentException e) {
            return UiTranslations.format("schematica.ui.save.invalid_name");
        }
        if (!directory().isDirectory()) return UiTranslations.format("schematica.ui.save.invalid_directory");
        return "";
    }

    @Override
    protected void opened() {
        ClientProxy.updatePoints();
        super.opened();
    }

    @Override
    protected void tickScreen() {
        super.tickScreen();
        problem = validateSelection();
        save.setEnabled(problem.isEmpty());
        save.setTooltip(problem.isEmpty() ? UiTranslations.format(area.boxes().size() > 1
            ? "schematica.ui.area.extended" : "litematica.gui.button.save_schematic") : problem);
    }

    private void saveSelection() {
        tickScreen();
        if (!problem.isEmpty()) return;
        if (!QueueTickHandler.INSTANCE.canQueue(mc.thePlayer)) {
            setStatus(UiTranslations.format("schematica.ui.save.busy"));
            return;
        }
        try {
            File file = SchematicSaveTarget.resolve(ConfigurationHandler.schematicDirectory, directory(), name.text(), extended);
            World world = mc.theWorld;
            AreaSelections.capture();
            RegionSelection selection = area.snapshot();
            Runnable submit = () -> submit(file, world, selection);
            if (file.exists()) {
                confirm(UiTranslations.format("schematica.ui.save.overwrite_title"),
                    UiTranslations.format("schematica.ui.save.overwrite", file.getName()), submit);
            } else submit.run();
        } catch (IOException | IllegalArgumentException e) {
            Reference.logger.error("Invalid schematic save target", e);
            setStatus(UiTranslations.format("schematica.ui.save.invalid_directory"));
        }
    }

    private void submit(File file, World world, RegionSelection selection) {
        if (!selectionContext() || mc.theWorld != world || mc.thePlayer == null || !SchematicaPlus.proxy.isSaveEnabled) {
            setStatus(UiTranslations.format("schematica.ui.save.disabled"));
            return;
        }
        if (SchematicaPlus.proxy.saveSchematic(mc.thePlayer, file.getParentFile(), file.getName(), world, selection)) {
            WorldHandler.INSTANCE.saveSession();
            setStatus(UiTranslations.format("schematica.ui.save.queued", file.getName()));
        } else {
            setStatus(UiTranslations.format("schematica.ui.save.failed"));
        }
        tickScreen();
    }

    @Override
    protected void layoutActions() {
        name.setBounds(10, 32, Math.max(1, width - 260), 18);
        int saveWidth = fontRendererObj.getStringWidth(save.label()) + 10;
        save.setBounds(10, 54, saveWidth, 20);
        options.setBounds(14 + saveWidth, 54, fontRendererObj.getStringWidth(options.label()) + 10, 20);
        for (int i = 0; i < checkboxes.length; i++) checkboxes[i].setBounds(name.bounds().right() + 4, 28 + i * 12, 240, 11);
    }

    private final class SaveOptions extends UiScreen {
        private UiButton format;
        private UiToggleButton nbt;
        private UiToggleButton guide;
        private UiButton back;

        SaveOptions() { super(GuiSchematicSave.this, UiTranslations.format("schematica.ui.save.options")); }

        @Override
        protected void createWidgets() {
            format = root.add(new UiButton(() -> extended ? ".schemplus" : ".schematic", button -> extended = !extended));
            format.setTooltip(UiTranslations.format("schematica.ui.save.format_hint"));
            nbt = root.add(new UiToggleButton(() -> UiTranslations.format("schematica.gui.savenbt"), () -> SchematicFormat.saveNBT,
                value -> SchematicFormat.saveNBT = value));
            guide = root.add(new UiToggleButton(() -> UiTranslations.format("schematica.ui.save.guide"), () -> ClientProxy.isRenderingGuide,
                value -> { if (selectionContext()) { ClientProxy.isRenderingGuide = value; AreaSelections.saveCurrent(); } }));
            back = addButton("gui.back", this::closeScreen);
        }

        @Override
        protected void layoutWidgets() {
            format.setBounds(12, 30, 160, 20);
            nbt.setBounds(12, 52, 160, 20);
            guide.setBounds(12, 74, 160, 20);
            back.setBounds(12, height - 26, 80, 20);
        }
    }
}

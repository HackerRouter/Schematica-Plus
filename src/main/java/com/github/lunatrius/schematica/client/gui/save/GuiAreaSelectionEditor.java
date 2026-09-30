// SPDX-License-Identifier: LGPL-3.0-only
// Litematica simple area editor layout, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.save;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.world.World;

import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiCheckBox;
import com.github.lunatrius.schematica.client.gui.framework.UiIntegerField;
import com.github.lunatrius.schematica.client.gui.framework.UiLabel;
import com.github.lunatrius.schematica.client.gui.framework.UiScreen;
import com.github.lunatrius.schematica.client.gui.framework.UiSprite;
import com.github.lunatrius.schematica.client.gui.framework.UiTextField;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Area;
import com.github.lunatrius.schematica.client.selection.AreaSelections;
import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;

public final class GuiAreaSelectionEditor extends UiScreen {
    private final AreaSelectionLibrary library = AreaSelections.library();
    private final Area area = library.selected();
    private final World world = Minecraft.getMinecraft().theWorld;
    private String message = "";
    private boolean syncing;
    private UiLabel status;
    private UiButton setName;
    private UiButton setBox;
    private final List<UiButton> coordinateButtons = new ArrayList<>();
    private UiTextField name;
    private UiTextField boxName;
    private UiButton selectionMode;
    private UiButton cornerMode;
    private UiButton origin;
    private UiCheckBox guide;
    private UiButton save;
    private final UiIntegerField[][] coordinates = new UiIntegerField[2][3];

    public GuiAreaSelectionEditor(GuiScreen parent) {
        super(parent, UiTranslations.format("litematica.gui.title.area_editor_simple"));
    }

    private UiButton fixed(String key, Object... arguments) {
        return unavailable(root.add(new UiButton(() -> UiTranslations.format(key, arguments), button -> {})));
    }

    private void label(String key, int x, int y) {
        root.add(new UiLabel(() -> UiTranslations.format(key))).setBounds(x, y, 202, 12);
    }

    @Override
    protected void createWidgets() {
        selectionMode = fixed("litematica.gui.button.area_editor.change_selection_mode", UiTranslations.format("litematica.gui.label.area_selection.mode.simple"));
        selectionMode.setTooltip(UiTranslations.format("schematica.ui.area.single_box"));
        cornerMode = fixed("litematica.gui.button.area_editor.change_corner_mode", UiTranslations.format("litematica.hud.area_selection.mode.corners"));
        origin = fixed("litematica.gui.button.area_editor.origin_enabled", "\u00a7c" + UiTranslations.format("options.off"));
        label("litematica.gui.label.area_editor.selection_name", 12, 44);
        name = root.add(new UiTextField(fontRendererObj, 200, text -> {}));
        name.setBounds(12, 59, 202, 16);
        setName = addButton("litematica.gui.button.area_editor.set_selection_name", () -> change(() -> {
            library.rename(area, name.text());
            name.setText(area.name());
        }));
        setName.setBounds(218, 57, fontRendererObj.getStringWidth(setName.label()) + 10, 20);
        label("litematica.gui.label.area_editor.box_name", 12, 77);
        boxName = root.add(new UiTextField(fontRendererObj, 200, text -> {}));
        boxName.setBounds(12, 92, 202, 16);
        setBox = addButton("litematica.gui.button.area_editor.set_box_name", () -> change(() -> {
            library.renameBox(area, boxName.text());
            boxName.setText(area.boxName());
        }));
        setBox.setBounds(218, 90, fontRendererObj.getStringWidth(setBox.label()) + 10, 20);
        for (int point = 0; point < 2; point++) {
            final int index = point;
            int x = 12 + point * 110;
            UiCheckBox corner = root.add(new UiCheckBox(() -> UiTranslations.format("litematica.gui.label.area_editor.corner_" + (index + 1)),
                () -> false, value -> {}));
            unavailable(corner).setBounds(x, 113, 100, 11);
            for (int axis = 0; axis < 3; axis++) {
                final int component = axis;
                root.add(new UiLabel(() -> "XYZ".charAt(component) + ":")).setBounds(x, 124 + axis * 20, 12, 20);
                UiIntegerField field = root.add(new UiIntegerField(fontRendererObj, 0, axis == 1 ? 0 : -30000000, axis == 1 ? 255 : 29999999,
                    value -> {
                        if (syncing) return;
                        change(() -> {
                            Vector3i target = point(index);
                            if (component == 0) target.x = value;
                            else if (component == 1) target.y = value;
                            else target.z = value;
                            library.setPoints(area, index == 0 ? target : area.first(), index == 1 ? target : area.second());
                        });
                    }));
                coordinates[point][axis] = field;
                field.setBounds(x + 12, 126 + axis * 20, 68, 16);
                UiButton nudge = root.add(new UiButton(() -> "", button -> field.setValue((long) field.value() + (button == 0 ? 1 : -1)))
                    .setSprite(UiSprite.PLUS_MINUS).setBackground(false));
                coordinateButtons.add(nudge);
                nudge.setBounds(x + 84, 126 + axis * 20, 16, 16);
                nudge.setTooltip(UiTranslations.format("schematica.ui.save.coordinate_hint"));
            }
            UiButton move = addButton("litematica.gui.button.move_to_player", () -> change(() -> {
                Vector3i target = GuiAreaSelectionManager.playerPoint();
                library.setPoints(area, index == 0 ? target : area.first(), index == 1 ? target : area.second());
                sync();
            }));
            coordinateButtons.add(move);
            move.setBounds(x + 10, 186, fontRendererObj.getStringWidth(move.label()) + 10, 20);
        }
        save = addButton("litematica.gui.button.area_editor.create_schematic",
            () -> { if (available()) mc.displayGuiScreen(new GuiSchematicSave(this, area.name())); });
        save.setBounds(22, 208, fontRendererObj.getStringWidth(save.label()) + 10, 20);
        UiButton analyze = unavailable(addButton("litematica.gui.button.area_editor.analyze_area", () -> {}));
        analyze.setBounds(132, 208, fontRendererObj.getStringWidth(analyze.label()) + 10, 20);
        guide = root.add(new UiCheckBox(() -> UiTranslations.format("schematica.ui.save.guide"), () -> area != null && area.guide(),
            value -> change(() -> library.setGuide(area, value))));
        guide.setBounds(250, 48, 160, 11);
        status = root.add(new UiLabel(this::statusText, 0xFFFFA0A0));
    }

    private boolean available() {
        return mc.theWorld == world && world != null && mc.thePlayer != null && AreaSelections.available(library)
            && area != null && library.selected() == area && SchematicaPlus.proxy.isSaveEnabled;
    }

    private void change(Runnable action) {
        if (!available()) return;
        try {
            action.run();
            AreaSelections.apply();
            AreaSelections.saveCurrent();
            message = "";
        } catch (IllegalArgumentException e) {
            message = GuiAreaSelectionManager.error(e);
        }
    }

    private Vector3i point(int index) { return area == null ? new Vector3i() : index == 0 ? area.first() : area.second(); }

    private void sync() {
        syncing = true;
        for (int index = 0; index < 2; index++) {
            Vector3i point = point(index);
            coordinates[index][0].setValue(point.x);
            coordinates[index][1].setValue(point.y);
            coordinates[index][2].setValue(point.z);
        }
        syncing = false;
    }

    @Override protected void opened() {
        if (available()) AreaSelections.capture();
        name.setText(area == null ? "" : area.name());
        boxName.setText(area == null ? "" : area.boxName());
        sync();
    }

    @Override protected void closed() { if (available()) AreaSelections.saveCurrent(); }

    private String statusText() {
        if (!available()) return UiTranslations.format("litematica.error.area_editor.no_selection");
        if (AreaSelections.saveFailed()) return UiTranslations.format("schematica.ui.area.persistence_failed");
        return message;
    }

    @Override protected void tickScreen() {
        boolean enabled = available();
        guide.setEnabled(enabled);
        save.setEnabled(enabled);
        name.setEnabled(enabled);
        boxName.setEnabled(enabled);
        setName.setEnabled(enabled);
        setBox.setEnabled(enabled);
        for (UiButton button : coordinateButtons) button.setEnabled(enabled);
        for (UiIntegerField[] point : coordinates) for (UiIntegerField field : point) field.setEnabled(enabled);
        status.setTooltip(statusText());
    }

    @Override protected void layoutWidgets() {
        int x = 10;
        for (UiButton button : new UiButton[] {selectionMode, cornerMode, origin}) {
            int w = fontRendererObj.getStringWidth(button.label()) + 10;
            button.setBounds(x, 24, w, 20);
            x += w + 4;
        }
        status.setBounds(12, height - 12, width - 24, 12);
    }
}

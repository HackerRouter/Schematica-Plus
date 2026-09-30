// SPDX-License-Identifier: LGPL-3.0-only
// Litematica subregion editor layout, adapted for 1.7.10 by HackerRouter, 2026.
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
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Box;
import com.github.lunatrius.schematica.client.selection.AreaSelections;
import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;

public final class GuiSubRegionEditor extends UiScreen {
    private final AreaSelectionLibrary library = AreaSelections.library();
    private final Area area = library.selected();
    private final Box box;
    private final World world = Minecraft.getMinecraft().theWorld;
    private String message = "";
    private boolean syncing;
    private UiLabel status;
    private UiButton setBox;
    private final List<UiButton> coordinateButtons = new ArrayList<>();
    private UiTextField boxName;
    private final List<UiCheckBox> corners = new ArrayList<>();
    private final UiIntegerField[][] coordinates = new UiIntegerField[2][3];

    public GuiSubRegionEditor(GuiScreen parent, Box box) {
        super(parent, UiTranslations.format("litematica.gui.title.area_editor_sub_region"));
        this.box = box;
    }

    private void label(String key, int x, int y) {
        root.add(new UiLabel(() -> UiTranslations.format(key))).setBounds(x, y, 202, 12);
    }

    @Override
    protected void createWidgets() {
        label("litematica.gui.label.area_editor.box_name", 12, 24);
        boxName = root.add(new UiTextField(fontRendererObj, 200, text -> {}));
        boxName.setBounds(12, 39, 202, 16);
        setBox = addButton("litematica.gui.button.area_editor.set_box_name", () -> change(() -> {
            library.renameBox(area, box, boxName.text());
            boxName.setText(box.name());
        }));
        setBox.setBounds(218, 37, fontRendererObj.getStringWidth(setBox.label()) + 10, 20);
        for (int point = 0; point < 2; point++) {
            final int index = point;
            int x = 12 + point * 110;
            UiCheckBox corner = root.add(new UiCheckBox(() -> UiTranslations.format("litematica.gui.label.area_editor.corner_" + (index + 1)),
                () -> area.selectedBox() == box && !area.originSelected() && area.selectedCorner() == (index == 0 ? AreaSelectionLibrary.Corner.FIRST : AreaSelectionLibrary.Corner.SECOND),
                value -> change(() -> library.selectCorner(area, box, !value ? AreaSelectionLibrary.Corner.NONE
                    : index == 0 ? AreaSelectionLibrary.Corner.FIRST : AreaSelectionLibrary.Corner.SECOND))));
            corner.setBounds(x, 60, 100, 11);
            corners.add(corner);
            for (int axis = 0; axis < 3; axis++) {
                final int component = axis;
                root.add(new UiLabel(() -> "XYZ".charAt(component) + ":")).setBounds(x, 71 + axis * 20, 12, 20);
                UiIntegerField field = root.add(new UiIntegerField(fontRendererObj, 0, axis == 1 ? 0 : -30000000, axis == 1 ? 255 : 29999999,
                    value -> {
                        if (syncing) return;
                        change(() -> {
                            Vector3i target = point(index);
                            if (component == 0) target.x = value;
                            else if (component == 1) target.y = value;
                            else target.z = value;
                            library.setPoints(area, box, index == 0 ? target : box.first(), index == 1 ? target : box.second());
                        });
                    }));
                coordinates[point][axis] = field;
                field.setBounds(x + 12, 73 + axis * 20, 68, 16);
                UiButton nudge = root.add(new UiButton(() -> "", button -> field.setValue((long) field.value() + (button == 0 ? 1 : -1)))
                    .setSprite(UiSprite.PLUS_MINUS).setBackground(false));
                coordinateButtons.add(nudge);
                nudge.setBounds(x + 84, 73 + axis * 20, 16, 16);
                nudge.setTooltip(UiTranslations.format("schematica.ui.save.coordinate_hint"));
            }
            UiButton move = addButton("litematica.gui.button.move_to_player", () -> change(() -> {
                Vector3i target = GuiAreaSelectionManager.playerPoint();
                library.setPoints(area, box, index == 0 ? target : box.first(), index == 1 ? target : box.second());
                sync();
            }));
            coordinateButtons.add(move);
            move.setBounds(x + 10, 133, fontRendererObj.getStringWidth(move.label()) + 10, 20);
        }
        status = root.add(new UiLabel(this::statusText, 0xFFFFA0A0));
    }

    private boolean available() {
        return mc.theWorld == world && world != null && mc.thePlayer != null && AreaSelections.available(library)
            && area != null && library.selected() == area && area.boxes().contains(box) && SchematicaPlus.proxy.isSaveEnabled;
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

    private Vector3i point(int index) { return index == 0 ? box.first() : box.second(); }

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
        boxName.setText(box.name());
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
        boxName.setEnabled(enabled);
        setBox.setEnabled(enabled);
        for (UiCheckBox corner : corners) corner.setEnabled(enabled);
        for (UiButton button : coordinateButtons) button.setEnabled(enabled);
        for (UiIntegerField[] point : coordinates) for (UiIntegerField field : point) field.setEnabled(enabled);
        status.setTooltip(statusText());
    }

    @Override protected void layoutWidgets() {
        status.setBounds(12, height - 12, width - 24, 12);
    }
}

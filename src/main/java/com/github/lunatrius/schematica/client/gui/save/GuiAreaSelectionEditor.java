// SPDX-License-Identifier: LGPL-3.0-only
// Litematica simple area editor layout, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.save;

import net.minecraft.client.gui.GuiScreen;

import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiCheckBox;
import com.github.lunatrius.schematica.client.gui.framework.UiIntegerField;
import com.github.lunatrius.schematica.client.gui.framework.UiLabel;
import com.github.lunatrius.schematica.client.gui.framework.UiScreen;
import com.github.lunatrius.schematica.client.gui.framework.UiSprite;
import com.github.lunatrius.schematica.client.gui.framework.UiTextField;
import com.github.lunatrius.schematica.handler.client.WorldHandler;
import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;
import com.github.lunatrius.schematica.proxy.ClientProxy;

public final class GuiAreaSelectionEditor extends UiScreen {
    private String selectionName = "";
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
        cornerMode = fixed("litematica.gui.button.area_editor.change_corner_mode", UiTranslations.format("litematica.hud.area_selection.mode.corners"));
        origin = fixed("litematica.gui.button.area_editor.origin_enabled", "\u00a7c" + UiTranslations.format("options.off"));
        label("litematica.gui.label.area_editor.selection_name", 12, 44);
        name = root.add(new UiTextField(fontRendererObj, 200, text -> {}));
        name.setBounds(12, 59, 202, 16);
        UiButton setName = addButton("litematica.gui.button.area_editor.set_selection_name", () -> {
            selectionName = name.text();
            boxName.setText(selectionName);
        });
        setName.setBounds(218, 57, fontRendererObj.getStringWidth(setName.label()) + 10, 20);
        label("litematica.gui.label.area_editor.box_name", 12, 77);
        boxName = root.add(new UiTextField(fontRendererObj, 200, text -> {}));
        boxName.setBounds(12, 92, 202, 16);
        UiButton setBox = addButton("litematica.gui.button.area_editor.set_box_name", () -> {
            selectionName = boxName.text();
            name.setText(selectionName);
        });
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
                UiIntegerField field = root.add(new UiIntegerField(fontRendererObj, 0, Integer.MIN_VALUE, Integer.MAX_VALUE,
                    value -> {
                        Vector3i target = point(index);
                        if (component == 0) target.x = value;
                        else if (component == 1) target.y = value;
                        else target.z = value;
                        ClientProxy.updatePoints();
                    }));
                coordinates[point][axis] = field;
                field.setBounds(x + 12, 126 + axis * 20, 68, 16);
                UiButton nudge = root.add(new UiButton(() -> "", button -> field.setValue((long) field.value() + (button == 0 ? 1 : -1)))
                    .setSprite(UiSprite.PLUS_MINUS).setBackground(false));
                nudge.setBounds(x + 84, 126 + axis * 20, 16, 16);
                nudge.setTooltip(UiTranslations.format("schematica.ui.save.coordinate_hint"));
            }
            UiButton move = addButton("litematica.gui.button.move_to_player", () -> {
                if (mc.thePlayer != null) { ClientProxy.movePointToPlayer(point(index)); sync(); }
            });
            move.setBounds(x + 10, 186, fontRendererObj.getStringWidth(move.label()) + 10, 20);
        }
        save = addButton("litematica.gui.button.area_editor.create_schematic",
            () -> mc.displayGuiScreen(new GuiSchematicSave(this, selectionName)));
        save.setBounds(22, 208, fontRendererObj.getStringWidth(save.label()) + 10, 20);
        UiButton analyze = unavailable(addButton("litematica.gui.button.area_editor.analyze_area", () -> {}));
        analyze.setBounds(132, 208, fontRendererObj.getStringWidth(analyze.label()) + 10, 20);
        guide = root.add(new UiCheckBox(() -> UiTranslations.format("schematica.ui.save.guide"), () -> ClientProxy.isRenderingGuide,
            value -> { ClientProxy.isRenderingGuide = value; WorldHandler.INSTANCE.saveSession(); }));
        guide.setBounds(250, 48, 160, 11);
    }

    private Vector3i point(int index) { return index == 0 ? ClientProxy.pointA : ClientProxy.pointB; }

    private void sync() {
        for (int index = 0; index < 2; index++) {
            Vector3i point = point(index).clone();
            coordinates[index][0].setValue(point.x);
            coordinates[index][1].setValue(point.y);
            coordinates[index][2].setValue(point.z);
        }
        ClientProxy.updatePoints();
    }

    @Override
    protected void opened() { sync(); }

    @Override
    protected void tickScreen() {
        boolean enabled = mc.thePlayer != null && mc.theWorld != null && SchematicaPlus.proxy.isSaveEnabled;
        guide.setEnabled(enabled);
        save.setEnabled(enabled);
    }

    @Override
    protected void layoutWidgets() {
        int x = 10;
        for (UiButton button : new UiButton[] {selectionMode, cornerMode, origin}) {
            int w = fontRendererObj.getStringWidth(button.label()) + 10;
            button.setBounds(x, 24, w, 20);
            x += w + 4;
        }
    }
}

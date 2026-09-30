// SPDX-License-Identifier: LGPL-3.0-only
// Litematica normal area editor and rows, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.save;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.world.World;
import org.lwjgl.input.Keyboard;
import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.client.gui.GuiSchematicMainMenu;
import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiCheckBox;
import com.github.lunatrius.schematica.client.gui.framework.UiDraw;
import com.github.lunatrius.schematica.client.gui.framework.UiIntegerField;
import com.github.lunatrius.schematica.client.gui.framework.UiLabel;
import com.github.lunatrius.schematica.client.gui.framework.UiListModel;
import com.github.lunatrius.schematica.client.gui.framework.UiPanel;
import com.github.lunatrius.schematica.client.gui.framework.UiRowList;
import com.github.lunatrius.schematica.client.gui.framework.UiScreen;
import com.github.lunatrius.schematica.client.gui.framework.UiSprite;
import com.github.lunatrius.schematica.client.gui.framework.UiTextField;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Area;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Box;
import com.github.lunatrius.schematica.client.selection.AreaSelections;
import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;

public final class GuiAreaSelectionEditor extends UiScreen {
    private final AreaSelectionLibrary library = AreaSelections.library();
    private final Area area = library.selected();
    private final World world = Minecraft.getMinecraft().theWorld;
    private final UiListModel<Box> model = new UiListModel<>(22, Box::name);
    private final List<UiButton> actions = new ArrayList<>();
    private UiRowList<Box> list;
    private UiTextField name;
    private UiButton setName, mode, corners, create, origin, save, browser, analyze, main;
    private UiCheckBox guide;
    private UiLabel count, status;
    private String message = "";
    private UiPanel originControls;
    private UiCheckBox originSelected;
    private UiButton originToPlayer;
    private final UiIntegerField[] originCoordinates = new UiIntegerField[3];
    private final UiLabel[] originAxes = new UiLabel[3];
    private final UiButton[] originNudges = new UiButton[3];
    private boolean syncingOrigin;
    private final GuiScreen menuParent;
    private final boolean simple = library.mode() == AreaSelectionLibrary.Mode.SIMPLE;
    private UiTextField boxName;
    private UiLabel boxLabel;
    private UiButton setBoxName;
    private final AreaCornerControls[] cornerControls = new AreaCornerControls[2];

    public GuiAreaSelectionEditor(GuiScreen parent) {
        super(parent, UiTranslations.format(AreaSelections.library().mode() == AreaSelectionLibrary.Mode.SIMPLE
            ? "litematica.gui.title.area_editor_simple" : "litematica.gui.title.area_editor_normal"));
        menuParent = parent;
    }

    private boolean available() {
        return world != null && mc.theWorld == world && mc.thePlayer != null && AreaSelections.available(library)
            && area != null && library.selected() == area && SchematicaPlus.proxy.isSaveEnabled;
    }

    private UiButton action(String key, Runnable run) {
        UiButton button = addButton(key, () -> { if (available()) run.run(); });
        actions.add(button);
        return button;
    }

    @Override protected void createWidgets() {
        mode = root.add(new UiButton(() -> UiTranslations.format("litematica.gui.button.area_editor.change_selection_mode",
            UiTranslations.format(AreaSelections.modeKey())), button -> {
                if (!available()) return;
                if (simple && library.normalSelection() == null) {
                    message = UiTranslations.format("litematica.error.area_editor.switch_mode.no_selection");
                    return;
                }
                AreaSelections.switchMode();
                mc.displayGuiScreen(new GuiAreaSelectionEditor(menuParent));
            }));
        mode.setTooltip(UiTranslations.format("schematica.ui.area.modes_hint"));
        corners = root.add(new UiButton(() -> UiTranslations.format("litematica.gui.button.area_editor.change_corner_mode",
            UiTranslations.format(AreaSelections.cornerModeKey())), button -> change(() -> library.setCornerMode(
                library.cornerMode() == AreaSelectionLibrary.CornerMode.CORNERS ? AreaSelectionLibrary.CornerMode.EXPAND : AreaSelectionLibrary.CornerMode.CORNERS))));
        corners.setTooltip(UiTranslations.format("schematica.ui.area.tool_hint"));
        root.add(new UiLabel(() -> UiTranslations.format("litematica.gui.label.area_editor.selection_name"))).setBounds(12, 44, 202, 12);
        name = root.add(new UiTextField(fontRendererObj, 200, value -> {}));
        name.setBounds(12, 59, 202, 16);
        setName = action("litematica.gui.button.area_editor.set_selection_name", () -> change(() -> {
            library.rename(area, name.text());
            name.setText(area.name());
            if (simple) boxName.setText(area.boxName());
        }));
        setName.setBounds(218, 57, fontRendererObj.getStringWidth(setName.label()) + 10, 20);
        create = action("litematica.gui.button.area_editor.create_sub_region", () -> promptName("litematica.gui.title.area_editor.sub_region_name", "", value -> {
            Vector3i point = GuiAreaSelectionManager.playerPoint();
            library.addBox(area, value, point, point);
        }));
        origin = root.add(new UiButton(() -> UiTranslations.format("litematica.gui.button.area_editor.origin_enabled",
            (manualOrigin() ? "\u00a7a" : "\u00a7c") + UiTranslations.format(manualOrigin() ? "options.on" : "options.off")),
            button -> { if (button == 0) change(() -> library.setOrigin(area, manualOrigin() ? null : simple ? area.origin() : GuiAreaSelectionManager.playerPoint())); }));
        origin.setTooltip(UiTranslations.format("schematica.ui.area.origin_hint"));
        save = action("litematica.gui.button.area_editor.create_schematic", () -> mc.displayGuiScreen(new GuiSchematicSave(this, area.name())));
        count = root.add(new UiLabel(() -> UiTranslations.format("litematica.gui.label.area_editor.sub_regions", area == null ? 0 : area.boxes().size())));
        guide = root.add(new UiCheckBox(() -> UiTranslations.format("schematica.ui.save.guide"), () -> area != null && area.guide(),
            value -> change(() -> library.setGuide(area, value))));
        list = root.add(new UiRowList<>(model, Entry::new));
        browser = addButton("litematica.gui.button.change_menu.show_area_selections", () -> mc.displayGuiScreen(new GuiAreaSelectionManager(this)))
            .setSprite(UiSprite.AREA_SELECTION);
        analyze = action("litematica.gui.button.area_editor.analyze_area", () -> {
            AreaSelections.capture();
            mc.displayGuiScreen(new com.github.lunatrius.schematica.client.gui.control.GuiSchematicMaterials(this, area));
        });
        main = addButton("litematica.gui.button.change_menu.to_main_menu", () -> mc.displayGuiScreen(new GuiSchematicMainMenu(this)));
        status = root.add(new UiLabel(this::statusText, 0xFFFFA0A0));
        createOriginControls();
        if (simple) {
            boxLabel = root.add(new UiLabel(() -> UiTranslations.format("litematica.gui.label.area_editor.box_name")));
            boxName = root.add(new UiTextField(fontRendererObj, 200, value -> {}));
            setBoxName = action("litematica.gui.button.area_editor.set_box_name", () -> change(() -> library.renameBox(area, boxName.text())));
            for (int i = 0; i < 2; i++) cornerControls[i] = root.add(new AreaCornerControls(fontRendererObj, library, area, area.selectedBox(),
                i == 0 ? AreaSelectionLibrary.Corner.FIRST : AreaSelectionLibrary.Corner.SECOND, this::change));
        }
    }

    private boolean manualOrigin() { return area != null && area.manualOrigin() != null; }

    private void createOriginControls() {
        originControls = root.add(new UiPanel());
        originSelected = originControls.add(new UiCheckBox(() -> UiTranslations.format("litematica.gui.label.area_editor.origin"),
            () -> area != null && area.originSelected(), value -> change(() -> library.selectOrigin(area, value))));
        originSelected.setTooltip(UiTranslations.format("schematica.ui.area.origin_hint"));
        for (int axis = 0; axis < 3; axis++) {
            final int component = axis;
            originAxes[axis] = originControls.add(new UiLabel(() -> "XYZ".charAt(component) + ":"));
            UiIntegerField field = originControls.add(new UiIntegerField(fontRendererObj, 0,
                axis == 1 ? 0 : -30000000, axis == 1 ? 255 : 29999999, value -> {
                    if (syncingOrigin || !manualOrigin()) return;
                    change(() -> {
                        Vector3i point = area.manualOrigin();
                        if (component == 0) point.x = value;
                        else if (component == 1) point.y = value;
                        else point.z = value;
                        library.setOrigin(area, point);
                    });
                }));
            originCoordinates[axis] = field;
            originNudges[axis] = originControls.add(new UiButton(() -> "", button -> field.setValue((long) field.value() + (button == 0 ? 1 : -1)))
                .setSprite(UiSprite.PLUS_MINUS).setBackground(false));
            originNudges[axis].setTooltip(UiTranslations.format("schematica.ui.save.coordinate_hint"));
        }
        originToPlayer = originControls.add(new UiButton(() -> UiTranslations.format("litematica.gui.button.move_to_player"),
            button -> { if (button == 0) change(() -> library.setOrigin(area, GuiAreaSelectionManager.playerPoint())); }));
    }

    private void syncOrigin() {
        syncingOrigin = true;
        try {
            Vector3i point = area == null ? new Vector3i() : area.origin();
            originCoordinates[0].setValue(point.x);
            originCoordinates[1].setValue(point.y);
            originCoordinates[2].setValue(point.z);
        } finally { syncingOrigin = false; }
    }

    private void change(Runnable action) {
        if (!available()) return;
        try {
            AreaSelections.capture();
            action.run();
            persist();
        } catch (IllegalArgumentException | ArithmeticException error) { message = GuiAreaSelectionManager.error(error); }
    }

    private void persist() {
        AreaSelections.apply();
        AreaSelections.saveCurrent();
        message = "";
        syncOrigin();
        refresh();
        layoutWidgets();
    }

    private void promptName(String title, String initial, Consumer<String> action) {
        prompt(UiTranslations.format(title), initial, value -> {
            if (!available()) return UiTranslations.format("schematica.ui.area.context");
            try {
                AreaSelections.capture();
                action.accept(value);
                persist();
                return null;
            } catch (IllegalArgumentException | ArithmeticException error) { return GuiAreaSelectionManager.error(error); }
        });
    }

    private void refresh() { model.setEntries(area == null ? java.util.Collections.emptyList() : area.boxes()); list.sync(); }
    @Override protected void opened() {
        if (available()) AreaSelections.capture();
        name.setText(area == null ? "" : area.name());
        syncOrigin();
        if (simple) { boxName.setText(area.boxName()); for (AreaCornerControls controls : cornerControls) controls.sync(); }
        refresh();
    }
    @Override protected void closed() { if (available()) AreaSelections.saveCurrent(); }
    private String statusText() {
        if (!available()) return UiTranslations.format("litematica.error.area_editor.no_selection");
        if (AreaSelections.saveFailed()) return UiTranslations.format("schematica.ui.area.persistence_failed");
        return message;
    }
    @Override protected void tickScreen() {
        boolean enabled = available();
        for (UiButton button : actions) button.setEnabled(enabled);
        save.setEnabled(enabled && !area.boxes().isEmpty());
        analyze.setEnabled(enabled && !area.boxes().isEmpty());
        name.setEnabled(enabled); guide.setEnabled(enabled); list.setEnabled(enabled);
        origin.setEnabled(enabled);
        mode.setEnabled(enabled); corners.setEnabled(enabled);
        if (simple) {
            boxName.setEnabled(enabled);
            for (AreaCornerControls controls : cornerControls) controls.setEnabled(enabled);
        }
        originControls.setEnabled(enabled && manualOrigin());
        status.setTooltip(statusText());
    }

    private int place(UiButton button, int x, int y, boolean icon) {
        int w = fontRendererObj.getStringWidth(button.label()) + (icon ? 30 : 10);
        button.setBounds(x, y, w, 20);
        return x + w + 4;
    }
    @Override protected void layoutWidgets() {
        if (simple) { layoutSimple(); return; }
        int x = place(mode, 10, 24, false);
        int originX = place(corners, x, 24, false);
        x = place(create, 10, 81, false);
        x = place(origin, x, 81, false);
        x = place(save, x, 81, false);
        originX = Math.max(originX, Math.max(x, setName.bounds().right() + 15));
        originControls.setVisible(manualOrigin());
        originControls.setBounds(originX, 5, Math.max(100, fontRendererObj.getStringWidth(originToPlayer.label()) + 20), 96);
        originSelected.setBounds(originX, 8, 100, 11);
        for (int axis = 0; axis < 3; axis++) {
            originAxes[axis].setBounds(originX, 19 + axis * 20, 12, 20);
            originCoordinates[axis].setBounds(originX + 12, 21 + axis * 20, 68, 16);
            originNudges[axis].setBounds(originX + 84, 21 + axis * 20, 16, 16);
        }
        originToPlayer.setBounds(originX + 10, 81, fontRendererObj.getStringWidth(originToPlayer.label()) + 10, 20);
        String label = UiTranslations.format("litematica.gui.label.area_editor.sub_regions", area == null ? 0 : area.boxes().size());
        int countWidth = fontRendererObj.getStringWidth(label) + 4;
        count.setBounds(12, 104, countWidth, 12);
        guide.setBounds(24 + countWidth, 104, 150, 11);
        list.setBounds(8, 116, width - 20, Math.max(0, height - 146));
        int statusX = setName.bounds().right() + 10;
        status.setBounds(statusX, 59, Math.max(0, (manualOrigin() ? originX : width) - statusX - 12), 16);
        x = place(browser, 12, height - 26, true);
        place(analyze, x, height - 26, false);
        place(main, width - fontRendererObj.getStringWidth(main.label()) - 20, height - 26, false);
    }

    private void layoutSimple() {
        int x = place(mode, 10, 24, false);
        x = place(corners, x, 24, false);
        place(origin, x, 24, false);
        create.setVisible(false); count.setVisible(false); list.setVisible(false);
        boxLabel.setBounds(12, 77, 202, 12);
        boxName.setBounds(12, 92, 202, 16);
        place(setBoxName, 218, 90, false);
        guide.setBounds(232, 77, 150, 11);
        for (int i = 0; i < 2; i++) {
            cornerControls[i].setBounds(12 + i * 110, 110, 110, 96);
            cornerControls[i].layout(root.bounds());
        }
        originControls.setVisible(manualOrigin());
        originControls.setBounds(232, 110, 120, 96);
        originSelected.setBounds(232, 113, 100, 11);
        for (int axis = 0; axis < 3; axis++) {
            originAxes[axis].setBounds(232, 124 + axis * 20, 12, 20);
            originCoordinates[axis].setBounds(244, 126 + axis * 20, 68, 16);
            originNudges[axis].setBounds(316, 126 + axis * 20, 16, 16);
        }
        originToPlayer.setBounds(242, 186, 100, 20);
        place(save, 22, 208, false); place(analyze, 132, 208, false);
        browser.setVisible(false); main.setVisible(false);
        status.setBounds(setName.bounds().right() + 10, 59, Math.max(0, width - setName.bounds().right() - 22), 16);
    }

    @Override protected int titleRightMargin() {
        return !simple && manualOrigin() && originControls != null ? Math.max(30, width - originControls.bounds().x + 8) : 30;
    }

    private final class Entry extends UiPanel {
        private final Box box;
        private final int index;
        private final List<UiButton> buttons = new ArrayList<>();
        private int buttonsStart;
        Entry(Box box, int index) {
            this.box = box; this.index = index;
            button("remove", () -> change(() -> library.removeBox(area, box)));
            button("rename", () -> promptName("litematica.gui.title.rename_area_sub_region", box.name(), value -> library.renameBox(area, box, value)));
            button("configure", () -> {
                AreaSelections.selectBox(box);
                mc.displayGuiScreen(new GuiSubRegionEditor(GuiAreaSelectionEditor.this, box));
            });
        }
        private void button(String key, Runnable action) {
            buttons.add(add(new UiButton(() -> UiTranslations.format("litematica.gui.button." + key), mouse -> {
                if (mouse == 0 && available() && area.boxes().contains(box)) action.run();
            })));
        }
        @Override public void layout(UiBounds screen) {
            int right = bounds().right() - 2;
            for (UiButton button : buttons) {
                int w = fontRendererObj.getStringWidth(button.label()) + 10;
                button.setBounds(right - w, bounds().y + 1, w, 20);
                right -= w + 2;
            }
            buttonsStart = right;
        }
        @Override public void draw(UiDraw draw, int mouseX, int mouseY) {
            boolean selected = area.selectedBox() == box;
            draw.fill(bounds(), selected || containsVisible(mouseX, mouseY) ? 0xA0707070 : index % 2 == 1 ? 0xA0101010 : 0xA0303030);
            if (selected) draw.border(bounds(), 0xFFE0E0E0);
            draw.text(draw.trim(box.name(), Math.max(0, buttonsStart - bounds().x - 4)), bounds().x + 2, bounds().y + 7, 0xFFFFFFFF);
            super.draw(draw, mouseX, mouseY);
        }
        @Override public List<String> tooltip(int x, int y) {
            Vector3i a = box.first(), b = box.second();
            return java.util.Arrays.asList(UiTranslations.format("litematica.gui.label.area_editor.pos1") + ": " + a.x + ", " + a.y + ", " + a.z,
                UiTranslations.format("litematica.gui.label.area_editor.pos2") + ": " + b.x + ", " + b.y + ", " + b.z,
                UiTranslations.format("litematica.gui.label.area_editor.dimensions") + ": " + (Math.abs(a.x - b.x) + 1) + " x "
                    + (Math.abs(a.y - b.y) + 1) + " x " + (Math.abs(a.z - b.z) + 1));
        }
        private void select() { if (available() && area.boxes().contains(box)) AreaSelections.selectBox(area.selectedBox() == box ? null : box); }
        @Override public boolean isFocusable() { return true; }
        @Override public boolean mouseDown(int x, int y, int button) {
            if (button != 0 || x >= buttonsStart) return false;
            select(); return true;
        }
        @Override public boolean keyTyped(char character, int keyCode) {
            if (keyCode != Keyboard.KEY_RETURN && keyCode != Keyboard.KEY_SPACE) return false;
            select(); return true;
        }
    }
}

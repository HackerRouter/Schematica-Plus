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

    public GuiAreaSelectionEditor(GuiScreen parent) {
        super(parent, UiTranslations.format("litematica.gui.title.area_editor_normal"));
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
        mode = unavailable(root.add(new UiButton(() -> UiTranslations.format("litematica.gui.button.area_editor.change_selection_mode",
            UiTranslations.format("litematica.gui.label.area_selection.mode.normal")), button -> {})));
        mode.setTooltip(UiTranslations.format("schematica.ui.area.multi_box"));
        corners = unavailable(root.add(new UiButton(() -> UiTranslations.format("litematica.gui.button.area_editor.change_corner_mode",
            UiTranslations.format("litematica.hud.area_selection.mode.corners")), button -> {})));
        root.add(new UiLabel(() -> UiTranslations.format("litematica.gui.label.area_editor.selection_name"))).setBounds(12, 44, 202, 12);
        name = root.add(new UiTextField(fontRendererObj, 200, value -> {}));
        name.setBounds(12, 59, 202, 16);
        setName = action("litematica.gui.button.area_editor.set_selection_name", () -> change(() -> library.rename(area, name.text())));
        setName.setBounds(218, 57, fontRendererObj.getStringWidth(setName.label()) + 10, 20);
        create = action("litematica.gui.button.area_editor.create_sub_region", () -> promptName("litematica.gui.title.area_editor.sub_region_name", "", value -> {
            Vector3i point = GuiAreaSelectionManager.playerPoint();
            library.addBox(area, value, point, point);
        }));
        origin = unavailable(root.add(new UiButton(() -> UiTranslations.format("litematica.gui.button.area_editor.origin_enabled",
            "\u00a7c" + UiTranslations.format("options.off")), button -> {})));
        save = action("litematica.gui.button.area_editor.create_schematic", () -> mc.displayGuiScreen(new GuiSchematicSave(this, area.name())));
        count = root.add(new UiLabel(() -> UiTranslations.format("litematica.gui.label.area_editor.sub_regions", area == null ? 0 : area.boxes().size())));
        guide = root.add(new UiCheckBox(() -> UiTranslations.format("schematica.ui.save.guide"), () -> area != null && area.guide(),
            value -> change(() -> library.setGuide(area, value))));
        list = root.add(new UiRowList<>(model, Entry::new));
        browser = addButton("litematica.gui.button.change_menu.show_area_selections", () -> mc.displayGuiScreen(new GuiAreaSelectionManager(this)))
            .setSprite(UiSprite.AREA_SELECTION);
        analyze = unavailable(addButton("litematica.gui.button.area_editor.analyze_area", () -> {}));
        main = addButton("litematica.gui.button.change_menu.to_main_menu", () -> mc.displayGuiScreen(new GuiSchematicMainMenu(this)));
        status = root.add(new UiLabel(this::statusText, 0xFFFFA0A0));
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
        name.setEnabled(enabled); guide.setEnabled(enabled); list.setEnabled(enabled);
        status.setTooltip(statusText());
    }

    private int place(UiButton button, int x, int y, boolean icon) {
        int w = fontRendererObj.getStringWidth(button.label()) + (icon ? 30 : 10);
        button.setBounds(x, y, w, 20);
        return x + w + 4;
    }
    @Override protected void layoutWidgets() {
        int x = place(mode, 10, 24, false);
        place(corners, x, 24, false);
        x = place(create, 10, 81, false);
        x = place(origin, x, 81, false);
        place(save, x, 81, false);
        String label = UiTranslations.format("litematica.gui.label.area_editor.sub_regions", area == null ? 0 : area.boxes().size());
        int countWidth = fontRendererObj.getStringWidth(label) + 4;
        count.setBounds(12, 104, countWidth, 12);
        guide.setBounds(24 + countWidth, 104, 150, 11);
        list.setBounds(8, 116, width - 20, Math.max(0, height - 146));
        int statusX = setName.bounds().right() + 10;
        status.setBounds(statusX, 59, Math.max(0, width - statusX - 12), 16);
        x = place(browser, 12, height - 26, true);
        place(analyze, x, height - 26, false);
        place(main, width - fontRendererObj.getStringWidth(main.label()) - 20, height - 26, false);
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

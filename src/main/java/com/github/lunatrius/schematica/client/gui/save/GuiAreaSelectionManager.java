// SPDX-License-Identifier: LGPL-3.0-only
// Litematica area manager and row layout, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.save;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import org.lwjgl.input.Keyboard;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
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
import com.github.lunatrius.schematica.client.selection.AreaSelections;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;
import com.github.lunatrius.schematica.proxy.ClientProxy;

public final class GuiAreaSelectionManager extends UiScreen {
    private final AreaSelectionLibrary library = AreaSelections.library();
    private final World world = Minecraft.getMinecraft().theWorld;
    private final UiListModel<Area> model = new UiListModel<>(22, area -> area.name() + " " + area.boxes().stream().map(box -> box.name()).collect(java.util.stream.Collectors.joining(" ")));
    private UiRowList<Area> list;
    private UiTextField search;
    private UiButton searchButton;
    private UiButton editor;
    private UiButton unselect;
    private UiButton fromPlacement;
    private UiButton create;
    private UiLabel status;
    private boolean searching;

    public GuiAreaSelectionManager(GuiScreen parent) {
        super(parent, UiTranslations.format("litematica.gui.title.area_selection_manager"));
    }

    private boolean available() {
        return world != null && mc.theWorld == world && mc.thePlayer != null
            && AreaSelections.available(library) && SchematicaPlus.proxy.isSaveEnabled;
    }

    @Override protected void createWidgets() {
        editor = addButton("litematica.gui.button.change_menu.area_editor", () -> configure(library.normalSelection())).setSprite(UiSprite.AREA_EDITOR);
        unselect = addButton("litematica.gui.button.area_selections.unselect", () -> { if (available()) AreaSelections.select(null); });
        unselect.setTooltip(UiTranslations.format("litematica.gui.button.hover.area_selections.unselect"));
        fromPlacement = addButton("litematica.gui.button.area_selections.create_selection_from_placement", this::fromPlacement);
        create = addButton("litematica.gui.button.area_selections.create_new_selection", () -> {
            if (!available()) return;
            promptName("litematica.gui.title.create_area_selection", "", name -> {
                Vector3i point = playerPoint();
                AreaSelections.select(library.create(name, point, point));
            });
        });
        searchButton = root.add(new UiButton(() -> "", button -> {
            searching = !searching;
            search.setVisible(searching);
            if (searching) input.focus(search);
            else search.setText("");
        }).setSprite(UiSprite.SEARCH).setBackground(false));
        search = root.add(new UiTextField(fontRendererObj, 200, text -> { model.setQuery(text); list.sync(); }));
        search.setVisible(false);
        list = root.add(new UiRowList<>(model, Entry::new));
        status = root.add(new UiLabel(this::statusText));
    }

    @Override protected void opened() {
        if (available()) AreaSelections.capture();
        refresh();
    }

    @Override protected void closed() { if (available()) AreaSelections.saveCurrent(); }

    private void refresh() {
        model.setEntries(library.areas());
        list.sync();
    }

    private void promptName(String titleKey, String initial, Consumer<String> action, Object... titleArgs) {
        prompt(UiTranslations.format(titleKey, titleArgs), initial, name -> {
            if (!available()) return UiTranslations.format("schematica.ui.area.context");
            try {
                AreaSelections.capture();
                action.accept(name);
                AreaSelections.apply();
                AreaSelections.saveCurrent();
                refresh();
                return null;
            } catch (IllegalArgumentException | ArithmeticException e) {
                return error(e);
            }
        });
    }

    private void configure(Area area) {
        if (available() && library.contains(area)) {
            AreaSelections.select(area);
            mc.displayGuiScreen(new GuiAreaSelectionEditor(this));
        }
    }

    private void fromPlacement() {
        SchematicWorld placement = ClientProxy.schematic;
        if (!available() || placement == null || !ClientProxy.loadedSchematics.contains(placement)) return;
        promptName("litematica.gui.title.create_area_selection_from_placement", placement.name, name -> {
            if (!ClientProxy.loadedSchematics.contains(placement)) throw new IllegalArgumentException("Placement is no longer loaded");
            Vector3i first = placement.position.clone();
            Vector3i second = new Vector3i(Math.addExact(first.x, placement.getWidth() - 1),
                Math.addExact(first.y, placement.getHeight() - 1), Math.addExact(first.z, placement.getLength() - 1));
            List<com.github.lunatrius.schematica.api.SchematicRegion> regions = new ArrayList<>();
            if (!placement.isEnabled()) throw new IllegalArgumentException(UiTranslations.format("schematica.ui.placement.disabled"));
            if (!placement.hasEnabledRegions()) throw new IllegalArgumentException(UiTranslations.format("schematica.ui.placement.no_regions"));
            for (com.github.lunatrius.schematica.api.SchematicRegion region : placement.getSchematic().getRegions()) {
                regions.add(region.offset(first.x, first.y, first.z));
            }
            if (regions.isEmpty()) regions.add(new com.github.lunatrius.schematica.api.SchematicRegion(name,
                first.x, first.y, first.z, second.x, second.y, second.z));
            com.github.lunatrius.schematica.api.SchematicOrigin origin = placement.originPosition();
            AreaSelections.select(library.createFromRegions(name, regions, new Vector3i(origin.x, origin.y, origin.z)));
        });
    }

    static Vector3i playerPoint() {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null) return new Vector3i();
        return new Vector3i((int) Math.max(-30000000, Math.min(29999999, Math.floor(player.posX))),
            (int) Math.max(0, Math.min(255, Math.floor(player.posY - player.yOffset))),
            (int) Math.max(-30000000, Math.min(29999999, Math.floor(player.posZ))));
    }

    static String error(RuntimeException error) {
        if (!(error instanceof AreaSelectionLibrary.NameConflictException)) return UiTranslations.format("schematica.ui.area.invalid");
        return UiTranslations.format(((AreaSelectionLibrary.NameConflictException) error).subregion
            ? "litematica.error.area_editor.create_sub_region.exists" : "litematica.error.area_selection.rename.already_exists", error.getMessage());
    }

    private String statusText() {
        if (AreaSelections.saveFailed()) return UiTranslations.format("schematica.ui.area.persistence_failed");
        if (!available()) return UiTranslations.format("schematica.ui.area.context");
        Area area = library.normalSelection();
        return area == null ? UiTranslations.format("litematica.error.area_editor.no_selection")
            : UiTranslations.format("litematica.gui.label.area_selection_manager.current_selection", area.name());
    }

    @Override protected void tickScreen() {
        boolean available = available();
        editor.setEnabled(available && library.normalSelection() != null);
        unselect.setEnabled(available && library.normalSelection() != null);
        fromPlacement.setEnabled(available && ClientProxy.schematic != null);
        create.setEnabled(available);
        list.setEnabled(available);
        status.setTooltip(statusText());
    }

    @Override protected void layoutWidgets() {
        editor.setBounds(10, 24, fontRendererObj.getStringWidth(editor.label()) + 30, 20);
        int right = width - 13;
        for (UiButton button : new UiButton[] {unselect, fromPlacement, create}) {
            int w = fontRendererObj.getStringWidth(button.label()) + 10;
            right -= w + 2;
            button.setBounds(right, 24, w, 20);
        }
        searchButton.setBounds(14, 54, 12, 12);
        search.setBounds(30, 53, Math.max(0, width - 52), 16);
        list.setBounds(10, 72, width - 20, Math.max(0, height - 90));
        status.setBounds(10, height - 15, width - 20, 14);
    }

    private final class Entry extends UiPanel {
        private final Area area;
        private final int index;
        private final List<UiButton> buttons = new ArrayList<>();
        private int buttonsStart;

        Entry(Area area, int index) {
            this.area = area;
            this.index = index;
            UiButton remove = add(new UiButton(() -> "\u00a7c-", button -> {
                if (button == 0 && available() && library.contains(area)) {
                    AreaSelections.capture();
                    library.remove(area);
                    AreaSelections.apply();
                    AreaSelections.saveCurrent();
                    refresh();
                }
            }));
            remove.setTooltip(UiTranslations.format("litematica.gui.button.remove"));
            buttons.add(remove);
            button("rename", () -> promptName("litematica.gui.title.rename_area_selection", area.name(), name -> library.rename(area, name)));
            button("copy", () -> promptName("litematica.gui.title.copy_area_selection", area.name(), name -> library.copy(area, name), area.name()));
            button("configure", () -> configure(area));
        }

        private void button(String action, Runnable run) {
            buttons.add(add(new UiButton(() -> UiTranslations.format("litematica.gui.button." + action), mouse -> {
                if (mouse == 0 && available() && library.contains(area)) run.run();
            })));
        }

        @Override public void layout(UiBounds screen) {
            int right = bounds().right() - 2;
            for (UiButton button : buttons) {
                int width = Math.max(20, fontRendererObj.getStringWidth(button.label()) + 10);
                button.setBounds(right - width, bounds().y + 1, width, 20);
                right -= width + 2;
            }
            buttonsStart = right;
        }

        @Override public void draw(UiDraw draw, int mouseX, int mouseY) {
            boolean selected = library.normalSelection() == area;
            draw.fill(bounds(), selected || containsVisible(mouseX, mouseY) ? 0xA0707070 : index % 2 == 1 ? 0xA0101010 : 0xA0303030);
            if (selected) draw.border(bounds(), 0xFFE0E0E0);
            UiSprite.AREA_SELECTION.draw(draw, bounds().x + 2, bounds().y + 5, false, false);
            draw.text(draw.trim(area.name(), buttonsStart - bounds().x - 24), bounds().x + 20, bounds().y + 7, 0xFFFFFFFF);
            super.draw(draw, mouseX, mouseY);
        }

        @Override public List<String> tooltip(int x, int y) {
            Vector3i origin = area.origin();
            List<String> lines = new ArrayList<>();
            lines.add(area.name());
            lines.add(UiTranslations.format("litematica.gui.label.area_selection_box_count", area.boxes().size()));
            lines.add(UiTranslations.format("litematica.gui.label.area_selection_origin", origin.x + ", " + origin.y + ", " + origin.z)
                + " (" + UiTranslations.format(area.manualOrigin() == null ? "litematica.gui.label.origin.auto" : "litematica.gui.label.origin.manual") + ")");
            if (area.selectedBox() != null) {
                Vector3i a = area.first(), b = area.second();
                lines.add(area.boxName());
                lines.add(String.format("%d, %d, %d -> %d, %d, %d", a.x, a.y, a.z, b.x, b.y, b.z));
            }
            return lines;
        }

        private void select() {
            if (available() && library.contains(area)) {
                model.select(index);
                AreaSelections.select(library.normalSelection() == area ? null : area);
            }
        }

        @Override public boolean isFocusable() { return true; }
        @Override public boolean mouseDown(int x, int y, int button) {
            if (button != 0 || x >= buttonsStart) return false;
            select();
            return true;
        }
        @Override public boolean keyTyped(char character, int keyCode) {
            if (keyCode != Keyboard.KEY_RETURN && keyCode != Keyboard.KEY_SPACE) return false;
            select();
            return true;
        }
    }
}

// SPDX-License-Identifier: LGPL-3.0-only
// Litematica loaded/placement list layouts, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.placement;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import org.lwjgl.input.Keyboard;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiDraw;
import com.github.lunatrius.schematica.client.gui.framework.UiListModel;
import com.github.lunatrius.schematica.client.gui.framework.UiPanel;
import com.github.lunatrius.schematica.client.gui.framework.UiRowList;
import com.github.lunatrius.schematica.client.gui.framework.UiScreen;
import com.github.lunatrius.schematica.client.gui.framework.UiSprite;
import com.github.lunatrius.schematica.client.gui.framework.UiTextField;
import com.github.lunatrius.schematica.client.gui.load.GuiSchematicLoad;
import com.github.lunatrius.schematica.client.renderer.RendererSchematicGlobal;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.client.WorldHandler;
import com.github.lunatrius.schematica.proxy.ClientProxy;

abstract class GuiSchematicList extends UiScreen {
    private final boolean placements;
    private final UiListModel<SchematicWorld> model = new UiListModel<>(22,
        world -> world.name + " " + (world.sourceFilename == null ? "" : world.sourceFilename));
    private List<SchematicWorld> snapshot = new ArrayList<>();
    private UiRowList<SchematicWorld> list;
    private UiTextField search;
    private UiButton searchButton;
    private UiButton first;
    private UiButton second;
    private UiButton menu;
    private boolean searching;

    GuiSchematicList(GuiScreen parent, boolean placements) {
        super(parent, I18n.format(placements ? "litematica.gui.title.manage_schematic_placements"
            : "litematica.gui.title.manage_loaded_schematics"));
        this.placements = placements;
    }

    @Override
    protected void createWidgets() {
        list = root.add(new UiRowList<>(model, Entry::new));
        search = root.add(new UiTextField(fontRendererObj, 256, text -> { model.setQuery(text); list.sync(); }));
        search.setVisible(false);
        searchButton = root.add(new UiButton(() -> "", button -> {
            searching = !searching;
            search.setVisible(searching);
            if (searching) input.focus(search);
            else search.setText("");
        }).setSprite(UiSprite.SEARCH).setBackground(false));
        searchButton.setTooltip(I18n.format("schematica.ui.browser.search_hint"));
        first = addButton("litematica.gui.button.change_menu." + (placements ? "show_loaded_schematics" : "load_schematics_to_memory"),
            () -> mc.displayGuiScreen(placements ? new GuiSchematicLoadedList(this) : new GuiSchematicLoad(this)))
            .setSprite(placements ? UiSprite.LOADED_SCHEMATICS : UiSprite.SCHEMATIC_BROWSER);
        if (!placements) second = addButton("litematica.gui.button.change_menu.show_schematic_placements",
            () -> mc.displayGuiScreen(new GuiSchematicPlacementsList(this))).setSprite(UiSprite.SCHEMATIC_PLACEMENTS);
        menu = addButton("litematica.gui.button.change_menu.to_main_menu", this::mainMenu);
    }

    @Override
    protected void opened() { refresh(); }

    private void refresh() {
        snapshot = new ArrayList<>(ClientProxy.loadedSchematics);
        model.setEntries(snapshot);
        list.sync();
    }

    @Override
    protected void tickScreen() {
        if (!snapshot.equals(ClientProxy.loadedSchematics)) refresh();
    }

    @Override
    protected void layoutWidgets() {
        searchButton.setBounds(14, 35, 12, 12);
        search.setBounds(30, 34, Math.max(0, width - 52), 14);
        list.setBounds(14, 51, width - 24, Math.max(0, height - (placements ? 89 : 93)));
        int w = fontRendererObj.getStringWidth(first.label()) + 30;
        first.setBounds(12, height - 26, w, 20);
        if (second != null) second.setBounds(16 + w, height - 26, fontRendererObj.getStringWidth(second.label()) + 30, 20);
        int menuWidth = fontRendererObj.getStringWidth(menu.label()) + 20;
        menu.setBounds(width - menuWidth - 10, height - 26, menuWidth, 20);
    }

    private void remove(SchematicWorld world) {
        if (!ClientProxy.loadedSchematics.contains(world)) return;
        if (ClientProxy.schematic == world) SchematicaPlus.proxy.unloadSchematic();
        else {
            ClientProxy.loadedSchematics.remove(world);
            RendererSchematicGlobal.INSTANCE.removeRendererSchematicChunks(world);
            WorldHandler.INSTANCE.saveSession();
        }
        refresh();
    }

    private final class Entry extends UiPanel {
        private final SchematicWorld world;
        private final int index;
        private final List<UiButton> buttons = new ArrayList<>();
        private int buttonsStart;

        Entry(SchematicWorld world, int index) {
            this.world = world;
            this.index = index;
            if (placements) {
                button("litematica.gui.button.schematic_placements.remove", () -> GuiSchematicList.this.remove(world));
                UiButton toggle = add(new UiButton(() -> I18n.format("litematica.gui.button.schematic_placements.placement_enabled",
                    (world.isRendering ? "\u00a7a" : "\u00a7c") + I18n.format(world.isRendering ? "options.on" : "options.off")),
                    mouse -> { if (mouse == 0) { world.isRendering = !world.isRendering; WorldHandler.INSTANCE.saveSession(); } }));
                buttons.add(toggle);
                button("litematica.gui.button.schematic_placements.configure", () -> {
                    mc.displayGuiScreen(new GuiPlacementConfiguration(GuiSchematicList.this, world));
                });
            } else {
                button("litematica.gui.button.unload", () -> GuiSchematicList.this.remove(world));
                unavailable(button("litematica.gui.button.reload", () -> {}));
                unavailable(button("litematica.gui.button.save_to_file", () -> {}));
                unavailable(button("litematica.gui.button.create_placement", () -> {}));
            }
            setTooltip(world.name, world.sourceFilename == null ? "" : world.sourceFilename,
                I18n.format("schematica.ui.linked_instances"));
        }

        private UiButton button(String key, Runnable action) {
            UiButton button = add(new UiButton(() -> I18n.format(key), mouse -> { if (mouse == 0) action.run(); }));
            buttons.add(button);
            return button;
        }

        @Override
        public void layout(UiBounds screen) {
            int right = bounds().right() - (placements ? 2 : 0);
            for (int i = 0; i < buttons.size(); i++) {
                UiButton button = buttons.get(i);
                int width = fontRendererObj.getStringWidth(button.label()) + 10;
                button.setBounds(right - width, bounds().y + 1, width, 20);
                right -= width + (placements && i != 1 ? 1 : 2);
            }
            buttonsStart = right;
        }

        @Override
        public void draw(UiDraw draw, int mouseX, int mouseY) {
            boolean selected = placements && ClientProxy.schematic == world;
            boolean hover = containsVisible(mouseX, mouseY);
            int color = placements ? selected || hover ? 0xA0707070 : index % 2 == 1 ? 0xA0101010 : 0xA0303030
                : hover || model.selected() == world ? 0x70FFFFFF : index % 2 == 1 ? 0x20FFFFFF : 0x50FFFFFF;
            draw.fill(bounds(), color);
            if (selected) draw.border(bounds(), 0xFFE0E0E0);
            (world.sourceFilename == null ? UiSprite.MEMORY : UiSprite.FILE).draw(draw, bounds().x + 2, bounds().y + 5, false, false);
            String name = placements ? (world.isRendering ? "\u00a7a" : "\u00a7c") + world.name : world.name;
            draw.text(draw.trim(name, buttonsStart - bounds().x - 24), bounds().x + 20, bounds().y + 7, 0xFFFFFFFF);
            super.draw(draw, mouseX, mouseY);
        }

        @Override
        public boolean isFocusable() { return true; }

        @Override
        public boolean mouseDown(int x, int y, int button) {
            if (button != 0 || x >= buttonsStart) return false;
            select();
            return true;
        }

        private void select() {
            model.select(index);
            if (placements) {
                ClientProxy.selectSchematic(ClientProxy.schematic == world ? null : world);
                WorldHandler.INSTANCE.saveSession();
            }
        }

        @Override
        public boolean keyTyped(char character, int keyCode) {
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_SPACE) {
                select();
                return true;
            }
            return false;
        }
    }
}

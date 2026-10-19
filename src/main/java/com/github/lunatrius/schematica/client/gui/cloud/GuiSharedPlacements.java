// Syncmatica server placement list, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.cloud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import net.minecraft.client.gui.GuiScreen;

import com.github.lunatrius.schematica.client.gui.control.GuiSchematicMaterials;
import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiDraw;
import com.github.lunatrius.schematica.client.gui.framework.UiListModel;
import com.github.lunatrius.schematica.client.gui.framework.UiPanel;
import com.github.lunatrius.schematica.client.gui.framework.UiRowList;
import com.github.lunatrius.schematica.client.gui.framework.UiScreen;
import com.github.lunatrius.schematica.client.gui.framework.UiSprite;
import com.github.lunatrius.schematica.client.gui.framework.UiTextField;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.gui.framework.UiWidget;
import com.github.lunatrius.schematica.client.gui.material.MaterialList;
import com.github.lunatrius.schematica.client.gui.material.MaterialLists;
import com.github.lunatrius.schematica.client.gui.placement.GuiSchematicPlacementsList;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.cloud.SharedPlacement;
import com.github.lunatrius.schematica.handler.client.SharedPlacementsClient;
import com.github.lunatrius.schematica.reference.Reference;

import org.lwjgl.input.Keyboard;

public final class GuiSharedPlacements extends UiScreen {
    private static final String PREFIX = "syncmatica.gui.";
    private static final int INFO_WIDTH = 170;
    private final UiListModel<SharedPlacement> model = new UiListModel<>(22, placement -> placement.name + " " + placement.fileName);
    private int revision = -1;
    private boolean searching;

    private UiRowList<SharedPlacement> list;
    private UiTextField search;
    private UiButton searchButton, placements, menu;
    private UiWidget info;

    public GuiSharedPlacements(GuiScreen parent) {
        super(parent, UiTranslations.format(PREFIX + "title.manage_server_placements", "v" + Reference.VERSION));
    }

    @Override
    protected void createWidgets() {
        list = root.add(new UiRowList<>(model, Row::new));
        search = root.add(new UiTextField(fontRendererObj, 256, text -> { model.setQuery(text); list.sync(); }));
        search.setVisible(false);
        searchButton = root.add(new UiButton(() -> "", button -> {
            searching = !searching;
            search.setVisible(searching);
            if (searching) input.focus(search);
            else search.setText("");
        }).setSprite(UiSprite.SEARCH).setBackground(false));
        info = root.add(new UiWidget() {
            @Override public void draw(UiDraw draw, int mouseX, int mouseY) {
                draw.fill(bounds(), 0xA0000000);
                draw.border(bounds(), 0xFF999999);
                SharedPlacement selected = selected();
                if (selected != null) drawInfo(draw, selected, bounds().x + 3, bounds().y + 3, bounds().width - 4);
            }
        });
        placements = addButton("litematica.gui.button.change_menu.show_schematic_placements",
            () -> mc.displayGuiScreen(new GuiSchematicPlacementsList(this))).setSprite(UiSprite.SCHEMATIC_PLACEMENTS);
        menu = addButton("litematica.gui.button.change_menu.to_main_menu", this::mainMenu);
    }

    private SharedPlacement selected() {
        return model.selectedIndex() < 0 ? null : model.selected();
    }

    private static double distance(SharedPlacement placement, int dimension, double x, double y, double z) {
        double scale = placement.dimension == dimension ? 1 : placement.dimension == -1 ? 8 : dimension == -1 ? 1 / 8.0 : 1;
        double dx = placement.origin[0] * scale - x, dy = placement.origin[1] - y, dz = placement.origin[2] * scale - z;
        return dx * dx + dy * dy + dz * dz;
    }

    private void rebuild() {
        List<SharedPlacement> entries = new ArrayList<>(SharedPlacementsClient.INSTANCE.placements());
        if (mc.thePlayer != null) {
            int dimension = mc.thePlayer.dimension;
            double x = mc.thePlayer.posX, y = mc.thePlayer.posY, z = mc.thePlayer.posZ;
            entries.sort(Comparator.comparingInt((SharedPlacement placement) -> placement.dimension == dimension ? 0
                : placement.dimension == -1 && dimension == 0 || placement.dimension == 0 && dimension == -1 ? 1 : 2)
                .thenComparingDouble(placement -> distance(placement, dimension, x, y, z)));
        }
        model.setEntries(entries);
        list.sync();
    }

    @Override
    protected void tickScreen() {
        if (revision != SharedPlacementsClient.INSTANCE.revision() && list != null) {
            revision = SharedPlacementsClient.INSTANCE.revision();
            rebuild();
        }
    }

    @Override
    protected void layoutWidgets() {
        int listWidth = Math.max(24, width - 24 - INFO_WIDTH - 6);
        searchButton.setBounds(14, 35, 12, 12);
        search.setBounds(30, 34, Math.max(0, listWidth - 16), 14);
        list.setBounds(14, 51, listWidth, Math.max(0, height - 89));
        info.setBounds(width - 10 - INFO_WIDTH, 51, INFO_WIDTH, Math.min(310, Math.max(0, height - 89)));
        int w = fontRendererObj.getStringWidth(placements.label()) + 30;
        placements.setBounds(12, height - 26, w, 20);
        int menuWidth = fontRendererObj.getStringWidth(menu.label()) + 20;
        menu.setBounds(width - menuWidth - 10, height - 26, menuWidth, 20);
    }

    private void drawInfo(UiDraw draw, SharedPlacement placement, int x, int y, int width) {
        String[][] rows = {
            {"display_name", placement.name},
            {"file_name", placement.fileName},
            {"dimension_id", Integer.toString(placement.dimension)},
            {"position", String.format(Locale.ROOT, "%d %d %d", placement.origin[0], placement.origin[1], placement.origin[2])},
            {"owner", placement.ownerName},
            {"last_modified", placement.modifierName}};
        for (String[] row : rows) {
            draw.text(UiTranslations.format(PREFIX + "label.placement_info." + row[0]), x, y, 0xC0C0C0C0);
            draw.text(draw.trim(row[1], width - 8), x + 4, y += 12, 0xFFFFFFFF);
            y += 12;
        }
    }

    private final class Row extends UiPanel {
        private final SharedPlacement placement;
        private final int index;
        private final List<UiButton> buttons = new ArrayList<>();
        private int buttonsStart;

        Row(SharedPlacement placement, int index) {
            this.placement = placement;
            this.index = index;
            button(() -> UiTranslations.format(PREFIX + "button.remove"), () -> SharedPlacementsClient.INSTANCE.remove(placement.id));
            button(() -> UiTranslations.format(PREFIX + "button.material_gathering_placement"), () -> {
                SchematicWorld world = SharedPlacementsClient.INSTANCE.loaded(placement.id);
                if (world == null) return;
                MaterialList materials = MaterialList.placement(world);
                MaterialLists.setCurrent(materials);
                mc.displayGuiScreen(new GuiSchematicMaterials(GuiSharedPlacements.this, materials));
            });
            button(this::state, () -> {
                SharedPlacementsClient client = SharedPlacementsClient.INSTANCE;
                if (client.downloading(placement.id)) return;
                if (client.loaded(placement.id) != null) client.unload(placement.id, true);
                else client.load(placement);
            });
            setTooltip(placement.name, placement.fileName);
        }

        private void button(Supplier<String> label, Runnable action) {
            buttons.add(add(new UiButton(label, mouse -> { if (mouse == 0) action.run(); })));
        }

        private String state() {
            SharedPlacementsClient client = SharedPlacementsClient.INSTANCE;
            String key = client.downloading(placement.id) ? "downloading" : client.loaded(placement.id) != null ? "unload"
                : client.cached(placement) ? "load" : "download";
            return UiTranslations.format(PREFIX + "button." + key);
        }

        private int stateWidth() {
            int width = fontRendererObj.getStringWidth(buttons.get(2).label());
            for (String key : new String[] {"load", "unload"}) {
                width = Math.max(width, fontRendererObj.getStringWidth(UiTranslations.format(PREFIX + "button." + key)));
            }
            return width;
        }

        @Override
        public void layout(UiBounds screen) {
            int right = bounds().right() - 2;
            for (int i = 0; i < buttons.size(); i++) {
                UiButton button = buttons.get(i);
                int width = (i == 2 ? stateWidth() : fontRendererObj.getStringWidth(button.label())) + 10;
                button.setBounds(right - width, bounds().y + 1, width, 20);
                right -= width + 1;
            }
            buttonsStart = right;
        }

        @Override
        public void draw(UiDraw draw, int mouseX, int mouseY) {
            SharedPlacementsClient client = SharedPlacementsClient.INSTANCE;
            boolean loaded = client.loaded(placement.id) != null;
            buttons.get(1).setEnabled(loaded);
            buttons.get(2).setEnabled(!client.downloading(placement.id));
            layout(bounds());
            boolean chosen = model.selectedIndex() == index;
            draw.fill(bounds(), chosen || containsVisible(mouseX, mouseY) ? 0xA0707070 : index % 2 == 1 ? 0xA0101010 : 0xA0303030);
            if (chosen) draw.border(bounds(), 0xFFE0E0E0);
            UiSprite.schematicFile(placement.fileName).draw(draw, bounds().x + 2, bounds().y + 5, false, false);
            String name = (loaded ? "\u00a7a" : "") + placement.name;
            draw.text(draw.trim(name, buttonsStart - bounds().x - 24), bounds().x + 20, bounds().y + 7, 0xFFFFFFFF);
            super.draw(draw, mouseX, mouseY);
        }

        @Override
        public boolean isFocusable() { return true; }

        @Override
        public boolean mouseDown(int x, int y, int button) {
            if (button != 0 || x >= buttonsStart) return false;
            model.select(index);
            return true;
        }

        @Override
        public boolean keyTyped(char character, int keyCode) {
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_SPACE) {
                model.select(index);
                return true;
            }
            return false;
        }
    }
}

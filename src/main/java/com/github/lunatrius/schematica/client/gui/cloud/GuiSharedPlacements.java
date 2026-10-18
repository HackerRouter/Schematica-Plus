// Syncmatica server placement list, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.cloud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.GuiScreen;

import com.github.lunatrius.schematica.client.gui.control.GuiSchematicMaterials;
import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiDraw;
import com.github.lunatrius.schematica.client.gui.framework.UiListModel;
import com.github.lunatrius.schematica.client.gui.framework.UiPanel;
import com.github.lunatrius.schematica.client.gui.framework.UiRowList;
import com.github.lunatrius.schematica.client.gui.framework.UiScreen;
import com.github.lunatrius.schematica.client.gui.framework.UiTextField;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.gui.framework.UiWidget;
import com.github.lunatrius.schematica.client.gui.material.MaterialList;
import com.github.lunatrius.schematica.client.gui.material.MaterialLists;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.cloud.SharedPlacement;
import com.github.lunatrius.schematica.handler.client.SharedPlacementsClient;
import com.github.lunatrius.schematica.reference.Reference;

public final class GuiSharedPlacements extends UiScreen {
    private static final String PREFIX = "syncmatica.gui.";
    private static final int INFO_WIDTH = 170;
    private final UiListModel<SharedPlacement> model = new UiListModel<>(22, placement -> placement.name);
    private SharedPlacement selected;
    private int revision = -1;

    private UiWidget frame, info;
    private UiTextField search;
    private UiRowList<SharedPlacement> list;
    private UiButton menu;

    public GuiSharedPlacements(GuiScreen parent) {
        super(parent, UiTranslations.format(PREFIX + "title.manage_server_placements", "v" + Reference.VERSION));
    }

    @Override
    protected void createWidgets() {
        frame = root.add(new UiWidget() {
            @Override public void draw(UiDraw draw, int mouseX, int mouseY) {
                draw.fill(bounds(), 0xB0000000);
                draw.border(bounds(), 0xFF999999);
            }
        });
        search = root.add(new UiTextField(fontRendererObj, 256, text -> model.setQuery(text)));
        list = root.add(new UiRowList<>(model, Row::new));
        info = root.add(new UiWidget() {
            @Override public void draw(UiDraw draw, int mouseX, int mouseY) {
                if (selected == null) return;
                draw.fill(bounds(), 0xA0000000);
                draw.border(bounds(), 0xFF999999);
                drawInfo(draw, bounds().x + 3, bounds().y + 3, bounds().width - 6);
            }
        });
        menu = addButton("litematica.gui.button.change_menu.to_main_menu", this::mainMenu);
    }

    private static double distance(SharedPlacement placement, int dimension, double x, double y, double z) {
        double scale = placement.dimension == dimension ? 1 : placement.dimension == -1 ? 8 : dimension == -1 ? 1 / 8.0 : 1;
        double dx = placement.origin[0] * scale - x, dy = placement.origin[1] - y, dz = placement.origin[2] * scale - z;
        return dx * dx + dy * dy + dz * dz;
    }

    private void rebuild() {
        List<SharedPlacement> placements = new ArrayList<>(SharedPlacementsClient.INSTANCE.placements());
        if (mc.thePlayer != null) {
            int dimension = mc.thePlayer.dimension;
            double x = mc.thePlayer.posX, y = mc.thePlayer.posY, z = mc.thePlayer.posZ;
            placements.sort(Comparator.comparingInt((SharedPlacement placement) -> placement.dimension == dimension ? 0
                : placement.dimension == -1 && dimension == 0 || placement.dimension == 0 && dimension == -1 ? 1 : 2)
                .thenComparingDouble(placement -> distance(placement, dimension, x, y, z)));
        }
        model.setEntries(placements);
        if (selected != null) selected = SharedPlacementsClient.INSTANCE.get(selected.id);
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
        int x = 12, y = 30, totalWidth = width - 20, height = this.height - 68;
        int browserWidth = totalWidth - 6 - INFO_WIDTH;
        frame.setBounds(x, y, browserWidth, height);
        search.setBounds(x + 2, y + 4, browserWidth - 14, 14);
        list.setBounds(x + 2, y + 21, browserWidth - 4, Math.max(0, height - 23));
        info.setBounds(x + totalWidth - INFO_WIDTH, y, INFO_WIDTH, Math.min(290, height));
        int menuWidth = fontRendererObj.getStringWidth(menu.label()) + 20;
        menu.setBounds(width - menuWidth - 10, this.height - 26, menuWidth, 20);
    }

    private void drawInfo(UiDraw draw, int x, int y, int width) {
        SharedPlacement placement = selected;
        String[][] rows = {
            {"display_name", placement.name},
            {"file_name", placement.fileName},
            {"dimension_id", Integer.toString(placement.dimension)},
            {"position", String.format(Locale.ROOT, "%d %d %d", placement.origin[0], placement.origin[1], placement.origin[2])},
            {"owner", placement.ownerName},
            {"last_modified", placement.modifierName}};
        for (String[] row : rows) {
            draw.text(UiTranslations.format(PREFIX + "label.placement_info." + row[0]), x, y, 0xC0C0C0C0);
            draw.text(draw.trim(row[1], width - 4), x + 4, y += 12, 0xFFFFFFFF);
            y += 12;
        }
    }

    private final class Row extends UiPanel {
        private final SharedPlacement placement;
        private final int index;
        private final UiButton remove, materials, action;
        private int buttonsStart;

        Row(SharedPlacement placement, int index) {
            this.placement = placement;
            this.index = index;
            remove = add(new UiButton(() -> UiTranslations.format(PREFIX + "button.remove"), mouse -> {
                if (mouse == 0) SharedPlacementsClient.INSTANCE.remove(placement.id);
            }));
            materials = add(new UiButton(() -> UiTranslations.format(PREFIX + "button.material_gathering_placement"), mouse -> {
                SchematicWorld world = SharedPlacementsClient.INSTANCE.loaded(placement.id);
                if (mouse != 0 || world == null) return;
                MaterialList list = MaterialList.placement(world);
                MaterialLists.setCurrent(list);
                mc.displayGuiScreen(new GuiSchematicMaterials(GuiSharedPlacements.this, list));
            }));
            action = add(new UiButton(this::state, mouse -> {
                if (mouse != 0) return;
                SharedPlacementsClient client = SharedPlacementsClient.INSTANCE;
                if (client.downloading(placement.id)) return;
                if (client.loaded(placement.id) != null) client.unload(placement.id, true);
                else client.load(placement);
            }));
        }

        private String state() {
            SharedPlacementsClient client = SharedPlacementsClient.INSTANCE;
            String key = client.downloading(placement.id) ? "downloading" : client.loaded(placement.id) != null ? "unload"
                : client.cached(placement) ? "load" : "download";
            return UiTranslations.format(PREFIX + "button." + key);
        }

        private int actionWidth() {
            int widest = 0;
            for (String key : new String[] {"downloading", "download", "load", "unload"}) {
                widest = Math.max(widest, fontRendererObj.getStringWidth(UiTranslations.format(PREFIX + "button." + key)) + 10);
            }
            return widest;
        }

        @Override
        public void layout(UiBounds screen) {
            int right = bounds().right();
            int removeWidth = fontRendererObj.getStringWidth(remove.label()) + 10;
            remove.setBounds(right - removeWidth, bounds().y + 1, removeWidth, 20);
            right -= removeWidth + 2;
            int materialsWidth = fontRendererObj.getStringWidth(materials.label()) + 10;
            materials.setBounds(right - materialsWidth, bounds().y + 1, materialsWidth, 20);
            right -= materialsWidth;
            int width = actionWidth();
            action.setBounds(right - width, bounds().y + 1, width, 20);
            buttonsStart = right - width;
        }

        @Override
        public void draw(UiDraw draw, int mouseX, int mouseY) {
            SharedPlacementsClient client = SharedPlacementsClient.INSTANCE;
            materials.setEnabled(client.loaded(placement.id) != null);
            action.setEnabled(!client.downloading(placement.id));
            boolean chosen = selected != null && selected.id.equals(placement.id);
            int color = chosen || containsVisible(mouseX, mouseY) ? 0x70FFFFFF : index % 2 == 1 ? 0x20FFFFFF : 0x50FFFFFF;
            draw.fill(bounds(), color);
            draw.text(draw.trim(placement.name, buttonsStart - bounds().x - 24), bounds().x + 20, bounds().y + 7, 0xFFFFFFFF);
            super.draw(draw, mouseX, mouseY);
        }

        @Override
        public boolean isFocusable() { return true; }

        @Override
        public boolean mouseDown(int x, int y, int button) {
            if (button != 0 || x >= buttonsStart) return false;
            selected = placement;
            model.select(index);
            return true;
        }
    }
}

// SPDX-License-Identifier: LGPL-3.0-only
// Litematica material-list screen and rows, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.control;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;

import org.lwjgl.input.Keyboard;

import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.world.RenderLayerSettings;
import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiDraw;
import com.github.lunatrius.schematica.client.gui.framework.UiIntegerField;
import com.github.lunatrius.schematica.client.gui.framework.UiLabel;
import com.github.lunatrius.schematica.client.gui.framework.UiListModel;
import com.github.lunatrius.schematica.client.gui.framework.UiPanel;
import com.github.lunatrius.schematica.client.gui.framework.UiRowList;
import com.github.lunatrius.schematica.client.gui.framework.UiScreen;
import com.github.lunatrius.schematica.client.gui.framework.UiSprite;
import com.github.lunatrius.schematica.client.gui.framework.UiTextField;
import com.github.lunatrius.schematica.client.gui.framework.UiWidget;
import com.github.lunatrius.schematica.client.gui.material.MaterialColumns;
import com.github.lunatrius.schematica.client.gui.material.MaterialItemKey;
import com.github.lunatrius.schematica.client.gui.material.MaterialListExport;
import com.github.lunatrius.schematica.client.gui.material.MaterialListModel;
import com.github.lunatrius.schematica.client.gui.material.MaterialListModel.Entry;
import com.github.lunatrius.schematica.client.gui.material.MaterialScan;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Reference;

public class GuiSchematicMaterials extends UiScreen {
    private static final String PREFIX = "litematica.gui.button.material_list.";
    private static final String LABEL = "litematica.gui.label.material_list.";
    private static final String[] HEADERS = { "item", "total", "missing", "available" };
    private final SchematicWorld schematic;
    private final WorldClient openedWorld;
    private final MaterialListModel<MaterialItemKey> materials = new MaterialListModel<>();
    private final UiListModel<Entry<MaterialItemKey>> rowsModel = new UiListModel<>(22, entry -> entry.name);
    private final List<UiButton> primary = new ArrayList<>();
    private final List<UiButton> secondary = new ArrayList<>();
    private UiRowList<Entry<MaterialItemKey>> rows;
    private UiPanel header;
    private UiTextField search;
    private UiButton searchButton;
    private UiIntegerField multiplier;
    private UiLabel multiplierLabel;
    private UiButton refresh;
    private UiButton scope;
    private UiButton export;
    private UiButton menu;
    private UiWidget info;
    private UiLabel progress;
    private UiLabel empty;
    private MaterialScan scan;
    private boolean hasResult;
    private boolean searching;
    private boolean renderLayers;
    private int[] columns = {4, 44, 84, 124, 164};
    private int skipped;
    private int ticks;
    private int[] geometry;
    private Object source;
    private boolean placementEnabled;
    private List<String> transforms = Collections.emptyList();
    private String notice = "";
    private long noticeUntil;

    public GuiSchematicMaterials(GuiScreen parent) { this(parent, ClientProxy.schematic); }

    public GuiSchematicMaterials(GuiScreen parent, SchematicWorld schematic) {
        super(parent, UiTranslations.format("litematica.gui.title.material_list.placement", schematic == null ? "-" : schematic.name));
        this.schematic = schematic;
        openedWorld = Minecraft.getMinecraft().theWorld;
        materials.restoreSort(ConfigurationHandler.sortType);
    }

    @Override
    protected void createWidgets() {
        refresh = action("refresh_list", this::refresh);
        scope = root.add(new UiButton(() -> UiTranslations.format(PREFIX + "list_type", UiTranslations.format(
            "litematica.gui.label.block_info_list_type." + (renderLayers ? "render_layers" : "all"))), button -> {
                renderLayers = !renderLayers;
                refresh();
                layoutWidgets();
            }));
        scope.setTooltip(UiTranslations.format("schematica.ui.material.scope"));
        UiButton hide = root.add(new UiButton(() -> toggleLabel("hide_available", materials.hideAvailable()), button -> {
            materials.setHideAvailable(!materials.hideAvailable());
            refreshRows();
        }));
        UiButton hud = unavailable(root.add(new UiButton(() -> toggleLabel("toggle_info_hud", false), button -> {})));
        primary.addAll(Arrays.asList(refresh, scope, hide, hud));
        UiButton clearIgnored = action("clear_ignored", () -> { materials.clearIgnored(); refreshRows(); });
        UiButton cache = unavailable(action("clear_cache", () -> {}));
        cache.setTooltip(UiTranslations.format("schematica.ui.material.cache_pending"));
        export = action("write_to_file", this::export);
        export.setTooltip(UiTranslations.format("schematica.ui.material.export"));
        UiButton raw = unavailable(action("write_to_json", () -> {}));
        raw.setTooltip(UiTranslations.format("schematica.ui.material.raw_pending"));
        secondary.addAll(Arrays.asList(clearIgnored, cache, export, raw));
        multiplierLabel = root.add(new UiLabel(() -> UiTranslations.format(LABEL + "multiplier"), 0xFFFFFFFF));
        multiplier = root.add(new UiIntegerField(fontRendererObj, 1, 1, Integer.MAX_VALUE, value -> {
            materials.setMultiplier(value);
            if (rows != null) { refreshRows(); layoutWidgets(); }
        }));
        multiplier.setTooltip(UiTranslations.format("schematica.ui.material.multiplier"));
        info = root.add(new UiWidget() {
            @Override public void draw(UiDraw draw, int x, int y) {
                UiSprite.INFO.draw(draw, bounds().x, bounds().y, true, containsVisible(x, y));
            }
            @Override public List<String> tooltip(int x, int y) {
                long unknown = materials.progress()[4];
                return Arrays.asList(UiTranslations.format("schematica.ui.material.info"),
                    UiTranslations.format("schematica.ui.material.unverified", unknown, skipped));
            }
        });
        header = root.add(new UiPanel());
        for (int i = 0; i < 4; i++) header.add(new ColumnHeader(i));
        rows = root.add(new UiRowList<>(rowsModel, MaterialRow::new, 10));
        search = root.add(new UiTextField(fontRendererObj, 256, text -> {
            materials.setQuery(searching ? text : ""); refreshRows();
        }));
        searchButton = root.add(new UiButton(() -> "", button -> setSearching(!searching))
            .setSprite(UiSprite.SEARCH).setBackground(false));
        searchButton.setTooltip(UiTranslations.format("schematica.ui.browser.search_hint"));
        menu = addButton("litematica.gui.button.change_menu.to_main_menu", this::mainMenu);
        progress = root.add(new UiLabel(this::progressText, 0xFFFFFFFF));
        empty = root.add(new UiLabel(() -> UiTranslations.format("schematica.ui.material.empty"), 0xFFAAAAAA));
    }

    private UiButton action(String key, Runnable action) { return addButton(PREFIX + key, action); }

    private String toggleLabel(String key, boolean enabled) {
        return UiTranslations.format(PREFIX + key, UiTranslations.format("malilib.gui.label_colored." + (enabled ? "on" : "off")));
    }

    private boolean validContext() {
        return schematic != null && openedWorld != null && mc.theWorld == openedWorld && mc.thePlayer != null
            && ClientProxy.loadedSchematics.contains(schematic);
    }

    private int[] geometry() {
        return new int[] {schematic.position.x, schematic.position.y, schematic.position.z,
            schematic.getWidth(), schematic.getHeight(), schematic.getLength(),
            renderLayers && schematic.isRenderingLayer ? schematic.renderingLayer : -1};
    }

    @Override
    protected void opened() { if (scan == null && !hasResult) refresh(); }

    @Override
    protected int titleRightMargin() { return 50; }

    private long layerRevision;

    private boolean geometryChanged() {
        if (renderLayers && layerRevision != RenderLayerSettings.RANGE.revision()) return true;
        return placementEnabled != schematic.isEnabled() || !Arrays.equals(geometry, geometry()) || source != schematic.getSchematic()
            || !transforms.equals(schematic.transformOperations);
    }

    private void refresh() {
        scan = null;
        hasResult = false;
        skipped = 0;
        notice = "";
        materials.setEntries(Collections.emptyList());
        refreshRows();
        if (validContext()) {
            geometry = geometry();
            layerRevision = RenderLayerSettings.RANGE.revision();
            source = schematic.getSchematic();
            placementEnabled = schematic.isEnabled();
            transforms = new ArrayList<>(schematic.transformOperations);
            scan = new MaterialScan(schematic, openedWorld, mc.thePlayer, renderLayers);
        }
        updateButtons();
    }

    private void refreshRows() {
        if (rows == null) return;
        rowsModel.setEntries(materials.visible());
        updateColumns();
        rows.sync();
        if (empty != null) empty.setVisible(hasResult && rowsModel.entries().isEmpty());
    }

    private void updateColumns() {
        int name = fontRendererObj.getStringWidth("§l" + UiTranslations.format(LABEL + "title.item"));
        int[] counts = new int[3];
        for (int i = 0; i < 3; i++) counts[i] = fontRendererObj.getStringWidth("§l" + UiTranslations.format(LABEL + "title." + HEADERS[i + 1]));
        for (Entry<MaterialItemKey> entry : materials.entries()) {
            name = Math.max(name, fontRendererObj.getStringWidth(entry.name));
            counts[0] = Math.max(counts[0], fontRendererObj.getStringWidth(Long.toString(materials.total(entry))));
            counts[1] = Math.max(counts[1], fontRendererObj.getStringWidth(Long.toString(materials.missing(entry))));
            counts[2] = Math.max(counts[2], fontRendererObj.getStringWidth(Long.toString(entry.available)));
        }
        columns = MaterialColumns.positions(Math.max(0, width - 34), name, counts,
            fontRendererObj.getStringWidth(UiTranslations.format(PREFIX + "ignore")) + 10);
        if (header != null) layoutHeader();
    }

    private void updateButtons() {
        boolean valid = validContext();
        refresh.setEnabled(valid);
        scope.setEnabled(valid);
        export.setEnabled(valid && hasResult && !rowsModel.entries().isEmpty());
        multiplier.setEnabled(valid);
    }

    @Override
    protected void tickScreen() {
        if (!validContext()) {
            if (scan != null || hasResult) refresh();
            updateButtons();
            return;
        }
        if (scan == null && !hasResult || geometryChanged()) refresh();
        if (scan != null) {
            scan.step();
            if (scan.done()) {
                materials.setEntries(scan.result());
                skipped = scan.skipped();
                scan = null;
                hasResult = true;
                refreshRows();
            }
        } else if (++ticks % 20 == 0 && MaterialScan.updateAvailable(materials.entries(), mc.thePlayer)) refreshRows();
        updateButtons();
        progress.setTooltip(progressText(), UiTranslations.format("schematica.ui.material.unverified", materials.progress()[4], skipped));
    }

    @Override
    protected void closed() { scan = null; }

    private int buttonWidth(UiButton button) {
        if (button == primary.get(2)) return Math.max(fontRendererObj.getStringWidth(toggleLabel("hide_available", false)),
            fontRendererObj.getStringWidth(toggleLabel("hide_available", true))) + 10;
        return fontRendererObj.getStringWidth(button.label()) + 10;
    }

    @Override
    protected void layoutWidgets() {
        int multiplierWidth = fontRendererObj.getStringWidth(UiTranslations.format(LABEL + "multiplier"));
        int multiplierX = width - multiplierWidth - 56;
        multiplierLabel.setBounds(multiplierX, 29, multiplierWidth, 12);
        multiplier.setBounds(width - 52, 26, 40, 16);
        info.setBounds(width - 23, 10, 11, 11);
        int total = multiplierWidth + 130;
        for (UiButton button : primary) total += button == primary.get(2) || button == primary.get(3)
            ? buttonWidth(button) : fontRendererObj.getStringWidth(button.label());
        for (UiButton button : secondary) total += fontRendererObj.getStringWidth(button.label());
        boolean narrow = width < total;
        int x = 12;
        int y = 24;
        for (UiButton button : primary) {
            int w = buttonWidth(button);
            int right = y == 24 ? multiplierX - 4 : width - 12;
            if (x > 12 && x + w > right) { x = 12; y += 22; }
            button.setBounds(x, y, w, 20);
            x += w + 1;
        }
        int browserY = y + 20;
        int footerY = height - 36;
        int menuWidth = fontRendererObj.getStringWidth(menu.label()) + 20;
        if (narrow) {
            int rowCount = 1;
            int used = 12;
            for (UiButton button : secondary) {
                int w = buttonWidth(button);
                if (used > 12 && used + w > width - 12) { rowCount++; used = 12; }
                used += w + 1;
            }
            x = 12;
            y = height - rowCount * 22;
            if (rowCount > 1 || used > width - menuWidth - 14) footerY = y - 22;
            for (UiButton button : secondary) {
                int w = buttonWidth(button);
                if (x > 12 && x + w > width - 12) { x = 12; y += 22; }
                button.setBounds(x, y, w, 20);
                x += w + 1;
            }
        } else {
            for (UiButton button : secondary) {
                int w = buttonWidth(button);
                button.setBounds(x, y, w, 20);
                x += w + 1;
            }
        }
        menu.setBounds(width - menuWidth - 10, footerY, menuWidth, 20);
        progress.setBounds(12, footerY, Math.max(0, menu.bounds().x - 20), 12);
        header.setBounds(12, browserY + 4, Math.max(0, width - 34), 22);
        searchButton.setBounds(width - 37, browserY + 9, 12, 12);
        search.setBounds(13, browserY + 8, Math.max(0, width - 55), 14);
        search.setVisible(searching);
        header.setVisible(!searching);
        rows.setBounds(12, browserY + 26, Math.max(0, width - 24), Math.max(0, footerY - browserY - 26));
        empty.setBounds(16, browserY + 31, width - 40, 14);
        empty.setVisible(hasResult && rowsModel.entries().isEmpty());
        updateColumns();
    }

    private void layoutHeader() {
        for (int i = 0; i < 4; i++) header.children().get(i).setBounds(header.bounds().x + columns[i] - 3,
            header.bounds().y + 1, Math.max(0, columns[i + 1] - columns[i] - 2), 20);
    }

    private void setSearching(boolean searching) {
        this.searching = searching;
        materials.setQuery(searching ? search.text() : "");
        refreshRows();
        layoutWidgets();
        if (searching) input.focus(search);
    }

    @Override
    protected boolean interceptKey(char character, int code) {
        if (code == Keyboard.KEY_ESCAPE && searching && !isShiftKeyDown()) { setSearching(false); return true; }
        return false;
    }

    @Override
    protected boolean handleKey(char character, int code) {
        if (code == Keyboard.KEY_F5 && !(input.focused() instanceof UiTextField)) { refresh(); return true; }
        if (!(input.focused() instanceof UiTextField) && character > 32 && character != 127 && !isCtrlKeyDown()) {
            setSearching(true);
            search.setText(Character.toString(character));
            return true;
        }
        return false;
    }

    private String progressText() {
        if (!validContext()) return UiTranslations.format("schematica.ui.material.context");
        if (scan != null) return UiTranslations.format("schematica.ui.material.scanning", scan.percent());
        if (System.nanoTime() < noticeUntil) return notice;
        long[] counts = materials.progress();
        if (counts[4] > 0 || skipped > 0) return UiTranslations.format("schematica.ui.material.unverified", counts[4], skipped);
        if (counts[0] == 0) return UiTranslations.format(LABEL + "total", 0);
        String done = UiTranslations.format(LABEL + "progress.done", percent(counts[1], counts[0]));
        String missing = UiTranslations.format(LABEL + "progress.missing", percent(counts[2], counts[0]));
        String wrong = UiTranslations.format(LABEL + "progress.mismatch", percent(counts[3], counts[0]));
        return UiTranslations.format(LABEL + "total", counts[0]) + " / "
            + UiTranslations.format(LABEL + "progress", done + " / " + missing + " / " + wrong);
    }

    private String percent(long part, long total) { return String.format(Locale.ROOT, "%.1f %%", part * 100.0 / total); }

    private void export() {
        if (!validContext() || !hasResult) return;
        if (geometryChanged()) { refresh(); return; }
        MaterialListExport.Format format = Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU)
            ? MaterialListExport.Format.JSON
            : isShiftKeyDown() ? MaterialListExport.Format.CSV : MaterialListExport.Format.TXT;
        try {
            String title = schematic.name + " (" + (renderLayers ? "render layers" : "all") + ")";
            String text = MaterialListExport.format(materials, title, format, key -> {
                NBTTagCompound tag = new NBTTagCompound();
                key.stack().writeToNBT(tag);
                return tag.toString();
            });
            Path path = MaterialListExport.write(SchematicaPlus.proxy.getDirectory("dumps").toPath(), format, text);
            notice = UiTranslations.format("litematica.message.material_list_written_to_file", path.getFileName().toString());
            mc.thePlayer.addChatMessage(new ChatComponentText(notice + "\n" + path.toAbsolutePath()));
        } catch (Exception e) {
            Reference.logger.error("Could not export the material list", e);
            notice = UiTranslations.format("schematica.ui.material.export_failed");
        }
        noticeUntil = System.nanoTime() + 5_000_000_000L;
    }

    private final class ColumnHeader extends UiWidget {
        private final int column;
        ColumnHeader(int column) { this.column = column; setTooltip(UiTranslations.format(LABEL + "title." + HEADERS[column])); }
        @Override public boolean isFocusable() { return true; }
        @Override public boolean mouseDown(int x, int y, int button) { if (button != 0) return false; sort(); return true; }
        @Override public boolean keyTyped(char character, int code) {
            if (code != Keyboard.KEY_RETURN && code != Keyboard.KEY_SPACE) return false;
            sort(); return true;
        }
        private void sort() {
            materials.sortBy(MaterialListModel.Sort.values()[column]);
            ConfigurationHandler.propSortType.set(materials.savedSort());
            ConfigurationHandler.loadConfiguration();
            refreshRows();
        }
        @Override public void draw(UiDraw draw, int x, int y) {
            draw.fill(bounds(), 0xA0101010);
            draw.border(bounds(), containsVisible(x, y) || isFocused() ? 0xFFFFFFFF : 0xC0707070);
            boolean sorted = materials.sort().ordinal() == column;
            draw.text(draw.trim("§l" + UiTranslations.format(LABEL + "title." + HEADERS[column]), bounds().width - (sorted ? 21 : 4)),
                bounds().x + 3, bounds().y + 6, 0xFFFFFFFF);
            if (sorted) (materials.descending() ? UiSprite.SORT_DOWN : UiSprite.SORT_UP)
                .draw(draw, bounds().right() - 16, bounds().y + 2, true, containsVisible(x, y));
        }
    }

    private final class MaterialRow extends UiPanel {
        private final Entry<MaterialItemKey> entry;
        private final ItemStack stack;
        private final int index;
        private final UiButton ignore;
        private boolean failedIcon;

        MaterialRow(Entry<MaterialItemKey> entry, int index) {
            this.entry = entry;
            this.index = index;
            stack = entry.key.stack();
            ignore = add(new UiButton(() -> UiTranslations.format(PREFIX + "ignore"), button -> {
                if (button == 0) { materials.ignore(entry.key); refreshRows(); }
            }));
        }
        @Override public void layout(UiBounds screen) {
            int w = fontRendererObj.getStringWidth(ignore.label()) + 10;
            ignore.setBounds(bounds().right() - w, bounds().y + 1, w, 20);
        }
        @Override public void draw(UiDraw draw, int mouseX, int mouseY) {
            draw.fill(bounds(), containsVisible(mouseX, mouseY) ? 0xA0707070 : index % 2 == 0 ? 0xA0303030 : 0xA0101010);
            int y = bounds().y + 7;
            int x = bounds().x;
            draw.text(draw.trim(entry.name, Math.max(0, columns[1] - columns[0] - 24)), x + columns[0] + 20, y, 0xFFFFFFFF);
            long missing = materials.missing(entry);
            drawCount(draw, 1, materials.total(entry), 0xFFFFFFFF);
            drawCount(draw, 2, missing, missing == 0 ? 0xFF55FF55 : entry.available >= missing ? 0xFFFFAA00 : 0xFFFF5555);
            drawCount(draw, 3, entry.available, entry.available >= missing ? 0xFF55FF55 : 0xFFFF5555);
            draw.fill(new UiBounds(x + columns[0], bounds().y + 3, 16, 16), 0x20FFFFFF);
            renderItem(draw, x + columns[0], bounds().y + 3);
            super.draw(draw, mouseX, mouseY);
        }
        private void drawCount(UiDraw draw, int column, long count, int color) {
            draw.text(draw.trim(Long.toString(count), Math.max(0, columns[column + 1] - columns[column] - 4)),
                bounds().x + columns[column], bounds().y + 7, color);
        }
        private void renderItem(UiDraw draw, int x, int y) {
            if (failedIcon) { draw.text("?", x + 4, y + 4, 0xFFFF5555); return; }
            try { draw.item(stack, x, y); }
            catch (Exception e) { failedIcon = true; Reference.logger.debug("Could not render material icon {}", entry.registryName, e); }
        }
        @Override public boolean drawTooltip(UiDraw draw, int mouseX, int mouseY, UiBounds screen) {
            int labels = 0;
            for (int i = 0; i < 3; i++) labels = Math.max(labels, draw.textWidth("§l" + UiTranslations.format(LABEL + "title." + HEADERS[i])));
            String total = MaterialColumns.stackCount(materials.total(entry), stack.getMaxStackSize());
            String missing = MaterialColumns.stackCount(materials.missing(entry), stack.getMaxStackSize());
            int values = Math.max(draw.textWidth(entry.name) + 20, Math.max(draw.textWidth(total), draw.textWidth(missing)));
            int w = Math.min(screen.width - 8, labels + values + 60);
            int x = Math.max(4, Math.min(mouseX + 10, screen.width - w - 4));
            int y = Math.max(4, Math.min(mouseY - 10, screen.height - 64));
            UiBounds box = new UiBounds(x, y, w, 60);
            draw.fill(box, 0xFF000000);
            draw.border(box, 0xFF808080);
            int valueX = x + labels + 30;
            for (int i = 0; i < 3; i++) draw.text("§l" + UiTranslations.format(LABEL + "title." + HEADERS[i]), x + 10, y + 10 + i * 16, 0xFFFFFFFF);
            try (UiDraw.Clip ignored = draw.clip(box.inset(2))) {
                renderItem(draw, valueX, y + 6);
                draw.text(draw.trim(entry.name, box.right() - valueX - 28), valueX + 20, y + 10, 0xFFFFFFFF);
                draw.text(draw.trim(total, box.right() - valueX - 8), valueX, y + 26, 0xFFFFFFFF);
                draw.text(draw.trim(missing, box.right() - valueX - 8), valueX, y + 42, 0xFFFFFFFF);
            }
            return true;
        }
    }
}

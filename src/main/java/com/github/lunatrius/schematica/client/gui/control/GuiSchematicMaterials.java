// SPDX-License-Identifier: LGPL-3.0-only
// Litematica material-list screen and rows, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.control;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;

import org.lwjgl.input.Keyboard;

import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
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
import com.github.lunatrius.schematica.client.gui.material.MaterialCache;
import com.github.lunatrius.schematica.client.gui.material.MaterialColumns;
import com.github.lunatrius.schematica.client.gui.material.MaterialList;
import com.github.lunatrius.schematica.client.gui.material.MaterialLists;
import com.github.lunatrius.schematica.client.gui.material.RawMaterialExport;
import com.github.lunatrius.schematica.client.gui.material.RecipeIndex;
import com.github.lunatrius.schematica.client.gui.material.MaterialItemKey;
import com.github.lunatrius.schematica.client.gui.material.MaterialListExport;
import com.github.lunatrius.schematica.client.gui.material.MaterialListModel;
import com.github.lunatrius.schematica.client.gui.material.MaterialListModel.Entry;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.handler.client.WorldHandler;
import com.github.lunatrius.schematica.reference.Reference;

public class GuiSchematicMaterials extends UiScreen {
    private static final String PREFIX = "litematica.gui.button.material_list.";
    private static final String LABEL = "litematica.gui.label.material_list.";
    private static final String[] HEADERS = { "item", "total", "missing", "available" };
    private final MaterialList list;
    private final MaterialListModel<MaterialItemKey> materials;
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
    private UiButton hide;
    private UiButton hud;
    private UiButton export;
    private UiButton raw;
    private UiButton menu;
    private UiWidget info;
    private UiLabel progress;
    private boolean searching;
    private int[] columns = {4, 44, 84, 124, 164};
    private int shownRevision = -1;
    private String notice = "";
    private long noticeUntil;

    public GuiSchematicMaterials(GuiScreen parent, MaterialList list) {
        super(parent, list.title());
        this.list = list;
        materials = list.model();
        // Remember the last opened material list, for the hotkey
        if (MaterialLists.current() == null) MaterialLists.setCurrent(list);
    }

    @Override
    protected void createWidgets() {
        refresh = action("refresh_list", list::refresh);
        scope = root.add(new UiButton(() -> UiTranslations.format(PREFIX + "list_type", UiTranslations.format(
            "litematica.gui.label.block_info_list_type." + (list.renderLayers() ? "render_layers" : "all"))), button -> {
                list.setRenderLayers(!list.renderLayers());
                saveSettings();
                layoutWidgets();
            }));
        scope.setTooltip(UiTranslations.format(list.kind == MaterialList.Kind.AREA ? "schematica.ui.area.analysis_scope" : "schematica.ui.material.scope"));
        scope.setVisible(list.supportsRenderLayers());
        hide = root.add(new UiButton(() -> toggleLabel("hide_available", materials.hideAvailable()), button -> {
            materials.setHideAvailable(!materials.hideAvailable());
            saveSettings();
            refreshRows();
        }));
        hud = root.add(new UiButton(() -> toggleLabel("toggle_info_hud", list.hud()), button -> {
            list.setHud(!list.hud());
            if (list.hud()) MaterialLists.setCurrent(list);
        }));
        UiButton all = root.add(new UiButton(() -> UiTranslations.format("schematica.ui.material.all_placements"), button -> {
            if (button == 0) mc.displayGuiScreen(new GuiSchematicMaterials(this, MaterialList.combined()));
        }));
        all.setTooltip(UiTranslations.format("schematica.ui.material.all_placements.hover").split("\\n"));
        all.setVisible(list.kind == MaterialList.Kind.PLACEMENT && com.github.lunatrius.schematica.proxy.ClientProxy.loadedSchematics.size() > 1);
        primary.addAll(Arrays.asList(refresh, scope, hide, hud, all));
        UiButton clearIgnored = action("clear_ignored", () -> { materials.clearIgnored(); saveSettings(); refreshRows(); });
        UiButton cache = action("clear_cache", () -> {
            MaterialCache.INSTANCE.clear();
            showNotice(UiTranslations.format("litematica.message.material_list.material_cache_cleared"));
        });
        cache.setTooltip(UiTranslations.format("litematica.gui.button.hover.material_list.clear_cache").split("\\n"));
        export = action("write_to_file", this::export);
        export.setTooltip(UiTranslations.format("litematica.gui.button.hover.material_list.write_hold_shift_for_csv").split("\\n"));
        raw = action("write_to_json", this::exportRaw);
        raw.setTooltip(UiTranslations.format("litematica.gui.button.hover.material_list.json_hold_shift_for_missing_only").split("\\n"));
        secondary.addAll(Arrays.asList(clearIgnored, cache, export, raw));
        if (com.github.lunatrius.schematica.compat.nei.NeiBridge.available()) {
            UiButton nei = addButton("schematica.nei.send", this::sendToNei);
            nei.setTooltip(UiTranslations.format("schematica.nei.send.hover").split("\\n"));
            secondary.add(nei);
        }
        multiplierLabel = root.add(new UiLabel(() -> UiTranslations.format(LABEL + "multiplier"), 0xFFFFFFFF));
        multiplier = root.add(new UiIntegerField(fontRendererObj, materials.multiplier(), 1, Integer.MAX_VALUE, value -> {
            materials.setMultiplier(value);
            saveSettings();
            if (rows != null) { refreshRows(); layoutWidgets(); }
        }));
        info = root.add(new UiWidget() {
            @Override public void draw(UiDraw draw, int x, int y) {
                UiSprite.INFO.draw(draw, bounds().x, bounds().y, true, containsVisible(x, y));
            }
            @Override public List<String> tooltip(int x, int y) {
                List<String> lines = new ArrayList<>(Arrays.asList(UiTranslations.format("litematica.info.material_list").split("\\n")));
                lines.add("");
                lines.add(UiTranslations.format(list.kind == MaterialList.Kind.AREA ? "schematica.ui.area.analysis_info" : "schematica.ui.material.info"));
                lines.add(incompleteText());
                return lines;
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
        menu = addButton("litematica.gui.button.change_menu.to_main_menu", this::mainMenu);
        progress = root.add(new UiLabel(this::progressText, 0xFFFFFFFF));
        if (mc.thePlayer == null) showNotice(UiTranslations.format("litematica.message.warn.material_list.no_player_inv").replace('\n', ' '));
    }

    private void saveSettings() {
        if (list.kind == MaterialList.Kind.PLACEMENT) WorldHandler.INSTANCE.saveSession();
    }

    private void showNotice(String text) {
        notice = text;
        noticeUntil = System.nanoTime() + 5_000_000_000L;
    }

    private UiButton action(String key, Runnable action) { return addButton(PREFIX + key, action); }

    private String toggleLabel(String key, boolean enabled) {
        return UiTranslations.format(PREFIX + key, UiTranslations.format("malilib.gui.label_colored." + (enabled ? "on" : "off")));
    }

    @Override
    protected int titleRightMargin() { return 50; }

    private void refreshRows() {
        if (rows == null) return;
        rowsModel.setEntries(materials.visible());
        updateColumns();
        rows.sync();
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
        columns = MaterialColumns.positions(Math.max(0, width - 34), name, counts, rowButtonsWidth());
        if (header != null) layoutHeader();
    }

    private int rowButtonsWidth() {
        return fontRendererObj.getStringWidth(UiTranslations.format(PREFIX + "ignore")) + 10
            + fontRendererObj.getStringWidth(UiTranslations.format("schematica.ui.material.replace")) + 11 + 21;
    }

    /** Counts this material as the held item (LitematList's replace); with an empty hand or a right click the replacements into it are undone. */
    private void replace(Entry<MaterialItemKey> entry, int button) {
        ItemStack held = mc.thePlayer == null ? null : mc.thePlayer.getHeldItem();
        if (button == 1 || held == null || held.getItem() == null) {
            if (materials.replacedNames(entry.key).isEmpty()) {
                if (button == 0) showNotice(UiTranslations.format("schematica.ui.material.replace_hold"));
                return;
            }
            materials.restoreReplaced(entry.key);
        } else {
            ItemStack copy = held.copy();
            copy.stackSize = 1;
            materials.replace(entry.key, new MaterialListModel.Replacement<>(new MaterialItemKey(copy), copy.getDisplayName(),
                String.valueOf(cpw.mods.fml.common.registry.GameData.getItemRegistry().getNameForObject(copy.getItem()))));
        }
        com.github.lunatrius.schematica.client.gui.material.MaterialScan.updateAvailable(materials.entries(), mc.thePlayer);
        saveSettings();
        refreshRows();
    }

    /** A new NEI bookmark group in crafting chain mode with the missing counts (Shift: the totals), ignored rows left out. */
    private void sendToNei() {
        if (!list.hasResult()) return;
        boolean totals = isShiftKeyDown();
        List<ItemStack> stacks = new ArrayList<>();
        for (Entry<MaterialItemKey> entry : materials.entries()) {
            if (materials.ignored().contains(entry.key)) continue;
            long count = totals ? materials.total(entry) : materials.missing(entry);
            if (count <= 0) continue;
            ItemStack stack = entry.key.stack();
            stack.stackSize = (int) Math.min(Integer.MAX_VALUE, count);
            stacks.add(stack);
        }
        int sent = com.github.lunatrius.schematica.compat.nei.NeiBridge.sendGroup(stacks);
        showNotice(UiTranslations.format(sent < 0 ? "schematica.nei.send_failed" : "schematica.nei.sent", Math.max(0, sent)));
    }

    private void updateButtons() {
        boolean valid = list.validContext();
        refresh.setEnabled(valid);
        scope.setEnabled(valid);
        export.setEnabled(valid && list.hasResult() && !rowsModel.entries().isEmpty());
        raw.setEnabled(valid && list.hasResult() && !materials.entries().isEmpty());
        multiplier.setEnabled(valid);
    }

    @Override
    protected void tickScreen() {
        list.tick();
        if (shownRevision != list.revision()) { shownRevision = list.revision(); refreshRows(); }
        updateButtons();
        progress.setTooltip(progressText(), incompleteText());
    }

    private int buttonWidth(UiButton button) {
        if (button == hide || button == hud) {
            String key = button == hide ? "hide_available" : "toggle_info_hud";
            return Math.max(fontRendererObj.getStringWidth(toggleLabel(key, false)), fontRendererObj.getStringWidth(toggleLabel(key, true))) + 10;
        }
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
        for (UiButton button : primary) if (button.isVisible()) total += button == hide || button == hud
            ? buttonWidth(button) : fontRendererObj.getStringWidth(button.label());
        for (UiButton button : secondary) total += fontRendererObj.getStringWidth(button.label());
        boolean narrow = width < total;
        int x = 12;
        int y = 24;
        for (UiButton button : primary) {
            if (!button.isVisible()) continue;
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
        if (code == Keyboard.KEY_F5 && !(input.focused() instanceof UiTextField)) { list.refresh(); return true; }
        if (!(input.focused() instanceof UiTextField) && character > 32 && character != 127 && !isCtrlKeyDown()) {
            setSearching(true);
            search.setText(Character.toString(character));
            return true;
        }
        return false;
    }

    private String progressText() {
        if (!list.validContext()) return UiTranslations.format("schematica.ui.material.context");
        if (!list.scanError().isEmpty()) return list.scanError();
        if (list.scanning()) return UiTranslations.format("schematica.ui.material.scanning", list.scanPercent());
        if (System.nanoTime() < noticeUntil) return notice;
        long[] counts = materials.progress();
        if (counts[4] > 0 || list.unverified() > 0 || list.skipped() > 0) return incompleteText();
        if (list.kind != MaterialList.Kind.PLACEMENT) return UiTranslations.format(LABEL + "total", counts[0]);
        if (counts[0] == 0) return UiTranslations.format(LABEL + "total", 0);
        String done = UiTranslations.format(LABEL + "progress.done", percent(counts[1], counts[0]));
        String missing = UiTranslations.format(LABEL + "progress.missing", percent(counts[2], counts[0]));
        String wrong = UiTranslations.format(LABEL + "progress.mismatch", percent(counts[3], counts[0]));
        return UiTranslations.format(LABEL + "total", counts[0]) + " / "
            + UiTranslations.format(LABEL + "progress", done + " / " + missing + " / " + wrong);
    }

    private String incompleteText() {
        boolean area = list.kind == MaterialList.Kind.AREA;
        return UiTranslations.format(area ? "schematica.ui.area.analysis_incomplete" : "schematica.ui.material.unverified",
            area ? list.unverified() : materials.progress()[4], list.skipped());
    }

    private String percent(long part, long total) { return String.format(Locale.ROOT, "%.1f %%", part * 100.0 / total); }

    private String exportTitle() {
        if (list.kind == MaterialList.Kind.SCHEMATIC) return list.fileName();
        return list.name() + " (" + UiTranslations.format(list.renderLayers()
            ? "litematica.gui.label.block_info_list_type.render_layers" : "litematica.gui.label.block_info_list_type.all") + ")";
    }

    private boolean exportable() {
        if (!list.validContext() || !list.hasResult()) return false;
        if (list.geometryChanged()) { list.refresh(); return false; }
        return true;
    }

    private void export() {
        if (!exportable()) return;
        MaterialListExport.Format format = Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU)
            ? MaterialListExport.Format.JSON
            : isShiftKeyDown() ? MaterialListExport.Format.CSV : MaterialListExport.Format.TXT;
        try {
            String text = MaterialListExport.format(materials, exportTitle(), format, key -> {
                NBTTagCompound tag = new NBTTagCompound();
                key.stack().writeToNBT(tag);
                return tag.toString();
            }, UiTranslations::format);
            if (list.kind == MaterialList.Kind.AREA) text = MaterialListExport.withAnalysisStatus(text, format, list.unverified(), list.skipped(), UiTranslations::format);
            Path path = MaterialListExport.write(SchematicaPlus.proxy.getDirectory("dumps").toPath(), format, text);
            showNotice(UiTranslations.format("litematica.message.material_list_written_to_file", path.getFileName().toString()));
            mc.thePlayer.addChatMessage(new ChatComponentText(notice + "\n" + path.toAbsolutePath()));
        } catch (Exception e) {
            Reference.logger.error("Could not export the material list", e);
            showNotice(UiTranslations.format("schematica.ui.material.export_failed"));
        }
    }

    /** write_to_json: the raw materials, resolved through the crafting and smelting recipes, as three JSON files. */
    private void exportRaw() {
        if (!exportable()) return;
        boolean missingOnly = isShiftKeyDown();
        boolean craftingOnly = Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
        try {
            List<Path> files = RawMaterialExport.write(SchematicaPlus.proxy.getDirectory("dumps").toPath(), materials, missingOnly, craftingOnly,
                ConfigurationHandler.materialListRecipeDetails, RecipeIndex.fromGame());
            if (files.isEmpty()) {
                showNotice(UiTranslations.format("litematica.message.error.json_material_list_copy_failure"));
                return;
            }
            Path last = files.get(files.size() - 1);
            showNotice(UiTranslations.format("litematica.message.material_list_written_to_json_file", last.getFileName().toString()));
            mc.thePlayer.addChatMessage(new ChatComponentText(notice + "\n" + last.toAbsolutePath()));
        } catch (Exception e) {
            Reference.logger.error("Could not export the raw material list", e);
            showNotice(UiTranslations.format("litematica.message.error.json_material_list_write_failure"));
        }
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
            saveSettings();
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
        private final UiButton ignore, replace, star;
        private boolean failedIcon;

        MaterialRow(Entry<MaterialItemKey> entry, int index) {
            this.entry = entry;
            this.index = index;
            stack = entry.key.stack();
            ignore = add(new UiButton(() -> UiTranslations.format(PREFIX + "ignore"), button -> {
                if (button == 0) { materials.ignore(entry.key); saveSettings(); refreshRows(); }
            }));
            replace = add(new UiButton(() -> UiTranslations.format("schematica.ui.material.replace"), button -> replace(entry, button)));
            replace.setTooltip(UiTranslations.format("schematica.ui.material.replace.hover").split("\\n"));
            star = add(new UiButton(() -> materials.isStarred(entry.key) ? "\u00a7e\u2605" : "\u2606", button -> {
                if (button == 0) { materials.toggleStar(entry.key); saveSettings(); refreshRows(); }
            }));
            star.setTooltip(UiTranslations.format("schematica.ui.material.star"));
        }
        @Override public void layout(UiBounds screen) {
            int w = fontRendererObj.getStringWidth(ignore.label()) + 10;
            ignore.setBounds(bounds().right() - w, bounds().y + 1, w, 20);
            int r = fontRendererObj.getStringWidth(replace.label()) + 10;
            replace.setBounds(ignore.bounds().x - r - 1, bounds().y + 1, r, 20);
            star.setBounds(replace.bounds().x - 21, bounds().y + 1, 20, 20);
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
            if (!containsVisible(mouseX, mouseY) || ignore.bounds().contains(mouseX, mouseY) || replace.bounds().contains(mouseX, mouseY)
                || star.bounds().contains(mouseX, mouseY)) return false;
            int size = stack.getMaxStackSize();
            long missing = materials.missing(entry);
            List<String[]> lines = new ArrayList<>();
            lines.add(new String[] {"§l" + UiTranslations.format(LABEL + "title." + HEADERS[1]), MaterialColumns.stackCount(materials.total(entry), size)});
            lines.add(new String[] {"§l" + UiTranslations.format(LABEL + "title." + HEADERS[2]), MaterialColumns.stackCount(missing, size)});
            // LitematList's magnitude conversion, in 1.7.10 chests
            lines.add(new String[] {"§l" + UiTranslations.format("schematica.ui.material.storage"),
                MaterialColumns.storage(missing > 0 ? missing : materials.total(entry), size, UiTranslations::format)});
            List<String> replaced = materials.replacedNames(entry.key);
            if (!replaced.isEmpty()) lines.add(new String[] {"§l" + UiTranslations.format("schematica.ui.material.replaces"), String.join(", ", replaced)});
            int labels = 0, values = draw.textWidth(entry.name) + 20;
            for (String[] line : lines) { labels = Math.max(labels, draw.textWidth(line[0])); values = Math.max(values, draw.textWidth(line[1])); }
            int w = Math.min(screen.width - 8, labels + values + 40), h = 28 + lines.size() * 16;
            int x = Math.max(4, Math.min(mouseX + 10, screen.width - w - 4));
            int y = Math.max(4, Math.min(mouseY - 10, screen.height - h - 4));
            UiBounds box = new UiBounds(x, y, w, h);
            draw.fill(box, 0xFF000000);
            draw.border(box, 0xFF808080);
            int valueX = x + labels + 30;
            try (UiDraw.Clip ignored = draw.clip(box.inset(2))) {
                renderItem(draw, x + 8, y + 6);
                draw.text(draw.trim((materials.isStarred(entry.key) ? "§e\u2605 §r" : "") + entry.name, box.right() - x - 36), x + 28, y + 10, 0xFFFFFFFF);
                for (int i = 0; i < lines.size(); i++) {
                    draw.text(lines.get(i)[0], x + 10, y + 28 + i * 16, 0xFFFFFFFF);
                    draw.text(draw.trim(lines.get(i)[1], box.right() - valueX - 8), valueX, y + 28 + i * 16, 0xFFFFFFFF);
                }
            }
            return true;
        }
    }
}

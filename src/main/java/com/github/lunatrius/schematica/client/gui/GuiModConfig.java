// SPDX-License-Identifier: LGPL-3.0-only
// Litematica GuiConfigs and MaLiLib config rows, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.GameSettings;
import com.github.lunatrius.schematica.client.input.Hotkey;
import com.github.lunatrius.schematica.client.input.Hotkeys;
import com.github.lunatrius.schematica.client.input.HotkeyHooks;
import com.github.lunatrius.schematica.client.gui.config.HotkeySettingsPanel;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Property;

import org.lwjgl.input.Keyboard;

import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.gui.config.ConfigPropertyDraft;
import com.github.lunatrius.schematica.client.gui.config.ColorPickerPanel;
import com.github.lunatrius.schematica.handler.RenderColors;
import com.github.lunatrius.schematica.util.ColorValue;
import com.github.lunatrius.schematica.client.gui.config.RenderLayerPanel;
import com.github.lunatrius.schematica.client.world.RenderLayerSettings;
import com.github.lunatrius.schematica.handler.client.WorldHandler;
import com.github.lunatrius.schematica.client.gui.config.ConfigStringListPanel;
import com.github.lunatrius.schematica.client.gui.config.UiConfigSlider;
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
import com.github.lunatrius.schematica.client.gui.framework.UiWidget;
import com.github.lunatrius.schematica.client.renderer.RendererSchematicGlobal;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.handler.BlockInfoHudSettings;
import com.github.lunatrius.schematica.util.HudAlignment;
import com.github.lunatrius.schematica.reference.Names;
import com.github.lunatrius.schematica.client.gui.config.ConfigTranslations;
import com.github.lunatrius.schematica.reference.Reference;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.Event;

public class GuiModConfig extends UiScreen implements HotkeyHooks.Capture {
    private enum Tab {
        ALL("malilib.gui.title.all", 204),
        GENERIC("litematica.gui.button.config_gui.generic", 180),
        INFO_OVERLAYS("litematica.gui.button.config_gui.info_overlays", 140),
        VISUALS("litematica.gui.button.config_gui.visuals", 180),
        COLORS("litematica.gui.button.config_gui.colors", 100),
        HOTKEYS("litematica.gui.button.config_gui.hotkeys", 204),
        RENDER_LAYERS("litematica.gui.button.config_gui.render_layers", 204);

        final String key;
        final int width;
        Tab(String key, int width) { this.key = key; this.width = width; }
        boolean keySearch() { return this == ALL || this == GENERIC || this == VISUALS || this == HOTKEYS; }
    }

    private static Tab lastTab = Tab.GENERIC;
    private final List<Entry> entries = new ArrayList<>();
    private final Map<Tab, UiButton> tabs = new EnumMap<>(Tab.class);
    private final UiListModel<Entry> model = new UiListModel<>(22, Entry::searchText);
    private UiRowList<Entry> rows;
    private RenderLayerPanel layers;
    private UiButton searchButton;
    private UiButton keySearch;
    private UiButton done;
    private UiTextField search;
    private UiLabel status;
    private boolean searchOpen;
    private boolean keysChanged;
    private int keyFilter;
    private int labelWidth;
    private Tab tab = lastTab;
    private UiButton capturingButton;
    private Hotkey capturingKey;
    private boolean captureFirst;

    public GuiModConfig(GuiScreen parent) {
        super(parent, Reference.NAME + " v" + Reference.VERSION + " - " + UiTranslations.format("litematica.gui.button.change_menu.configuration_menu"));
    }

    @Override
    protected void createWidgets() {
        for (String categoryName : ConfigurationHandler.configuration.getCategoryNames()) {
            ConfigCategory category = ConfigurationHandler.configuration.getCategory(categoryName);
            for (Property property : category.values()) {
                if (property.showInGui()) entries.add(new Entry(categoryName, property));
            }
        }
        for (Hotkey key : Hotkeys.ALL) entries.add(new Entry(key));
        entries.sort(Comparator.comparing(Entry::name, String.CASE_INSENSITIVE_ORDER));
        for (Tab value : Tab.values()) {
            UiButton button = addButton(value.key, () -> changeTab(value));
            tabs.put(value, button);
        }
        searchButton = root.add(new UiButton(() -> "", button -> {
            searchOpen = !searchOpen;
            capturingButton = null;
            refreshEntries();
            layoutWidgets();
            if (searchOpen) input.focus(search);
        }).setSprite(UiSprite.CONFIG_SEARCH).setBackground(false));
        search = root.add(new UiTextField(fontRendererObj, 256, text -> refreshEntries()));
        search.setTooltip(UiTranslations.format("malilib.gui.button.hover.search_bar_hotkey"));
        keySearch = root.add(new UiButton(() -> capturingButton == keySearch ? captureLabel() : keyLabel(keyFilter),
            button -> beginCapture(null, keySearch)));
        rows = root.add(new UiRowList<>(model, (entry, index) -> new ConfigRow(entry), 12));
        done = addButton("gui.done", this::closeScreen);
        status = root.add(new UiLabel(this::statusText, 0xFFFFA0A0));
        layers = root.add(new RenderLayerPanel(fontRendererObj, RenderLayerSettings.RANGE, () -> input.focus(null)));
        refreshEntries();
    }

    private void changeTab(Tab selected) {
        input.focus(null);
        tab = selected;
        lastTab = selected;
        capturingButton = null;
        keyFilter = 0;
        searchOpen = false;
        search.setText("");
        model.setOffset(0);
        refreshEntries();
        layoutWidgets();
    }

    private void refreshEntries() {
        List<Entry> visible = new ArrayList<>();
        for (Entry entry : entries) {
            if (tab != Tab.ALL && tab != entry.tab()) continue;
            if (searchOpen && tab.keySearch() && keyFilter != 0
                && (entry.key == null || !entry.key.keys().contains(keyFilter))) continue;
            visible.add(entry);
        }
        if (tab == Tab.COLORS) visible.sort(Comparator.comparingInt(entry -> entry.color.ordinal()));
        model.setEntries(visible);
        model.setQuery(searchOpen && search != null ? search.text() : "");
        labelWidth = 0;
        for (Entry entry : model.entries()) labelWidth = Math.max(labelWidth, fontRendererObj.getStringWidth(entry.label()));
        if (rows != null) rows.sync();
    }

    @Override
    protected void layoutWidgets() {
        int x = 10;
        int y = 26;
        for (Tab value : Tab.values()) {
            UiButton button = tabs.get(value);
            button.setVisible(tab != Tab.RENDER_LAYERS || value != Tab.ALL);
            if (!button.isVisible()) continue;
            int w = fontRendererObj.getStringWidth(button.label()) + 10;
            if (x > 10 && x + w > width - 10) { x = 10; y += 22; }
            button.setBounds(x, y, w, 20);
            button.setEnabled(value != tab);
            x += w + 2;
        }
        boolean renderingLayers = tab == Tab.RENDER_LAYERS;
        layers.setVisible(renderingLayers);
        layers.setBounds(10, y + 34, Math.max(0, width - 20), Math.max(0, height - y - 44));
        layers.layout(root.bounds());
        rows.setVisible(!renderingLayers);
        searchButton.setVisible(!renderingLayers);
        done.setVisible(!renderingLayers);
        status.setVisible(!renderingLayers);
        int listY = y + 24;
        boolean withKeys = tab.keySearch();
        int searchY = listY + (withKeys ? 7 : 4);
        searchButton.setBounds(14, searchY + 1, 12, 12);
        search.setBounds(30, searchY, Math.max(20, width - (withKeys ? 213 : 53)), 14);
        search.setVisible(!renderingLayers && searchOpen);
        keySearch.setBounds(width - 174, listY + 4, 140, 20);
        keySearch.setVisible(!renderingLayers && searchOpen && withKeys);
        int rowY = listY + 4 + (withKeys ? 23 : 17);
        rows.setBounds(12, rowY, Math.max(1, width - 24), Math.max(0, height - 34 - rowY));
        done.setBounds(10, height - 26, 80, 20);
        status.setBounds(98, height - 24, Math.max(0, width - 110), 16);
    }

    @Override
    protected void tickScreen() {
        if (capturingButton != null && (input.focused() != capturingButton || !org.lwjgl.opengl.Display.isActive())) capturingButton = null;
    }

    @Override
    protected boolean interceptKey(char character, int code) {
        if (Keyboard.isRepeatEvent() && input.focused() instanceof UiButton) return true;
        if (!input.modalPanels().isEmpty()) return false;
        if (code == Keyboard.KEY_ESCAPE && searchOpen && !isShiftKeyDown()) {
            searchOpen = false;
            refreshEntries();
            layoutWidgets();
            return true;
        }
        return false;
    }

    @Override
    protected boolean handleKey(char character, int code) {
        if (tab == Tab.RENDER_LAYERS) return false;
        if (!input.modalPanels().isEmpty() || input.focused() instanceof UiTextField || isCtrlKeyDown()
            || character <= 32 || character == 127) return false;
        searchOpen = true;
        search.setText(Character.toString(character));
        layoutWidgets();
        input.focus(search);
        return true;
    }

    private void beginCapture(Hotkey key, UiButton button) {
        capturingKey = key; capturingButton = button; captureFirst = true;
        input.focus(button); HotkeyHooks.reset();
    }

    @Override public boolean capturingHotkey() { return capturingButton != null; }

    @Override public boolean captureHotkey(int code, boolean down) {
        if (!down) return true;
        if (code < 0) {
            int x = org.lwjgl.input.Mouse.getEventX() * width / mc.displayWidth;
            int y = height - org.lwjgl.input.Mouse.getEventY() * height / mc.displayHeight - 1;
            if (!capturingButton.bounds().contains(x, y)) { endCapture(); return false; }
        }
        if (code == Keyboard.KEY_ESCAPE) {
            if (captureFirst) {
                if (capturingKey == null) keyFilter = 0;
                else { capturingKey.setKeys(java.util.Collections.emptyList()); keysChanged = true; }
            }
            endCapture(); return true;
        }
        if (capturingKey == null) { keyFilter = code; endCapture(); return true; }
        List<Integer> codes = captureFirst ? new ArrayList<>() : new ArrayList<>(capturingKey.keys());
        if (!codes.contains(code) && codes.size() < 16) codes.add(code);
        capturingKey.setKeys(codes); captureFirst = false; keysChanged = true;
        return true;
    }

    private void endCapture() { capturingButton = null; HotkeyHooks.reset(); refreshEntries(); }
    private String captureLabel() {
        return "§e> " + (capturingKey == null ? keyLabel(keyFilter) : chordLabel(capturingKey)) + " <§r";
    }
    private static String keyLabel(int code) {
        return code == 0 ? UiTranslations.format("malilib.gui.button.empty_keybind") : GameSettings.getKeyDisplayString(code);
    }
    public static String chordLabel(Hotkey key) {
        if (key.keys().isEmpty()) return keyLabel(0);
        List<String> labels = new ArrayList<>();
        for (int code : key.keys()) labels.add(keyLabel(code));
        return String.join(" + ", labels);
    }
    private String bindingLabel(Hotkey key) {
        if (capturingButton != null && capturingKey == key) return captureLabel();
        for (Hotkey other : Hotkeys.ALL) {
            if (other != key && !key.keys().isEmpty() && key.keys().equals(other.keys())
                && key.settings.cancel && other.settings.cancel
                && (key.settings.context == other.settings.context || key.settings.context == Hotkey.Context.ANY || other.settings.context == Hotkey.Context.ANY))
                return "§6" + chordLabel(key);
        }
        return chordLabel(key);
    }

    private String statusText() {
        int invalid = 0;
        for (Entry entry : entries) if (entry.available() && entry.draft != null && !entry.draft.valid()) invalid++;
        return invalid == 0 ? "" : UiTranslations.format("schematica.ui.config.invalid_count", invalid);
    }

    @Override
    protected void closed() {
        WorldHandler.INSTANCE.saveSession();
        capturingButton = null;
        boolean changed = false;
        boolean restart = false;
        for (Entry entry : entries) {
            if (entry.available() && entry.draft != null && entry.draft.apply()) {
                changed = true;
                restart |= entry.draft.property.requiresMcRestart();
            }
        }
        if (changed) {
            ConfigChangedEvent.OnConfigChangedEvent event = new ConfigChangedEvent.OnConfigChangedEvent(
                Reference.MODID, null, mc.theWorld != null, restart);
            FMLCommonHandler.instance().bus().post(event);
            if (event.getResult() != Event.Result.DENY) {
                FMLCommonHandler.instance().bus().post(new ConfigChangedEvent.PostConfigChangedEvent(
                    Reference.MODID, null, mc.theWorld != null, restart));
            }
            RendererSchematicGlobal.INSTANCE.refresh();
        }
        if (keysChanged) {
            Hotkeys.save();
            keysChanged = false;
        }
    }

    private final class Entry {
        final String category;
        final ConfigPropertyDraft draft;
        final Hotkey key;
        final RenderColors color;

        Entry(String category, Property property) {
            this.category = category;
            color = RenderColors.find(category, property.getName());
            draft = new ConfigPropertyDraft(property, color != null);
            key = null;
        }

        Entry(Hotkey key) {
            category = "hotkeys";
            draft = null;
            this.key = key;
            color = null;
        }

        boolean available() { return color == null || color.available; }
        int previewColor() {
            try { return ColorValue.parse(draft.text()); }
            catch (IllegalArgumentException ignored) {
                try { return ColorValue.parse(draft.property.getString()); }
                catch (IllegalArgumentException invalid) { return color.defaultColor; }
            }
        }

        String name() { return key == null ? draft.property.getName() : key.translationKey(); }
        String label() { return UiTranslations.format(ConfigTranslations.label(key == null ? draft.property.getLanguageKey() : key.translationKey())); }
        String searchText() { return name() + " " + label() + " " + category + (modified() ? " modified" : ""); }
        boolean modified() { return key == null ? draft.modified() : key.modified(); }
        Tab tab() {
            if (key != null) return Tab.HOTKEYS;
            if (color != null) return Tab.COLORS;
            if (BlockInfoHudSettings.CATEGORY.equals(category)) return Tab.INFO_OVERLAYS;
            if (Names.Config.Category.RENDER.equals(category)) return Tab.VISUALS;
            return Names.Config.Category.DEBUG.equals(category) ? Tab.INFO_OVERLAYS : Tab.GENERIC;
        }
        String description() {
            if (key != null) return UiTranslations.format("litematica.config.hotkeys.comment." + key.id);
            if (color != null) {
                String description = UiTranslations.format("litematica.config.colors.comment." + color.key);
                if (!color.available) description += "\n" + UiTranslations.format("schematica.ui.color.pending");
                else if (color == RenderColors.WRONG_STATE) description += "\n" + UiTranslations.format("schematica.ui.color.metadata");
                return description;
            }
            String commentKey = ConfigTranslations.comment(draft.property.getLanguageKey());
            String translated = UiTranslations.format(commentKey);
            String description = translated.equals(commentKey)
                ? draft.property.comment : translated;
            return category + ": " + name() + "\n" + description
                + (Names.Config.Category.SERVER.equals(category) ? "\n" + UiTranslations.format("schematica.ui.config.server") : "");
        }
    }

    private final class ConfigRow extends UiPanel {
        private final Entry entry;
        private final UiLabel label;
        private final UiButton reset;
        private final UiWidget editor;
        private UiTextField text;
        private UiWidget slider;
        private UiButton sliderToggle;
        private UiWidget keySettings;
        private UiButton swatch;

        ConfigRow(Entry entry) {
            this.entry = entry;
            label = add(new UiLabel(entry::label, entry.available() ? 0xFFFFFFFF : 0xFF888888));
            label.setTooltip(entry.description().split("\n"));
            reset = add(new UiButton(() -> UiTranslations.format("malilib.gui.button.reset.caps"), button -> reset()));
            if (entry.key != null) {
                editor = add(new UiButton(() -> bindingLabel(entry.key), button -> captureEntry()));
                keySettings = add(new UiButton(() -> "", button -> {
                    if (button == 1) { entry.key.resetSettings(); HotkeyHooks.reset(); keysChanged = true; }
                    else {
                        HotkeySettingsPanel panel = new HotkeySettingsPanel(fontRendererObj, entry.key, () -> keysChanged = true);
                        panel.layout(root.bounds()); input.pushModal(panel);
                    }
                }) {
                    @Override public void draw(UiDraw draw, int mouseX, int mouseY) {
                        draw.fill(bounds(), 0xFF000000);
                        draw.border(bounds(), entry.key.settingsModified() ? 0xFFFFBB33 : 0xFFFFFFFF);
                        Hotkey.Settings s = entry.key.settings;
                        int[] v = {s.action.ordinal() * 18, s.allowExtra ? 0 : 18, s.ordered ? 18 : 0, s.exclusive ? 18 : 0, s.cancel ? 18 : 0};
                        for (int i = 0; i < v.length; i++) draw.texture("schematica_plus:textures/gui/malilib_widgets.png",
                            new UiBounds(bounds().x + 1, bounds().y + 1, 18, 18), i * 18, v[i], 18, 18, 256, 256);
                        if (bounds().contains(mouseX, mouseY)) draw.border(bounds(), 0xFFFFFF55);
                    }
                });
                keySettings.setTooltip(UiTranslations.format("malilib.gui.label.keybind_settings.title_advanced_keybind_settings"),
                    UiTranslations.format("malilib.gui.label.keybind_settings.tips"));
            } else if (entry.draft.property.isList()) {
                editor = add(new UiButton(() -> "[ " + String.join(", ", entry.draft.values()) + " ]", button -> {
                    ConfigStringListPanel panel = new ConfigStringListPanel(fontRendererObj, entry.draft);
                    panel.layout(root.bounds());
                    input.pushModal(panel);
                }));
            } else if (entry.draft.property.getName().equals("blockInfoOverlayAlignment") && BlockInfoHudSettings.CATEGORY.equals(entry.category)) {
                editor = add(new UiButton(() -> UiTranslations.format("center".equals(entry.draft.text())
                    ? "litematica.label.alignment.center" : "litematica.label.alignment.top_center"),
                    button -> entry.draft.setText("center".equals(entry.draft.text()) ? "top_center" : "center")));
            } else if ((entry.draft.property.getName().equals("blockInfoLinesAlignment") || entry.draft.property.getName().equals("infoHudAlignment"))
                && BlockInfoHudSettings.CATEGORY.equals(entry.category)) {
                editor = add(new UiButton(() -> UiTranslations.format(HudAlignment.parse(entry.draft.text()).translationKey()),
                    button -> {
                        HudAlignment[] values = HudAlignment.values();
                        int current = HudAlignment.parse(entry.draft.text()).ordinal();
                        entry.draft.setText(values[Math.floorMod(current + (button == 1 ? -1 : 1), values.length)].value());
                    }));
            } else if (entry.draft.property.getType() == Property.Type.BOOLEAN) {
                editor = add(new UiButton(() -> UiTranslations.format(Boolean.parseBoolean(entry.draft.text())
                    ? "malilib.gui.button.true" : "malilib.gui.button.false"),
                    button -> entry.draft.setText(Boolean.toString(!Boolean.parseBoolean(entry.draft.text())))));
            } else {
                text = add(new UiTextField(fontRendererObj, entry.color == null ? 65535 : 12, entry.draft::setText));
                text.setText(entry.draft.text());
                editor = text;
                if (entry.color != null) {
                    swatch = add(new UiButton(() -> "", button -> {
                        ColorPickerPanel panel = new ColorPickerPanel(fontRendererObj, entry.previewColor(), color -> {
                            entry.draft.setText(ColorValue.format(color));
                            text.setText(entry.draft.text());
                        });
                        panel.layout(root.bounds());
                        input.pushModal(panel);
                    }) {
                        @Override public void draw(UiDraw draw, int mouseX, int mouseY) {
                            draw.fill(bounds(), isEnabled() ? 0xFFFFFFFF : 0xFF808080);
                            draw.fill(bounds().inset(1), 0xFF000000);
                            draw.fill(bounds().inset(2), entry.previewColor() | 0xFF000000);
                            if (isFocused()) draw.border(bounds(), 0xFFFFFF00);
                        }
                    });
                    swatch.setEnabled(entry.available());
                    swatch.setTooltip(UiTranslations.format(entry.available() ? "malilib.hover.color_indicator.open_color_editor" : "schematica.ui.color.pending"));
                }
                if (entry.draft.supportsSlider()) {
                    slider = add(new UiConfigSlider(entry.draft));
                    sliderToggle = add(new UiButton(() -> "", button -> {
                        entry.draft.slider = !entry.draft.slider;
                        if (!entry.draft.slider) text.setText(entry.draft.text());
                        layout(bounds());
                    }).setBackground(false));
                }
            }
            editor.setEnabled(entry.available());
            tick();
        }

        private void reset() {
            if (entry.key != null) { entry.key.reset(); HotkeyHooks.reset(); keysChanged = true; }
            else {
                entry.draft.reset();
                if (text != null) text.setText(entry.draft.text());
            }
            tick();
        }

        private void captureEntry() { beginCapture(entry.key, (UiButton) editor); }

        @Override
        public void layout(UiBounds screen) {
            int resetWidth = fontRendererObj.getStringWidth(reset.label()) + 10;
            int optionWidth = Math.min(tab.width, Math.max(60, bounds().width - resetWidth - 90));
            int nameWidth = Math.min(labelWidth, Math.max(20, bounds().width - optionWidth - resetWidth - 12));
            int x = bounds().x + nameWidth + 10;
            int y = bounds().y + 1;
            label.setBounds(bounds().x, y + 5, nameWidth, 12);
            reset.setBounds(x + optionWidth + 2, y, resetWidth, 20);
            if (text != null) {
                int fieldWidth = optionWidth - (swatch != null ? 22 : sliderToggle == null ? 0 : 18);
                text.setBounds(x + 2, y + 1, fieldWidth - 4, 17);
                if (swatch != null) swatch.setBounds(x + fieldWidth + 2, y + 1, 18, 18);
                if (sliderToggle != null) {
                    text.setVisible(!entry.draft.slider);
                    slider.setVisible(entry.draft.slider);
                    slider.setBounds(x, y, fieldWidth, 20);
                    sliderToggle.setBounds(x + fieldWidth + 2, y + 2, 16, 16);
                    sliderToggle.setSprite(entry.draft.slider ? UiSprite.TEXT_FIELD : UiSprite.SLIDER);
                }
            } else {
                editor.setBounds(x, y, optionWidth - (keySettings == null ? 0 : 22), 20);
                if (keySettings != null) keySettings.setBounds(x + optionWidth - 20, y, 20, 20);
            }
        }

        @Override
        public void tick() {
            reset.setEnabled(entry.available() && entry.modified());
            if (text != null) text.setTooltip(entry.draft.valid() || !entry.available() ? entry.description().split("\n")
                : new String[] {entry.color != null ? UiTranslations.format("schematica.ui.color.hex")
                    : UiTranslations.format("schematica.ui.config.invalid", entry.draft.property.getMinValue(), entry.draft.property.getMaxValue())});
            super.tick();
        }

        @Override
        public void draw(UiDraw draw, int mouseX, int mouseY) {
            super.draw(draw, mouseX, mouseY);
            if (text != null && entry.available() && !entry.draft.valid()) draw.border(text.bounds(), 0xFFFF5555);
        }
    }
}

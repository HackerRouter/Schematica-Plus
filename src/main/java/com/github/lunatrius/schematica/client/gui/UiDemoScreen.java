package com.github.lunatrius.schematica.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiIcon;
import com.github.lunatrius.schematica.client.gui.framework.UiIntegerField;
import com.github.lunatrius.schematica.client.gui.framework.UiLabel;
import com.github.lunatrius.schematica.client.gui.framework.UiList;
import com.github.lunatrius.schematica.client.gui.framework.UiListModel;
import com.github.lunatrius.schematica.client.gui.framework.UiScreen;
import com.github.lunatrius.schematica.client.gui.framework.UiTextField;
import com.github.lunatrius.schematica.client.gui.framework.UiToggleButton;

public final class UiDemoScreen extends UiScreen {

    private final UiListModel<String> model = new UiListModel<>(20, value -> value);
    private UiButton back;
    private UiLabel help;
    private UiTextField search;
    private UiButton clear;
    private UiList<String> list;
    private UiLabel numberLabel;
    private UiIntegerField number;
    private UiToggleButton toggle;
    private UiLabel statusLabel;
    private UiButton dialog;
    private UiButton child;
    private String status = "";
    private boolean listEnabled = true;

    public UiDemoScreen(GuiScreen parent) {
        super(parent, I18n.format("schematica.ui.demo.title"));
    }

    @Override
    protected void createWidgets() {
        back = root.add(new UiButton(() -> "", UiIcon.BACK, button -> {
            if (button == 0) closeScreen();
        }));
        back.setTooltip(I18n.format("gui.back"));
        help = root.add(new UiLabel(() -> I18n.format("schematica.ui.demo.help")));
        help.setTooltip(I18n.format("schematica.ui.demo.help"));
        search = root.add(new UiTextField(fontRendererObj, 256, model::setQuery));
        search.setTooltip(I18n.format("schematica.ui.demo.search"));
        clear = root.add(new UiButton(() -> I18n.format("schematica.ui.demo.clear"), button -> {
            if (button == 0) {
                search.setText("");
                input.focus(search);
            }
        }));
        list = root.add(new UiList<>(model, I18n.format("schematica.ui.demo.empty"), value -> {
            status = I18n.format("schematica.ui.demo.activated", value);
        }));
        List<String> examples = new ArrayList<>();
        for (int i = 1; i <= 200; i++) examples.add(String.format(Locale.ROOT, "Demo_%03d.schemplus", i));
        model.setEntries(examples);
        numberLabel = root.add(new UiLabel(() -> I18n.format("schematica.ui.demo.number")));
        number = root.add(new UiIntegerField(fontRendererObj, 0, -9999, 9999,
            value -> status = I18n.format("schematica.ui.demo.value", value)));
        number.setTooltip(I18n.format("schematica.ui.demo.number_help"));
        toggle = root.add(new UiToggleButton(() -> I18n.format("schematica.ui.demo.list"), () -> listEnabled, value -> {
            listEnabled = value;
            list.setEnabled(value);
        }));
        statusLabel = root.add(new UiLabel(() -> status.isEmpty()
            ? I18n.format("schematica.ui.demo.selected", model.selected() == null ? "-" : model.selected()) : status));
        dialog = root.add(new UiButton(() -> I18n.format("schematica.ui.demo.dialog"), button -> {
            if (button == 0) confirm(I18n.format("schematica.ui.demo.dialog"),
                I18n.format("schematica.ui.demo.confirm"), () -> {
                    search.setText("");
                    number.setValue(0);
                    status = I18n.format("schematica.ui.demo.reset");
                });
        }));
        child = root.add(new UiButton(() -> I18n.format("schematica.ui.demo.child"), button -> {
            if (button == 0) mc.displayGuiScreen(new UiDemoScreen(this));
        }));
    }

    @Override
    protected void layoutWidgets() {
        int available = Math.max(1, width - 24);
        back.setBounds(width - 32, 6, 20, 20);
        help.setBounds(12, 28, available, 12);
        search.setBounds(12, 44, available - 64, 20);
        clear.setBounds(width - 72, 44, 60, 20);
        list.setBounds(12, 70, available, Math.max(20, height - 152));
        numberLabel.setBounds(12, height - 74, 54, 20);
        number.setBounds(70, height - 74, Math.max(40, Math.min(100, available - 174)), 20);
        toggle.setBounds(width - 108, height - 74, 96, 20);
        statusLabel.setBounds(12, height - 50, available, 14);
        int buttonWidth = (available - 4) / 2;
        dialog.setBounds(12, height - 30, buttonWidth, 20);
        child.setBounds(16 + buttonWidth, height - 30, buttonWidth, 20);
    }
}

package com.github.lunatrius.schematica.client.gui.browser;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import org.lwjgl.Sys;

import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiLabel;
import com.github.lunatrius.schematica.client.gui.framework.UiList;
import com.github.lunatrius.schematica.client.gui.framework.UiListModel;
import com.github.lunatrius.schematica.client.gui.framework.UiScreen;
import com.github.lunatrius.schematica.client.gui.framework.UiTextField;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.util.FileUtils;

public abstract class GuiSchematicBrowser extends UiScreen {

    protected SchematicBrowserModel browser;
    protected final UiListModel<SchematicBrowserModel.Entry> files = new UiListModel<>(20, SchematicBrowserModel.Entry::label);
    private final boolean directoriesOnly;
    private UiLabel path;
    private UiButton up;
    private UiButton home;
    private UiButton refresh;
    private UiButton openFolder;
    private UiLabel searchLabel;
    private UiTextField search;
    private UiButton clear;
    private UiList<SchematicBrowserModel.Entry> list;
    private UiLabel selected;
    private UiLabel message;
    private final List<UiButton> actions = new ArrayList<>();
    private String status = "";

    protected GuiSchematicBrowser(GuiScreen parent, String title, boolean directoriesOnly) {
        super(parent, title);
        this.directoriesOnly = directoriesOnly;
    }

    @Override
    protected final void createWidgets() {
        path = root.add(new UiLabel(() -> browser == null ? "" : browser.relativeDirectory()));
        up = addButton("schematica.ui.browser.up", () -> navigate(browser.directory().getParentFile()));
        home = addButton("schematica.ui.browser.root", () -> navigate(browser.root()));
        refresh = addButton("schematica.ui.browser.refresh", this::refreshFiles);
        openFolder = addButton("schematica.gui.openFolder", this::openFolder);
        searchLabel = root.add(new UiLabel(() -> I18n.format("schematica.ui.browser.search")));
        search = root.add(new UiTextField(fontRendererObj, 256, files::setQuery));
        search.setTooltip(I18n.format("schematica.ui.browser.search_hint"));
        clear = addButton("schematica.ui.demo.clear", () -> {
            search.setText("");
            input.focus(search);
        });
        list = root.add(new UiList<>(files, I18n.format("schematica.ui.browser.empty"), this::activate));
        selected = root.add(new UiLabel(this::selectionInfo));
        message = root.add(new UiLabel(() -> status));
        createActions();
        actions.add(addButton("gui.back", this::closeScreen));
    }

    protected abstract void createActions();

    protected final UiButton addAction(String key, Runnable action) {
        UiButton button = addButton(key, action);
        actions.add(button);
        return button;
    }

    @Override
    protected void opened() {
        refreshFiles();
        tickScreen();
    }

    protected boolean accept(SchematicBrowserModel.Entry entry) {
        return !directoriesOnly || entry.directory;
    }

    protected void activate(SchematicBrowserModel.Entry entry) {
        if (entry.directory) navigate(entry.file);
        else activateFile(entry);
    }

    protected void activateFile(SchematicBrowserModel.Entry entry) {}

    protected final void navigate(File directory) {
        if (browser == null) return;
        try {
            browser.navigate(directory);
            search.setText("");
            files.setOffset(0);
            updateFiles();
            setStatus("");
        } catch (IOException e) {
            updateFiles();
            fail("schematica.ui.browser.unreadable", e);
        }
    }

    protected final void refreshFiles() {
        try {
            if (browser == null) browser = new SchematicBrowserModel(ConfigurationHandler.schematicDirectory);
            browser.refresh();
            updateFiles();
            setStatus("");
        } catch (IOException e) {
            if (browser != null) updateFiles();
            fail("schematica.ui.browser.unreadable", e);
        }
    }

    private void updateFiles() {
        List<SchematicBrowserModel.Entry> entries = new ArrayList<>();
        for (SchematicBrowserModel.Entry entry : browser.entries()) if (accept(entry)) entries.add(entry);
        files.setEntries(entries);
        path.setTooltip(browser.directory().getAbsolutePath());
        tickScreen();
    }

    protected final void setStatus(String status) {
        this.status = status;
        message.setTooltip(status);
    }

    protected final void fail(String key, Exception error) {
        Reference.logger.error(I18n.format(key), error);
        setStatus(I18n.format(key));
    }

    protected final SchematicBrowserModel.Entry selection() {
        return files.selectedIndex() < 0 ? null : files.selected();
    }

    private String selectionInfo() {
        SchematicBrowserModel.Entry entry = selection();
        if (entry == null) return I18n.format("schematica.ui.browser.entries", files.entries().size());
        return entry.directory ? entry.name() + "/" : entry.name() + " | " + FileUtils.humanReadableByteCount(entry.size)
            + " | " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(entry.modified));
    }

    private void openFolder() {
        if (browser == null) return;
        try {
            Desktop.getDesktop().open(browser.directory());
        } catch (Exception e) {
            if (!Sys.openURL(browser.directory().toURI().toString())) fail("schematica.ui.browser.open_failed", e);
        }
    }

    @Override
    protected void tickScreen() {
        up.setEnabled(browser != null && browser.canGoUp());
        home.setEnabled(browser != null);
        openFolder.setEnabled(browser != null && browser.directory().isDirectory());
        selected.setTooltip(selectionInfo());
    }

    @Override
    protected final void layoutWidgets() {
        int available = width - 24;
        path.setBounds(12, 28, available, 12);
        UiButton[] navigation = {up, home, refresh, openFolder};
        int cell = (available - 12) / 4;
        for (int i = 0; i < navigation.length; i++) navigation[i].setBounds(12 + i * (cell + 4), 44, cell, 20);
        searchLabel.setBounds(12, 68, 46, 20);
        search.setBounds(60, 68, available - 106, 20);
        clear.setBounds(width - 66, 68, 54, 20);
        list.setBounds(12, 94, available, Math.max(20, height - 162));
        selected.setBounds(12, height - 65, available, 13);
        message.setBounds(12, height - 49, available, 13);
        int actionWidth = (available - (actions.size() - 1) * 4) / actions.size();
        for (int i = 0; i < actions.size(); i++) actions.get(i).setBounds(12 + i * (actionWidth + 4), height - 30, actionWidth, 20);
    }
}

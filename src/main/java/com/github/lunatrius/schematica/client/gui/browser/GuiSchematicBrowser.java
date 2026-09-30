// SPDX-License-Identifier: LGPL-3.0-only
// Litematica/MaLiLib browser layout, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.browser;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.gui.GuiScreen;

import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.client.gui.framework.UiDraw;
import com.github.lunatrius.schematica.client.gui.framework.UiSprite;
import com.github.lunatrius.schematica.client.gui.framework.UiWidget;
import com.github.lunatrius.schematica.client.gui.framework.UiLabel;
import com.github.lunatrius.schematica.client.gui.framework.UiList;
import com.github.lunatrius.schematica.client.gui.framework.UiListModel;
import com.github.lunatrius.schematica.client.gui.framework.UiScreen;
import com.github.lunatrius.schematica.client.gui.framework.UiTextField;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.util.FileUtils;
import com.github.lunatrius.schematica.proxy.ClientProxy;

public abstract class GuiSchematicBrowser extends UiScreen {

    protected SchematicBrowserModel browser;
    protected final UiListModel<SchematicBrowserModel.Entry> files = new UiListModel<>(14, SchematicBrowserModel.Entry::label);
    private final boolean directoriesOnly;
    private UiLabel path;
    private UiButton up;
    private UiButton home;
    private UiButton createDirectory;
    private UiButton searchButton;
    private UiButton back;
    private UiTextField search;
    private UiWidget frame;
    private UiWidget info;
    private boolean searching;
    private UiList<SchematicBrowserModel.Entry> list;
    private UiLabel message;
    private final List<UiButton> actions = new ArrayList<>();
    private String status = "";
    private SchematicBrowserModel.Entry lastSelection;

    protected GuiSchematicBrowser(GuiScreen parent, String title, boolean directoriesOnly) {
        super(parent, title);
        this.directoriesOnly = directoriesOnly;
    }

    @Override
    protected final void createWidgets() {
        frame = root.add(new UiWidget() {
            @Override public void draw(UiDraw draw, int mouseX, int mouseY) {
                draw.fill(bounds(), 0xB0000000);
                draw.border(bounds(), 0xFF999999);
            }
        });
        path = root.add(new UiLabel(() -> browser == null ? "" : browser.relativeDirectory()));
        up = icon(UiSprite.UP, "malilib.gui.button.hover.directory_widget.up", () -> navigate(browser.directory().getParentFile()));
        home = icon(UiSprite.ROOT, "malilib.gui.button.hover.directory_widget.root", () -> navigate(browser.root()));
        createDirectory = icon(UiSprite.CREATE_DIRECTORY, "malilib.gui.button.hover.directory_widget.create_directory", this::createDirectory);
        search = root.add(new UiTextField(fontRendererObj, 256, files::setQuery));
        search.setTooltip(UiTranslations.format("schematica.ui.browser.search_hint"));
        searchButton = icon(UiSprite.SEARCH, "schematica.ui.browser.search", () -> {
            searching = !searching;
            layoutWidgets();
            if (searching) input.focus(search);
            else search.setText("");
        });
        list = root.add(new UiList<>(files, UiTranslations.format("schematica.ui.browser.empty"), this::activate));
        list.setFileStyle(entry -> entry.directory ? UiSprite.DIRECTORY
            : UiSprite.schematicFile(entry.name()),
            entry -> entry.directory ? entry.name() : entry.name().substring(0, entry.name().lastIndexOf('.')));
        info = root.add(new UiWidget() {
            @Override public void draw(UiDraw draw, int mouseX, int mouseY) {
                draw.fill(bounds(), 0xA0000000);
                draw.border(bounds(), 0xFF999999);
                SchematicBrowserModel.Entry entry = selection();
                if (entry == null || entry.directory) return;
                int x = bounds().x + 3;
                int y = bounds().y + 3;
                draw.text(UiTranslations.format("litematica.gui.label.schematic_info.name"), x, y, 0xC0C0C0C0);
                draw.text(draw.trim(entry.name(), bounds().width - 10), x + 4, y + 12, 0xFFFFFFFF);
                draw.text(FileUtils.humanReadableByteCount(entry.size), x, y + 36, 0xC0C0C0C0);
                String date = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(entry.modified));
                draw.text(draw.trim(date, bounds().width - 6), x, y + 48, 0xC0C0C0C0);
                draw.text(draw.trim(UiTranslations.format("schematica.ui.browser.file_info"), bounds().width - 6), x, y + 60, 0xC0C0C0C0);
            }
        });
        message = root.add(new UiLabel(() -> status));
        createActions();
        back = addButton(directoriesOnly ? "gui.back" : "litematica.gui.button.change_menu.to_main_menu",
            directoriesOnly ? this::closeScreen : this::mainMenu);
    }

    private UiButton icon(UiSprite sprite, String key, Runnable action) {
        UiButton button = root.add(new UiButton(() -> "", mouse -> { if (mouse == 0) action.run(); })
            .setSprite(sprite).setBackground(false));
        button.setTooltip(UiTranslations.format(key));
        return button;
    }

    protected int browserX() { return 12; }
    protected int browserY() { return 24; }
    protected int browserHeight() { return height - 70; }
    protected boolean showFooter() { return true; }

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

    protected void selectionChanged(SchematicBrowserModel.Entry entry) {}

    private void createDirectory() {
        if (browser == null) return;
        prompt(UiTranslations.format("malilib.gui.title.create_directory"), "", name -> {
            try {
                File directory = browser.createDirectory(name);
                selectResult(directory);
                return null;
            } catch (IOException e) {
                return fileError(e);
            }
        });
    }

    protected final void renameSelectedFile() {
        SchematicBrowserModel.Entry entry = selection();
        if (entry == null || entry.directory) return;
        prompt(UiTranslations.format("litematica.gui.title.rename_file"), entry.name(), name -> {
            try {
                File source = entry.file.getCanonicalFile();
                ClientProxy.SCHEMATICS.checkRename(source, new File(entry.file.getParentFile(), name));
                File target = browser.rename(entry, name);
                ClientProxy.sourceRenamed(source, target);
                selectResult(target);
                return null;
            } catch (IOException e) {
                return fileError(e);
            }
        });
    }

    protected final void copySelectedFile() {
        SchematicBrowserModel.Entry entry = selection();
        if (entry == null || entry.directory) return;
        prompt(UiTranslations.format("litematica.gui.title.copy_file"), entry.name(), name -> {
            try {
                selectResult(browser.copy(entry, name));
                return null;
            } catch (IOException e) {
                return fileError(e);
            }
        });
    }

    protected final void deleteSelectedFile() {
        SchematicBrowserModel.Entry entry = selection();
        if (entry == null || entry.directory) return;
        confirm(UiTranslations.format("litematica.gui.title.confirm_file_deletion"),
            UiTranslations.format("schematica.ui.files.delete_confirm", entry.name()), () -> {
                try {
                    browser.delete(entry);
                    refreshFiles();
                    setStatus(UiTranslations.format("schematica.ui.files.deleted", entry.name()));
                } catch (IOException e) {
                    setStatus(fileError(e));
                }
            });
    }

    private void selectResult(File result) {
        search.setText("");
        refreshFiles();
        for (int i = 0; i < files.entries().size(); i++) {
            if (files.entries().get(i).file.equals(result)) {
                files.select(i);
                files.revealSelection();
                break;
            }
        }
        tickScreen();
    }

    private String fileError(IOException error) {
        String key = error instanceof SchematicBrowserModel.FileOperationException
            ? ((SchematicBrowserModel.FileOperationException) error).translationKey
            : error instanceof FileAlreadyExistsException ? "schematica.ui.files.error.exists" : "schematica.ui.files.error.io";
        Reference.logger.warn("Schematic file operation failed", error);
        return UiTranslations.format(key);
    }

    @Override
    protected boolean handleKey(char character, int keyCode) {
        if (keyCode == Keyboard.KEY_F5) {
            refreshFiles();
            return true;
        }
        if (!(input.focused() instanceof UiTextField) && keyCode == Keyboard.KEY_BACK && browser != null && browser.canGoUp()) {
            navigate(browser.directory().getParentFile());
            return true;
        }
        return false;
    }

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
        Reference.logger.error(UiTranslations.format(key), error);
        setStatus(UiTranslations.format(key));
    }

    protected final SchematicBrowserModel.Entry selection() {
        return files.selectedIndex() < 0 ? null : files.selected();
    }

    private String selectionInfo() {
        SchematicBrowserModel.Entry entry = selection();
        if (entry == null) return UiTranslations.format("schematica.ui.browser.entries", files.entries().size());
        return entry.directory ? entry.name() + "/" : entry.name() + " | " + FileUtils.humanReadableByteCount(entry.size)
            + " | " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(entry.modified));
    }

    @Override
    protected void tickScreen() {
        up.setEnabled(browser != null && browser.canGoUp());
        home.setEnabled(browser != null);
        createDirectory.setEnabled(browser != null && browser.directory().isDirectory());
        info.setTooltip(selectionInfo());
        SchematicBrowserModel.Entry current = selection();
        if (!java.util.Objects.equals(current, lastSelection)) {
            lastSelection = current;
            selectionChanged(current);
        }
    }

    @Override
    protected final void layoutWidgets() {
        int x = browserX();
        int y = browserY();
        int browserWidth = Math.max(24, width - 196);
        int browserHeight = Math.max(0, browserHeight());
        frame.setBounds(x, y, browserWidth, browserHeight);
        home.setBounds(x + 2, y + 5, 12, 12);
        up.setBounds(x + 16, y + 5, 12, 12);
        createDirectory.setBounds(x + 30, y + 5, 12, 12);
        searchButton.setBounds(x + browserWidth - 24, y + 5, 12, 12);
        path.setBounds(x + 48, y + 4, browserWidth - 76, 14);
        search.setBounds(x + 2, y + 4, browserWidth - 28, 14);
        search.setVisible(searching);
        path.setVisible(!searching);
        home.setVisible(!searching);
        up.setVisible(!searching);
        createDirectory.setVisible(!searching);
        list.setBounds(x + 2, y + 21, browserWidth - 4, Math.max(0, browserHeight - 25));
        info.setBounds(x + width - 190, y, 170, Math.min(310, browserHeight + (showFooter() ? 10 : 0)));
        message.setBounds(info.bounds().x + 3, info.bounds().bottom() - 25, 164, 22);
        back.setVisible(showFooter());
        int menuWidth = fontRendererObj.getStringWidth(back.label()) + 20;
        back.setBounds(width - menuWidth - 10, height - 26, menuWidth, 20);
        layoutActions();
    }

    protected void layoutActions() {
        int x = 12;
        for (UiButton button : actions) {
            int w = button.preferredWidth(fontRendererObj.getStringWidth(button.label()));
            button.setBounds(x, height - 26, w, 20);
            x += w + 4;
        }
    }
}

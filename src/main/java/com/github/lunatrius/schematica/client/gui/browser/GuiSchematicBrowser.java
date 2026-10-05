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
    private UiButton openFolder;
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
    private final SchematicInfoCache infoCache = new SchematicInfoCache();
    private static final long PREVIEW_3D_MAX_FILE = 32L << 20;
    private int infoMouseX, infoMouseY;
    private UiBounds preview3DBox;
    private static final java.text.SimpleDateFormat DATE_FORMAT = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

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
        home = root.add(new UiButton(() -> "", mouse -> {
            if (browser == null) return;
            if (mouse == 0) navigate(browser.root());
            else if (mouse == 1) com.github.lunatrius.schematica.client.util.FolderOpener.open(browser.root());
        }).setSprite(UiSprite.ROOT).setBackground(false));
        home.setTooltip(UiTranslations.format("malilib.gui.button.hover.directory_widget.root"));
        createDirectory = icon(UiSprite.CREATE_DIRECTORY, "malilib.gui.button.hover.directory_widget.create_directory", this::createDirectory);
        openFolder = icon(UiSprite.DIRECTORY, "malilib.gui.button.hover.directory_widget.open_directory",
            () -> { if (browser != null) com.github.lunatrius.schematica.client.util.FolderOpener.open(browser.directory()); });
        search = root.add(new UiTextField(fontRendererObj, 256, files::setQuery));
        searchButton = icon(UiSprite.SEARCH, null, () -> {
            searching = !searching;
            layoutWidgets();
            if (searching) input.focus(search);
            else search.setText("");
        });
        list = root.add(new UiList<>(files, "", this::activate));
        list.setFileStyle(entry -> entry.directory ? UiSprite.DIRECTORY
            : UiSprite.schematicFile(entry.name()),
            entry -> entry.directory ? entry.name() : entry.name().substring(0, entry.name().lastIndexOf('.')));
        info = root.add(new UiWidget() {
            @Override public void draw(UiDraw draw, int mouseX, int mouseY) {
                draw.fill(bounds(), 0xA0000000);
                draw.border(bounds(), 0xFF999999);
                infoMouseX = mouseX;
                infoMouseY = mouseY;
                preview3DBox = null;
                drawInfo(draw, bounds().x + 3, bounds().y + 3, bounds().width, selection());
            }

            @Override public boolean scroll(int x, int y, int amount) {
                SchematicPreview3D preview = preview3DBox != null && preview3DBox.contains(x, y) && selection() != null
                    ? infoCache.preview3D(selection().file) : null;
                if (preview == null) return false;
                preview.zoom(amount);
                return true;
            }
        });
        message = root.add(new UiLabel(() -> status));
        createActions();
        back = addButton(directoriesOnly ? "gui.back" : "litematica.gui.button.change_menu.to_main_menu",
            directoriesOnly ? this::closeScreen : this::mainMenu);
    }

    /** Draws the info panel for the selected entry: Litematica metadata and preview for .litematic and .nbt files. */
    protected void drawInfo(UiDraw draw, int x, int y, int width, SchematicBrowserModel.Entry entry) {
        if (entry == null || entry.directory) return;
        com.github.lunatrius.schematica.world.schematic.SchematicFiles.Info meta = infoCache.get(entry.file);
        if (meta == null) {
            draw.text(UiTranslations.format("litematica.gui.label.schematic_info.name"), x, y, 0xC0C0C0C0);
            draw.text(draw.trim(entry.name(), width - 10), x + 4, y + 12, 0xFFFFFFFF);
            draw.text(FileUtils.humanReadableByteCount(entry.size), x, y + 36, 0xC0C0C0C0);
            draw.text(draw.trim(DATE_FORMAT.format(new Date(entry.modified)), width - 6), x, y + 48, 0xC0C0C0C0);
            if (infoCache.infoDone(entry.file)) drawPreview3D(draw, entry, x, y + 72, width, -1);
            return;
        }
        int text = 0xC0C0C0C0, value = 0xFFFFFFFF, limit = width - 8;
        draw.text(UiTranslations.format("litematica.gui.label.schematic_info.name"), x, y, text);
        draw.text(draw.trim(meta.name, limit - 4), x + 4, y += 12, value);
        draw.text(draw.trim(UiTranslations.format("litematica.gui.label.schematic_info.schematic_author", meta.author), limit), x, y += 12, text);
        draw.text(draw.trim(UiTranslations.format("litematica.gui.label.schematic_info.time_created",
            DATE_FORMAT.format(new Date(meta.created))), limit), x, y += 12, text);
        if (meta.hasBeenModified()) {
            draw.text(draw.trim(UiTranslations.format("litematica.gui.label.schematic_info.time_modified",
                DATE_FORMAT.format(new Date(meta.modified))), limit), x, y += 12, text);
        }
        draw.text(UiTranslations.format("litematica.gui.label.schematic_info.region_count", meta.regions), x, y += 12, text);
        String size = String.format("%d x %d x %d", meta.sizeX, meta.sizeY, meta.sizeZ);
        if (height >= 340) {
            draw.text(UiTranslations.format("litematica.gui.label.schematic_info.total_volume", meta.volume), x, y += 12, text);
            if (meta.blocks > 0) draw.text(UiTranslations.format("litematica.gui.label.schematic_info.total_blocks", meta.blocks), x, y += 12, text);
            draw.text(UiTranslations.format("litematica.gui.label.schematic_info.enclosing_size"), x, y += 12, text);
            draw.text(size, x + 4, y += 12, value);
        } else {
            draw.text(draw.trim(meta.blocks > 0
                ? UiTranslations.format("litematica.gui.label.schematic_info.total_blocks_and_volume", meta.blocks, meta.volume)
                : UiTranslations.format("litematica.gui.label.schematic_info.total_volume", meta.volume), limit), x, y += 12, text);
            draw.text(UiTranslations.format("litematica.gui.label.schematic_info.enclosing_size_value", size), x, y += 12, text);
        }
        draw.text(meta.litematic ? UiTranslations.format("litematica.gui.label.schematic_info.version", meta.version)
            : UiTranslations.format("litematica.gui.label.schematic_info.vanilla_version"), x, y += 12, text);
        String schema = meta.dataVersion < 0 ? null : com.github.lunatrius.schematica.world.schematic.DataVersions.name(meta.dataVersion);
        if (schema != null) {
            draw.text(draw.trim(UiTranslations.format("litematica.gui.label.schematic_info.schema", schema, meta.dataVersion), limit), x, y += 12, text);
        }
        net.minecraft.util.ResourceLocation preview = infoCache.preview(entry.file, meta);
        if (preview == null || ConfigurationHandler.schematicPreview3DReplacesImage) {
            drawPreview3D(draw, entry, x, y + 24, width, meta.volume);
        } else {
            y += 24;
            int iconSize = Math.min(SchematicPreview.SIZE, Math.min(width - 14, info.bounds().bottom() - y - 30));
            if (iconSize > 8) {
                UiBounds box = new UiBounds(x + 4, y, iconSize, iconSize);
                draw.fill(box, 0xA0000000);
                draw.texture(preview.toString(), box, 0, 0, 1, 1, 1, 1);
                draw.border(new UiBounds(box.x - 1, box.y - 1, iconSize + 2, iconSize + 2), 0xFF999999);
            }
        }
    }

    /** The selected file drawn in 3D into the square below the info, when enabled and not too large. */
    private void drawPreview3D(UiDraw draw, SchematicBrowserModel.Entry entry, int x, int y, int width, long volume) {
        if (!ConfigurationHandler.schematicPreview3D || volume > SchematicPreview3D.MAX_VOLUME || entry.size > PREVIEW_3D_MAX_FILE
            || !SchematicBrowserModel.supported(entry.name())) return;
        int size = Math.min(SchematicPreview.SIZE, Math.min(width - 14, info.bounds().bottom() - y - 30));
        if (size <= 8) return;
        UiBounds box = new UiBounds(x + 4, y, size, size);
        draw.fill(box, 0xA0000000);
        draw.border(new UiBounds(box.x - 1, box.y - 1, size + 2, size + 2), 0xFF999999);
        preview3DBox = box;
        SchematicPreview3D preview = infoCache.preview3D(entry.file);
        if (preview == null) {
            draw.text("...", box.x + 3, box.bottom() - 11, 0xFFAAAAAA);
            return;
        }
        preview.draw(mc, box.x, box.y, size, infoMouseX, infoMouseY);
    }

    protected SchematicBrowserModel createModel() throws IOException {
        return new SchematicBrowserModel(ConfigurationHandler.schematicDirectory);
    }

    private UiButton icon(UiSprite sprite, String key, Runnable action) {
        UiButton button = root.add(new UiButton(() -> "", mouse -> { if (mouse == 0) action.run(); })
            .setSprite(sprite).setBackground(false));
        if (key != null) button.setTooltip(UiTranslations.format(key));
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

    @Override
    protected void closed() { infoCache.clear(); }

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
            UiTranslations.format("litematica.message.delete_confirm", entry.name()), () -> {
                try {
                    browser.delete(entry);
                    refreshFiles();
                    setStatus(UiTranslations.format("malilib.message.file_or_directory_deleted", entry.name()));
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
        Reference.logger.warn("Schematic file operation failed", error);
        if (error instanceof SchematicBrowserModel.FileOperationException) {
            SchematicBrowserModel.FileOperationException failure = (SchematicBrowserModel.FileOperationException) error;
            return UiTranslations.format(failure.translationKey, failure.arguments);
        }
        if (error instanceof FileAlreadyExistsException) {
            return UiTranslations.format("malilib.message.error.file_or_directory_already_exists", new File(((FileAlreadyExistsException) error).getFile()).getName());
        }
        return UiTranslations.format("schematica.ui.files.error.io");
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
            com.github.lunatrius.schematica.client.util.UiState.setLastDirectory(getClass().getSimpleName(), browser.directory());
            search.setText("");
            files.setOffset(0);
            updateFiles();
            setStatus("");
        } catch (IOException e) {
            updateFiles();
            fail("schematica.ui.browser.unreadable", e);
        }
    }

    /** Litematica's last_directories: each browser reopens where it was left, if that is still inside its root. */
    private void restoreDirectory() {
        String last = com.github.lunatrius.schematica.client.util.UiState.lastDirectory(getClass().getSimpleName());
        if (last == null) return;
        try {
            File directory = new File(last).getCanonicalFile();
            File root = browser.root().getCanonicalFile();
            if (directory.isDirectory() && directory.getPath().startsWith(root.getPath())) browser.navigate(directory);
        } catch (IOException | RuntimeException ignored) {
            // the root directory stays
        }
    }

    protected final void refreshFiles() {
        infoCache.clear();
        try {
            if (browser == null) {
                browser = createModel();
                restoreDirectory();
            }
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
        this.status = status.replaceAll("\\s*\n\\s*", " ");
        message.setTooltip(status.split("\n"));
    }

    protected final void fail(String key, Exception error, Object... arguments) {
        Reference.logger.error(UiTranslations.format(key, arguments), error);
        setStatus(UiTranslations.format(key, arguments));
    }

    protected final SchematicBrowserModel.Entry selection() {
        return files.selectedIndex() < 0 ? null : files.selected();
    }

    private String selectionInfo() {
        SchematicBrowserModel.Entry entry = selection();
        if (entry == null) return "";
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
        openFolder.setBounds(x + 44, y + 5, 12, 12);
        searchButton.setBounds(x + browserWidth - 24, y + 5, 12, 12);
        path.setBounds(x + 62, y + 4, browserWidth - 90, 14);
        search.setBounds(x + 2, y + 4, browserWidth - 28, 14);
        search.setVisible(searching);
        path.setVisible(!searching);
        home.setVisible(!searching);
        up.setVisible(!searching);
        createDirectory.setVisible(!searching);
        openFolder.setVisible(!searching);
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

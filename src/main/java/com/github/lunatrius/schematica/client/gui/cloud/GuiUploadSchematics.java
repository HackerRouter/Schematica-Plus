// Schematic Synchronizer upload screen, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.cloud;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.client.gui.GuiScreen;

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
import com.github.lunatrius.schematica.client.util.FolderOpener;
import com.github.lunatrius.schematica.cloud.CloudFile;
import com.github.lunatrius.schematica.cloud.CloudPath;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.handler.client.CloudClient;
import com.github.lunatrius.schematica.util.FileUtils;
import com.github.lunatrius.schematica.util.MessageType;
import com.github.lunatrius.schematica.world.schematic.SchematicFiles;

public final class GuiUploadSchematics extends UiScreen {
    private static final String PREFIX = "schematic_synchronizer.gui.";
    private final CloudPath target;
    private final File base = ConfigurationHandler.schematicDirectory;
    private final UiListModel<File> model = new UiListModel<>(20, File::getName);
    private final Set<File> selection = new LinkedHashSet<>();
    private File directory = base;
    private File anchor;
    private File lastClicked;
    private long lastClick;
    private File describedFile;
    private CloudFile described;

    private UiRowList<File> list;
    private UiWidget pathBar, info;
    private UiButton home, up, openFolder, upload, back;
    private UiTextField search;

    public GuiUploadSchematics(GuiScreen parent, CloudPath target) {
        super(parent, UiTranslations.format(PREFIX + "title.upload_schematics"));
        this.target = target;
    }

    private static boolean hidden(File file) {
        return file.getName().startsWith(".");
    }

    private static boolean schematic(File file) {
        return file.isFile() && CloudPath.schematicName(file.getName());
    }

    @Override
    protected void createWidgets() {
        home = icon(UiSprite.ROOT, "malilib.gui.button.hover.directory_widget.root", () -> navigate(base));
        up = icon(UiSprite.UP, "malilib.gui.button.hover.directory_widget.up", () -> { if (!directory.equals(base)) navigate(directory.getParentFile()); });
        openFolder = icon(UiSprite.DIRECTORY, "malilib.gui.button.hover.directory_widget.open_directory", () -> FolderOpener.open(directory));
        pathBar = root.add(new UiWidget() {
            @Override public void draw(UiDraw draw, int mouseX, int mouseY) {
                draw.fill(bounds(), 0x50000000);
                draw.border(bounds(), 0x80FFFFFF);
                draw.flatText(draw.trim("§e/" + relative(directory), bounds().width - 8), bounds().x + 4, bounds().y + 5, 0xFFFFFFFF);
            }
        });
        search = root.add(new UiTextField(fontRendererObj, 256, text -> refresh()));
        list = root.add(new UiRowList<>(model, Row::new));
        info = root.add(new UiWidget() {
            @Override public void draw(UiDraw draw, int mouseX, int mouseY) {
                CloudInfo.box(draw, bounds());
                try (UiDraw.Clip ignored = draw.clip(new UiBounds(bounds().x + 1, bounds().y + 1, bounds().width - 2, bounds().height - 2))) {
                    drawInfo(draw, bounds().x + 6, bounds().y + 6, bounds().width - 12);
                }
            }
        });
        upload = root.add(new UiButton(() -> selection.size() > 1 ? UiTranslations.format(PREFIX + "button.upload_to_server_count", selection.size())
            : UiTranslations.format(PREFIX + "button.upload_to_server"), mouse -> { if (mouse == 0) uploadSelection(); }));
        back = addButton("gui.back", this::closeScreen);
    }

    private UiButton icon(UiSprite sprite, String tooltip, Runnable action) {
        UiButton button = root.add(new UiButton(() -> "", mouse -> { if (mouse == 0) action.run(); }).setSprite(sprite).setBackground(false));
        button.setTooltip(UiTranslations.format(tooltip));
        return button;
    }

    @Override
    protected void opened() { refresh(); }

    private String relative(File file) {
        return base.toPath().relativize(file.toPath()).toString().replace('\\', '/');
    }

    private void navigate(File next) {
        directory = next;
        selection.clear();
        anchor = null;
        model.setOffset(0);
        search.setText("");
        refresh();
    }

    private void refresh() {
        List<File> entries = new ArrayList<>();
        String query = search.text().trim().toLowerCase(Locale.ROOT);
        if (!directory.equals(base) && query.isEmpty()) entries.add(new File(directory, ".."));
        File[] children = directory.listFiles();
        if (children != null) {
            Arrays.sort(children, Comparator.comparing((File file) -> !file.isDirectory()).thenComparing(file -> file.getName().toLowerCase(Locale.ROOT)));
            for (File child : children) {
                if (hidden(child) || !child.isDirectory() && !schematic(child)) continue;
                if (!query.isEmpty() && !child.getName().toLowerCase(Locale.ROOT).contains(query)) continue;
                entries.add(child);
            }
        }
        model.setEntries(entries);
        selection.retainAll(entries);
        list.sync();
    }

    private static boolean up(File file) { return file.getName().equals(".."); }

    static int count(File directory) {
        File[] children = directory.listFiles();
        if (children == null) return 0;
        int count = 0;
        for (File child : children) {
            if (hidden(child)) continue;
            count += child.isDirectory() ? count(child) : schematic(child) ? 1 : 0;
        }
        return count;
    }

    private static long size(File file) {
        if (!file.isDirectory()) return file.length();
        File[] children = file.listFiles();
        long size = 0;
        if (children != null) for (File child : children) if (!hidden(child)) size += size(child);
        return size;
    }

    private void click(File file, int index) {
        long now = System.currentTimeMillis();
        boolean twice = file.equals(lastClicked) && now - lastClick < 500;
        lastClick = now;
        lastClicked = file;
        if (up(file)) {
            navigate(directory.getParentFile());
            return;
        }
        if (twice && file.isDirectory()) {
            navigate(file);
            return;
        }
        List<File> entries = model.entries();
        if (isShiftKeyDown() && anchor != null && entries.contains(anchor)) {
            if (!isCtrlKeyDown()) selection.clear();
            int from = entries.indexOf(anchor), to = index;
            for (int i = Math.min(from, to); i <= Math.max(from, to); i++) if (!up(entries.get(i))) selection.add(entries.get(i));
            return;
        }
        if (isCtrlKeyDown()) {
            if (!selection.remove(file)) selection.add(file);
        } else {
            selection.clear();
            selection.add(file);
        }
        anchor = file;
    }

    private void uploadSelection() {
        if (target == null || selection.isEmpty()) return;
        for (File file : selection) queue(file, target);
        CloudClient.INSTANCE.refresh();
        closeScreen();
    }

    private void queue(File file, CloudPath folder) {
        try {
            if (file.isDirectory()) {
                CloudPath next = folder.child(file.getName());
                File[] children = file.listFiles();
                if (children != null) for (File child : children) if (!hidden(child)) queue(child, next);
            } else if (schematic(file)) {
                CloudClient.INSTANCE.upload(file, folder.child(file.getName()));
            }
        } catch (IllegalArgumentException e) {
            addMessage(MessageType.ERROR, "schematica.message.cloud.invalid_name", file.getName());
        }
    }

    @Override
    protected void tickScreen() {
        up.setEnabled(!directory.equals(base));
        upload.setEnabled(target != null && !selection.isEmpty());
    }

    @Override
    protected void layoutWidgets() {
        int infoWidth = CloudInfo.infoWidth(width), browserWidth = CloudInfo.browserWidth(width), browserHeight = height - 74;
        int iconY = 28;
        home.setBounds(12, iconY - 1, 16, 16);
        up.setBounds(28, iconY - 1, 16, 16);
        openFolder.setBounds(44, iconY - 1, 16, 16);
        int searchWidth = Math.min(130, Math.max(70, browserWidth / 4));
        search.setBounds(10 + browserWidth - searchWidth - 4, 28, searchWidth, 15);
        pathBar.setBounds(63, 26 + 2, Math.max(0, search.bounds().x - 4 - 63), 18);
        list.setBounds(10, 50, browserWidth, Math.max(0, browserHeight - 24));
        info.setBounds(10 + browserWidth + 4, 26, infoWidth, browserHeight);
        int uploadWidth = fontRendererObj.getStringWidth(upload.label()) + 20;
        upload.setBounds(10, height - 24, uploadWidth, 20);
        int backWidth = fontRendererObj.getStringWidth(back.label()) + 20;
        back.setBounds(width - backWidth - 10, height - 24, backWidth, 20);
    }

    private String targetName() {
        return target == null ? "/" : GuiServerSchematics.display(target);
    }

    private void drawInfo(UiDraw draw, int x, int y, int width) {
        if (selection.isEmpty()) {
            draw.text("§6§l" + UiTranslations.format(PREFIX + "upload_info.title"), x, y, 0xFFFFFFFF);
            for (String line : fontRendererObj.listFormattedStringToWidth("§7" + UiTranslations.format(PREFIX + "upload_info.select_hint"), width)) {
                draw.text(line, x, y += CloudInfo.LINE, 0xFFFFFFFF);
            }
            y = CloudInfo.divider(draw, x, y + CloudInfo.LINE, width);
            draw.text(draw.trim(UiTranslations.format(PREFIX + "upload_info.target_server_folder", targetName()), width), x, y, 0xFFFFFFFF);
            return;
        }
        if (selection.size() > 1) {
            long size = 0;
            for (File file : selection) size += size(file);
            draw.text("§a§l" + UiTranslations.format(PREFIX + "upload_info.multiple_selected", selection.size()), x, y, 0xFFFFFFFF);
            draw.text(FileUtils.humanReadableByteCount(size), x, y += CloudInfo.LINE, 0xFFAAAAAA);
            y = CloudInfo.divider(draw, x, y + CloudInfo.LINE, width);
            int bottom = info.bounds().bottom() - 30;
            for (File file : selection) {
                if (y > bottom) {
                    draw.text("...", x, y, 0xFFAAAAAA);
                    break;
                }
                draw.text(draw.trim((file.isDirectory() ? "\u00a7e" + file.getName() + "/" : "\u00a7f" + file.getName()), width), x, y, 0xFFFFFFFF);
                y += CloudInfo.LINE;
            }
            y = CloudInfo.divider(draw, x, y, width);
            draw.text(draw.trim(UiTranslations.format(PREFIX + "upload_info.target_server_folder", targetName()), width), x, y, 0xFFFFFFFF);
            return;
        }
        File file = selection.iterator().next();
        if (file.isDirectory()) {
            draw.text(draw.trim("§e§l" + file.getName() + "/", width), x, y, 0xFFFFFFFF);
            draw.text(UiTranslations.format(PREFIX + "upload_info.directory_schematics", count(file)), x, y += 14, 0xFFFFFFFF);
            y = CloudInfo.divider(draw, x, y + CloudInfo.LINE, width);
            draw.text(draw.trim(UiTranslations.format(PREFIX + "upload_info.target_server_folder", targetName()), width), x, y, 0xFFFFFFFF);
            return;
        }
        y = CloudInfo.schematic(draw, x, y, width, describe(file));
        draw.text(draw.trim(UiTranslations.format(PREFIX + "info.file_label", "§f" + FileUtils.humanReadableByteCount(file.length()) + "§r", ""), width), x, y, CloudInfo.LABEL);
        y = CloudInfo.divider(draw, x, y + CloudInfo.LINE, width);
        draw.text(draw.trim(UiTranslations.format(PREFIX + "upload_info.target_server_folder", targetName()), width), x, y, 0xFFFFFFFF);
    }

    private CloudFile describe(File file) {
        if (file.equals(describedFile)) return described;
        CloudFile result = new CloudFile(CloudPath.root(CloudPath.PRIVATE), file.length(), file.lastModified(), "", "");
        try {
            result.describe(SchematicFiles.info(file));
        } catch (IOException | RuntimeException e) {
            result.name = file.getName();
        }
        if (result.name.isEmpty()) result.name = file.getName();
        describedFile = file;
        described = result;
        return result;
    }

    private final class Row extends UiPanel {
        private final File file;
        private final int index;

        Row(File file, int index) {
            this.file = file;
            this.index = index;
        }

        @Override
        public void draw(UiDraw draw, int mouseX, int mouseY) {
            UiBounds box = bounds();
            boolean chosen = selection.contains(file);
            if (chosen) {
                draw.fill(box, 0x70FFFFFF);
                draw.border(box, 0xFFFFFFFF);
            } else if (containsVisible(mouseX, mouseY)) {
                draw.fill(box, 0x40FFFFFF);
                draw.border(box, 0xA0FFFFFF);
            } else draw.fill(box, index % 2 == 1 ? 0x20FFFFFF : 0x10FFFFFF);
            boolean isUp = up(file);
            UiSprite icon = isUp ? UiSprite.UP : file.isDirectory() ? UiSprite.DIRECTORY : UiSprite.schematicFile(file.getName());
            icon.draw(draw, box.x + 3, box.y + (box.height - icon.height) / 2, true, false);
            int textY = box.y + (box.height - 8) / 2, right = box.right() - 6;
            if (isUp) {
                draw.text("§e..", box.x + 19, textY, 0xFFFFFFFF);
            } else if (file.isDirectory()) {
                String count = "§7(" + count(file) + ")";
                draw.text(count, right - draw.textWidth(count), textY, 0xFFFFFFFF);
                draw.text(draw.trim("§6" + file.getName() + "/", right - draw.textWidth(count) - box.x - 26), box.x + 19, textY, 0xFFFFFFFF);
            } else {
                String size = "§7" + FileUtils.humanReadableByteCount(file.length());
                draw.text(size, right - draw.textWidth(size), textY, 0xFFFFFFFF);
                draw.text(draw.trim(file.getName(), right - draw.textWidth(size) - box.x - 26), box.x + 19, textY, chosen ? 0xFFFFFFFF : 0xFFDDDDDD);
            }
        }

        @Override
        public boolean isFocusable() { return true; }

        @Override
        public boolean mouseDown(int x, int y, int button) {
            if (button != 0) return false;
            click(file, index);
            return true;
        }
    }
}

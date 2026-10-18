// Schematic Synchronizer server schematics screen, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.cloud;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.gui.GuiScreen;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.api.SchematicRegion;
import com.github.lunatrius.schematica.client.cloud.CloudCache;
import com.github.lunatrius.schematica.client.gui.control.GuiSchematicMaterials;
import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiCheckBox;
import com.github.lunatrius.schematica.client.gui.framework.UiDraw;
import com.github.lunatrius.schematica.client.gui.framework.UiListModel;
import com.github.lunatrius.schematica.client.gui.framework.UiPanel;
import com.github.lunatrius.schematica.client.gui.framework.UiRowList;
import com.github.lunatrius.schematica.client.gui.framework.UiScreen;
import com.github.lunatrius.schematica.client.gui.framework.UiSprite;
import com.github.lunatrius.schematica.client.gui.framework.UiTextField;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.gui.framework.UiWidget;
import com.github.lunatrius.schematica.client.gui.load.SchematicGuiLoader;
import com.github.lunatrius.schematica.client.gui.material.MaterialList;
import com.github.lunatrius.schematica.client.gui.material.MaterialLists;
import com.github.lunatrius.schematica.client.util.FolderOpener;
import com.github.lunatrius.schematica.client.util.UiState;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.cloud.CloudCatalog;
import com.github.lunatrius.schematica.cloud.CloudFile;
import com.github.lunatrius.schematica.cloud.CloudPath;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.handler.client.CloudClient;
import com.github.lunatrius.schematica.util.FileUtils;
import com.github.lunatrius.schematica.util.MessageType;
import com.github.lunatrius.schematica.world.schematic.SchematicFormat;

import org.lwjgl.input.Keyboard;

public final class GuiServerSchematics extends UiScreen {
    private static final String PREFIX = "schematic_synchronizer.gui.";
    private final UiListModel<CloudNode> model = new UiListModel<>(22, CloudNode::name);
    private CloudPath directory;
    private CloudNode selected;
    private CloudPath selectedPath;
    private boolean publishShown;
    private int revision = -1;
    private long lastClick;
    private CloudNode lastClicked;
    private boolean placeOnLoad = Boolean.parseBoolean(UiState.get("create_placement_on_load", "true"));

    private UiRowList<CloudNode> list;
    private UiWidget pathBar, info, empty;
    private UiButton home, up, createDirectory, openFolder, upload;
    private UiTextField search;
    private UiCheckBox createPlacement;
    private UiButton load, materials, download, delete, publish, menu;

    public GuiServerSchematics(GuiScreen parent) {
        super(parent, UiTranslations.format(PREFIX + "title.server_schematics"));
    }

    static String scopeName(String scope) {
        return UiTranslations.format("schematica.ui.cloud." + scope);
    }

    static String display(CloudPath path) {
        if (path == null) return "/";
        return "/" + scopeName(path.scope) + (path.isRoot() ? "" : "/" + path.relative()) + "/";
    }

    @Override
    protected void createWidgets() {
        home = icon(UiSprite.ROOT, "malilib.gui.button.hover.directory_widget.root", () -> navigate(null));
        up = icon(UiSprite.UP, "malilib.gui.button.hover.directory_widget.up", this::goUp);
        createDirectory = icon(UiSprite.CREATE_DIRECTORY, "malilib.gui.button.hover.directory_widget.create_directory", this::createDirectory);
        openFolder = icon(UiSprite.DIRECTORY, "malilib.gui.button.hover.directory_widget.open_directory", this::openFolder);
        upload = icon(UiSprite.UPLOAD, PREFIX + "button.hover.upload_to_server",
            () -> mc.displayGuiScreen(new GuiUploadSchematics(this, directory)));
        pathBar = root.add(new UiWidget() {
            @Override public void draw(UiDraw draw, int mouseX, int mouseY) {
                draw.fill(bounds(), 0x20FFFFFF);
                draw.border(bounds(), 0x40FFFFFF);
                String text = display(directory);
                int room = bounds().width - 8;
                if (draw.textWidth(text) > room) {
                    while (text.length() > 1 && draw.textWidth("..." + text) > room) text = text.substring(1);
                    text = "..." + text;
                }
                draw.flatText(text, bounds().x + 4, bounds().y + 3, 0xFFDDDDDD);
            }
        });
        search = root.add(new UiTextField(fontRendererObj, 256, text -> rebuild()));
        list = root.add(new UiRowList<>(model, Row::new));
        empty = root.add(new UiWidget() {
            @Override public void draw(UiDraw draw, int mouseX, int mouseY) { drawEmpty(draw); }
        });
        info = root.add(new UiWidget() {
            @Override public void draw(UiDraw draw, int mouseX, int mouseY) {
                CloudInfo.box(draw, bounds());
                try (UiDraw.Clip ignored = draw.clip(new UiBounds(bounds().x + 1, bounds().y + 1, bounds().width - 2, bounds().height - 2))) {
                    drawInfo(draw, bounds().x + 6, bounds().y + 6, bounds().width - 12);
                }
            }
        });
        createPlacement = root.add(new UiCheckBox(() -> UiTranslations.format("litematica.gui.label.schematic_load.checkbox.create_placement"),
            () -> placeOnLoad, value -> {
                placeOnLoad = value;
                UiState.set("create_placement_on_load", Boolean.toString(value));
                layoutWidgets();
            }));
        createPlacement.setTooltip(UiTranslations.format("litematica.gui.label.schematic_load.hoverinfo.create_placement").split("\n"));
        load = root.add(new UiButton(() -> UiTranslations.format(placeOnLoad ? "litematica.gui.button.create_placement"
            : "litematica.gui.button.load_schematic_to_memory"), mouse -> { if (mouse == 0) loadSelected(); }));
        materials = addButton("litematica.gui.button.material_list", this::materialList);
        download = addButton("schematica.ui.cloud.download", this::downloadSelected);
        publish = addButton("schematica.ui.cloud.publish", this::publishSelected);
        delete = addButton("schematica.ui.cloud.delete", this::deleteSelected);
        menu = addButton("litematica.gui.button.change_menu.to_main_menu", this::mainMenu);
    }

    private UiButton icon(UiSprite sprite, String tooltip, Runnable action) {
        UiButton button = root.add(new UiButton(() -> "", mouse -> { if (mouse == 0) action.run(); }).setSprite(sprite).setBackground(false));
        button.setTooltip(UiTranslations.format(tooltip));
        return button;
    }

    @Override
    protected void opened() {
        CloudClient.INSTANCE.refresh();
    }

    private void rebuild() {
        List<CloudNode> nodes = CloudNode.list(CloudClient.INSTANCE.catalog(), directory, search.text());
        model.setEntries(nodes);
        if (selected != null && !nodes.contains(selected)) {
            selected = null;
            for (CloudNode node : nodes) if (node.type != CloudNode.Type.UP && node.path.equals(selectedPath)) selected = node;
        }
        list.sync();
    }

    private void select(CloudNode node) {
        selected = node;
        selectedPath = node == null ? null : node.path;
    }

    private void navigate(CloudPath target) {
        directory = target;
        select(null);
        model.setOffset(0);
        search.setText("");
        rebuild();
    }

    private void goUp() {
        if (directory != null) navigate(directory.isRoot() ? null : directory.parent());
    }

    private CloudCatalog catalog() { return CloudClient.INSTANCE.catalog(); }

    private boolean mayWrite(CloudPath path) {
        CloudCatalog catalog = catalog();
        return catalog != null && path != null && (CloudPath.PRIVATE.equals(path.scope) || catalog.publish);
    }

    private void createDirectory() {
        if (!mayWrite(directory)) return;
        prompt(UiTranslations.format("malilib.gui.title.create_directory"), "", name -> {
            if (!CloudPath.validName(name.trim())) return UiTranslations.format("schematica.message.cloud.invalid_name", name);
            CloudClient.INSTANCE.createDirectory(directory.child(name.trim()));
            return null;
        });
    }

    private void openFolder() {
        File folder = directory == null ? CloudCache.root() : CloudCache.file(directory);
        if (!folder.isDirectory() && !folder.mkdirs()) return;
        FolderOpener.open(folder);
    }

    private CloudFile selectedFile() {
        return selected != null && selected.type == CloudNode.Type.SCHEMATIC ? selected.file : null;
    }

    private void fetch(CloudFile file, Consumer<File> done) {
        CloudClient.INSTANCE.fetch(file, local -> {
            if (local == null) addMessage(MessageType.ERROR, "schematica.message.cloud.download_failed", file.path.name());
            else done.accept(local);
        });
    }

    private void loadSelected() {
        CloudFile file = selectedFile();
        if (file == null) return;
        if (!SchematicaPlus.proxy.isLoadEnabled || mc.theWorld == null || mc.thePlayer == null) {
            addMessage(MessageType.ERROR, "schematica.ui.load.disabled");
            return;
        }
        fetch(file, local -> {
            try {
                SchematicGuiLoader.loadAsync(mc, local, placeOnLoad,
                    source -> addMessage(MessageType.SUCCESS, "litematica.message.schematic_read_from_file_success", local.getName()),
                    error -> addMessage(MessageType.ERROR, "litematica.error.schematic_read_from_file_failed.exception", local.getName()));
            } catch (IOException | RuntimeException e) {
                addMessage(MessageType.ERROR, "litematica.error.schematic_read_from_file_failed.exception", local.getName());
            }
        });
    }

    private void materialList() {
        CloudFile file = selectedFile();
        if (file == null) return;
        fetch(file, local -> {
            ISchematic schematic = SchematicFormat.readFromFile(local);
            if (schematic == null) {
                addMessage(MessageType.ERROR, "litematica.error.schematic_read_from_file_failed.exception", local.getName());
                return;
            }
            SchematicWorld world = new SchematicWorld(schematic, local.getName());
            world.sourceFilename = local.getName();
            MaterialList list = MaterialList.schematic(world, file.name, Collections.<SchematicRegion>emptyList());
            MaterialLists.setCurrent(list);
            mc.displayGuiScreen(new GuiSchematicMaterials(this, list));
        });
    }

    private void downloadSelected() {
        CloudFile file = selectedFile();
        if (file == null) return;
        fetch(file, local -> {
            try {
                File target = unique(ConfigurationHandler.schematicDirectory, file.path.name());
                Files.copy(local.toPath(), target.toPath());
                addMessage(MessageType.SUCCESS, "schematica.message.cloud.downloaded", target.getName());
            } catch (IOException e) {
                addMessage(MessageType.ERROR, "schematica.message.cloud.download_failed", file.path.name());
            }
        });
    }

    static File unique(File directory, String name) {
        File target = new File(directory, name);
        int dot = name.lastIndexOf('.');
        for (int i = 2; target.exists(); i++) target = new File(directory, name.substring(0, dot) + " (" + i + ")" + name.substring(dot));
        return target;
    }

    private void publishSelected() {
        CloudFile file = selectedFile();
        if (file != null && CloudPath.PRIVATE.equals(file.path.scope)) CloudClient.INSTANCE.publish(file.path, CloudPath.root(CloudPath.PUBLIC));
    }

    private void deleteSelected() {
        if (selected == null || selected.type == CloudNode.Type.UP || selected.path.isRoot()) return;
        CloudPath path = selected.path;
        confirm(UiTranslations.format("schematica.ui.cloud.delete"), UiTranslations.format("schematica.ui.cloud.delete_confirm", path.name()),
            () -> CloudClient.INSTANCE.delete(path));
    }

    private void activate(CloudNode node) {
        if (node.type == CloudNode.Type.UP) navigate(node.path);
        else if (node.type == CloudNode.Type.DIRECTORY) navigate(node.path);
        else loadSelected();
    }

    @Override
    protected boolean handleKey(char character, int keyCode) {
        if ((keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) && selected != null && !search.isFocused()) {
            activate(selected);
            return true;
        }
        return false;
    }

    @Override
    protected void tickScreen() {
        if (CloudClient.INSTANCE.revision() != revision && list != null) {
            revision = CloudClient.INSTANCE.revision();
            rebuild();
        }
        boolean file = selectedFile() != null;
        boolean entry = selected != null && selected.type != CloudNode.Type.UP && !selected.path.isRoot();
        CloudCatalog catalog = catalog();
        up.setEnabled(directory != null);
        createDirectory.setEnabled(mayWrite(directory));
        upload.setEnabled(catalog != null && catalog.upload && mayWrite(directory));
        load.setEnabled(file && SchematicaPlus.proxy.isLoadEnabled);
        materials.setEnabled(file);
        download.setEnabled(file);
        publish.setVisible(file && CloudPath.PRIVATE.equals(selected.path.scope));
        publish.setEnabled(catalog != null && catalog.publish);
        delete.setEnabled(entry && catalog != null && (CloudPath.PRIVATE.equals(selected.path.scope) || catalog.manage || owned(selected)));
        if (publishShown != publish.isVisible()) {
            publishShown = publish.isVisible();
            layoutWidgets();
        }
    }

    private boolean owned(CloudNode node) {
        return node.file == null || mc.thePlayer != null && node.file.owner.equals(mc.thePlayer.getCommandSenderName());
    }

    @Override
    protected void layoutWidgets() {
        int infoWidth = CloudInfo.infoWidth(width), browserWidth = CloudInfo.browserWidth(width), browserHeight = height - 74;
        int iconY = 29;
        home.setBounds(12, iconY - 1, 16, 16);
        up.setBounds(28, iconY - 1, 16, 16);
        createDirectory.setBounds(44, iconY - 1, 16, 16);
        openFolder.setBounds(60, iconY - 1, 16, 16);
        upload.setBounds(76, iconY - 1, 16, 16);
        int searchWidth = Math.min(130, Math.max(70, browserWidth / 4));
        search.setBounds(10 + browserWidth - searchWidth - 4, 29, searchWidth, 15);
        pathBar.setBounds(95, iconY, Math.max(0, search.bounds().x - 4 - 95), 14);
        list.setBounds(10, 48, browserWidth, Math.max(0, browserHeight - 22));
        empty.setBounds(10, 48, browserWidth, Math.max(0, browserHeight - 22));
        info.setBounds(10 + browserWidth + 4, 26, infoWidth, browserHeight);
        createPlacement.setBounds(10, height - 44, Math.min(width - 20, fontRendererObj.getStringWidth(createPlacement.label()) + 20), 11);
        int x = 10;
        load.setBounds(x, height - 24, buttonWidth(load, 18), 20);
        x += load.bounds().width + 4;
        for (UiButton button : new UiButton[] {materials, download, publish, delete}) {
            if (!button.isVisible()) continue;
            button.setBounds(x, height - 24, buttonWidth(button, 16), 20);
            x += button.bounds().width + 4;
        }
        int menuWidth = fontRendererObj.getStringWidth(menu.label()) + 20;
        menu.setBounds(width - menuWidth - 10, height - 24, menuWidth, 20);
    }

    private int buttonWidth(UiButton button, int padding) {
        return fontRendererObj.getStringWidth(button.label()) + padding;
    }

    private void drawEmpty(UiDraw draw) {
        if (!model.entries().isEmpty() && catalog() != null) return;
        List<String[]> lines = new ArrayList<>();
        if (!CloudClient.INSTANCE.available()) {
            lines.add(new String[] {UiTranslations.format("schematica.ui.cloud.unavailable"), "FFDDDDDD"});
        } else if (catalog() == null) {
            lines.add(new String[] {UiTranslations.format("schematica.ui.cloud.loading"), "FFDDDDDD"});
        } else if (catalog().files.isEmpty() && catalog().directories.isEmpty()) {
            lines.add(new String[] {UiTranslations.format(PREFIX + "browser.empty_server"), "FFDDDDDD"});
            lines.add(new String[] {UiTranslations.format(PREFIX + "browser.empty_server_upload"), "FFAAAAAA"});
        } else {
            lines.add(new String[] {UiTranslations.format(PREFIX + "browser.empty_folder"), "FFDDDDDD"});
        }
        int y = empty.bounds().y + empty.bounds().height / 2 - lines.size() * 7;
        for (String[] line : lines) {
            draw.text(line[0], empty.bounds().x + (empty.bounds().width - draw.textWidth(line[0])) / 2, y, (int) Long.parseLong(line[1], 16));
            y += 14;
        }
    }

    private void drawInfo(UiDraw draw, int x, int y, int width) {
        CloudCatalog catalog = catalog();
        if (selected == null) {
            draw.text("§6§l" + UiTranslations.format(PREFIX + "info.title"), x, y, 0xFFFFFFFF);
            draw.text("§7" + UiTranslations.format(PREFIX + "info.select_schematic_line1"), x, y += 14, 0xFFFFFFFF);
            draw.text("§7" + UiTranslations.format(PREFIX + "info.select_schematic_line2"), x, y += CloudInfo.LINE, 0xFFFFFFFF);
            y = CloudInfo.divider(draw, x, y + CloudInfo.LINE, width);
            draw.text("§7" + UiTranslations.format(PREFIX + "info.server_folder"), x, y, 0xFFFFFFFF);
            draw.text(draw.trim("§e" + display(directory), width), x, y += CloudInfo.LINE, 0xFFFFFFFF);
            if (catalog == null) return;
            draw.text(UiTranslations.format(PREFIX + "info.total_schematics", catalog.files.size()), x, y += 14, 0xFFFFFFFF);
            String quota = catalog.quota < 0 ? FileUtils.humanReadableByteCount(catalog.used)
                : FileUtils.humanReadableByteCount(catalog.used) + " / " + FileUtils.humanReadableByteCount(catalog.quota);
            draw.text(draw.trim(UiTranslations.format("schematica.ui.cloud.storage", quota), width), x, y + CloudInfo.LINE, 0xFFFFFFFF);
            return;
        }
        if (selected.type == CloudNode.Type.UP) {
            draw.text("§7§l" + UiTranslations.format(PREFIX + "info.up_title"), x, y, 0xFFFFFFFF);
            draw.text("§a" + UiTranslations.format(PREFIX + "info.up_hint1"), x, y += 14, 0xFFFFFFFF);
            draw.text("§a" + UiTranslations.format(PREFIX + "info.up_hint2"), x, y + CloudInfo.LINE, 0xFFFFFFFF);
            return;
        }
        if (selected.type == CloudNode.Type.DIRECTORY) {
            draw.text(draw.trim("§e§l" + (selected.path.isRoot() ? scopeName(selected.path.scope) : selected.path.name()) + "/", width), x, y, 0xFFFFFFFF);
            draw.text(draw.trim("§7" + UiTranslations.format(PREFIX + "info.dir_path", display(selected.path).substring(1)), width), x, y += 14, 0xFFFFFFFF);
            draw.text(UiTranslations.format(PREFIX + "info.dir_schematics", selected.count), x, y += CloudInfo.LINE, 0xFFFFFFFF);
            y = CloudInfo.divider(draw, x, y + CloudInfo.LINE, width);
            draw.text("§a" + UiTranslations.format(PREFIX + "info.dir_hint1"), x, y, 0xFFFFFFFF);
            draw.text("§a" + UiTranslations.format(PREFIX + "info.dir_hint2"), x, y + CloudInfo.LINE, 0xFFFFFFFF);
            return;
        }
        CloudFile file = selected.file;
        y = CloudInfo.schematic(draw, x, y, width, file);
        if (!file.owner.isEmpty()) {
            draw.text(draw.trim(UiTranslations.format(PREFIX + "info.owner") + ": §f" + file.owner, width), x, y, CloudInfo.LABEL);
            y += CloudInfo.LINE;
        }
        String status = CloudCache.cached(file) ? "§a✔ " + UiTranslations.format(PREFIX + "info.status_cached")
            : "§e☁ " + UiTranslations.format(PREFIX + "info.status_server");
        draw.text(draw.trim(UiTranslations.format(PREFIX + "info.file_label", "§f" + FileUtils.humanReadableByteCount(file.size) + "§r", status), width), x, y, CloudInfo.LABEL);
    }

    private final class Row extends UiPanel {
        private final CloudNode node;
        private final int index;

        Row(CloudNode node, int index) {
            this.node = node;
            this.index = index;
            if (node.file != null && CloudCache.cached(node.file)) {
                setTooltip(UiTranslations.format(PREFIX + "hover.saved_path", CloudCache.file(node.file.path).getPath()));
            }
        }

        @Override
        public void draw(UiDraw draw, int mouseX, int mouseY) {
            UiBounds box = bounds();
            boolean hovered = containsVisible(mouseX, mouseY);
            if (node.equals(selected)) {
                draw.fill(box, 0x70FFFFFF);
                draw.border(box, 0xEEEEEEEE);
            } else if (hovered) {
                draw.border(box, 0xEEEEEEEE);
                draw.fill(box, 0x38FFFFFF);
            } else draw.fill(box, index % 2 == 1 ? 0x20FFFFFF : 0x38FFFFFF);
            UiSprite icon = node.type == CloudNode.Type.UP ? UiSprite.UP : node.type == CloudNode.Type.DIRECTORY ? UiSprite.DIRECTORY
                : UiSprite.schematicFile(node.name());
            icon.draw(draw, box.x + 3, box.y + (box.height - icon.height) / 2, true, false);
            int textY = box.y + (box.height - 8) / 2 + 1, right = box.right() - 6;
            if (node.type == CloudNode.Type.UP) {
                draw.text("..", box.x + 22, textY, 0xFFAAAAAA);
            } else if (node.type == CloudNode.Type.DIRECTORY) {
                String count = "(" + node.count + ")";
                draw.text(count, right - draw.textWidth(count), textY, 0xFFAAAAAA);
                String name = node.path.isRoot() ? scopeName(node.path.scope) : node.name();
                draw.text(draw.trim(name + "/", right - draw.textWidth(count) - box.x - 30), box.x + 22, textY, 0xFFFFFF55);
            } else {
                if (CloudCache.cached(node.file)) {
                    UiSprite.DOWNLOADED.draw(draw, right - 11, box.y + (box.height - 11) / 2, true, false);
                    right -= 16;
                }
                draw.text(draw.trim(node.name(), right - box.x - 24), box.x + 22, textY, 0xFFFFFFFF);
            }
        }

        @Override
        public boolean isFocusable() { return true; }

        @Override
        public boolean mouseDown(int x, int y, int button) {
            if (button != 0) return false;
            long now = System.currentTimeMillis();
            boolean twice = node.equals(lastClicked) && now - lastClick < 500;
            lastClick = now;
            lastClicked = node;
            if (node.type == CloudNode.Type.UP) {
                navigate(node.path);
                return true;
            }
            select(node);
            if (twice && node.type == CloudNode.Type.DIRECTORY) navigate(node.path);
            return true;
        }
    }
}

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
import com.github.lunatrius.schematica.client.gui.framework.UiLabel;
import com.github.lunatrius.schematica.client.gui.framework.UiList;
import com.github.lunatrius.schematica.client.gui.framework.UiListModel;
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
    private final UiListModel<CloudNode> model = new UiListModel<>(14, CloudNode::name);
    private final List<UiButton> actions = new ArrayList<>();
    private CloudPath directory;
    private int revision = -1;
    private boolean searching;
    private boolean placeOnLoad = Boolean.parseBoolean(UiState.get("create_placement_on_load", "true"));

    private UiWidget frame, info;
    private UiLabel path, status;
    private UiList<CloudNode> list;
    private UiTextField search;
    private UiButton home, up, createDirectory, openFolder, searchButton, back;
    private UiButton load, materials, upload, download, publish, delete;
    private UiCheckBox createPlacement;

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
        frame = root.add(new UiWidget() {
            @Override public void draw(UiDraw draw, int mouseX, int mouseY) {
                draw.fill(bounds(), 0xB0000000);
                draw.border(bounds(), 0xFF999999);
            }
        });
        path = root.add(new UiLabel(() -> display(directory)));
        home = icon(UiSprite.ROOT, "malilib.gui.button.hover.directory_widget.root", () -> navigate(null));
        up = icon(UiSprite.UP, "malilib.gui.button.hover.directory_widget.up", this::goUp);
        createDirectory = icon(UiSprite.CREATE_DIRECTORY, "malilib.gui.button.hover.directory_widget.create_directory", this::createDirectory);
        openFolder = icon(UiSprite.DIRECTORY, "malilib.gui.button.hover.directory_widget.open_directory", this::openFolder);
        search = root.add(new UiTextField(fontRendererObj, 256, text -> rebuild()));
        searchButton = icon(UiSprite.SEARCH, null, () -> {
            searching = !searching;
            layoutWidgets();
            if (searching) input.focus(search);
            else search.setText("");
        });
        list = root.add(new UiList<>(model, "", this::activate));
        list.setFileStyle(node -> node.type == CloudNode.Type.DIRECTORY ? UiSprite.DIRECTORY : UiSprite.schematicFile(node.name()),
            node -> node.type == CloudNode.Type.DIRECTORY ? (node.path.isRoot() ? scopeName(node.path.scope) : node.name())
                : node.name().substring(0, node.name().lastIndexOf('.')));
        list.setBadges(node -> node.file != null && CloudCache.cached(node.file) ? UiSprite.DOWNLOADED : null);
        list.setEntryTooltip(node -> node.file != null && CloudCache.cached(node.file)
            ? Collections.singletonList(UiTranslations.format(PREFIX + "hover.saved_path", relative(CloudCache.file(node.file.path))))
            : Collections.singletonList(node.name()));
        info = root.add(new UiWidget() {
            @Override public void draw(UiDraw draw, int mouseX, int mouseY) {
                draw.fill(bounds(), 0xA0000000);
                draw.border(bounds(), 0xFF999999);
                try (UiDraw.Clip ignored = draw.clip(new UiBounds(bounds().x + 1, bounds().y + 1, bounds().width - 2, bounds().height - 30))) {
                    drawInfo(draw, bounds().x + 3, bounds().y + 3, bounds().width - 4);
                }
            }
        });
        status = root.add(new UiLabel(this::storage));
        load = action("litematica.gui.button.load_schematic_to_memory", this::loadSelected);
        materials = action("litematica.gui.button.material_list", this::materialList);
        upload = action(PREFIX + "button.upload_to_server", () -> mc.displayGuiScreen(new GuiUploadSchematics(this, directory)));
        upload.setTooltip(UiTranslations.format(PREFIX + "button.hover.upload_to_server"));
        download = action("schematica.ui.cloud.download", this::downloadSelected);
        publish = action("schematica.ui.cloud.publish", this::publishSelected);
        delete = action("schematica.ui.cloud.delete", this::deleteSelected);
        createPlacement = root.add(new UiCheckBox(() -> UiTranslations.format("litematica.gui.label.schematic_load.checkbox.create_placement"),
            () -> placeOnLoad, value -> {
                placeOnLoad = value;
                UiState.set("create_placement_on_load", Boolean.toString(value));
            }));
        createPlacement.setTooltip(UiTranslations.format("litematica.gui.label.schematic_load.hoverinfo.create_placement").split("\n"));
        back = addButton("litematica.gui.button.change_menu.to_main_menu", this::mainMenu);
    }

    private UiButton icon(UiSprite sprite, String tooltip, Runnable action) {
        UiButton button = root.add(new UiButton(() -> "", mouse -> { if (mouse == 0) action.run(); }).setSprite(sprite).setBackground(false));
        if (tooltip != null) button.setTooltip(UiTranslations.format(tooltip));
        return button;
    }

    private UiButton action(String key, Runnable action) {
        UiButton button = addButton(key, action);
        actions.add(button);
        return button;
    }

    private String relative(File file) {
        return mc.mcDataDir.toPath().toAbsolutePath().relativize(file.toPath().toAbsolutePath()).toString().replace('\\', '/');
    }

    @Override
    protected void opened() {
        CloudClient.INSTANCE.refresh();
    }

    private CloudCatalog catalog() { return CloudClient.INSTANCE.catalog(); }

    private CloudNode selection() { return model.selectedIndex() < 0 ? null : model.selected(); }

    private CloudFile selectedFile() {
        CloudNode node = selection();
        return node != null && node.type == CloudNode.Type.SCHEMATIC ? node.file : null;
    }

    private void rebuild() {
        model.setEntries(CloudNode.list(catalog(), directory, search.text()));
        list.setEmptyText(!CloudClient.INSTANCE.available() ? UiTranslations.format("schematica.ui.cloud.unavailable")
            : catalog() == null ? UiTranslations.format("schematica.ui.cloud.loading")
            : catalog().files.isEmpty() && catalog().directories.isEmpty() ? UiTranslations.format(PREFIX + "browser.empty_server")
            : UiTranslations.format(PREFIX + "browser.empty_folder"));
    }

    private void navigate(CloudPath target) {
        directory = target;
        model.setOffset(0);
        search.setText("");
        rebuild();
    }

    private void goUp() {
        if (directory != null) navigate(directory.isRoot() ? null : directory.parent());
    }

    private void activate(CloudNode node) {
        if (node.type == CloudNode.Type.DIRECTORY) navigate(node.path);
        else loadSelected();
    }

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
        CloudNode node = selection();
        if (node == null || node.path.isRoot()) return;
        CloudPath target = node.path;
        confirm(UiTranslations.format("litematica.gui.title.confirm_file_deletion"), UiTranslations.format("schematica.ui.cloud.delete_confirm", target.name()),
            () -> CloudClient.INSTANCE.delete(target));
    }

    private boolean owned(CloudNode node) {
        return node.file == null || mc.thePlayer != null && node.file.owner.equals(mc.thePlayer.getCommandSenderName());
    }

    @Override
    protected boolean handleKey(char character, int keyCode) {
        if (!(input.focused() instanceof UiTextField) && keyCode == Keyboard.KEY_BACK && directory != null) {
            goUp();
            return true;
        }
        if (keyCode == Keyboard.KEY_F5) {
            CloudClient.INSTANCE.refresh();
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
        CloudCatalog catalog = catalog();
        CloudNode node = selection();
        boolean file = selectedFile() != null;
        boolean entry = node != null && !node.path.isRoot();
        up.setEnabled(directory != null);
        createDirectory.setEnabled(mayWrite(directory));
        boolean changed = load.isVisible() != file || delete.isVisible() != entry
            || publish.isVisible() != (file && CloudPath.PRIVATE.equals(node.path.scope));
        load.setVisible(file);
        materials.setVisible(file);
        download.setVisible(file);
        publish.setVisible(file && CloudPath.PRIVATE.equals(node.path.scope));
        delete.setVisible(entry);
        load.setEnabled(SchematicaPlus.proxy.isLoadEnabled);
        upload.setEnabled(catalog != null && catalog.upload && mayWrite(directory));
        publish.setEnabled(catalog != null && catalog.publish);
        delete.setEnabled(entry && catalog != null && (CloudPath.PRIVATE.equals(node.path.scope) || catalog.manage || owned(node)));
        if (changed && back != null) layoutActions();
    }

    private String storage() {
        CloudCatalog catalog = catalog();
        if (catalog == null) return "";
        return UiTranslations.format("schematica.ui.cloud.storage", catalog.quota < 0 ? FileUtils.humanReadableByteCount(catalog.used)
            : FileUtils.humanReadableByteCount(catalog.used) + " / " + FileUtils.humanReadableByteCount(catalog.quota));
    }

    @Override
    protected void layoutWidgets() {
        int x = 12, y = 24, browserWidth = Math.max(24, width - 196), browserHeight = Math.max(0, height - 70);
        frame.setBounds(x, y, browserWidth, browserHeight);
        home.setBounds(x + 2, y + 5, 12, 12);
        up.setBounds(x + 16, y + 5, 12, 12);
        createDirectory.setBounds(x + 30, y + 5, 12, 12);
        openFolder.setBounds(x + 44, y + 5, 12, 12);
        searchButton.setBounds(x + browserWidth - 24, y + 5, 12, 12);
        path.setBounds(x + 62, y + 4, browserWidth - 28 - 62, 14);
        search.setBounds(x + 2, y + 4, browserWidth - 28, 14);
        search.setVisible(searching);
        for (UiWidget widget : new UiWidget[] {path, home, up, createDirectory, openFolder}) widget.setVisible(!searching);
        list.setBounds(x + 2, y + 21, browserWidth - 4, Math.max(0, browserHeight - 25));
        info.setBounds(x + width - 190, y, 170, Math.min(310, browserHeight + 10));
        status.setBounds(info.bounds().x + 3, info.bounds().bottom() - 25, 164, 22);
        createPlacement.setBounds(12, height - 40, Math.min(width - 24, fontRendererObj.getStringWidth(createPlacement.label()) + 20), 11);
        int menuWidth = fontRendererObj.getStringWidth(back.label()) + 20;
        back.setBounds(width - menuWidth - 10, height - 26, menuWidth, 20);
        layoutActions();
    }

    private void layoutActions() {
        int total = 0, count = 0;
        for (UiButton button : actions) {
            if (!button.isVisible()) continue;
            total += button.preferredWidth(fontRendererObj.getStringWidth(button.label())) + 4;
            count++;
        }
        int available = back.bounds().x - 4 - 12;
        double scale = total > available && total > 0 ? Math.max(0, available - 4.0 * count) / (total - 4.0 * count) : 1;
        int x = 12;
        for (UiButton button : actions) {
            if (!button.isVisible()) continue;
            int w = Math.max(20, (int) (button.preferredWidth(fontRendererObj.getStringWidth(button.label())) * scale));
            button.setBounds(x, height - 26, w, 20);
            x += w + 4;
        }
    }

    private void drawInfo(UiDraw draw, int x, int y, int width) {
        CloudNode node = selection();
        if (node == null) return;
        if (node.type == CloudNode.Type.DIRECTORY) {
            draw.text(UiTranslations.format("litematica.gui.label.schematic_info.name"), x, y, CloudInfo.LABEL);
            draw.text(draw.trim(node.path.isRoot() ? scopeName(node.path.scope) : node.name(), width - 12), x + 4, y += CloudInfo.LINE, CloudInfo.VALUE);
            draw.text(UiTranslations.format(PREFIX + "info.dir_schematics", node.count), x, y + CloudInfo.LINE, CloudInfo.LABEL);
            return;
        }
        CloudFile file = node.file;
        y = CloudInfo.schematic(draw, x, y, width, file);
        if (!file.owner.isEmpty()) {
            draw.text(draw.trim(UiTranslations.format(PREFIX + "info.owner") + ": " + file.owner, width - 8), x, y, CloudInfo.LABEL);
            y += CloudInfo.LINE;
        }
        String state = UiTranslations.format(PREFIX + (CloudCache.cached(file) ? "info.status_cached" : "info.status_server"));
        draw.text(draw.trim(UiTranslations.format(PREFIX + "info.file_label", FileUtils.humanReadableByteCount(file.size), state), width - 8), x, y, CloudInfo.LABEL);
    }
}

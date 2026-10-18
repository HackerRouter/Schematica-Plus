package com.github.lunatrius.schematica.client.gui.cloud;

import java.io.File;
import java.io.IOException;

import net.minecraft.client.gui.GuiScreen;

import com.github.lunatrius.schematica.client.cloud.CloudCache;
import com.github.lunatrius.schematica.client.gui.browser.GuiSchematicBrowser;
import com.github.lunatrius.schematica.client.gui.browser.SchematicBrowserModel;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.cloud.CloudPath;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.handler.client.CloudClient;
import com.github.lunatrius.schematica.util.MessageType;

public final class GuiUploadSchematics extends GuiSchematicBrowser {
    private static final String PREFIX = "schematic_synchronizer.gui.";
    private final CloudPath target;
    private UiButton upload;

    public GuiUploadSchematics(GuiScreen parent, CloudPath target) {
        super(parent, UiTranslations.format(PREFIX + "title.upload_schematics"), false);
        this.target = target;
    }

    @Override
    protected SchematicBrowserModel createModel() throws IOException {
        return new SchematicBrowserModel(ConfigurationHandler.schematicDirectory, SchematicBrowserModel::supported);
    }

    @Override
    protected boolean returnsToParent() { return true; }

    @Override
    protected boolean accept(SchematicBrowserModel.Entry entry) {
        return !entry.name().startsWith(".") && !entry.name().equals(CloudCache.DIRECTORY);
    }

    @Override
    protected void createActions() {
        upload = addAction(PREFIX + "button.upload_to_server", this::uploadSelection);
        upload.setTooltip(UiTranslations.format(PREFIX + "button.hover.upload_to_server"));
    }

    @Override
    protected void opened() {
        super.opened();
        setStatus(GuiServerSchematics.display(target));
    }

    @Override
    protected void activateFile(SchematicBrowserModel.Entry entry) {
        uploadSelection();
    }

    private void uploadSelection() {
        SchematicBrowserModel.Entry entry = selection();
        if (entry == null || target == null) return;
        queue(entry.file, target);
        CloudClient.INSTANCE.refresh();
        closeScreen();
    }

    private void queue(File file, CloudPath folder) {
        try {
            if (file.isDirectory()) {
                if (file.getName().startsWith(".")) return;
                CloudPath next = folder.child(file.getName());
                File[] children = file.listFiles();
                if (children != null) for (File child : children) queue(child, next);
            } else if (CloudPath.schematicName(file.getName())) {
                CloudClient.INSTANCE.upload(file, folder.child(file.getName()));
            }
        } catch (IllegalArgumentException e) {
            addMessage(MessageType.ERROR, "schematica.message.cloud.invalid_name", file.getName());
        }
    }

    @Override
    protected void tickScreen() {
        super.tickScreen();
        if (upload != null) upload.setEnabled(target != null && selection() != null);
    }
}

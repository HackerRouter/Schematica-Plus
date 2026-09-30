package com.github.lunatrius.schematica.client.gui.browser;

import java.io.File;
import java.util.function.Consumer;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import com.github.lunatrius.schematica.client.gui.framework.UiButton;

public final class GuiSchematicDirectory extends GuiSchematicBrowser {

    private final File initial;
    private final Consumer<File> chosen;
    private UiButton choose;

    public GuiSchematicDirectory(GuiScreen parent, File initial, Consumer<File> chosen) {
        super(parent, I18n.format("schematica.ui.save.directory_title"), true);
        this.initial = initial;
        this.chosen = chosen;
    }

    @Override
    protected void createActions() {
        choose = addAction("schematica.ui.save.use_directory", () -> {
            if (browser != null && browser.directory().isDirectory()) {
                chosen.accept(browser.directory());
                closeScreen();
            }
        });
    }

    @Override
    protected void opened() {
        super.opened();
        if (initial != null) navigate(initial);
    }

    @Override
    protected void tickScreen() {
        super.tickScreen();
        choose.setEnabled(browser != null && browser.directory().isDirectory());
    }
}

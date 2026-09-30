package com.github.lunatrius.schematica.client.gui;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraft.util.StatCollector;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import cpw.mods.fml.client.CustomModLoadingErrorDisplayException;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class SchematicaPlusConflictException extends CustomModLoadingErrorDisplayException {
    private final String conflict;

    public SchematicaPlusConflictException(String message, String conflict) {
        super(message, null);
        this.conflict = conflict;
    }

    @Override public void initGui(GuiErrorScreen screen, FontRenderer font) {}

    @Override public void drawScreen(GuiErrorScreen screen, FontRenderer font, int mouseX, int mouseY, float partialTicks) {
        String title = StatCollector.canTranslate("schematica.ui.conflict.title")
            ? UiTranslations.format("schematica.ui.conflict.title") : "Schematica Plus: incompatible mods";
        String message = StatCollector.canTranslate("schematica.ui.conflict.message")
            ? UiTranslations.format("schematica.ui.conflict.message", conflict) : getMessage();
        screen.drawCenteredString(font, title, screen.width / 2, 40, 0xffffff);
        font.drawSplitString(message, 20, 75, screen.width - 40, 0xffffff);
    }
}

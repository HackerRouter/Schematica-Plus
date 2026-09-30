package com.github.lunatrius.schematica.client.gui;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiErrorScreen;
import cpw.mods.fml.client.CustomModLoadingErrorDisplayException;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class SchematicaPlusConflictException extends CustomModLoadingErrorDisplayException {
    public SchematicaPlusConflictException(String message) {
        super(message, null);
    }

    @Override public void initGui(GuiErrorScreen screen, FontRenderer font) {}

    @Override public void drawScreen(GuiErrorScreen screen, FontRenderer font, int mouseX, int mouseY, float partialTicks) {
        screen.drawCenteredString(font, "Schematica Plus: incompatible mods", screen.width / 2, 40, 0xffffff);
        font.drawSplitString(getMessage(), 20, 75, screen.width - 40, 0xffffff);
    }
}

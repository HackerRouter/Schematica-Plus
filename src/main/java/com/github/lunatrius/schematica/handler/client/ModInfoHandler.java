package com.github.lunatrius.schematica.handler.client;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiOpenEvent;
import com.github.lunatrius.schematica.reference.Reference;
import cpw.mods.fml.client.GuiModList;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;

public final class ModInfoHandler {
    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.gui != null && event.gui.getClass() == GuiModList.class) {
            GuiScreen parent = ReflectionHelper.getPrivateValue(GuiModList.class, (GuiModList) event.gui, "mainMenu");
            event.gui = new WrappedModList(parent);
        }
    }

    private static final class WrappedModList extends GuiModList {
        private WrappedModList(GuiScreen parent) {
            super(parent);
        }

        @Override public int drawLine(String line, int offset, int y) {
            ModContainer selected = ReflectionHelper.getPrivateValue(GuiModList.class, this, "selectedMod");
            int availableWidth = width - offset - 20;
            if (selected == null || !Reference.MODID.equals(selected.getModId()) || availableWidth <= 20) {
                return super.drawLine(line, offset, y);
            }
            for (String wrapped : fontRendererObj.listFormattedStringToWidth(line, availableWidth)) {
                fontRendererObj.drawString(wrapped, offset, y, 0xd7edea);
                y += 10;
            }
            return y;
        }
    }
}

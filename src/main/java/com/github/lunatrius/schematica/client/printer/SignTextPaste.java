// SPDX-License-Identifier: LGPL-3.0-only
// Litematica sign text paste (MixinAbstractSignEditScreen), adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.printer;

import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraftforge.client.event.GuiOpenEvent;

import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Reference;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;

/** Fills a newly opened sign editor with the text of the schematic sign at the same position. */
public final class SignTextPaste {
    public static final SignTextPaste INSTANCE = new SignTextPaste();

    private SignTextPaste() {}

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (!ConfigurationHandler.signTextPaste || !(event.gui instanceof GuiEditSign)) return;
        try {
            TileEntitySign sign = ReflectionHelper.getPrivateValue(GuiEditSign.class, (GuiEditSign) event.gui, "tileSign", "field_146848_f");
            if (sign == null) return;
            String[] text = schematicText(sign.xCoord, sign.yCoord, sign.zCoord);
            if (text == null) return;
            for (int i = 0; i < sign.signText.length && i < text.length; i++) {
                String line = text[i] == null ? "" : text[i];
                sign.signText[i] = line.length() > 15 ? line.substring(0, 15) : line;
            }
        } catch (RuntimeException error) {
            Reference.logger.warn("Could not paste the schematic sign text", error);
        }
    }

    private static String[] schematicText(int x, int y, int z) {
        for (SchematicWorld world : ClientProxy.visiblePlacements()) {
            if (!world.isEnabled()) continue;
            int lx = x - world.position.x, ly = y - world.position.y, lz = z - world.position.z;
            if (!world.getSchematic().containsBlock(lx, ly, lz)) continue;
            TileEntity tile = world.getTileEntity(lx, ly, lz);
            if (tile instanceof TileEntitySign) return ((TileEntitySign) tile).signText;
        }
        return null;
    }
}

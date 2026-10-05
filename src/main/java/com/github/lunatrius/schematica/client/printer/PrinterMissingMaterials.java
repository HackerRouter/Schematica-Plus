// Printer missing material HUD after the behavior of litematica-printer: items the printer needed but did not find, by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.printer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

import com.github.lunatrius.schematica.client.gui.framework.MinecraftUiDraw;
import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.handler.InfoHudSettings;
import com.github.lunatrius.schematica.reference.Reference;

/**
 * The items the printer wanted to place but the player does not have, listed under the info HUD while printing; an
 * item leaves the list a few seconds after the printer last missed it.
 */
public final class PrinterMissingMaterials {
    private static final long KEEP_MILLIS = 3000;
    private static final int MAX_LINES = 10, LINE_HEIGHT = 16;
    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

    private static final class Entry {
        final ItemStack stack;
        long lastSeen;
        Entry(ItemStack stack) { this.stack = stack; }
    }

    private PrinterMissingMaterials() {}

    public static synchronized void record(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return;
        String key = net.minecraft.item.Item.getIdFromItem(stack.getItem()) + ":" + stack.getItemDamage();
        Entry entry = ENTRIES.get(key);
        if (entry == null) {
            ItemStack copy = stack.copy();
            copy.stackSize = 1;
            ENTRIES.put(key, entry = new Entry(copy));
        }
        entry.lastSeen = System.currentTimeMillis();
    }

    public static synchronized void clear() { ENTRIES.clear(); }

    private static synchronized List<ItemStack> current() {
        long now = System.currentTimeMillis();
        List<ItemStack> stacks = new ArrayList<>();
        for (Iterator<Entry> it = ENTRIES.values().iterator(); it.hasNext();) {
            Entry entry = it.next();
            if (now - entry.lastSeen > KEEP_MILLIS) it.remove();
            else stacks.add(entry.stack);
        }
        return stacks;
    }

    /** Draws the list with its top (or bottom) offset by the height of the HUDs above it; returns the height used. */
    public static int render(Minecraft mc, int offsetHeight) {
        if (!ConfigurationHandler.printMissingMaterialHud) return 0;
        List<ItemStack> missing = current();
        if (missing.isEmpty()) return 0;
        int shown = Math.min(missing.size(), MAX_LINES);
        boolean overflow = missing.size() > shown;
        double scale = InfoHudSettings.scale;
        int bgMargin = 2;
        int contentHeight = (shown + 1 + (overflow ? 1 : 0)) * LINE_HEIGHT;
        ScaledResolution screen = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        try (MinecraftUiDraw draw = new MinecraftUiDraw(mc)) {
            String title = UiTranslations.format("schematica.printer.missing.title", missing.size());
            String[] names = new String[shown];
            int maxText = draw.textWidth(title);
            for (int i = 0; i < shown; i++) {
                String name;
                try { name = missing.get(i).getDisplayName(); }
                catch (RuntimeException error) { name = String.valueOf(missing.get(i).getItem()); }
                names[i] = name;
                maxText = Math.max(maxText, draw.textWidth(name) + 18);
            }
            int lineLength = maxText + 4;
            int offsetY = (int) ((InfoHudSettings.offsetY + offsetHeight) / scale);
            int x = InfoHudSettings.alignment.x(screen.getScaledWidth(), lineLength, scale, InfoHudSettings.offsetX) + bgMargin;
            int y = InfoHudSettings.alignment.y(screen.getScaledHeight(), contentHeight, scale, offsetY) + bgMargin;
            GL11.glScalef((float) scale, (float) scale, 1);
            draw.fill(new UiBounds(x - bgMargin, y - bgMargin, lineLength + bgMargin * 2, contentHeight + bgMargin), 0xA0000000);
            draw.text(title, x + 2, y + 2, 0xFFFFFFFF);
            int rowY = y + LINE_HEIGHT;
            for (int i = 0; i < shown; i++, rowY += LINE_HEIGHT) {
                try { draw.item(missing.get(i), x, rowY); }
                catch (RuntimeException error) { Reference.logger.debug("Could not render a missing material icon", error); }
                draw.text(names[i], x + 18, rowY + 4, 0xFFFFFFFF);
            }
            if (overflow) draw.text(UiTranslations.format("schematica.printer.missing.overflow", missing.size() - shown), x + 2, rowY + 4, 0xFFAAAAAA);
        }
        return (int) Math.ceil((contentHeight + 4) * scale);
    }
}

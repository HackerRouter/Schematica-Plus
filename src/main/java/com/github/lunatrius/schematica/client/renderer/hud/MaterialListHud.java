// SPDX-License-Identifier: LGPL-3.0-only
// Litematica MaterialListHudRenderer, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.renderer.hud;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;

import org.lwjgl.opengl.GL11;

import com.github.lunatrius.schematica.client.gui.framework.MinecraftUiDraw;
import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.gui.material.MaterialCache;
import com.github.lunatrius.schematica.client.gui.material.MaterialItemKey;
import com.github.lunatrius.schematica.client.gui.material.MaterialList;
import com.github.lunatrius.schematica.client.gui.material.MaterialListModel;
import com.github.lunatrius.schematica.client.gui.material.MaterialListModel.Entry;
import com.github.lunatrius.schematica.client.gui.material.MaterialLists;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.handler.InfoHudSettings;
import com.github.lunatrius.schematica.handler.RenderColors;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Reference;

import cpw.mods.fml.relauncher.ReflectionHelper;

/** The missing items of the last viewed material list, drawn below the info HUD text, and the inventory highlight. */
public final class MaterialListHud {
    private static List<Entry<MaterialItemKey>> list = Collections.emptyList();
    private static MaterialList shown;
    private static long lastUpdate;
    private static Block lastBlock;
    private static int lastMeta = -1;
    private static MaterialCache.BuildItems lastItems;
    private static Field guiLeft, guiTop;

    private MaterialListHud() {}

    private static MaterialList active(Minecraft mc) {
        MaterialList current = MaterialLists.current();
        return current != null && current.hud() && mc.thePlayer != null && current.validContext() ? current : null;
    }

    /** Draws the HUD with its top (or bottom) offset by the height of the info HUD text above it; returns the height used. */
    public static int render(Minecraft mc, int infoHeight, boolean inGui) {
        if (inGui && !ConfigurationHandler.renderMaterialListInGuis) return 0;
        MaterialList materials = active(mc);
        if (materials == null) { shown = null; return 0; }
        MaterialListModel<MaterialItemKey> model = materials.model();
        long now = System.currentTimeMillis();
        if (shown != materials || now - lastUpdate > 2000) {
            list = model.missingOnly();
            shown = materials;
            lastUpdate = now;
        }
        if (list.isEmpty()) return 0;
        double scale = ConfigurationHandler.materialListHudScale;
        int size = Math.min(list.size(), ConfigurationHandler.materialListHudMaxLines);
        int bgMargin = 2, lineHeight = 16;
        int contentHeight = size * lineHeight + bgMargin + 10;
        ScaledResolution screen = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        try (MinecraftUiDraw draw = new MinecraftUiDraw(mc)) {
            String[] names = new String[size], counts = new String[size];
            int maxText = 0, maxCount = 0;
            for (int i = 0; i < size; i++) {
                Entry<MaterialItemKey> entry = list.get(i);
                names[i] = entry.name;
                counts[i] = countText(model.hudCount(entry), entry.key.stack().getMaxStackSize());
                maxText = Math.max(maxText, draw.textWidth(names[i]));
                maxCount = Math.max(maxCount, draw.textWidth(counts[i]));
            }
            int lineLength = maxText + maxCount + 30;
            int offsetY = (int) ((InfoHudSettings.offsetY + infoHeight) / scale);
            int x = InfoHudSettings.alignment.x(screen.getScaledWidth(), lineLength, scale, InfoHudSettings.offsetX) + bgMargin;
            int y = InfoHudSettings.alignment.y(screen.getScaledHeight(), contentHeight, scale, offsetY) + bgMargin;
            GL11.glScalef((float) scale, (float) scale, 1);
            draw.fill(new UiBounds(x - bgMargin, y - bgMargin, lineLength + bgMargin * 2, contentHeight + bgMargin), 0xA0000000);
            draw.text("§l" + UiTranslations.format("litematica.gui.button.material_list") + "§r", x + 2, y + 2, 0xFFFFFFFF);
            int countColor = RenderColors.MATERIAL_HUD.color();
            for (int i = 0; i < size; i++) {
                int rowY = y + 12 + i * lineHeight;
                try { draw.item(list.get(i).key.stack(), x, rowY); }
                catch (RuntimeException error) { Reference.logger.debug("Could not render a material HUD icon", error); }
                draw.text(names[i], x + 18, rowY + 4, 0xFFFFFFFF);
                draw.text(counts[i], x + lineLength - draw.textWidth(counts[i]) - 2, rowY + 4, countColor);
            }
        }
        return (int) Math.ceil((contentHeight + 4) * scale);
    }

    static String countText(long count, int maxStackSize) {
        int stack = Math.max(1, maxStackSize);
        long stacks = count / stack, remainder = count % stack;
        double boxes = (double) count / (27.0 * stack);
        if (count <= stack) return Long.toString(count);
        if (boxes >= 1.0) return String.format(Locale.ROOT, "%d (%.2f %s)", count, boxes,
            UiTranslations.format("litematica.gui.label.material_list.abbr.shulker_box"));
        if (remainder > 0) return String.format(Locale.ROOT, "%d (%d x %d + %d)", count, stacks, stack, remainder);
        return String.format(Locale.ROOT, "%d (%d x %d)", count, stacks, stack);
    }

    /** highlightBlockInInventory: outlines the slots holding the item of the looked at schematic block. */
    public static void highlightInventory(Minecraft mc, GuiContainer gui) {
        if (!ConfigurationHandler.highlightBlockInInventory) return;
        MaterialCache.BuildItems items = lookedAtItems(mc);
        if (items == null || items.isEmpty()) return;
        int left, top;
        try {
            if (guiLeft == null) {
                guiLeft = ReflectionHelper.findField(GuiContainer.class, "guiLeft", "field_147003_i");
                guiTop = ReflectionHelper.findField(GuiContainer.class, "guiTop", "field_147009_r");
            }
            left = guiLeft.getInt(gui);
            top = guiTop.getInt(gui);
        } catch (ReflectiveOperationException | RuntimeException error) {
            return;
        }
        int color = RenderColors.INVENTORY.color();
        try (MinecraftUiDraw draw = new MinecraftUiDraw(mc)) {
            GL11.glTranslatef(0, 0, 300);
            for (Object object : gui.inventorySlots.inventorySlots) {
                Slot slot = (Slot) object;
                ItemStack stack = slot.getStack();
                if (stack == null || stack.getItem() == null || !matches(stack, items)) continue;
                UiBounds box = new UiBounds(left + slot.xDisplayPosition, top + slot.yDisplayPosition, 16, 16);
                draw.fill(box.inset(1), color);
                draw.border(box, color | 0xFF000000);
            }
        }
    }

    /** areStacksEqualIgnoreNbt: the item and damage. */
    private static boolean matches(ItemStack stack, MaterialCache.BuildItems items) {
        for (int i = 0; i < items.size(); i++) {
            ItemStack wanted = items.key(i).stack();
            if (wanted.getItem() == stack.getItem() && wanted.getItemDamage() == stack.getItemDamage()) return true;
        }
        return false;
    }

    private static MaterialCache.BuildItems lookedAtItems(Minecraft mc) {
        SchematicWorld schematic = ClientProxy.schematic;
        MovingObjectPosition hit = ClientProxy.movingObjectPosition;
        if (schematic == null || hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || mc.renderViewEntity == null) return null;
        MovingObjectPosition vanilla = mc.objectMouseOver;
        if (hit.hitVec != null && vanilla != null && vanilla.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && vanilla.hitVec != null) {
            net.minecraft.util.Vec3 eye = mc.renderViewEntity.getPosition(1.0f);
            double schematicDistance = eye.squareDistanceTo(hit.hitVec.addVector(schematic.position.x, schematic.position.y, schematic.position.z));
            if (eye.squareDistanceTo(vanilla.hitVec) < schematicDistance) return null;
        }
        Block block = schematic.getBlock(hit.blockX, hit.blockY, hit.blockZ);
        int meta = schematic.getBlockMetadata(hit.blockX, hit.blockY, hit.blockZ);
        if (block == null || block.isAir(schematic, hit.blockX, hit.blockY, hit.blockZ)) return null;
        if (block != lastBlock || meta != lastMeta || block.hasTileEntity(meta)) {
            try { lastItems = MaterialCache.INSTANCE.items(schematic, hit.blockX, hit.blockY, hit.blockZ, block, meta, mc.thePlayer); }
            catch (RuntimeException error) { lastItems = null; }
            lastBlock = block;
            lastMeta = meta;
        }
        return lastItems;
    }
}

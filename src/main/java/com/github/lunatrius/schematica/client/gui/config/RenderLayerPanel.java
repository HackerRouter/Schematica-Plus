// SPDX-License-Identifier: LGPL-3.0-only
// Litematica GuiRenderLayer and MaLiLib layer editor, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;

import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiCheckBox;
import com.github.lunatrius.schematica.client.gui.framework.UiIntegerField;
import com.github.lunatrius.schematica.client.gui.framework.UiLabel;
import com.github.lunatrius.schematica.client.gui.framework.UiPanel;
import com.github.lunatrius.schematica.client.gui.framework.UiSprite;
import com.github.lunatrius.schematica.client.world.RenderLayerRange;

public final class RenderLayerPanel extends UiPanel {
    private static final String BUTTON = "malilib.gui.button.render_layers_gui.";
    private static final String LABEL = "malilib.gui.label.render_layers.";
    private final FontRenderer font;
    private final RenderLayerRange range;
    private final Runnable commitFocus;
    private final UiButton mode, axis, here, adjustMin, adjustMax;
    private final UiLabel labelMin, labelMax;
    private final UiIntegerField minimum, maximum;
    private final UiCheckBox hotkeyMin, hotkeyMax;
    private boolean syncing;
    private long revision = -1;

    public RenderLayerPanel(FontRenderer font, RenderLayerRange range, Runnable commitFocus) {
        this.font = font;
        this.range = range;
        this.commitFocus = commitFocus;
        mode = add(new UiButton(() -> UiTranslations.format(BUTTON + "layers", UiTranslations.format(range.mode().translationKey())), button -> {
            commitFocus.run();
            RenderLayerRange.Mode[] modes = RenderLayerRange.Mode.values();
            range.setMode(modes[Math.floorMod(range.mode().ordinal() + (button == 0 ? 1 : -1), modes.length)]);
            sync(); layout(bounds());
        }));
        mode.setTooltip(tooltip("schematica.ui.layers.scope"));
        axis = add(new UiButton(() -> UiTranslations.format(BUTTON + "axis", range.axis().name()), button -> {
            commitFocus.run();
            RenderLayerRange.Axis[] axes = RenderLayerRange.Axis.values();
            range.setAxis(axes[Math.floorMod(range.axis().ordinal() + (button == 0 ? 1 : -1), axes.length)]);
            sync(); layout(bounds());
        }));
        labelMin = add(new UiLabel(this::lowerLabel));
        labelMax = add(new UiLabel(() -> UiTranslations.format(LABEL + "layer_max") + ":"));
        minimum = add(new UiIntegerField(font, 0, Integer.MIN_VALUE, Integer.MAX_VALUE, value -> change(false, value)));
        maximum = add(new UiIntegerField(font, 0, Integer.MIN_VALUE, Integer.MAX_VALUE, value -> change(true, value)));
        adjustMin = adjust(false);
        adjustMax = adjust(true);
        hotkeyMin = hotkey(false);
        hotkeyMax = hotkey(true);
        here = add(new UiButton(() -> UiTranslations.format(BUTTON + "set_here"), button -> {
            if (button != 0) return;
            commitFocus.run();
            Entity camera = Minecraft.getMinecraft().renderViewEntity;
            if (camera != null) {
                double coordinate = range.axis() == RenderLayerRange.Axis.X ? camera.posX
                    : range.axis() == RenderLayerRange.Axis.Y ? camera.posY - camera.yOffset : camera.posZ;
                range.setHere(MathHelper.floor_double(coordinate));
                sync();
            }
        }));
        sync();
    }

    private static String[] tooltip(String key) { return UiTranslations.format(key).replace("\\n", "\n").split("\n"); }

    private void change(boolean upper, int value) {
        if (!syncing) { range.setValue(upper, value); sync(); }
    }

    private UiButton adjust(boolean upper) {
        UiButton button = add(new UiButton(() -> "", mouse -> {
            commitFocus.run();
            int amount = (mouse == 0 ? 1 : -1) * (GuiScreen.isShiftKeyDown() ? 16 : 1)
                * (GuiScreen.isCtrlKeyDown() ? 64 : 1);
            range.setValue(upper, (long) range.value(upper) + amount);
            sync();
        }).setSprite(UiSprite.PLUS_MINUS).setBackground(false));
        return button;
    }

    private UiCheckBox hotkey(boolean upper) {
        UiCheckBox box = add(new UiCheckBox(() -> UiTranslations.format("litematica.gui.label.render_layers.hotkey"),
            upper ? range::moveMax : range::moveMin, upper ? range::setMoveMax : range::setMoveMin));
        box.setTooltip(tooltip("litematica.gui.label.render_layers.hover.hotkey"));
        return box;
    }

    private String lowerLabel() {
        return UiTranslations.format(LABEL + (range.mode() == RenderLayerRange.Mode.LAYER_RANGE ? "layer_min" : "layer")) + ":";
    }

    private void sync() {
        syncing = true;
        try { minimum.setValue(range.value(false)); maximum.setValue(range.value(true)); }
        finally { syncing = false; }
        revision = range.revision();
    }

    @Override public void tick() {
        if (revision != range.revision()) {
            syncing = true;
            try { commitFocus.run(); } finally { syncing = false; }
            sync(); layout(bounds());
        }
        here.setEnabled(Minecraft.getMinecraft().renderViewEntity != null);
        super.tick();
    }

    @Override public void layout(UiBounds screen) {
        boolean active = range.mode() != RenderLayerRange.Mode.ALL;
        boolean interval = range.mode() == RenderLayerRange.Mode.LAYER_RANGE;
        int x = bounds().x;
        int y = bounds().y;
        int modeWidth = font.getStringWidth(mode.label()) + 10;
        mode.setBounds(x, y, modeWidth, 20);
        axis.setVisible(active);
        axis.setBounds(x + modeWidth + 2, y, font.getStringWidth(axis.label()) + 10, 20);
        y += 26;
        int labelWidth = font.getStringWidth(lowerLabel());
        if (interval) labelWidth = Math.max(labelWidth, font.getStringWidth(UiTranslations.format(LABEL + "layer_max") + ":"));
        int fieldX = x + labelWidth + 10;
        int lowerY = y + (interval ? 23 : 0);
        labelMax.setVisible(interval); maximum.setVisible(interval); adjustMax.setVisible(interval);
        hotkeyMax.setVisible(interval); hotkeyMin.setVisible(interval);
        labelMin.setVisible(active); minimum.setVisible(active); adjustMin.setVisible(active); here.setVisible(active);
        labelMax.setBounds(x, y, labelWidth, 20);
        labelMin.setBounds(x, lowerY, labelWidth, 20);
        maximum.setBounds(fieldX, y, 60, 20);
        minimum.setBounds(fieldX, lowerY, 60, 20);
        adjustMax.setBounds(fieldX + 63, y + 2, 16, 16);
        adjustMin.setBounds(fieldX + 63, lowerY + 2, 16, 16);
        int checkboxWidth = Math.max(0, Math.min(font.getStringWidth(hotkeyMax.label()) + 15,
            bounds().x + bounds().width - fieldX - 84));
        hotkeyMax.setBounds(fieldX + 84, y + 4, checkboxWidth, 11);
        hotkeyMin.setBounds(fieldX + 84, lowerY + 4, checkboxWidth, 11);
        here.setBounds(fieldX - 1, lowerY + 23, font.getStringWidth(here.label()) + 10, 20);
    }
}

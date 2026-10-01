// SPDX-License-Identifier: LGPL-3.0-only
// Litematica PlacementGridSettingsScreen (maruohon, liteloader_1.12.2), adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.placement;

import java.util.function.IntConsumer;

import net.minecraft.client.gui.GuiScreen;

import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiIntegerField;
import com.github.lunatrius.schematica.client.gui.framework.UiLabel;
import com.github.lunatrius.schematica.client.gui.framework.UiScreen;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.world.GridSettings;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.client.WorldHandler;
import com.github.lunatrius.schematica.proxy.ClientProxy;

/** Grid size and repeat counts of a placement; the copies follow on the next client tick. */
public final class GuiPlacementGridSettings extends UiScreen {
    private static final String[] AXES = {"x", "y", "z"};
    private final SchematicWorld placement;
    private final UiIntegerField[] size = new UiIntegerField[3], negative = new UiIntegerField[3], positive = new UiIntegerField[3];
    private final UiLabel[] sizeLabels = new UiLabel[3], negativeLabels = new UiLabel[3], positiveLabels = new UiLabel[3];
    private UiButton toggle, reset, back;
    private UiLabel sizeLabel, repeatLabel;
    private boolean syncing;

    public GuiPlacementGridSettings(GuiScreen parent, SchematicWorld placement) {
        super(parent, UiTranslations.format("schematica.ui.placement.grid.title", placement.name));
        this.placement = placement;
        int[] box = placement.enclosingBox();
        placement.grid.setDefaultSize(new int[] {box[3] - box[0] + 1, box[4] - box[1] + 1, box[5] - box[2] + 1});
    }

    static String toggleLabel(SchematicWorld placement) {
        boolean on = placement.grid.isEnabled();
        return UiTranslations.format("schematica.ui.placement.grid_settings",
            (on ? "§a" : "§c") + UiTranslations.format(on ? "options.on" : "options.off") + "§r");
    }

    private boolean available() {
        return mc.theWorld != null && ClientProxy.loadedSchematics.contains(placement);
    }

    private UiIntegerField field(int value, int minimum, int maximum, IntConsumer changed) {
        return root.add(new UiIntegerField(fontRendererObj, value, minimum, maximum, next -> {
            if (syncing || !available()) return;
            changed.accept(next);
            WorldHandler.INSTANCE.saveSession();
        }));
    }

    @Override
    protected void createWidgets() {
        GridSettings grid = placement.grid;
        toggle = root.add(new UiButton(() -> toggleLabel(placement), mouse -> {
            if (mouse != 0 || !available()) return;
            grid.toggleEnabled();
            WorldHandler.INSTANCE.saveSession();
        }));
        reset = addButton("schematica.ui.placement.grid.reset_size", () -> {
            if (!available()) return;
            grid.resetSize();
            sync();
            WorldHandler.INSTANCE.saveSession();
        });
        sizeLabel = root.add(new UiLabel(() -> UiTranslations.format("schematica.ui.placement.grid.size"), 0xFFFFFFFF));
        repeatLabel = root.add(new UiLabel(() -> UiTranslations.format("schematica.ui.placement.grid.repeat_count"), 0xFFFFFFFF));
        int[] defaults = grid.defaultSize();
        for (int i = 0; i < 3; i++) {
            final int axis = i;
            size[i] = field(grid.size()[i], Math.max(1, defaults[i]), GridSettings.MAX_SIZE, value -> grid.setSize(axis, value));
            negative[i] = field(grid.repeatNegative()[i], 0, GridSettings.MAX_REPEAT, value -> {
                int[] next = grid.repeatNegative();
                next[axis] = value;
                grid.setRepeatNegative(next);
            });
            positive[i] = field(grid.repeatPositive()[i], 0, GridSettings.MAX_REPEAT, value -> {
                int[] next = grid.repeatPositive();
                next[axis] = value;
                grid.setRepeatPositive(next);
            });
            sizeLabels[i] = root.add(new UiLabel(() -> AXES[axis], 0xFFFFFFFF));
            negativeLabels[i] = root.add(new UiLabel(() -> "-" + AXES[axis], 0xFFFFFFFF));
            positiveLabels[i] = root.add(new UiLabel(() -> "+" + AXES[axis], 0xFFFFFFFF));
        }
        back = addButton("gui.back", this::closeScreen);
    }

    private void sync() {
        syncing = true;
        try {
            for (int i = 0; i < 3; i++) {
                size[i].setValue(placement.grid.size()[i]);
                negative[i].setValue(placement.grid.repeatNegative()[i]);
                positive[i].setValue(placement.grid.repeatPositive()[i]);
            }
        } finally {
            syncing = false;
        }
    }

    @Override
    protected void tickScreen() {
        boolean enabled = available();
        toggle.setEnabled(enabled);
        reset.setEnabled(enabled);
        for (int i = 0; i < 3; i++) {
            size[i].setEnabled(enabled);
            negative[i].setEnabled(enabled);
            positive[i].setEnabled(enabled);
        }
    }

    @Override
    protected void layoutWidgets() {
        int x = Math.max(10, width / 2 - 170), y = Math.max(24, height / 2 - 90);
        int toggleWidth = Math.max(fontRendererObj.getStringWidth(toggleLabel(placement)) + 10, 110);
        toggle.setBounds(x, y, toggleWidth, 20);
        reset.setBounds(x + toggleWidth + 8, y, fontRendererObj.getStringWidth(reset.label()) + 10, 20);
        sizeLabel.setBounds(x, y + 30, 200, 12);
        for (int i = 0; i < 3; i++) {
            sizeLabels[i].setBounds(x + 4, y + 47 + i * 18, 10, 12);
            size[i].setBounds(x + 16, y + 44 + i * 18, 60, 14);
        }
        repeatLabel.setBounds(x, y + 104, 200, 12);
        for (int i = 0; i < 3; i++) {
            int rowY = y + 118 + i * 18;
            negativeLabels[i].setBounds(x, rowY + 3, 14, 12);
            negative[i].setBounds(x + 16, rowY, 60, 14);
            positiveLabels[i].setBounds(x + 96, rowY + 3, 14, 12);
            positive[i].setBounds(x + 112, rowY, 60, 14);
        }
        back.setBounds(x, Math.min(height - 26, y + 178), fontRendererObj.getStringWidth(back.label()) + 20, 20);
    }
}

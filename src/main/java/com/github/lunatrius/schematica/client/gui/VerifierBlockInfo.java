// SPDX-License-Identifier: LGPL-3.0-only
// Litematica block comparison panel, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import cpw.mods.fml.common.registry.GameData;

import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.client.gui.framework.UiDraw;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.verifier.VerificationScan.Pair;
import com.github.lunatrius.schematica.client.verifier.VerificationScan.State;
import com.github.lunatrius.schematica.reference.Reference;

public final class VerifierBlockInfo {
    public static final int HEIGHT = 71;
    private final Pair pair;
    private final State single;
    private final String title;
    private final Visual expected, found;

    public VerifierBlockInfo(Pair pair) {
        this.pair = pair; single = null; title = null;
        expected = new Visual(pair.expected); found = new Visual(pair.found);
    }

    /** A single block state panel with the given title, as Litematica's BlockInfo. */
    public VerifierBlockInfo(State state, String titleKey) {
        pair = null; single = state; title = titleKey;
        expected = new Visual(state); found = null;
    }

    private int columnWidth(UiDraw draw, Visual visual, State state, String label) {
        return Math.max(draw.textWidth(label), Math.max(draw.textWidth(visual.name) + 24, Math.max(draw.textWidth(state.block), draw.textWidth(metadata(state)))));
    }

    private String metadata(State state) { return metadata(state.metadata, " = "); }

    /** MaLiLib's integer block state property line, with 1.7.10 metadata as the only property. */
    public static String metadata(int metadata, String separator) {
        return UiTranslations.format("malilib.label.block_state_properties.integer", "metadata", separator, metadata);
    }
    private String expectedLabel() { return "§l" + UiTranslations.format("litematica.gui.label.schematic_verifier.expected"); }
    private String foundLabel() { return "§l" + UiTranslations.format("litematica.gui.label.schematic_verifier.found"); }
    private String singleLabel() { return "§l" + UiTranslations.format(title); }
    public int width(UiDraw draw, int max) {
        if (single != null) return Math.min(max, columnWidth(draw, expected, single, singleLabel()) + 20);
        return Math.min(max, columnWidth(draw, expected, pair.expected, expectedLabel()) + columnWidth(draw, found, pair.found, foundLabel()) + 40);
    }

    public void draw(UiDraw draw, int x, int y, int width) {
        UiBounds bounds = new UiBounds(x, y, width, HEIGHT);
        draw.fill(bounds, 0xFF000000);
        draw.border(bounds, 0xFF999999);
        if (single != null) {
            column(draw, expected, single, singleLabel(), x + 10, y, Math.max(0, width - 20));
            return;
        }
        int usable = Math.max(0, width - 40);
        int left = columnWidth(draw, expected, pair.expected, expectedLabel());
        if (left + columnWidth(draw, found, pair.found, foundLabel()) > usable) left = usable / 2;
        column(draw, expected, pair.expected, expectedLabel(), x + 10, y, left);
        column(draw, found, pair.found, foundLabel(), x + left + 30, y, usable - left);
    }

    private void column(UiDraw draw, Visual visual, State state, String label, int x, int y, int width) {
        try (UiDraw.Clip ignored = draw.clip(new UiBounds(x, y + 1, width, HEIGHT - 2))) {
            draw.flatText(draw.trim(label, width), x, y + 4, 0xFFFFFFFF);
            visual.draw(draw, x, y + 13, width);
            draw.flatText(draw.trim(state.block, width), x, y + 36, 0xFF4060FF);
            draw.text(draw.trim(metadata(state), width), x, y + 49, 0xFFB0B0B0);
        }
    }

    public static final class Visual {
        public final String name;
        private ItemStack icon;
        public Visual(State state) {
            String label = state.block;
            try {
                Block block = GameData.getBlockRegistry().getObject(state.block);
                if (block != null) {
                    if (!state.air()) icon = new ItemStack(block, 1, state.metadata);
                    label = icon == null || icon.getItem() == null ? block.getLocalizedName() : icon.getDisplayName();
                    if (label == null || label.endsWith(".name")) label = state.block;
                }
            } catch (RuntimeException error) { icon = null; }
            name = label;
        }
        public void draw(UiDraw draw, int x, int y, int width) {
            if (width < 20) return;
            draw.fill(new UiBounds(x, y + 3, 16, 16), 0x20FFFFFF);
            if (icon != null) try { draw.item(icon, x, y + 3); }
            catch (RuntimeException error) { icon = null; Reference.logger.debug("Could not draw verifier item", error); }
            draw.flatText(draw.trim(name, Math.max(0, width - 24)), x + 20, y + 7, 0xFFFFFFFF);
        }
    }
}

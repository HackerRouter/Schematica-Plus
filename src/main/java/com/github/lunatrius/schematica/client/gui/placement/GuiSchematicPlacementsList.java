// SPDX-License-Identifier: LGPL-3.0-only
// Litematica placement list layout, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.placement;

import java.util.ArrayList;
import java.util.List;
import org.lwjgl.input.Keyboard;
import net.minecraft.client.gui.GuiScreen;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiDraw;
import com.github.lunatrius.schematica.client.gui.framework.UiPanel;
import com.github.lunatrius.schematica.client.gui.framework.UiSprite;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.client.WorldHandler;
import com.github.lunatrius.schematica.proxy.ClientProxy;

public final class GuiSchematicPlacementsList extends GuiSchematicList<SchematicWorld> {
    public GuiSchematicPlacementsList(GuiScreen parent) {
        super(parent, true, world -> world.name + " " + world.sourceFilename);
    }

    @Override protected List<SchematicWorld> entries() { return ClientProxy.loadedSchematics; }
    @Override protected UiPanel row(SchematicWorld world, int index) { return new Entry(world, index); }

    private final class Entry extends UiPanel {
        private final SchematicWorld world;
        private final int index;
        private final List<UiButton> buttons = new ArrayList<>();
        private int buttonsStart;

        Entry(SchematicWorld world, int index) {
            this.world = world;
            this.index = index;
            button("litematica.gui.button.schematic_placements.remove", () -> ClientProxy.removePlacement(world));
            UiButton toggle = add(new UiButton(() -> UiTranslations.format("litematica.gui.button.schematic_placements.placement_enabled",
                (world.isRendering ? "\u00a7a" : "\u00a7c") + UiTranslations.format(world.isRendering ? "options.on" : "options.off")),
                mouse -> { if (mouse == 0 && ClientProxy.loadedSchematics.contains(world)) { world.isRendering = !world.isRendering; WorldHandler.INSTANCE.saveSession(); } }));
            buttons.add(toggle);
            button("litematica.gui.button.schematic_placements.configure", () -> {
                mc.displayGuiScreen(new GuiPlacementConfiguration(GuiSchematicPlacementsList.this, world));
            });
            setTooltip(world.name, world.sourceFilename == null ? "" : world.sourceFilename,
                UiTranslations.format("schematica.ui.source.placement_hint"));
        }

        private UiButton button(String key, Runnable action) {
            UiButton button = add(new UiButton(() -> UiTranslations.format(key), mouse -> { if (mouse == 0 && ClientProxy.loadedSchematics.contains(world)) action.run(); }));
            buttons.add(button);
            return button;
        }

        @Override
        public void layout(UiBounds screen) {
            int right = bounds().right() - 2;
            for (int i = 0; i < buttons.size(); i++) {
                UiButton button = buttons.get(i);
                int width = fontRendererObj.getStringWidth(button.label()) + 10;
                button.setBounds(right - width, bounds().y + 1, width, 20);
                right -= width + (i != 1 ? 1 : 2);
            }
            buttonsStart = right;
        }

        @Override
        public void draw(UiDraw draw, int mouseX, int mouseY) {
            boolean selected = ClientProxy.schematic == world;
            boolean hover = containsVisible(mouseX, mouseY);
            int color = selected || hover ? 0xA0707070 : index % 2 == 1 ? 0xA0101010 : 0xA0303030;
            draw.fill(bounds(), color);
            if (selected) draw.border(bounds(), 0xFFE0E0E0);
            UiSprite.schematicFile(world.sourceFilename).draw(draw, bounds().x + 2, bounds().y + 5, false, false);
            String name = (world.isRendering ? "\u00a7a" : "\u00a7c") + world.name;
            draw.text(draw.trim(name, buttonsStart - bounds().x - 24), bounds().x + 20, bounds().y + 7, 0xFFFFFFFF);
            super.draw(draw, mouseX, mouseY);
        }

        @Override
        public boolean isFocusable() { return true; }

        @Override
        public boolean mouseDown(int x, int y, int button) {
            if (button != 0 || x >= buttonsStart) return false;
            select();
            return true;
        }

        private void select() {
            model.select(index);
            if (ClientProxy.loadedSchematics.contains(world)) {
                ClientProxy.selectSchematic(ClientProxy.schematic == world ? null : world);
                WorldHandler.INSTANCE.saveSession();
            }
        }

        @Override
        public boolean keyTyped(char character, int keyCode) {
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_SPACE) {
                select();
                return true;
            }
            return false;
        }
    }
}

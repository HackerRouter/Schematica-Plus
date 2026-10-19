// SPDX-License-Identifier: LGPL-3.0-only
// Litematica placement list layout, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.placement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;

import com.github.lunatrius.schematica.api.SchematicOrigin;
import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiDraw;
import com.github.lunatrius.schematica.client.gui.framework.UiPanel;
import com.github.lunatrius.schematica.client.gui.framework.UiSprite;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.renderer.RendererSchematicGlobal;
import com.github.lunatrius.schematica.client.world.Placements;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.client.SharedPlacementsClient;
import com.github.lunatrius.schematica.handler.client.WorldHandler;
import com.github.lunatrius.schematica.util.MessageType;

import org.lwjgl.input.Keyboard;

public final class GuiSchematicPlacementsList extends GuiSchematicList<SchematicWorld> {
    public GuiSchematicPlacementsList(GuiScreen parent) {
        super(parent, true, world -> world.name + " " + world.sourceFilename);
    }

    @Override protected List<SchematicWorld> entries() { return Placements.loaded; }
    @Override protected UiPanel row(SchematicWorld world, int index) { return new Entry(world, index); }

    private final class Entry extends UiPanel {
        private final SchematicWorld world;
        private final int index;
        private final List<UiButton> buttons = new ArrayList<>();
        private int buttonsStart;
        private UiButton share;

        Entry(SchematicWorld world, int index) {
            this.world = world;
            this.index = index;
            button("litematica.gui.button.schematic_placements.remove", () -> {
                if (world.sharedId != null) SharedPlacementsClient.INSTANCE.unload(world.sharedId, true);
                else Placements.remove(world);
            });
            UiButton toggle = add(new UiButton(() -> UiTranslations.format("litematica.gui.button.schematic_placements.placement_enabled",
                UiTranslations.format(world.isEnabled() ? "malilib.gui.label_colored.on" : "malilib.gui.label_colored.off")),
                mouse -> { if (mouse == 0 && Placements.loaded.contains(world)) { world.setPlacementSettings(world.placementSettings().enabled(!world.isEnabled()));
                    RendererSchematicGlobal.INSTANCE.refresh(world); WorldHandler.INSTANCE.saveSession(); } }));
            buttons.add(toggle);
            button("litematica.gui.button.schematic_placements.configure", () -> {
                mc.displayGuiScreen(new GuiPlacementConfiguration(GuiSchematicPlacementsList.this, world));
            });
            if (SharedPlacementsClient.INSTANCE.available()) {
                share = button("syncmatica.gui.button.share", () -> {
                    if (!isShiftKeyDown()) addMessage(MessageType.ERROR, "syncmatica.error.share_without_shift");
                    else SharedPlacementsClient.INSTANCE.share(world);
                });
                share.setEnabled(!SharedPlacementsClient.INSTANCE.shared(world));
            }
        }

        private List<UiSprite> icons() {
            List<UiSprite> icons = new ArrayList<>();
            if (world.subregions() != null && world.subregions().modified()) icons.add(UiSprite.NOTICE);
            if (world.placementSettings().locked) icons.add(UiSprite.LOCK);
            if (world.sharedId != null) icons.add(UiSprite.SERVER);
            return icons;
        }

        private UiButton button(String key, Runnable action) {
            UiButton button = add(new UiButton(() -> UiTranslations.format(key), mouse -> { if (mouse == 0 && Placements.loaded.contains(world)) action.run(); }));
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
            if (share != null) share.setEnabled(!SharedPlacementsClient.INSTANCE.shared(world));
            boolean selected = Placements.selected == world;
            boolean hover = containsVisible(mouseX, mouseY);
            int color = selected || hover ? 0xA0707070 : index % 2 == 1 ? 0xA0101010 : 0xA0303030;
            draw.fill(bounds(), color);
            if (selected) draw.border(bounds(), 0xFFE0E0E0);
            UiSprite.schematicFile(world.sourceFilename).draw(draw, bounds().x + 2, bounds().y + 5, false, false);
            String name = (world.isEnabled() ? "\u00a7a" : "\u00a7c") + world.name;
            List<UiSprite> icons = icons();
            int textEnd = buttonsStart - 13 * icons.size();
            UiSprite hovered = icon(mouseX, mouseY);
            for (int i = 0; i < icons.size(); i++) {
                UiSprite icon = icons.get(i);
                icon.draw(draw, buttonsStart - 13 * (i + 1), bounds().y + 6, true, icon == hovered);
            }
            draw.text(draw.trim(name, textEnd - bounds().x - 24), bounds().x + 20, bounds().y + 7, 0xFFFFFFFF);
            super.draw(draw, mouseX, mouseY);
        }

        private UiSprite icon(int mouseX, int mouseY) {
            if (mouseY < bounds().y + 6 || mouseY >= bounds().y + 17 || mouseX >= buttonsStart) return null;
            List<UiSprite> icons = icons();
            int index = (buttonsStart - 1 - mouseX) / 13;
            return index < icons.size() && mouseX < buttonsStart - 13 * index - 2 ? icons.get(index) : null;
        }

        @Override
        public List<String> tooltip(int mouseX, int mouseY) {
            UiSprite icon = icon(mouseX, mouseY);
            if (icon == UiSprite.SERVER) return Collections.singletonList(UiTranslations.format("schematic_synchronizer.gui.placement.hover.server_schematic"));
            if (icon == UiSprite.LOCK) return Collections.singletonList(UiTranslations.format("litematica.hud.schematic_placement.hover_info.placement_locked"));
            if (icon == UiSprite.NOTICE) return Collections.singletonList(UiTranslations.format("litematica.hud.schematic_placement.hover_info.placement_modified"));
            if (mouseX >= buttonsStart - 18) return super.tooltip(mouseX, mouseY);
            SchematicOrigin origin = world.originPosition();
            return Arrays.asList(
                UiTranslations.format("litematica.gui.label.schematic_placement.schematic_name", world.name),
                UiTranslations.format("litematica.gui.label.schematic_placement.schematic_file", world.sourceFilename != null
                    ? world.sourceFilename : UiTranslations.format("litematica.gui.label.schematic_placement.in_memory")),
                UiTranslations.format("litematica.gui.label.schematic_placement.origin", String.format("x: %d, y: %d, z: %d", origin.x, origin.y, origin.z)),
                UiTranslations.format("litematica.gui.label.schematic_placement.sub_region_count", String.valueOf(Math.max(1, world.getSchematic().getRegions().size()))),
                UiTranslations.format("litematica.gui.label.schematic_placement.enclosing_size",
                    String.format("%d x %d x %d", world.getWidth(), world.getHeight(), world.getLength())));
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
            if (Placements.loaded.contains(world)) {
                Placements.select(Placements.selected == world ? null : world);
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

// SPDX-License-Identifier: LGPL-3.0-only
// Litematica loaded schematic list layout, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.placement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiDraw;
import com.github.lunatrius.schematica.client.gui.framework.UiPanel;
import com.github.lunatrius.schematica.client.gui.framework.UiSprite;
import com.github.lunatrius.schematica.client.gui.load.SchematicGuiLoader;
import com.github.lunatrius.schematica.client.gui.save.GuiSchematicSourceSave;
import com.github.lunatrius.schematica.client.world.SchematicLibrary.Source;
import com.github.lunatrius.schematica.client.world.SchematicSourceData;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Reference;

public final class GuiSchematicLoadedList extends GuiSchematicList<Source<SchematicSourceData>> {
    public GuiSchematicLoadedList(GuiScreen parent) {
        super(parent, false, source -> source.name() + " " + source.file().getName());
    }

    @Override protected List<Source<SchematicSourceData>> entries() { return ClientProxy.SCHEMATICS.sources(); }
    @Override protected UiPanel row(Source<SchematicSourceData> source, int index) { return new Entry(source, index); }

    private final class Entry extends UiPanel {
        private final Source<SchematicSourceData> source;
        private final int index;
        private final List<UiButton> buttons = new ArrayList<>();
        private final UiButton reload;
        private final UiButton create;
        private int buttonsStart;

        Entry(Source<SchematicSourceData> source, int index) {
            this.source = source;
            this.index = index;
            button("unload", () -> { ClientProxy.unloadSource(source); refresh(); });
            reload = button("reload", () -> {
                try {
                    ClientProxy.reloadSource(source);
                    message(I18n.format("schematica.ui.source.reloaded", source.name()));
                } catch (IOException | RuntimeException e) {
                    Reference.logger.warn("Schematic source reload failed", e);
                    message(I18n.format("schematica.ui.source.reload_failed"));
                }
            });
            button("save_to_file", () -> mc.displayGuiScreen(new GuiSchematicSourceSave(GuiSchematicLoadedList.this, source)));
            create = button("create_placement", () -> {
                try {
                    SchematicGuiLoader.createPlacement(mc, source, false);
                    message(I18n.format("schematica.ui.source.placement_created", source.name()));
                } catch (IOException | RuntimeException e) {
                    Reference.logger.warn("Schematic placement creation failed", e);
                    message(I18n.format("schematica.ui.source.create_failed"));
                }
            });
            buttons.get(0).setTooltip(I18n.format("schematica.ui.source.unload_hint"));
            reload.setTooltip(I18n.format("schematica.ui.source.reload_hint"));
            buttons.get(2).setTooltip(I18n.format("schematica.ui.source.save_hint"));
            create.setTooltip(I18n.format("schematica.ui.source.create_hint"));
            tick();
        }

        private UiButton button(String name, Runnable action) {
            UiButton button = add(new UiButton(() -> I18n.format("litematica.gui.button." + name), mouse -> {
                if (mouse == 0 && entries().contains(source)) action.run();
            }));
            buttons.add(button);
            return button;
        }

        @Override public void tick() {
            boolean available = entries().contains(source);
            for (UiButton button : buttons) button.setEnabled(available);
            boolean canPlace = available && mc.theWorld != null && mc.thePlayer != null && SchematicaPlus.proxy.isLoadEnabled;
            create.setEnabled(canPlace);
            reload.setEnabled(canPlace && source.file().isFile());
            int count = 0;
            for (SchematicWorld world : ClientProxy.loadedSchematics) if (ClientProxy.SCHEMATICS.sourceOf(world) == source) count++;
            setTooltip(source.file().getAbsolutePath(), I18n.format("schematica.ui.source.details",
                source.data().width, source.data().height, source.data().length, count));
            super.tick();
        }

        @Override public void layout(UiBounds screen) {
            int right = bounds().right();
            for (UiButton button : buttons) {
                int width = fontRendererObj.getStringWidth(button.label()) + 10;
                button.setBounds(right - width, bounds().y + 1, width, 20);
                right -= width + 2;
            }
            buttonsStart = right;
        }

        @Override public void draw(UiDraw draw, int mouseX, int mouseY) {
            boolean hover = containsVisible(mouseX, mouseY);
            draw.fill(bounds(), hover || model.selected() == source ? 0x70FFFFFF : index % 2 == 1 ? 0x20FFFFFF : 0x50FFFFFF);
            UiSprite.FILE.draw(draw, bounds().x + 2, bounds().y + 5, false, false);
            draw.text(draw.trim(source.name(), buttonsStart - bounds().x - 24), bounds().x + 20, bounds().y + 7, 0xFFFFFFFF);
            super.draw(draw, mouseX, mouseY);
        }

        @Override public boolean isFocusable() { return true; }
        @Override public boolean mouseDown(int x, int y, int button) {
            if (button != 0 || x >= buttonsStart) return false;
            model.select(index);
            return true;
        }
        @Override public boolean keyTyped(char character, int keyCode) {
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_SPACE) { model.select(index); return true; }
            return false;
        }
    }
}

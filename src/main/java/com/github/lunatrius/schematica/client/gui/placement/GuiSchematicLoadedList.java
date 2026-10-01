// SPDX-License-Identifier: LGPL-3.0-only
// Litematica loaded schematic list layout, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.placement;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
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
            button("unload", () -> discarding(() -> { ClientProxy.unloadSource(source); refresh(); }));
            reload = button("reload", () -> discarding(() -> {
                try {
                    ClientProxy.reloadSource(source);
                    message(UiTranslations.format("litematica.message.schematic_read_from_file_success", source.name()));
                } catch (IOException | RuntimeException e) {
                    Reference.logger.warn("Schematic source reload failed", e);
                    message(UiTranslations.format("litematica.error.schematic_read_from_file_failed.exception", source.name()));
                }
            }));
            button("save_to_file", () -> mc.displayGuiScreen(new GuiSchematicSourceSave(GuiSchematicLoadedList.this, source)));
            create = button("create_placement", () -> {
                try {
                    SchematicGuiLoader.createPlacement(mc, source, false);
                    message(UiTranslations.format("litematica.message.schematic_placement_created", source.name()));
                } catch (IOException | RuntimeException e) {
                    Reference.logger.warn("Schematic placement creation failed", e);
                    message(UiTranslations.format("schematica.ui.source.create_failed"));
                }
            });
            buttons.get(0).setTooltip(UiTranslations.format("schematica.ui.source.unload_hint"));
            reload.setTooltip(UiTranslations.format("schematica.ui.source.reload_hint"));
            buttons.get(2).setTooltip(UiTranslations.format("schematica.ui.source.save_hint"));
            create.setTooltip(UiTranslations.format("schematica.ui.source.create_hint"),
                UiTranslations.format("litematica.gui.label.schematic_placement.hoverinfo.hold_shift_to_create_as_disabled"));
            tick();
        }

        private void discarding(Runnable action) {
            if (!source.data().unsaved()) { action.run(); return; }
            confirm(UiTranslations.format("schematica.ui.source.unsaved_title"),
                UiTranslations.format("schematica.ui.source.unsaved", source.name()), () -> { if (entries().contains(source)) action.run(); });
        }

        private UiButton button(String name, Runnable action) {
            UiButton button = add(new UiButton(() -> UiTranslations.format("litematica.gui.button." + name), mouse -> {
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
            String details = UiTranslations.format("schematica.ui.source.details", source.data().width, source.data().height, source.data().length, count);
            if (source.data().unsaved()) setTooltip(source.file().getAbsolutePath(), details, UiTranslations.format("litematica.gui.label.loaded_schematic.modified_on",
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(source.data().modifiedTime()))));
            else setTooltip(source.file().getAbsolutePath(), details);
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
            UiSprite.schematicFile(source.file().getName()).draw(draw, bounds().x + 2, bounds().y + 5, false, false);
            boolean unsaved = source.data().unsaved();
            draw.text(draw.trim(source.name(), buttonsStart - bounds().x - (unsaved ? 38 : 24)), bounds().x + 20, bounds().y + 7, unsaved ? 0xFFFF9010 : 0xFFFFFFFF);
            if (unsaved) UiSprite.NOTICE.draw(draw, buttonsStart - 13, bounds().y + 6, false, false);
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

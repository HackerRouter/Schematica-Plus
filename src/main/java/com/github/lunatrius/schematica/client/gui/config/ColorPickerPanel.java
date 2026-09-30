// SPDX-License-Identifier: LGPL-3.0-only
// MaLiLib GuiColorEditorHSV layout, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.config;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.IntConsumer;

import net.minecraft.client.gui.FontRenderer;

import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.gui.config.ColorPickerModel.Channel;
import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.client.gui.framework.UiDraw;
import com.github.lunatrius.schematica.client.gui.framework.UiIntegerField;
import com.github.lunatrius.schematica.client.gui.framework.UiLabel;
import com.github.lunatrius.schematica.client.gui.framework.UiPanel;
import com.github.lunatrius.schematica.client.gui.framework.UiTextField;

public final class ColorPickerPanel extends UiPanel {
    private final ColorPickerModel model;
    private final IntConsumer changed;
    private final UiColorSurface square, hue;
    private final Map<Channel, UiColorSurface> bars = new EnumMap<>(Channel.class);
    private final Map<Channel, UiLabel> labels = new EnumMap<>(Channel.class);
    private final Map<Channel, UiIntegerField> fields = new EnumMap<>(Channel.class);
    private final UiTextField hex;
    private final UiLabel hexLabel;
    private boolean syncing = true;
    private boolean validHex = true;

    public ColorPickerPanel(FontRenderer font, int color, IntConsumer changed) {
        model = new ColorPickerModel(color);
        this.changed = changed;
        square = add(new UiColorSurface(model, null, false, () -> publish(null, false)));
        hue = add(new UiColorSurface(model, Channel.H, true, () -> publish(null, false)));
        for (Channel channel : Channel.values()) {
            labels.put(channel, add(new UiLabel(() -> channel.name() + ":")));
            bars.put(channel, add(new UiColorSurface(model, channel, false, () -> publish(null, false))));
            UiIntegerField field = add(new UiIntegerField(font, model.component(channel), 0, channel.maximum, value -> {
                if (!syncing) { model.setComponent(channel, value); publish(channel, false); }
            }));
            field.setTooltip(UiTranslations.format("schematica.ui.color.component", channel.maximum));
            fields.put(channel, field);
        }
        hexLabel = add(new UiLabel(() -> "HEX:"));
        hex = add(new UiTextField(font, 12, value -> {
            if (syncing) return;
            validHex = model.setHex(value);
            if (validHex) publish(null, true);
        }));
        hex.setTooltip(UiTranslations.format("schematica.ui.color.hex"));
        sync(null, false);
    }

    private void publish(Channel source, boolean fromHex) {
        changed.accept(model.color());
        sync(source, fromHex);
    }

    private void sync(Channel source, boolean fromHex) {
        syncing = true;
        try {
            for (Channel channel : Channel.values()) if (channel != source) fields.get(channel).setValue(model.component(channel));
            if (!fromHex) { hex.setText(model.hex()); validHex = true; }
        } finally { syncing = false; }
    }

    @Override public void layout(UiBounds screen) {
        setBounds(Math.max(0, (screen.width - 300) / 2), Math.max(0, (screen.height - 180) / 2), 300, 180);
        int x = bounds().x;
        int y = bounds().y;
        square.setBounds(x + 5, y + 23, 104, 104);
        hue.setBounds(x + 116, y + 23, 16, 104);
        for (Channel channel : Channel.values()) {
            int rowY = y + 24 + channel.ordinal() * 18;
            labels.get(channel).setBounds(x + 148, rowY, 12, 12);
            bars.get(channel).setBounds(x + 159, rowY - 1, 92, 14);
            fields.get(channel).setBounds(x + 258, rowY, 32, 12);
        }
        hexLabel.setBounds(x + 134, y + 153, 26, 12);
        hex.setBounds(x + 160, y + 151, 68, 14);
    }

    @Override public void draw(UiDraw draw, int mouseX, int mouseY) {
        draw.fill(bounds(), 0xFF000000);
        draw.border(bounds(), 0xFF999999);
        draw.text(UiTranslations.format("malilib.gui.title.color_editor"), bounds().x + 10, bounds().y + 6, 0xFFFFFFFF);
        UiBounds preview = new UiBounds(bounds().x + 5, bounds().y + 133, 34, 34);
        draw.fill(preview.inset(1), model.color() | 0xFF000000);
        draw.border(preview, 0xC0FFFFFF);
        super.draw(draw, mouseX, mouseY);
        hue.drawMarkers(draw);
        for (UiColorSurface bar : bars.values()) bar.drawMarkers(draw);
        if (!validHex) draw.border(hex.bounds(), 0xFFFF5555);
    }
}

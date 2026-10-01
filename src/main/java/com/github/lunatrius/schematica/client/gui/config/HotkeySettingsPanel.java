// SPDX-License-Identifier: LGPL-3.0-only
// MaLiLib GuiKeybindSettings, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.FontRenderer;

import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiDraw;
import com.github.lunatrius.schematica.client.gui.framework.UiLabel;
import com.github.lunatrius.schematica.client.gui.framework.UiPanel;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.input.Hotkey;
import com.github.lunatrius.schematica.client.input.HotkeyHooks;

public final class HotkeySettingsPanel extends UiPanel {
    private static final String[] OPTIONS = {"activate_on", "context", "allow_empty_keybind", "allow_extra_keys",
        "order_sensitive", "exclusive", "cancel_further"};
    private final Hotkey key;
    private final List<UiLabel> labels = new ArrayList<>();
    private final List<UiButton> buttons = new ArrayList<>();
    private final int preferredWidth;

    public HotkeySettingsPanel(FontRenderer font, Hotkey key, Runnable changed) {
        this.key = key;
        int labelWidth = 0;
        for (int i = 0; i < OPTIONS.length; i++) {
            final int index = i;
            String name = UiTranslations.format("malilib.gui.label.keybind_settings." + OPTIONS[i]);
            labelWidth = Math.max(labelWidth, font.getStringWidth(name));
            UiLabel label = add(new UiLabel(() -> name, 0xFFFFFFFF));
            label.setTooltip(UiTranslations.format("malilib.config.comment.keybind_settings." + OPTIONS[i]).split("\n"));
            labels.add(label);
            buttons.add(add(new UiButton(() -> value(index), button -> { cycle(index, button == 1 ? -1 : 1); changed.run(); HotkeyHooks.reset(); })));
        }
        preferredWidth = Math.max(labelWidth + 130, font.getStringWidth(title()) + 20);
    }

    private String title() { return UiTranslations.format("malilib.gui.title.keybind_settings.advanced", UiTranslations.format(key.translationKey())); }
    private String value(int index) {
        if (index == 0) return UiTranslations.format("malilib.label.key_action." + key.settings.action.name().toLowerCase(Locale.ROOT));
        if (index == 1) return UiTranslations.format("malilib.label.key_context." + key.settings.context.name().toLowerCase(Locale.ROOT));
        boolean value = index == 2 ? key.settings.allowEmpty : index == 3 ? key.settings.allowExtra : index == 4
            ? key.settings.ordered : index == 5 ? key.settings.exclusive : key.settings.cancel;
        return UiTranslations.format(value ? "malilib.gui.button.true" : "malilib.gui.button.false");
    }
    private void cycle(int index, int direction) {
        switch (index) {
            case 0: key.settings.action = Hotkey.Action.values()[Math.floorMod(key.settings.action.ordinal() + direction, 3)]; break;
            case 1: key.settings.context = Hotkey.Context.values()[Math.floorMod(key.settings.context.ordinal() + direction, 3)]; break;
            case 2: key.settings.allowEmpty = !key.settings.allowEmpty; break;
            case 3: key.settings.allowExtra = !key.settings.allowExtra; break;
            case 4: key.settings.ordered = !key.settings.ordered; break;
            case 5: key.settings.exclusive = !key.settings.exclusive; break;
            case 6: key.settings.cancel = !key.settings.cancel; break;
            default: break;
        }
    }
    @Override public void layout(UiBounds screen) {
        int width = Math.min(preferredWidth, screen.width - 8), height = 184;
        setBounds((screen.width - width) / 2, Math.max(0, (screen.height - height) / 2), width, height);
        for (int i = 0; i < labels.size(); i++) {
            labels.get(i).setBounds(bounds().x + 10, bounds().y + 28 + i * 22, width - 130, 12);
            buttons.get(i).setBounds(bounds().right() - 110, bounds().y + 24 + i * 22, 100, 20);
        }
    }
    @Override public void draw(UiDraw draw, int x, int y) {
        draw.fill(bounds(), 0xFF000000); draw.border(bounds(), 0xFF808080);
        draw.text(draw.trim(title(), bounds().width - 20), bounds().x + 10, bounds().y + 6, 0xFFFFFFFF);
        super.draw(draw, x, y);
    }
}

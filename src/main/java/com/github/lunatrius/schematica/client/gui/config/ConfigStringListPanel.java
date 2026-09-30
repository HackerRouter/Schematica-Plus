// SPDX-License-Identifier: LGPL-3.0-only
// MaLiLib string-list editor layout, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;

import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiDraw;
import com.github.lunatrius.schematica.client.gui.framework.UiLabel;
import com.github.lunatrius.schematica.client.gui.framework.UiListModel;
import com.github.lunatrius.schematica.client.gui.framework.UiPanel;
import com.github.lunatrius.schematica.client.gui.framework.UiRowList;
import com.github.lunatrius.schematica.client.gui.framework.UiSprite;
import com.github.lunatrius.schematica.client.gui.framework.UiTextField;

public final class ConfigStringListPanel extends UiPanel {
    private final FontRenderer font;
    private final ConfigPropertyDraft draft;
    private final List<Value> values = new ArrayList<>();
    private final Value dummy = new Value("");
    private final UiListModel<Value> model = new UiListModel<>(22, value -> value.text);
    private final UiRowList<Value> rows = add(new UiRowList<>(model, (value, index) -> new Row(value, index), 12));

    public ConfigStringListPanel(FontRenderer font, ConfigPropertyDraft draft) {
        this.font = font;
        this.draft = draft;
        for (String text : draft.values()) values.add(new Value(text));
        refresh();
    }

    private void publish() {
        draft.setValues(values.stream().map(value -> value.text).toArray(String[]::new));
    }

    private void refresh() {
        publish();
        List<Value> entries = new ArrayList<>(values);
        entries.add(dummy);
        model.setEntries(entries);
        rows.sync();
    }

    @Override
    public void layout(UiBounds screen) {
        int width = Math.min(400, screen.width - 20);
        int height = Math.max(70, screen.height - 90);
        setBounds((screen.width - width) / 2, (screen.height - height) / 2, width, height);
        rows.setBounds(bounds().x + 10, bounds().y + 20, width - 14, height - 30);
    }

    @Override
    public void draw(UiDraw draw, int mouseX, int mouseY) {
        draw.fill(bounds(), 0xFF000000);
        draw.border(bounds(), 0xFF808080);
        String title = I18n.format("malilib.gui.title.string_list_edit", draft.property.getName());
        draw.text(draw.trim(title, bounds().width - 20), bounds().x + 10, bounds().y + 6, 0xFFFFFFFF);
        super.draw(draw, mouseX, mouseY);
    }

    private static final class Value {
        String text;
        Value(String text) { this.text = text; }
    }

    private final class Row extends UiPanel {
        private final Value value;
        private final int index;
        private final UiLabel number;
        private final UiTextField text;
        private final UiButton reset;
        private final UiButton insert;
        private final UiButton remove;
        private final UiButton down;
        private final UiButton up;

        Row(Value value, int index) {
            this.value = value;
            this.index = index;
            boolean isDummy = value == dummy;
            number = add(new UiLabel(() -> String.format("%3d:", index + 1), 0xFFC0C0C0));
            text = add(new UiTextField(font, 65535, edited -> { value.text = edited; if (!isDummy) publish(); }));
            text.setText(value.text);
            reset = add(new UiButton(() -> I18n.format("malilib.gui.button.reset.caps"), button -> text.setText("")));
            insert = action(UiSprite.ADD, "add", () -> { values.add(Math.min(index, values.size()), new Value("")); refresh(); });
            remove = action(UiSprite.REMOVE, "remove", () -> { values.remove(value); refresh(); });
            down = action(UiSprite.MOVE_DOWN, "move_down", () -> move(1));
            up = action(UiSprite.MOVE_UP, "move_up", () -> move(-1));
            number.setVisible(!isDummy);
            text.setVisible(!isDummy);
            reset.setVisible(!isDummy);
            remove.setVisible(!isDummy);
            down.setVisible(!isDummy && index < values.size() - 1);
            up.setVisible(!isDummy && index > 0);
        }

        private UiButton action(UiSprite sprite, String label, Runnable action) {
            UiButton button = add(new UiButton(() -> "", mouse -> { if (mouse == 0) action.run(); })
                .setSprite(sprite).setBackground(false));
            button.setTooltip(I18n.format("schematica.ui.config.list." + label));
            return button;
        }

        private void move(int delta) {
            int current = values.indexOf(value);
            int target = current + delta;
            if (current < 0 || target < 0 || target >= values.size()) return;
            Collections.swap(values, current, target);
            refresh();
        }

        @Override
        public void layout(UiBounds screen) {
            int x = bounds().x;
            int y = bounds().y;
            int resetWidth = font.getStringWidth(reset.label()) + 10;
            int textWidth = Math.max(30, bounds().width - Math.max(160, resetWidth + 100));
            number.setBounds(x + 2, y + 6, 20, 12);
            text.setBounds(x + 22, y + 2, textWidth - 4, 17);
            reset.setBounds(x + 22 + textWidth, y + 1, resetWidth, 20);
            int bx = value == dummy ? x + 20 : reset.bounds().right() + 4;
            insert.setBounds(bx, y + 4, 15, 15);
            remove.setBounds(bx + 18, y + 4, 15, 15);
            down.setBounds(bx + 36, y + 4, 15, 15);
            up.setBounds(bx + 54, y + 4, 15, 15);
        }

        @Override
        public void tick() {
            reset.setEnabled(!value.text.isEmpty());
            super.tick();
        }
    }
}

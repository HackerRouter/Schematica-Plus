// SPDX-License-Identifier: LGPL-3.0-only
// Litematica/MaLiLib visual conventions, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.framework;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class UiCheckBox extends UiButton {
    private final BooleanSupplier checked;

    public UiCheckBox(Supplier<String> label, BooleanSupplier checked, Consumer<Boolean> changed) {
        super(label, button -> { if (button == 0) changed.accept(!checked.getAsBoolean()); });
        this.checked = checked;
    }

    @Override
    public void draw(UiDraw draw, int mouseX, int mouseY) {
        UiBounds box = bounds();
        (checked.getAsBoolean() ? UiSprite.CHECK_ON : UiSprite.CHECK_OFF).draw(draw, box.x, box.y, isEnabled(), false);
        draw.text(draw.trim(label(), box.width - 15), box.x + 15, box.y + 2,
            isEnabled() ? 0xFFFFFFFF : 0xFFA0A0A0);
    }
}

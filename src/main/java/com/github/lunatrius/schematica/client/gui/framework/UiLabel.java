package com.github.lunatrius.schematica.client.gui.framework;

import java.util.function.Supplier;

public final class UiLabel extends UiWidget {

    private final Supplier<String> label;

    public UiLabel(Supplier<String> label) {
        this.label = label;
    }

    @Override
    public void draw(UiDraw draw, int mouseX, int mouseY) {
        draw.text(draw.trim(label.get(), bounds().width), bounds().x,
            bounds().y + (bounds().height - 8) / 2, UiTheme.TEXT);
    }
}

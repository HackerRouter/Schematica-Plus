package com.github.lunatrius.schematica.client.gui.framework;

import java.util.function.Supplier;

public final class UiLabel extends UiWidget {

    private final Supplier<String> label;
    private final int color;

    public UiLabel(Supplier<String> label) {
        this(label, UiTheme.TEXT);
    }

    public UiLabel(Supplier<String> label, int color) {
        this.label = label;
        this.color = color;
    }

    @Override
    public void draw(UiDraw draw, int mouseX, int mouseY) {
        draw.text(draw.trim(label.get(), bounds().width), bounds().x,
            bounds().y + (bounds().height - 8) / 2, color);
    }
}

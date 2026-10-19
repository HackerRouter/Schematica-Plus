package com.github.lunatrius.schematica.client.gui.framework;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class UiToggleButton extends UiButton {

    public UiToggleButton(Supplier<String> label, BooleanSupplier value, Consumer<Boolean> changed) {
        super(() -> label.get() + ": " + UiTranslations.format(value.getAsBoolean() ? "malilib.gui.label_colored.on" : "malilib.gui.label_colored.off"),
            button -> changed.accept(!value.getAsBoolean()));
    }
}

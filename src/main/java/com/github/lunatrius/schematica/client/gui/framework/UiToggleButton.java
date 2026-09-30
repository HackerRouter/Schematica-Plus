package com.github.lunatrius.schematica.client.gui.framework;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.client.resources.I18n;

public final class UiToggleButton extends UiButton {

    public UiToggleButton(Supplier<String> label, BooleanSupplier value, Consumer<Boolean> changed) {
        super(() -> label.get() + ": " + I18n.format(value.getAsBoolean() ? "options.on" : "options.off"),
            button -> changed.accept(!value.getAsBoolean()));
    }
}

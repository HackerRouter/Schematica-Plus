package com.github.lunatrius.schematica.client.gui.framework;

import java.util.function.IntConsumer;

import net.minecraft.client.gui.FontRenderer;

import org.lwjgl.input.Keyboard;

public final class UiIntegerField extends UiTextField {

    private final int minimum;
    private final int maximum;
    private final IntConsumer changed;
    private int value;

    public UiIntegerField(FontRenderer font, int value, int minimum, int maximum, IntConsumer changed) {
        super(font, 12, text -> {});
        if (minimum > maximum) throw new IllegalArgumentException("Invalid numeric range");
        this.minimum = minimum;
        this.maximum = maximum;
        this.changed = changed;
        setValidator(text -> text.matches("-?[0-9]{0,10}"));
        setValue(value);
    }

    public int value() {
        return value;
    }

    public void setValue(long value) {
        int next = (int) Math.max(minimum, Math.min(maximum, value));
        boolean different = this.value != next;
        this.value = next;
        setText(Integer.toString(next));
        if (different) changed.accept(next);
    }

    private void commit() {
        String text = text();
        setValue(text.isEmpty() || "-".equals(text) ? value : Long.parseLong(text));
    }

    @Override
    protected void focusChanged(boolean focused) {
        super.focusChanged(focused);
        if (!focused) commit();
    }

    @Override
    public boolean keyTyped(char character, int keyCode) {
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            commit();
            return true;
        }
        if (keyCode == Keyboard.KEY_UP || keyCode == Keyboard.KEY_DOWN) {
            commit();
            setValue((long) value + (keyCode == Keyboard.KEY_UP ? 1 : -1));
            return true;
        }
        return super.keyTyped(character, keyCode);
    }

    @Override
    public boolean scroll(int x, int y, int amount) {
        if (!isFocused()) return false;
        commit();
        setValue((long) value + Integer.signum(amount));
        return true;
    }
}

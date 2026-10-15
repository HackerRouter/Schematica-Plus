package com.github.lunatrius.schematica.client.gui.framework;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.lwjgl.input.Keyboard;

/** Two named positions; the thumb follows a drag and commits on release. */
public final class UiSlideSwitch extends UiWidget {
    private final Supplier<String> left, right;
    private final BooleanSupplier value;
    private final Consumer<Boolean> changed;
    private boolean dragging;
    private float position;

    public UiSlideSwitch(Supplier<String> left, Supplier<String> right, BooleanSupplier value, Consumer<Boolean> changed) {
        this.left = left; this.right = right; this.value = value; this.changed = changed;
    }

    @Override public boolean isFocusable() { return true; }

    @Override public void draw(UiDraw draw, int mouseX, int mouseY) {
        UiBounds b = bounds();
        float p = dragging ? position : value.getAsBoolean() ? 1 : 0;
        draw.fill(b, 0xFF202020);
        draw.border(b, isFocused() || containsVisible(mouseX, mouseY) ? 0xFFFFFFFF : 0xFF888888);
        int half = (b.width - 4) / 2;
        draw.fill(new UiBounds(b.x + 2 + Math.round(p * half), b.y + 2, half, b.height - 4), isEnabled() ? 0xFF586E85 : 0xFF444444);
        String a = draw.trim(left.get(), half - 4), c = draw.trim(right.get(), half - 4);
        int color = isEnabled() ? 0xFFFFFFFF : 0xFFA0A0A0;
        draw.text(a, b.x + 2 + (half - draw.textWidth(a)) / 2, b.y + (b.height - 8) / 2, color);
        draw.text(c, b.x + 2 + half + (half - draw.textWidth(c)) / 2, b.y + (b.height - 8) / 2, color);
    }

    @Override public boolean mouseDown(int x, int y, int button) {
        if (button != 0) return false;
        dragging = true;
        move(x);
        return true;
    }

    private void move(int x) {
        position = Math.max(0, Math.min(1, (x - bounds().x - bounds().width * 0.25f) / Math.max(1, bounds().width * 0.5f)));
    }

    @Override public void mouseDrag(int x, int y, int button) { if (dragging) move(x); }

    @Override public void mouseUp(int x, int y, int button) {
        if (dragging && button == 0) {
            move(x);
            dragging = false;
            changed.accept(position >= 0.5f);
        }
    }

    @Override public void cancelMouse() { dragging = false; }

    @Override public boolean keyTyped(char character, int keyCode) {
        if (keyCode == Keyboard.KEY_LEFT || keyCode == Keyboard.KEY_RIGHT) changed.accept(keyCode == Keyboard.KEY_RIGHT);
        else if (keyCode == Keyboard.KEY_SPACE || keyCode == Keyboard.KEY_RETURN) changed.accept(!value.getAsBoolean());
        else return false;
        return true;
    }
}

package com.github.lunatrius.schematica.client.gui.framework;

import java.util.function.IntConsumer;
import java.util.function.Supplier;

import org.lwjgl.input.Keyboard;

public class UiButton extends UiWidget {

    private final Supplier<String> label;
    private final IntConsumer action;
    private final UiIcon icon;
    private int pressed = -1;

    public UiButton(Supplier<String> label, IntConsumer action) {
        this(label, null, action);
    }

    public UiButton(Supplier<String> label, UiIcon icon, IntConsumer action) {
        this.label = label;
        this.icon = icon;
        this.action = action;
    }

    @Override
    public boolean isFocusable() {
        return true;
    }

    @Override
    public void draw(UiDraw draw, int mouseX, int mouseY) {
        UiBounds box = bounds();
        boolean hovered = containsVisible(mouseX, mouseY);
        draw.fill(box, isEnabled() && hovered ? UiTheme.HOVER : UiTheme.CONTROL);
        draw.border(box, isFocused() ? UiTheme.FOCUS : UiTheme.BORDER);
        int color = isEnabled() ? UiTheme.TEXT : UiTheme.DISABLED;
        String text = draw.trim(label.get(), box.width - (icon == null ? 8 : 21));
        if (icon != null) icon.draw(draw, box.x + 5, box.y + (box.height - 7) / 2, color);
        int x = icon == null ? box.x + (box.width - draw.textWidth(text)) / 2 : box.x + 17;
        draw.text(text, x, box.y + (box.height - 8) / 2 + (pressed >= 0 ? 1 : 0), color);
    }

    @Override
    public boolean mouseDown(int x, int y, int button) {
        if (button != 0 && button != 1) return false;
        pressed = button;
        return true;
    }

    @Override
    public void mouseUp(int x, int y, int button) {
        boolean activate = button == pressed && containsVisible(x, y);
        pressed = -1;
        if (activate) action.accept(button);
    }

    @Override
    public void cancelMouse() {
        pressed = -1;
    }

    @Override
    public boolean keyTyped(char character, int keyCode) {
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER || keyCode == Keyboard.KEY_SPACE) {
            action.accept(0);
            return true;
        }
        return false;
    }
}

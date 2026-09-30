package com.github.lunatrius.schematica.client.gui.framework;

import java.util.function.Consumer;
import java.util.function.Predicate;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;

public class UiTextField extends UiWidget {

    private final GuiTextField field;
    private final Consumer<String> changed;
    private int dragAnchor;
    private Predicate<String> validator = text -> true;

    public UiTextField(FontRenderer font, int maxLength, Consumer<String> changed) {
        field = new GuiTextField(font, 0, 0, 1, 1);
        field.setMaxStringLength(maxLength);
        field.setCanLoseFocus(false);
        field.setEnableBackgroundDrawing(false);
        this.changed = changed;
    }

    public String text() {
        return field.getText();
    }

    public void setText(String text) {
        if (!validator.test(text)) return;
        field.setText(text);
        changed.accept(field.getText());
    }

    public void setValidator(Predicate<String> validator) {
        this.validator = validator;
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        field.xPosition = x + 4;
        field.yPosition = y + (height - 8) / 2;
        field.width = Math.max(1, width - 8);
        field.height = 8;
        field.setSelectionPos(field.getSelectionEnd());
    }

    @Override
    public boolean isFocusable() {
        return true;
    }

    @Override
    protected void focusChanged(boolean focused) {
        field.setFocused(focused);
    }

    @Override
    public void tick() {
        field.updateCursorCounter();
    }

    @Override
    public void draw(UiDraw draw, int mouseX, int mouseY) {
        draw.fill(bounds(), UiTheme.FIELD);
        draw.border(bounds(), isFocused() ? UiTheme.FOCUS : UiTheme.BORDER);
        field.setEnabled(isEnabled());
        field.drawTextBox();
    }

    @Override
    public boolean mouseDown(int x, int y, int button) {
        if (button != 0) return false;
        field.mouseClicked(x, y, button);
        dragAnchor = field.getCursorPosition();
        return true;
    }

    @Override
    public void mouseDrag(int x, int y, int button) {
        field.mouseClicked(x, y, 0);
        int end = field.getCursorPosition();
        field.setCursorPosition(dragAnchor);
        field.setSelectionPos(end);
    }

    @Override
    public boolean keyTyped(char character, int keyCode) {
        String previous = text();
        int cursor = field.getCursorPosition();
        int selection = field.getSelectionEnd();
        field.setEnabled(isEnabled());
        boolean handled = field.textboxKeyTyped(character, keyCode);
        if (!validator.test(text())) {
            field.setText(previous);
            field.setCursorPosition(cursor);
            field.setSelectionPos(selection);
            return true;
        }
        if (!previous.equals(text())) changed.accept(text());
        return handled;
    }
}

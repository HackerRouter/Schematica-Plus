package com.github.lunatrius.schematica.client.gui.framework;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class UiWidget {

    private UiBounds bounds = new UiBounds(0, 0, 0, 0);
    private boolean visible = true;
    private boolean enabled = true;
    private boolean focused;
    private List<String> tooltip = Collections.emptyList();
    UiPanel parent;

    public final UiBounds bounds() {
        return bounds;
    }

    public final boolean containsVisible(int x, int y) {
        for (UiWidget widget = this; widget != null; widget = widget.parent) {
            if (!widget.isVisible() || !widget.bounds().contains(x, y)) return false;
        }
        return true;
    }

    public void setBounds(int x, int y, int width, int height) {
        bounds = new UiBounds(x, y, width, height);
    }

    public final boolean isVisible() {
        return visible;
    }

    public final void setVisible(boolean visible) {
        this.visible = visible;
    }

    public final boolean isEnabled() {
        return enabled && (parent == null || parent.isEnabled());
    }

    public final void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public final boolean isFocused() {
        return focused;
    }

    final void setFocused(boolean focused) {
        this.focused = focused;
        focusChanged(focused);
    }

    public void setTooltip(String... lines) {
        tooltip = Collections.unmodifiableList(Arrays.asList(lines.clone()));
    }

    public List<String> tooltip(int mouseX, int mouseY) {
        return tooltip;
    }

    public boolean drawTooltip(UiDraw draw, int mouseX, int mouseY, UiBounds screen) { return false; }

    public boolean isFocusable() {
        return false;
    }

    protected void focusChanged(boolean focused) {}

    public abstract void draw(UiDraw draw, int mouseX, int mouseY);

    public void tick() {}

    public boolean mouseDown(int x, int y, int button) {
        return false;
    }

    public void mouseUp(int x, int y, int button) {}

    public void mouseDrag(int x, int y, int button) {}

    public void cancelMouse() {}

    public boolean scroll(int x, int y, int amount) {
        return false;
    }

    public boolean keyTyped(char character, int keyCode) {
        return false;
    }
}

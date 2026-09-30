package com.github.lunatrius.schematica.client.gui.framework;

import java.util.ArrayList;
import java.util.List;

public final class UiInput {

    private final UiPanel root;
    private final List<Modal> modals = new ArrayList<>();
    private UiWidget focused;
    private UiWidget captured;
    private int capturedButton = -1;

    public UiInput(UiPanel root) {
        this.root = root;
    }

    private UiPanel active() {
        return modals.isEmpty() ? root : modals.get(modals.size() - 1).panel;
    }

    public void pushModal(UiPanel panel) {
        if (panel.parent != null || panel == root || modalPanels().contains(panel)) {
            throw new IllegalArgumentException("Modal must be a separate root");
        }
        validate();
        modals.add(new Modal(panel, focused));
        cancelCapture();
        focus(null);
        cycleFocus(false);
    }

    public boolean popModal() {
        if (modals.isEmpty()) return false;
        cancelCapture();
        Modal modal = modals.remove(modals.size() - 1);
        focus(modal.previousFocus);
        return true;
    }

    public List<UiPanel> modalPanels() {
        List<UiPanel> panels = new ArrayList<>();
        for (Modal modal : modals) panels.add(modal.panel);
        return panels;
    }

    public UiWidget focused() {
        validate();
        return focused;
    }

    public void focus(UiWidget widget) {
        if (widget != null && (!widget.isFocusable() || !available(widget))) widget = null;
        if (focused == widget) return;
        if (focused != null) focused.setFocused(false);
        focused = widget;
        if (focused != null) focused.setFocused(true);
    }

    private boolean available(UiWidget widget) {
        UiBounds visible = widget.bounds();
        for (UiWidget ancestor = widget; ancestor != null; ancestor = ancestor.parent) {
            if (!ancestor.isVisible() || !ancestor.isEnabled()) return false;
            visible = visible.intersect(ancestor.bounds());
            if (visible.isEmpty()) return false;
            if (ancestor == active()) return true;
        }
        return false;
    }

    public void validate() {
        if (focused != null && !available(focused)) focus(null);
        if (captured != null && !available(captured)) cancelCapture();
    }

    public UiWidget hit(int x, int y) {
        return hit(active(), x, y);
    }

    private UiWidget hit(UiWidget widget, int x, int y) {
        if (!widget.isVisible() || !widget.bounds().contains(x, y)) return null;
        if (widget instanceof UiPanel && widget.isEnabled()) {
            List<UiWidget> children = ((UiPanel) widget).children();
            for (int i = children.size() - 1; i >= 0; i--) {
                UiWidget found = hit(children.get(i), x, y);
                if (found != null) return found;
            }
        }
        return widget;
    }

    public void mouseDown(int x, int y, int button) {
        validate();
        if (captured != null) return;
        UiWidget target = hit(x, y);
        focus(target);
        if (target != null && available(target)) {
            UiPanel layer = active();
            if (target.mouseDown(x, y, button) && active() == layer && available(target)) {
                captured = target;
                capturedButton = button;
            }
        }
    }

    public void mouseUp(int x, int y, int button) {
        validate();
        if (captured != null && button == capturedButton) {
            UiWidget target = captured;
            captured = null;
            capturedButton = -1;
            target.mouseUp(x, y, button);
        }
    }

    public void mouseDrag(int x, int y, int button) {
        validate();
        if (captured != null && button == capturedButton) captured.mouseDrag(x, y, button);
    }

    public void scroll(int x, int y, int amount) {
        validate();
        if (amount == 0) return;
        for (UiWidget widget = hit(x, y); widget != null; widget = widget.parent) {
            if (!available(widget) || widget.scroll(x, y, amount) || widget == active()) break;
        }
    }

    public boolean keyTyped(char character, int keyCode) {
        validate();
        return focused != null && focused.keyTyped(character, keyCode);
    }

    public void cycleFocus(boolean backwards) {
        validate();
        List<UiWidget> candidates = new ArrayList<>();
        collectFocus(active(), candidates);
        if (candidates.isEmpty()) {
            focus(null);
            return;
        }
        int index = candidates.indexOf(focused);
        int next = index < 0 ? (backwards ? candidates.size() - 1 : 0)
            : Math.floorMod(index + (backwards ? -1 : 1), candidates.size());
        focus(candidates.get(next));
    }

    private void collectFocus(UiWidget widget, List<UiWidget> candidates) {
        if (!available(widget)) return;
        if (widget.isFocusable()) candidates.add(widget);
        if (widget instanceof UiPanel) {
            for (UiWidget child : ((UiPanel) widget).children()) collectFocus(child, candidates);
        }
    }

    private void cancelCapture() {
        if (captured != null) captured.cancelMouse();
        captured = null;
        capturedButton = -1;
    }

    public void suspend() {
        cancelCapture();
        focus(null);
    }

    private static final class Modal {
        final UiPanel panel;
        final UiWidget previousFocus;

        Modal(UiPanel panel, UiWidget previousFocus) {
            this.panel = panel;
            this.previousFocus = previousFocus;
        }
    }
}

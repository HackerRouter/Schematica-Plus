package com.github.lunatrius.schematica.client.gui.framework;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UiPanel extends UiWidget {

    private final List<UiWidget> children = new ArrayList<>();

    public <T extends UiWidget> T add(T widget) {
        if (widget.parent != null) throw new IllegalArgumentException("Widget already has a parent");
        for (UiWidget ancestor = this; ancestor != null; ancestor = ancestor.parent) {
            if (ancestor == widget) throw new IllegalArgumentException("Widget tree cycle");
        }
        children.add(widget);
        widget.parent = this;
        return widget;
    }

    public void remove(UiWidget widget) {
        if (children.remove(widget)) widget.parent = null;
    }

    public List<UiWidget> children() {
        return Collections.unmodifiableList(children);
    }

    public void layout(UiBounds screen) {}

    @Override
    public void draw(UiDraw draw, int mouseX, int mouseY) {
        try (UiDraw.Clip ignored = draw.clip(bounds())) {
            for (UiWidget widget : children) {
                if (widget.isVisible() && !bounds().intersect(widget.bounds()).isEmpty()) {
                    try (UiDraw.Clip childClip = draw.clip(widget.bounds())) {
                        widget.draw(draw, mouseX, mouseY);
                    }
                }
            }
        }
    }

    @Override
    public void tick() {
        for (UiWidget widget : new ArrayList<>(children)) {
            if (widget.isVisible()) widget.tick();
        }
    }
}

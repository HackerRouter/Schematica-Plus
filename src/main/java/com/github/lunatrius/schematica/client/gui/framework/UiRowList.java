package com.github.lunatrius.schematica.client.gui.framework;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public final class UiRowList<T> extends UiPanel {
    private final UiListModel<T> model;
    private final BiFunction<T, Integer, UiPanel> factory;
    private final int rowRightInset;
    private final UiPanel rows = add(new UiPanel());
    private final ScrollBar scrollBar = add(new ScrollBar());
    private List<T> visible = new ArrayList<>();
    private int firstIndex = -1;

    public UiRowList(UiListModel<T> model, BiFunction<T, Integer, UiPanel> factory) {
        this(model, factory, 12);
    }

    public UiRowList(UiListModel<T> model, BiFunction<T, Integer, UiPanel> factory, int rowRightInset) {
        if (rowRightInset < 8) throw new IllegalArgumentException("Rows overlap the scrollbar");
        this.model = model;
        this.factory = factory;
        this.rowRightInset = rowRightInset;
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        rows.setBounds(x, y, Math.max(0, width - rowRightInset), height);
        scrollBar.setBounds(x + width - 8, y, 8, height);
        model.setViewportHeight(height);
        sync();
    }

    public void sync() {
        int first = model.offset() / model.rowHeight();
        int end = Math.min(model.entries().size(), first + bounds().height / model.rowHeight() + 2);
        List<T> entries = model.entries().subList(first, end);
        if (first != firstIndex || !entries.equals(visible)) {
            for (UiWidget widget : new ArrayList<>(rows.children())) rows.remove(widget);
            for (int i = first; i < end; i++) rows.add(factory.apply(model.entries().get(i), i));
            visible = new ArrayList<>(entries);
            firstIndex = first;
        }
        for (int i = 0; i < rows.children().size(); i++) {
            UiWidget row = rows.children().get(i);
            row.setBounds(bounds().x, bounds().y + (first + i) * model.rowHeight() - model.offset(),
                rows.bounds().width, model.rowHeight());
            ((UiPanel) row).layout(bounds());
        }
    }

    @Override
    public void tick() {
        sync();
        super.tick();
    }

    @Override
    public boolean scroll(int x, int y, int amount) {
        model.setOffset(model.offset() - Integer.signum(amount) * model.rowHeight() * 3);
        sync();
        return true;
    }

    private final class ScrollBar extends UiWidget {
        private int grab;

        private UiBounds thumb() {
            int total = Math.max(1, model.entries().size() * model.rowHeight());
            int height = Math.min(bounds().height, Math.max(10, bounds().height * bounds().height / total));
            int travel = bounds().height - height;
            int y = model.maxOffset() == 0 ? 0 : (int) ((long) model.offset() * travel / model.maxOffset());
            return new UiBounds(bounds().x, bounds().y + y, bounds().width, height);
        }

        @Override
        public void draw(UiDraw draw, int mouseX, int mouseY) {
            draw.fill(bounds(), 0xA0101010);
            draw.fill(thumb(), 0xFF808080);
            draw.border(thumb(), 0xFFC0C0C0);
        }

        @Override
        public boolean mouseDown(int x, int y, int button) {
            if (button != 0) return false;
            grab = thumb().contains(x, y) ? y - thumb().y : thumb().height / 2;
            mouseDrag(x, y, button);
            return true;
        }

        @Override
        public void mouseDrag(int x, int y, int button) {
            int travel = Math.max(1, bounds().height - thumb().height);
            model.setOffset((int) ((long) (y - bounds().y - grab) * model.maxOffset() / travel));
            sync();
        }
    }
}

package com.github.lunatrius.schematica.client.gui.framework;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public final class UiListModel<T> {

    private final Function<T, String> label;
    private final int rowHeight;
    private List<T> entries = Collections.emptyList();
    private final List<T> filtered = new ArrayList<>();
    private String query = "";
    private int viewportHeight;
    private int offset;
    private T selected;

    public UiListModel(int rowHeight, Function<T, String> label) {
        if (rowHeight < 1) throw new IllegalArgumentException("Invalid row height");
        this.rowHeight = rowHeight;
        this.label = label;
    }

    public void setEntries(List<T> entries) {
        this.entries = new ArrayList<>(entries);
        int selection = this.entries.indexOf(selected);
        selected = selection < 0 ? null : this.entries.get(selection);
        refilter();
    }

    public void setQuery(String query) {
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        if (!normalized.equals(this.query)) {
            this.query = normalized;
            offset = 0;
            refilter();
        }
    }

    private void refilter() {
        filtered.clear();
        for (T entry : entries) {
            if (label.apply(entry).toLowerCase(Locale.ROOT).contains(query)) filtered.add(entry);
        }
        setOffset(offset);
    }

    public List<T> entries() {
        return Collections.unmodifiableList(filtered);
    }

    public String label(T entry) {
        return label.apply(entry);
    }

    public int rowHeight() {
        return rowHeight;
    }

    public int offset() {
        return offset;
    }

    public int maxOffset() {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, (long) filtered.size() * rowHeight - viewportHeight));
    }

    public void setOffset(int offset) {
        this.offset = Math.max(0, Math.min(maxOffset(), offset));
    }

    public void setViewportHeight(int height) {
        viewportHeight = Math.max(0, height);
        setOffset(offset);
    }

    public int indexAt(int y) {
        if (y < 0 || y >= viewportHeight) return -1;
        int index = (int) (((long) y + offset) / rowHeight);
        return index < filtered.size() ? index : -1;
    }

    public T selected() {
        return selected;
    }

    public int selectedIndex() {
        return filtered.indexOf(selected);
    }

    public void select(int index) {
        if (index >= 0 && index < filtered.size()) selected = filtered.get(index);
    }

    public void moveSelection(int delta) {
        int current = selectedIndex();
        select(Math.max(0, Math.min(filtered.size() - 1, current < 0 ? 0 : current + delta)));
        revealSelection();
    }

    public void revealSelection() {
        int index = selectedIndex();
        if (index < 0) return;
        int top = index * rowHeight;
        if (top < offset) setOffset(top);
        else if (top + rowHeight > offset + viewportHeight) setOffset(top + rowHeight - viewportHeight);
    }
}

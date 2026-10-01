package com.github.lunatrius.schematica.client.gui.material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class MaterialListModel<T> {
    public enum Sort { NAME, TOTAL, MISSING, AVAILABLE }

    public static final class Entry<T> {
        public final T key;
        public final String name;
        public final String registryName;
        public final int total;
        public final int missing;
        public final int mismatched;
        public final int unknown;
        public long available;

        public Entry(T key, String name, String registryName, int total, int missing, int mismatched, int unknown) {
            if (total < 0 || missing < 0 || missing > total || mismatched < 0 || unknown < 0
                || (long) mismatched + unknown > missing) throw new IllegalArgumentException("Invalid material counts");
            this.key = key;
            this.name = name;
            this.registryName = registryName;
            this.total = total;
            this.missing = missing;
            this.mismatched = mismatched;
            this.unknown = unknown;
        }
    }

    private final Set<T> ignored = new HashSet<>();
    private List<Entry<T>> entries = Collections.emptyList();
    private int multiplier = 1;
    private Sort sort = Sort.TOTAL;
    private boolean descending = true;
    private boolean hideAvailable;
    private String query = "";

    public List<Entry<T>> entries() { return entries; }

    public void setEntries(List<Entry<T>> entries) { this.entries = Collections.unmodifiableList(new ArrayList<>(entries)); }

    public int multiplier() { return multiplier; }

    public void setMultiplier(int multiplier) { this.multiplier = Math.max(1, multiplier); }

    public Sort sort() { return sort; }

    public boolean descending() { return descending; }

    public void sortBy(Sort sort) {
        descending = this.sort == sort ? !descending : sort != Sort.NAME;
        this.sort = sort;
    }

    public void setSort(Sort sort, boolean descending) {
        this.sort = sort;
        this.descending = descending;
    }

    public void restoreSort(String value) {
        try {
            if (value.startsWith("MATERIAL_")) {
                String[] parts = value.substring(9).split(":");
                Sort parsed = Sort.valueOf(parts[0]);
                boolean reverse = Boolean.parseBoolean(parts[1]);
                sort = parsed;
                descending = reverse;
            } else if (value.startsWith("NAME_") || value.startsWith("SIZE_")) {
                sort = value.startsWith("NAME_") ? Sort.NAME : Sort.TOTAL;
                descending = value.endsWith("DESC");
            }
        } catch (IllegalArgumentException | IndexOutOfBoundsException ignored) {}
    }

    public String savedSort() { return "MATERIAL_" + sort.name() + ":" + descending; }

    public boolean hideAvailable() { return hideAvailable; }

    public void setHideAvailable(boolean hideAvailable) { this.hideAvailable = hideAvailable; }

    public void setQuery(String query) { this.query = query.trim().toLowerCase(Locale.ROOT); }

    public void ignore(T key) { ignored.add(key); }

    public void clearIgnored() { ignored.clear(); }

    public long total(Entry<T> entry) { return (long) entry.total * multiplier; }

    public long missing(Entry<T> entry) { return multiplier == 1 ? entry.missing : total(entry); }

    public List<Entry<T>> visible() {
        List<Entry<T>> result = new ArrayList<>();
        for (Entry<T> entry : entries) {
            if (!ignored.contains(entry.key) && (!hideAvailable || entry.available < missing(entry))
                && (entry.name + " " + entry.registryName).toLowerCase(Locale.ROOT).contains(query)) result.add(entry);
        }
        result.sort(comparator());
        return result;
    }

    /** getMaterialsMissingOnly: the entries that are not ignored and not yet covered by the inventory, for the info HUD. */
    public List<Entry<T>> missingOnly() {
        List<Entry<T>> result = new ArrayList<>();
        for (Entry<T> entry : entries) if (!ignored.contains(entry.key) && entry.available < missing(entry)) result.add(entry);
        result.sort(comparator());
        return result;
    }

    /** The count the info HUD shows: what is still needed beyond the inventory, or the multiplied total. */
    public long hudCount(Entry<T> entry) {
        return multiplier == 1 ? Math.max(0, entry.missing - entry.available) : total(entry);
    }

    private Comparator<Entry<T>> comparator() {
        Comparator<Entry<T>> comparator;
        switch (sort) {
            case TOTAL: comparator = Comparator.comparingLong(this::total); break;
            case MISSING: comparator = Comparator.comparingLong(this::missing); break;
            case AVAILABLE: comparator = Comparator.comparingLong(entry -> entry.available); break;
            default: comparator = Comparator.comparing(entry -> entry.name, String.CASE_INSENSITIVE_ORDER);
        }
        if (descending) comparator = comparator.reversed();
        return comparator.thenComparing(entry -> entry.name).thenComparing(entry -> entry.registryName);
    }

    public long[] progress() {
        long[] counts = new long[5];
        for (Entry<T> entry : entries) {
            counts[0] += entry.total;
            counts[1] += entry.total - entry.missing;
            counts[2] += entry.missing - entry.mismatched - entry.unknown;
            counts[3] += entry.mismatched;
            counts[4] += entry.unknown;
        }
        return counts;
    }
}

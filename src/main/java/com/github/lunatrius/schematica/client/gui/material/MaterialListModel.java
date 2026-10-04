package com.github.lunatrius.schematica.client.gui.material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    /** The item another material is counted as (LitematList's replace); its name for the merged row. */
    public static final class Replacement<T> {
        public final T key;
        public final String name, registryName;
        public Replacement(T key, String name, String registryName) { this.key = key; this.name = name; this.registryName = registryName; }
    }

    private final Set<T> ignored = new HashSet<>();
    private final Set<T> starred = new LinkedHashSet<>();
    private final Map<T, Replacement<T>> replacements = new LinkedHashMap<>();
    private List<Entry<T>> raw = Collections.emptyList();
    private List<Entry<T>> entries = Collections.emptyList();
    private int multiplier = 1;
    private Sort sort = Sort.TOTAL;
    private boolean descending = true;
    private boolean hideAvailable;
    private String query = "";

    public List<Entry<T>> entries() { return entries; }

    public void setEntries(List<Entry<T>> entries) {
        raw = Collections.unmodifiableList(new ArrayList<>(entries));
        merge();
    }

    /** The counted entries with replaced materials added to their replacement (available counts carried over). */
    private void merge() {
        if (replacements.isEmpty()) { entries = raw; return; }
        Map<T, long[]> sums = new LinkedHashMap<>();
        Map<T, String[]> names = new LinkedHashMap<>();
        Map<T, Long> available = new LinkedHashMap<>();
        for (Entry<T> entry : raw) {
            Replacement<T> replacement = replacements.get(entry.key);
            T key = replacement == null ? entry.key : replacement.key;
            long[] sum = sums.computeIfAbsent(key, k -> new long[4]);
            sum[0] += entry.total; sum[1] += entry.missing; sum[2] += entry.mismatched; sum[3] += entry.unknown;
            if (replacement != null) names.putIfAbsent(key, new String[] {replacement.name, replacement.registryName});
            else names.put(key, new String[] {entry.name, entry.registryName});
            if (replacement == null) available.put(key, entry.available);
        }
        List<Entry<T>> merged = new ArrayList<>();
        for (Map.Entry<T, long[]> sum : sums.entrySet()) {
            long[] s = sum.getValue();
            int total = (int) Math.min(Integer.MAX_VALUE, s[0]);
            int missing = (int) Math.min(total, s[1]);
            int mismatched = (int) Math.min(missing, s[2]);
            int unknown = (int) Math.min(missing - mismatched, s[3]);
            String[] name = names.get(sum.getKey());
            Entry<T> entry = new Entry<>(sum.getKey(), name[0], name[1], total, missing, mismatched, unknown);
            entry.available = available.getOrDefault(sum.getKey(), 0L);
            merged.add(entry);
        }
        entries = Collections.unmodifiableList(merged);
    }

    /** The scanned entries before replacements. */
    public List<Entry<T>> rawEntries() { return raw; }

    public Map<T, Replacement<T>> replacements() { return Collections.unmodifiableMap(replacements); }

    /** Counts a material as another item; replacing with itself removes the replacement. */
    public void replace(T from, Replacement<T> to) {
        // Replacements point at the final item: through an existing replacement of the target, and from the materials replaced into this one
        if (to != null && replacements.containsKey(to.key)) to = replacements.get(to.key);
        if (to == null || from.equals(to.key)) {
            replacements.remove(from);
        } else {
            replacements.put(from, to);
            for (Map.Entry<T, Replacement<T>> entry : replacements.entrySet()) if (entry.getValue().key.equals(from)) entry.setValue(to);
        }
        merge();
    }

    /** Undoes the replacements into this row. */
    public void restoreReplaced(T key) {
        replacements.values().removeIf(replacement -> replacement.key.equals(key));
        merge();
    }

    /** The names of the materials counted in this row instead of their own. */
    public List<String> replacedNames(T key) {
        List<String> names = new ArrayList<>();
        for (Entry<T> entry : raw) {
            Replacement<T> replacement = replacements.get(entry.key);
            if (replacement != null && replacement.key.equals(key)) names.add(entry.name);
        }
        return names;
    }

    public void clearReplacements() { replacements.clear(); merge(); }

    public Set<T> ignored() { return Collections.unmodifiableSet(ignored); }

    public Set<T> starred() { return Collections.unmodifiableSet(starred); }

    public boolean isStarred(T key) { return starred.contains(key); }

    /** LitematList's marked materials, listed first. */
    public void toggleStar(T key) { if (!starred.remove(key)) starred.add(key); }

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
        Comparator<Entry<T>> stars = Comparator.comparing(entry -> !starred.contains(entry.key));
        return stars.thenComparing(comparator).thenComparing(entry -> entry.name).thenComparing(entry -> entry.registryName);
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

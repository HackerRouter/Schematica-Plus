package com.github.lunatrius.schematica.client.renderer;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

final class RenderUpdateScheduler<T> {
    private int nextGroup;
    private Map<List<T>, Integer> nextEntries = new IdentityHashMap<>();

    int update(List<? extends List<T>> groups, int budget, BooleanSupplier hasTime,
        Predicate<T> isDirty, Predicate<T> rebuild) {
        Map<List<T>, Integer> active = new IdentityHashMap<>();
        for (List<T> group : groups) active.put(group, nextEntries.getOrDefault(group, 0));
        nextEntries = active;
        if (groups.isEmpty()) return 0;
        int[] examined = new int[groups.size()];
        int updated = 0;
        int skipped = 0;
        boolean visited = false;
        while (updated < budget && skipped < groups.size()) {
            if (visited && !hasTime.getAsBoolean()) return updated;
            nextGroup %= groups.size();
            int index = nextGroup;
            List<T> group = groups.get(index);
            nextGroup = (nextGroup + 1) % groups.size();
            T pending = null;
            while (examined[index] < group.size()) {
                if (visited && !hasTime.getAsBoolean()) return updated;
                visited = true;
                int cursor = nextEntries.get(group) % group.size();
                T entry = group.get(cursor);
                nextEntries.put(group, (cursor + 1) % group.size());
                examined[index]++;
                if (isDirty.test(entry)) {
                    pending = entry;
                    break;
                }
            }
            if (pending == null) {
                skipped++;
            } else {
                if (rebuild.test(pending)) updated++;
                skipped = 0;
            }
        }
        return updated;
    }
}

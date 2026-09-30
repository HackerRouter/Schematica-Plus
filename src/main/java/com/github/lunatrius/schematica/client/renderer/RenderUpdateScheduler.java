package com.github.lunatrius.schematica.client.renderer;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

final class RenderUpdateScheduler<T> {
    private int nextGroup;

    int update(List<? extends List<T>> groups, int budget, BooleanSupplier hasTime,
        Predicate<T> isDirty, Consumer<T> rebuild) {
        if (groups.isEmpty()) return 0;
        int updated = 0;
        int skipped = 0;
        while (updated < budget && skipped < groups.size() && (updated == 0 || hasTime.getAsBoolean())) {
            nextGroup %= groups.size();
            List<T> group = groups.get(nextGroup);
            nextGroup = (nextGroup + 1) % groups.size();
            T pending = null;
            for (T entry : group) {
                if (isDirty.test(entry)) {
                    pending = entry;
                    break;
                }
            }
            if (pending == null) {
                skipped++;
            } else {
                rebuild.accept(pending);
                updated++;
                skipped = 0;
            }
        }
        return updated;
    }
}

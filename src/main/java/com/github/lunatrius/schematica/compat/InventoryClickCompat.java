// Optional inventory click integration, by HackerRouter, 2026.
package com.github.lunatrius.schematica.compat;

import java.lang.reflect.Field;
import java.util.function.Supplier;

import com.github.lunatrius.schematica.reference.Reference;

public final class InventoryClickCompat {
    private static final Field BOGO_GUARD = findGuard();

    private InventoryClickCompat() {}

    private static Field findGuard() {
        try {
            return Class.forName("com.cleanroommc.bogosorter.ShortcutHandler").getField("SetCanTakeStack");
        } catch (ClassNotFoundException absent) {
            return null;
        } catch (ReflectiveOperationException | LinkageError error) {
            Reference.logger.warn("Could not find the inventory shortcut guard", error);
            return null;
        }
    }

    public static <T> T click(Supplier<T> action) {
        return withGuard(BOGO_GUARD, action);
    }

    static <T> T withGuard(Field guard, Supplier<T> action) {
        if (guard == null) return action.get();
        final boolean previous;
        try {
            previous = guard.getBoolean(null);
            guard.setBoolean(null, true);
        } catch (IllegalAccessException error) {
            throw new IllegalStateException("Could not enable inventory clicks", error);
        }
        try {
            return action.get();
        } finally {
            try {
                guard.setBoolean(null, previous);
            } catch (IllegalAccessException error) {
                throw new IllegalStateException("Could not restore the inventory shortcut guard", error);
            }
        }
    }
}

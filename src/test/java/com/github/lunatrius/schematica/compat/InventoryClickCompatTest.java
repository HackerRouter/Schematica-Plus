// Inventory shortcut guard regressions, by HackerRouter, 2026.
package com.github.lunatrius.schematica.compat;

import java.lang.reflect.Field;

import org.junit.Test;
import static org.junit.Assert.*;

public class InventoryClickCompatTest {
    public static boolean allowClicks;

    private static Field guard() throws Exception {
        return InventoryClickCompatTest.class.getField("allowClicks");
    }

    @Test public void enablesPredictionAndRestoresDisabledGuard() throws Exception {
        allowClicks = false;
        Object result = new Object();
        assertSame(result, InventoryClickCompat.withGuard(guard(), () -> {
            assertTrue(allowClicks);
            return result;
        }));
        assertFalse(allowClicks);
    }

    @Test public void preservesAlreadyEnabledAndNestedClicks() throws Exception {
        Field guard = guard();
        allowClicks = true;
        InventoryClickCompat.withGuard(guard, () -> InventoryClickCompat.withGuard(guard, () -> true));
        assertTrue(allowClicks);
        allowClicks = false;
        InventoryClickCompat.withGuard(guard, () -> {
            InventoryClickCompat.withGuard(guard, () -> true);
            assertTrue(allowClicks);
            return true;
        });
        assertFalse(allowClicks);
    }

    @Test public void restoresGuardWhenNativeClickThrows() throws Exception {
        allowClicks = false;
        Field guard = guard();
        RuntimeException failure = new RuntimeException("rejected click");
        assertSame(failure, assertThrows(RuntimeException.class,
            () -> InventoryClickCompat.withGuard(guard, () -> { throw failure; })));
        assertFalse(allowClicks);
    }

    @Test public void absenceDoesNotChangeNativeClickResult() {
        assertEquals(Integer.valueOf(7), InventoryClickCompat.withGuard(null, () -> 7));
    }
}

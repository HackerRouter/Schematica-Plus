// Unknown blocks met while decoding a schematic file, by HackerRouter, 2026.
package com.github.lunatrius.schematica.world.schematic;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** The block names (or states) a decode on this thread could not find in this game, with what replaced them. */
public final class ImportReport {
    private static final ThreadLocal<Map<String, String>> CURRENT = new ThreadLocal<>();
    private static final ThreadLocal<Map<String, String>> LAST = ThreadLocal.withInitial(Collections::emptyMap);

    private ImportReport() {}

    static void begin() { CURRENT.set(new LinkedHashMap<>()); }

    static void end() {
        Map<String, String> report = CURRENT.get();
        CURRENT.remove();
        LAST.set(report == null ? Collections.<String, String>emptyMap() : Collections.unmodifiableMap(report));
    }

    public static void unknownBlock(String name, String replacement) {
        Map<String, String> report = CURRENT.get();
        if (report != null && !report.containsKey(name)) report.put(name, replacement);
    }

    /** The report of the last decode on this thread: unknown name or state to the replacing block. */
    public static Map<String, String> last() { return LAST.get(); }
}

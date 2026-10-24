package com.github.lunatrius.schematica.client.verifier;

import java.util.LinkedHashMap;
import java.util.Map;

final class VerificationChanges {
    private static final int LIMIT = 4096;
    private final Map<Long, Integer> sections = new LinkedHashMap<>();
    private boolean all;

    synchronized void add(int x0, int y0, int z0, int x1, int y1, int z1) {
        if (all || x0 > x1 || y0 > y1 || z0 > z1 || y1 < 0 || y0 > 255) return;
        int cx0 = x0 >> 4, cx1 = x1 >> 4, cz0 = z0 >> 4, cz1 = z1 >> 4;
        if ((cx1 - (long) cx0 + 1) * (cz1 - (long) cz0 + 1) > LIMIT) { all = true; sections.clear(); return; }
        int mask = 0;
        for (int y = Math.max(0, y0) >> 4; y <= (Math.min(255, y1) >> 4); y++) mask |= 1 << y;
        for (int x = cx0; x <= cx1; x++) for (int z = cz0; z <= cz1; z++) {
            long key = (long) x << 32 | (z & 0xffffffffL);
            sections.put(key, sections.getOrDefault(key, 0) | mask);
            if (sections.size() > LIMIT) { all = true; sections.clear(); return; }
        }
    }

    synchronized VerificationChanges drain() {
        VerificationChanges result = new VerificationChanges();
        result.all = all;
        result.sections.putAll(sections);
        sections.clear(); all = false;
        return result;
    }

    void apply(VerificationScan scan) {
        if (all) { scan.changed(Integer.MIN_VALUE, 0, Integer.MIN_VALUE, Integer.MAX_VALUE, 255, Integer.MAX_VALUE); return; }
        for (Map.Entry<Long, Integer> entry : sections.entrySet()) {
            int x = (int) (entry.getKey() >> 32) << 4, z = (int) (long) entry.getKey() << 4;
            for (int y = 0; y < 16; y++) if ((entry.getValue() & 1 << y) != 0) {
                scan.changed(x, y << 4, z, x + 15, (y << 4) + 15, z + 15);
            }
        }
    }
}

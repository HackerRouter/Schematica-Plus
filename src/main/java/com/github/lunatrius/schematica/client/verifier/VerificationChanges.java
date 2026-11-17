package com.github.lunatrius.schematica.client.verifier;

import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.world.ChunkPosition;

final class VerificationChanges {
    private static final int LIMIT = 4096;
    private final Set<ChunkPosition> sections = new LinkedHashSet<>();
    private boolean all;

    synchronized void add(int x0, int y0, int z0, int x1, int y1, int z1) {
        if (all || x0 > x1 || y0 > y1 || z0 > z1) return;
        int cx0 = x0 >> 4, cx1 = x1 >> 4, cy0 = y0 >> 4, cy1 = y1 >> 4, cz0 = z0 >> 4, cz1 = z1 >> 4;
        long columns = (cx1 - (long) cx0 + 1) * (cz1 - (long) cz0 + 1);
        if (columns > LIMIT || cy1 - (long) cy0 + 1 > LIMIT / columns) { all = true; sections.clear(); return; }
        for (int x = cx0; x <= cx1; x++) for (int y = cy0; y <= cy1; y++) for (int z = cz0; z <= cz1; z++) {
            sections.add(new ChunkPosition(x, y, z));
            if (sections.size() > LIMIT) { all = true; sections.clear(); return; }
        }
    }

    synchronized VerificationChanges drain() {
        VerificationChanges result = new VerificationChanges();
        result.all = all;
        result.sections.addAll(sections);
        sections.clear(); all = false;
        return result;
    }

    void apply(VerificationScan scan) {
        if (all) { scan.changed(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE); return; }
        for (ChunkPosition section : sections) {
            int x = section.chunkPosX << 4, y = section.chunkPosY << 4, z = section.chunkPosZ << 4;
            scan.changed(x, y, z, x + 15, y + 15, z + 15);
        }
    }
}

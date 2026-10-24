package com.github.lunatrius.schematica.client.renderer;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.client.renderer.Tessellator;
import com.github.lunatrius.schematica.client.ClientWorkPool;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.reference.Reference;
import org.lwjgl.opengl.GL11;

final class SortedGeometryCache {
    private static final Set<Entry> ENTRIES = new LinkedHashSet<>();
    private static final Set<Entry> WAITING = new LinkedHashSet<>();
    private static final long CAPACITY = 32L * 1024 * 1024;
    private static long retained, frame;
    private final Entry[] passes = new Entry[2];

    static void beginFrame() {
        frame++;
        if (!GeometrySupport.supported(Tessellator.instance)) {
            while (!ENTRIES.isEmpty()) ENTRIES.iterator().next().delete();
        }
    }

    static void suspend() {
        while (!WAITING.isEmpty()) WAITING.iterator().next().cancelJob();
    }

    void replace(int pass, NativeGeometry geometry) {
        Entry previous = passes[pass];
        if (previous != null && !previous.deleted && previous.geometry.same(geometry)) return;
        if (previous != null) previous.delete();
        passes[pass] = null;
        if (geometry == null) return;
        Entry next = new Entry(geometry, pass);
        while (!ENTRIES.isEmpty() && retained + next.bytes > CAPACITY) ENTRIES.iterator().next().delete();
        passes[pass] = next;
        ENTRIES.add(next); retained += next.bytes;
    }

    void touch(double x, double y, double z) {
        for (Entry entry : passes) if (entry != null && !entry.deleted) {
            entry.frameSeen = frame; entry.x = x; entry.y = y; entry.z = z;
        }
    }

    int list(int pass, int fallback) {
        Entry entry = pass < passes.length ? passes[pass] : null;
        return entry == null || entry.deleted || entry.list == 0 ? fallback : entry.list;
    }

    void clear() {
        for (int pass = 0; pass < passes.length; pass++) {
            if (passes[pass] != null) passes[pass].delete();
            passes[pass] = null;
        }
    }

    static void update() {
        long deadline = System.nanoTime() + 3_000_000L;
        int bytes = 8 * 1024 * 1024, submitted = 0;
        int remaining = Math.min(ENTRIES.size(), 512);
        while (remaining-- > 0 && System.nanoTime() < deadline && !ENTRIES.isEmpty()) {
            Entry entry = ENTRIES.iterator().next();
            ENTRIES.remove(entry); ENTRIES.add(entry);
            if (entry.frameSeen != frame || entry.pass == 0 && !ConfigurationHandler.enableAlpha) {
                entry.cancelJob();
                continue;
            }
            try {
                if (entry.job != null && entry.job.isDone()) {
                    QuadOrder.Result result = entry.job.result();
                    int size = result.vertices.length * 4;
                    if (size > bytes) continue;
                    if (entry.list == 0 || !Arrays.equals(entry.order, result.order)) {
                        if (entry.list == 0) {
                            entry.list = GL11.glGenLists(1);
                            if (entry.list == 0) throw new IllegalStateException("Could not allocate a sorted display list");
                        }
                        try (SchematicRenderPass context = new SchematicRenderPass(entry.pass)) {
                            entry.geometry.upload(entry.list, result.vertices);
                        }
                        entry.order = result.order;
                        bytes -= size;
                    }
                    entry.lastX = entry.requestX; entry.lastY = entry.requestY; entry.lastZ = entry.requestZ;
                    entry.cancelJob();
                }
                if (entry.job == null && submitted < 4 && ClientWorkPool.INSTANCE.availableSlots() > 2
                    && entry.moved()) {
                    int[] vertices = entry.geometry.vertices;
                    double x = entry.x, y = entry.y, z = entry.z;
                    entry.job = ClientWorkPool.INSTANCE.submit(() -> QuadOrder.sort(vertices, x, y, z));
                    if (entry.job != null) {
                        WAITING.add(entry);
                        entry.requestX = x; entry.requestY = y; entry.requestZ = z;
                        submitted++;
                    }
                }
            } catch (RuntimeException error) {
                Reference.logger.warn("Could not sort schematic geometry; retaining native rendering", error);
                entry.delete();
            }
        }
    }

    private static final class Entry {
        NativeGeometry geometry;
        final int pass;
        final long bytes;
        long frameSeen = -1;
        double x, y, z, requestX, requestY, requestZ, lastX = Double.NaN, lastY, lastZ;
        int list;
        int[] order;
        boolean deleted;
        ClientWorkPool.Job<QuadOrder.Result> job;

        Entry(NativeGeometry geometry, int pass) {
            this.geometry = geometry; this.pass = pass;
            bytes = (long) geometry.vertices.length * 4 + geometry.vertices.length / 8 + 256;
        }

        boolean moved() {
            double dx = x - lastX, dy = y - lastY, dz = z - lastZ;
            return Double.isNaN(lastX) || dx * dx + dy * dy + dz * dz > 1;
        }

        void cancelJob() {
            if (job != null) { job.close(); job = null; }
            WAITING.remove(this);
        }

        void delete() {
            if (deleted) return;
            deleted = true;
            cancelJob();
            if (list != 0) { GL11.glDeleteLists(list, 1); list = 0; }
            if (ENTRIES.remove(this)) retained -= bytes;
            geometry = null; order = null;
        }
    }
}

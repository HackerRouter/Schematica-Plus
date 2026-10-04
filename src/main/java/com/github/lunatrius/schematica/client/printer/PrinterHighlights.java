// Printer block highlights after the behavior of litematica-printer: placed, broken and failed positions fade out, by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.printer;

import java.util.ArrayDeque;
import java.util.Iterator;

import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

import com.github.lunatrius.schematica.handler.ConfigurationHandler;

public final class PrinterHighlights {
    public enum Type {
        PLACE(1f, 1f, 1f), BREAK(1f, 0f, 0f), FAILED(0.5f, 0.5f, 0.5f);
        final float r, g, b;
        Type(float r, float g, float b) { this.r = r; this.g = g; this.b = b; }
    }

    private static final class Entry {
        final int x, y, z;
        final Type type;
        final long time;
        Entry(int x, int y, int z, Type type) { this.x = x; this.y = y; this.z = z; this.type = type; this.time = System.currentTimeMillis(); }
    }

    private static final ArrayDeque<Entry> ENTRIES = new ArrayDeque<>();

    private PrinterHighlights() {}

    public static synchronized void add(int x, int y, int z, Type type) {
        if (!ConfigurationHandler.printHighlight) return;
        ENTRIES.removeIf(entry -> entry.x == x && entry.y == y && entry.z == z);
        ENTRIES.add(new Entry(x, y, z, type));
        while (ENTRIES.size() > 512) ENTRIES.removeFirst();
    }

    public static synchronized void clear() { ENTRIES.clear(); }

    /** Outlines at 50% alpha fading out over printHighlightFade tenths of a second; world coordinates minus the camera. */
    public static synchronized void render(double cx, double cy, double cz) {
        if (ENTRIES.isEmpty()) return;
        long now = System.currentTimeMillis(), duration = Math.max(1, ConfigurationHandler.printHighlightFade) * 100L;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LINE_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthMask(false);
        if (ConfigurationHandler.printHighlightThroughWalls) GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glLineWidth(2f);
        Tessellator t = Tessellator.instance;
        t.startDrawing(GL11.GL_LINES);
        for (Iterator<Entry> it = ENTRIES.iterator(); it.hasNext();) {
            Entry entry = it.next();
            long age = now - entry.time;
            if (age > duration) { it.remove(); continue; }
            t.setColorRGBA_F(entry.type.r, entry.type.g, entry.type.b, 0.5f * (1f - age / (float) duration));
            double e = 0.002, x0 = entry.x - e - cx, y0 = entry.y - e - cy, z0 = entry.z - e - cz;
            double x1 = entry.x + 1 + e - cx, y1 = entry.y + 1 + e - cy, z1 = entry.z + 1 + e - cz;
            for (double y : new double[] {y0, y1}) for (double z : new double[] {z0, z1}) { t.addVertex(x0, y, z); t.addVertex(x1, y, z); }
            for (double x : new double[] {x0, x1}) for (double z : new double[] {z0, z1}) { t.addVertex(x, y0, z); t.addVertex(x, y1, z); }
            for (double x : new double[] {x0, x1}) for (double y : new double[] {y0, y1}) { t.addVertex(x, y, z0); t.addVertex(x, y, z1); }
        }
        t.draw();
        GL11.glPopAttrib();
        GL11.glDepthMask(true);
    }
}

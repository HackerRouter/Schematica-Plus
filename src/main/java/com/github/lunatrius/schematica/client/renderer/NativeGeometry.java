package com.github.lunatrius.schematica.client.renderer;

import java.lang.reflect.Field;
import java.util.Arrays;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.shader.TesselatorVertexState;
import cpw.mods.fml.relauncher.ReflectionHelper;
import org.lwjgl.opengl.GL11;

final class NativeGeometry {
    private static final String[][] NAMES = {
        {"rawBuffer", "field_78405_h"}, {"rawBufferIndex", "field_147569_p"}, {"vertexCount", "field_78406_i"},
        {"drawMode", "field_78409_u"}, {"hasTexture", "field_78400_o"}, {"hasBrightness", "field_78414_p"},
        {"hasNormals", "field_78413_q"}, {"hasColor", "field_78399_n"}
    };
    private static Field[] fields;
    private static boolean unavailable;
    final int[] vertices;
    private final boolean texture, brightness, normals, color;

    private NativeGeometry(int[] vertices, boolean texture, boolean brightness, boolean normals, boolean color) {
        this.vertices = vertices; this.texture = texture; this.brightness = brightness; this.normals = normals; this.color = color;
        for (int i = 0; i < vertices.length; i += 8) {
            if (!texture) { vertices[i + 3] = 0; vertices[i + 4] = 0; }
            if (!color) vertices[i + 5] = 0;
            if (!normals) vertices[i + 6] = 0;
            if (!brightness) vertices[i + 7] = 0;
        }
    }

    static NativeGeometry capture(Tessellator tessellator) {
        if (unavailable || tessellator.getClass() != Tessellator.class || !GeometrySupport.supported(tessellator)) return null;
        try {
            if (fields == null) {
                fields = new Field[NAMES.length];
                for (int i = 0; i < fields.length; i++) fields[i] = ReflectionHelper.findField(Tessellator.class, NAMES[i]);
            }
            int size = fields[1].getInt(tessellator), count = fields[2].getInt(tessellator);
            if (size == 0 || size > 1048576 || count % 4 != 0 || size != count * 8 || fields[3].getInt(tessellator) != GL11.GL_QUADS) return null;
            int[] data = (int[]) fields[0].get(tessellator);
            if (data == null || data.length < size) return null;
            return new NativeGeometry(Arrays.copyOf(data, size), fields[4].getBoolean(tessellator), fields[5].getBoolean(tessellator),
                fields[6].getBoolean(tessellator), fields[7].getBoolean(tessellator));
        } catch (ReflectiveOperationException | RuntimeException error) {
            unavailable = true;
            return null;
        }
    }

    boolean same(NativeGeometry other) {
        return other != null && texture == other.texture && brightness == other.brightness && normals == other.normals
            && color == other.color && Arrays.equals(vertices, other.vertices);
    }

    void upload(int list, int[] sorted) {
        GL11.glNewList(list, GL11.GL_COMPILE);
        try {
            Tessellator.instance.startDrawingQuads();
            Tessellator.instance.setVertexState(new TesselatorVertexState(sorted, sorted.length, sorted.length / 8,
                texture, brightness, normals, color));
            Tessellator.instance.draw();
        } finally { GL11.glEndList(); }
    }
}

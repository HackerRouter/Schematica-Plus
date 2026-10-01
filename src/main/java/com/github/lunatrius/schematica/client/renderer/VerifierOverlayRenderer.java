// SPDX-License-Identifier: LGPL-3.0-only
// Litematica verifier overlays, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import com.github.lunatrius.schematica.client.verifier.VerificationManager;
import com.github.lunatrius.schematica.client.verifier.VerificationScan.Marker;
import com.github.lunatrius.schematica.client.verifier.VerificationScan.Type;
import com.github.lunatrius.schematica.handler.RenderColors;
import com.github.lunatrius.schematica.handler.VerifierOverlaySettings;
import com.github.lunatrius.schematica.util.VoxelRayTrace;

public final class VerifierOverlayRenderer {
    private static final int[] EDGES = {0, 1, 1, 3, 3, 2, 2, 0, 4, 5, 5, 7, 7, 6, 6, 4, 0, 4, 1, 5, 2, 6, 3, 7};
    private static final int[] FACES = {0, 1, 3, 2, 4, 6, 7, 5, 0, 4, 5, 1, 2, 3, 7, 6, 0, 2, 6, 4, 1, 5, 7, 3};

    private VerifierOverlayRenderer() {}

    public static Marker target(Minecraft mc, VerificationManager.Session session, float partialTicks) {
        EntityLivingBase camera = mc.renderViewEntity;
        if (camera == null) return null;
        Vec3 eye = camera.getPosition(partialTicks), look = camera.getLook(partialTicks);
        return VoxelRayTrace.trace(eye, eye.addVector(look.xCoord * 128, look.yCoord * 128, look.zCoord * 128), session.markers::at);
    }

    public static void render(Minecraft mc, float partialTicks, double cameraX, double cameraY, double cameraZ) {
        VerificationManager.Session session = VerificationManager.INSTANCE.overlay(mc);
        if (session == null || !VerifierOverlaySettings.enabled || mc.gameSettings.hideGUI || session.markers.markers().isEmpty()) return;
        Marker target = target(mc, session, partialTicks);
        int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        try {
            mc.entityRenderer.disableLightmap(partialTicks);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_FOG);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDepthMask(false);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            Tessellator tessellator = Tessellator.instance;
            if (VerifierOverlaySettings.sides && VerifierOverlaySettings.alpha > 0) {
                tessellator.startDrawingQuads();
                for (Marker marker : session.markers.markers()) box(tessellator, marker, FACES, VerifierOverlaySettings.alpha, cameraX, cameraY, cameraZ);
                tessellator.draw();
            }
            GL11.glLineWidth(2);
            tessellator.startDrawing(GL11.GL_LINES);
            Marker previous = null;
            for (Marker marker : session.markers.markers()) {
                if (marker != target) box(tessellator, marker, EDGES, 1, cameraX, cameraY, cameraZ);
                if (VerifierOverlaySettings.connections && previous != null) {
                    tint(tessellator, marker, 1);
                    tessellator.addVertex(previous.x + 0.5 - cameraX, previous.y + 0.5 - cameraY, previous.z + 0.5 - cameraZ);
                    tessellator.addVertex(marker.x + 0.5 - cameraX, marker.y + 0.5 - cameraY, marker.z + 0.5 - cameraZ);
                }
                previous = marker;
            }
            tessellator.draw();
            if (target != null) {
                GL11.glLineWidth(6);
                tessellator.startDrawing(GL11.GL_LINES);
                box(tessellator, target, EDGES, 1, cameraX, cameraY, cameraZ);
                tessellator.draw();
            }
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
            GL11.glMatrixMode(mode);
        }
    }

    private static void box(Tessellator tessellator, Marker marker, int[] vertices, double alpha, double cameraX, double cameraY, double cameraZ) {
        tint(tessellator, marker, alpha);
        for (int vertex : vertices) tessellator.addVertex(marker.x - cameraX + ((vertex & 1) == 0 ? -0.002 : 1.002),
            marker.y - cameraY + ((vertex & 2) == 0 ? -0.002 : 1.002), marker.z - cameraZ + ((vertex & 4) == 0 ? -0.002 : 1.002));
    }

    private static void tint(Tessellator tessellator, Marker marker, double alpha) {
        Type type = marker.group.type;
        int color = (type == Type.WRONG_BLOCK ? RenderColors.WRONG_BLOCK : type == Type.WRONG_STATE ? RenderColors.WRONG_STATE
            : type == Type.EXTRA ? RenderColors.EXTRA : RenderColors.MISSING).color();
        tessellator.setColorRGBA(color >>> 16 & 255, color >>> 8 & 255, color & 255, (int) (alpha * 255));
    }
}

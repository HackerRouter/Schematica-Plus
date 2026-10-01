// SPDX-License-Identifier: LGPL-3.0-only
// Litematica rebuild targeting overlay and MaLiLib block targeting overlay, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraftforge.common.util.ForgeDirection;
import org.lwjgl.opengl.GL11;

import com.github.lunatrius.schematica.client.input.Hotkeys;
import com.github.lunatrius.schematica.handler.RenderColors;
import com.github.lunatrius.schematica.tool.RebuildDirection;
import com.github.lunatrius.schematica.tool.SchematicRebuild;
import com.github.lunatrius.schematica.tool.ToolManager;
import com.github.lunatrius.schematica.tool.ToolMode;

public final class RebuildOverlayRenderer {
    private static final double OFFSET = 0.01;
    private static final double[][] PARTS = {
        {0.25, 0.25, 0.75, 0.25, 0.75, 0.75, 0.25, 0.75},
        {0, 0, 0.25, 0.25, 0.25, 0.75, 0, 1},
        {1, 0, 0.75, 0.25, 0.75, 0.75, 1, 1},
        {0, 0, 0.25, 0.25, 0.75, 0.25, 1, 0},
        {0, 1, 0.25, 0.75, 0.75, 0.75, 1, 1}};

    private RebuildOverlayRenderer() {}

    public static void render(Minecraft mc, float partialTicks, double cameraX, double cameraY, double cameraZ) {
        if (ToolManager.getCurrentMode() != ToolMode.REBUILD || mc.gameSettings.hideGUI || mc.currentScreen != null || mc.thePlayer == null) return;
        RenderColors color;
        boolean direction = false;
        if (Hotkeys.held("schematicEditBreakPlaceAll")) color = RenderColors.REBUILD_BREAK;
        else if (Hotkeys.held("schematicEditBreakAllExcept")) color = RenderColors.REBUILD_EXCEPT;
        else if (Hotkeys.held("schematicEditBreakPlaceDirection")) { color = RenderColors.REBUILD_BREAK; direction = true; }
        else if (Hotkeys.held("schematicEditReplaceAll") || Hotkeys.held("schematicEditReplaceBlock")) color = RenderColors.REBUILD_REPLACE;
        else if (Hotkeys.held("schematicEditReplaceDirection")) { color = RenderColors.REBUILD_REPLACE; direction = true; }
        else return;
        SchematicRebuild.Target target = SchematicRebuild.trace(20);
        if (target == null) return;
        ForgeDirection side = target.face(), facing = SchematicRebuild.facing();
        double ox = target.x - cameraX, oy = target.y - cameraY, oz = target.z - cameraZ;
        int argb = color.color();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
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
            tessellator.startDrawingQuads();
            if (direction) {
                double[] hit = RebuildDirection.facePosition(side, facing, target.hitX - target.x, target.hitY - target.y, target.hitZ - target.z);
                tessellator.setColorRGBA(255, 255, 255, (int) (0.18F * 255));
                quad(tessellator, side, facing, ox, oy, oz, 0, 0, 1, 0, 1, 1, 0, 1);
                color(tessellator, argb);
                double[] p = PARTS[RebuildDirection.part(hit[0], hit[1]).ordinal()];
                quad(tessellator, side, facing, ox, oy, oz, p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7]);
            } else {
                color(tessellator, argb);
                quad(tessellator, side, facing, ox, oy, oz, 0, 0, 1, 0, 1, 1, 0, 1);
            }
            tessellator.draw();
            GL11.glLineWidth(1.6F);
            tessellator.startDrawing(GL11.GL_LINES);
            tessellator.setColorRGBA(255, 255, 255, 255);
            if (direction) {
                loop(tessellator, side, facing, ox, oy, oz, 0.25, 0.75);
                double[][] corners = {{0, 0, 0.25, 0.25}, {0, 1, 0.25, 0.75}, {1, 0, 0.75, 0.25}, {1, 1, 0.75, 0.75}};
                for (double[] c : corners) line(tessellator, side, facing, ox, oy, oz, c[0], c[1], c[2], c[3]);
            } else loop(tessellator, side, facing, ox, oy, oz, 0.125, 0.875);
            tessellator.draw();
        } finally {
            GL11.glPopAttrib();
        }
    }

    private static void color(Tessellator tessellator, int argb) {
        tessellator.setColorRGBA(argb >>> 16 & 255, argb >>> 8 & 255, argb & 255, argb >>> 24);
    }

    private static void quad(Tessellator tessellator, ForgeDirection side, ForgeDirection facing, double x, double y, double z, double... hv) {
        for (int i = 0; i < 8; i += 2) vertex(tessellator, side, facing, x, y, z, hv[i], hv[i + 1]);
    }

    private static void loop(Tessellator tessellator, ForgeDirection side, ForgeDirection facing, double x, double y, double z, double min, double max) {
        line(tessellator, side, facing, x, y, z, min, min, max, min);
        line(tessellator, side, facing, x, y, z, max, min, max, max);
        line(tessellator, side, facing, x, y, z, max, max, min, max);
        line(tessellator, side, facing, x, y, z, min, max, min, min);
    }

    private static void line(Tessellator tessellator, ForgeDirection side, ForgeDirection facing, double x, double y, double z,
                             double h1, double v1, double h2, double v2) {
        vertex(tessellator, side, facing, x, y, z, h1, v1);
        vertex(tessellator, side, facing, x, y, z, h2, v2);
    }

    private static void vertex(Tessellator tessellator, ForgeDirection side, ForgeDirection facing, double x, double y, double z, double h, double v) {
        double[] point = RebuildDirection.facePoint(side, facing, h, v, OFFSET);
        tessellator.addVertex(x + point[0], y + point[1], z + point[2]);
    }
}

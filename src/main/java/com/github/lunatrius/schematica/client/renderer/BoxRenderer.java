// SPDX-License-Identifier: LGPL-3.0-only
// Litematica OverlayRenderer.renderBoxes / renderSelectionBox and MaLiLib RenderUtils box helpers, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.renderer;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

import com.github.lunatrius.schematica.client.projects.SchematicProjects;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary;
import com.github.lunatrius.schematica.client.selection.AreaSelections;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.client.world.SubRegionPlacements;
import com.github.lunatrius.schematica.handler.RenderColors;
import com.github.lunatrius.schematica.handler.VisualSettings;
import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;
import com.github.lunatrius.schematica.proxy.ClientProxy;

/** Area selection and placement boxes in Litematica's colors and line widths, depth tested like MaLiLib's LEQUAL pipelines. */
final class BoxRenderer {
    static final int[] KELLY_COLORS = {0xFFB300, 0x803E75, 0xFF6800, 0xA6BDD7, 0xC10020, 0xCEA262, 0x817066, 0x007D34,
        0xF6768E, 0x00538A, 0xFF7A5C, 0x53377A, 0xFF8E00, 0xB32851, 0xF4C800, 0x7F180D, 0x93AA00, 0x593315, 0xF13A13, 0x232C16};
    private static final float[] POS1 = {1f, 0.0625f, 0.0625f, 1f}, POS2 = {0.0625f, 0.0625f, 1f, 1f}, OVERLAPPING = {1f, 0.0625f, 1f, 1f};
    private static final float[] AXIS_X = {1f, 0.25f, 0.25f, 1f}, AXIS_Y = {0.25f, 1f, 0.25f, 1f}, AXIS_Z = {0.25f, 0.25f, 1f, 1f};
    private static final float[] AREA = {1f, 1f, 1f, 1f}, PLACEMENT_SELECTED = {0x16 / 255f, 1f, 1f, 1f}, SELECTED_CORNER = {0f, 1f, 1f, 1f};
    private static final float[] AREA_ORIGIN = {1f, 0x90 / 255f, 0x10 / 255f, 1f};
    private static int nextColorIndex;

    private final double cx, cy, cz;

    BoxRenderer(double cameraX, double cameraY, double cameraZ) { cx = cameraX; cy = cameraY; cz = cameraZ; }

    /** SchematicPlacement.getNextBoxColor: the next Kelly color no loaded placement uses. */
    static int boxColor(SchematicWorld placement) {
        if (placement.boxColor != null) return placement.boxColor;
        Set<Integer> used = new HashSet<>();
        for (SchematicWorld other : ClientProxy.loadedSchematics) if (other.boxColor != null) used.add(other.boxColor);
        int color = KELLY_COLORS[nextColorIndex];
        for (int i = 0; i < KELLY_COLORS.length; i++) {
            color = KELLY_COLORS[nextColorIndex];
            nextColorIndex = (nextColorIndex + 1) % KELLY_COLORS.length;
            if (!used.contains(color)) break;
        }
        placement.boxColor = color;
        return color;
    }

    void render() {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LINE_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glDepthMask(false);
        try {
            float expand = 0.001f, blockBox = 2f, areaWidth = SchematicProjects.hasProjectOpen() ? 3f : 1.5f;
            AreaSelectionLibrary.Area area = AreaSelections.library().selected();
            if (ClientProxy.isRenderingGuide && VisualSettings.areaBoxes && area != null) {
                for (AreaSelectionLibrary.Box box : area.boxes()) {
                    boolean selected = box == area.selectedBox();
                    AreaSelectionLibrary.Corner corner = selected && !area.originSelected() ? area.selectedCorner() : null;
                    selectionBox(box.first(), box.second(), selected ? AXIS_X : AREA, selected ? AXIS_Y : AREA, selected ? AXIS_Z : AREA,
                        corner == AreaSelectionLibrary.Corner.FIRST ? SELECTED_CORNER : POS1, corner == AreaSelectionLibrary.Corner.SECOND ? SELECTED_CORNER : POS2,
                        corner, VisualSettings.areaBoxSides ? argb(RenderColors.AREA_SIDES.color()) : null, expand, blockBox, areaWidth);
                }
                Vector3i origin = area.manualOrigin();
                if (origin != null) {
                    if (area.originSelected()) areaSides(origin, origin, alpha(AREA_ORIGIN, 0.4f));
                    blockOutline(origin, expand, blockBox, area.originSelected() ? SELECTED_CORNER : AREA_ORIGIN);
                }
            }
            if (VisualSettings.placementBoxes) {
                for (SchematicWorld placement : ClientProxy.loadedSchematics) placementBoxes(placement, expand, blockBox);
            }
        } finally {
            GL11.glPopAttrib();
            GL11.glDepthMask(true);
        }
    }

    private void placementBoxes(SchematicWorld placement, float expand, float blockBox) {
        if (!placement.isEnabled()) return;
        boolean current = placement == ClientProxy.schematic;
        float[] color = rgb(boxColor(placement));
        SubRegionPlacements regions = placement.subregions();
        boolean origin = regions == null || regions.selected == null;
        float sideAlpha = (float) VisualSettings.placementBoxSideAlpha;
        if (regions == null) {
            Vector3i min = new Vector3i(placement.position.x, placement.position.y, placement.position.z);
            Vector3i max = new Vector3i(min.x + placement.getWidth() - 1, min.y + placement.getHeight() - 1, min.z + placement.getLength() - 1);
            float[] boxColor = current ? PLACEMENT_SELECTED : color;
            selectionBox(min, max, boxColor, boxColor, boxColor, boxColor, boxColor, null,
                VisualSettings.placementBoxSides ? alpha(boxColor, sideAlpha) : null, expand, 1f, 1f);
        } else for (SubRegionPlacements.Region region : regions.regions()) {
            if (!region.enabled) continue;
            boolean selected = current && (origin || region.name().equals(regions.selected));
            float[] boxColor = selected ? PLACEMENT_SELECTED : color;
            int[] c = placement.subregionCorners(region.name());
            selectionBox(new Vector3i(c[0], c[1], c[2]), new Vector3i(c[3], c[4], c[5]), boxColor, boxColor, boxColor, boxColor, boxColor,
                null, VisualSettings.placementBoxSides ? alpha(boxColor, sideAlpha) : null, expand, 1f, 1f);
        }
        float[] originColor = current && origin ? SELECTED_CORNER : color;
        com.github.lunatrius.schematica.api.SchematicOrigin o = placement.originPosition();
        blockOutline(new Vector3i(o.x, o.y, o.z), expand, blockBox, originColor);
        if (VisualSettings.enclosingBox && placement.placementSettings().enclosingBox && placement.hasEnabledRegions()) {
            Vector3i min = new Vector3i(placement.position.x, placement.position.y, placement.position.z);
            Vector3i max = new Vector3i(min.x + placement.getWidth() - 1, min.y + placement.getHeight() - 1, min.z + placement.getLength() - 1);
            areaOutline(min, max, 1f, originColor);
            if (VisualSettings.enclosingBoxSides) areaSides(min, max, alpha(originColor, sideAlpha));
        }
    }

    /** OverlayRenderer.renderSelectionBox. */
    private void selectionBox(Vector3i pos1, Vector3i pos2, float[] colorX, float[] colorY, float[] colorZ, float[] color1, float[] color2,
                              AreaSelectionLibrary.Corner corner, float[] sides, float expand, float blockBox, float areaWidth) {
        if (!pos1.equals(pos2)) {
            areaOutlineNoCorners(pos1, pos2, areaWidth, colorX, colorY, colorZ);
            if (sides != null) areaSides(pos1, pos2, sides);
            if (corner == AreaSelectionLibrary.Corner.FIRST) areaSides(pos1, pos1, alpha(POS1, 0.4f));
            else if (corner == AreaSelectionLibrary.Corner.SECOND) areaSides(pos2, pos2, alpha(POS2, 0.4f));
            blockOutline(pos1, expand, blockBox, color1);
            blockOutline(pos2, expand, blockBox, color2);
        } else {
            overlappingOutline(pos1, expand, blockBox, color1, color2, OVERLAPPING);
        }
    }

    private void blockOutline(Vector3i pos, float expand, float width, float[] color) {
        lines(width);
        cuboidEdges(pos.x - expand, pos.y - expand, pos.z - expand, pos.x + 1 + expand, pos.y + 1 + expand, pos.z + 1 + expand, color, color, color);
        Tessellator.instance.draw();
    }

    /** RenderUtils.renderBlockOutlineOverlapping: the three edges at the minimum corner, the three at the maximum, the rest. */
    private void overlappingOutline(Vector3i pos, float expand, float width, float[] c1, float[] c2, float[] c3) {
        double x0 = pos.x - expand - cx, y0 = pos.y - expand - cy, z0 = pos.z - expand - cz;
        double x1 = pos.x + 1 + expand - cx, y1 = pos.y + 1 + expand - cy, z1 = pos.z + 1 + expand - cz;
        lines(width);
        line(x0, y0, z0, x1, y0, z0, c1); line(x0, y0, z0, x0, y1, z0, c1); line(x0, y0, z0, x0, y0, z1, c1);
        line(x0, y1, z1, x1, y1, z1, c2); line(x1, y0, z1, x1, y1, z1, c2); line(x1, y1, z0, x1, y1, z1, c2);
        line(x0, y1, z0, x1, y1, z0, c3); line(x0, y0, z1, x1, y0, z1, c3); line(x1, y0, z0, x1, y1, z0, c3);
        line(x0, y0, z1, x0, y1, z1, c3); line(x1, y0, z0, x1, y0, z1, c3); line(x0, y1, z0, x0, y1, z1, c3);
        Tessellator.instance.draw();
    }

    private void areaOutline(Vector3i pos1, Vector3i pos2, float width, float[] color) {
        lines(width);
        double e = 0.001;
        cuboidEdges(Math.min(pos1.x, pos2.x) - e, Math.min(pos1.y, pos2.y) - e, Math.min(pos1.z, pos2.z) - e,
            Math.max(pos1.x, pos2.x) + 1 + e, Math.max(pos1.y, pos2.y) + 1 + e, Math.max(pos1.z, pos2.z) + 1 + e, color, color, color);
        Tessellator.instance.draw();
    }

    /** RenderUtils.renderAreaOutlineNoCorners: every edge, shortened by one block where pos1 or pos2 sits on its end. */
    private void areaOutlineNoCorners(Vector3i pos1, Vector3i pos2, float width, float[] colorX, float[] colorY, float[] colorZ) {
        int[] min = {Math.min(pos1.x, pos2.x), Math.min(pos1.y, pos2.y), Math.min(pos1.z, pos2.z)};
        int[] max = {Math.max(pos1.x, pos2.x), Math.max(pos1.y, pos2.y), Math.max(pos1.z, pos2.z)};
        float[][] colors = {colorX, colorY, colorZ};
        double e = 0.001;
        lines(width);
        for (int axis = 0; axis < 3; axis++) {
            int a = (axis + 1) % 3, b = (axis + 2) % 3;
            for (int sideA = 0; sideA < 2; sideA++) for (int sideB = 0; sideB < 2; sideB++) {
                int[] block = new int[3];
                block[a] = sideA == 0 ? min[a] : max[a];
                block[b] = sideB == 0 ? min[b] : max[b];
                block[axis] = min[axis];
                int start = corner(block, pos1, pos2) ? min[axis] + 1 : min[axis];
                block[axis] = max[axis];
                int end = corner(block, pos1, pos2) ? max[axis] : max[axis] + 1;
                if (end <= start) continue;
                double[] from = new double[3], to = new double[3];
                from[a] = to[a] = sideA == 0 ? min[a] - e : max[a] + 1 + e;
                from[b] = to[b] = sideB == 0 ? min[b] - e : max[b] + 1 + e;
                from[axis] = start - e;
                to[axis] = end + e;
                line(from[0] - cx, from[1] - cy, from[2] - cz, to[0] - cx, to[1] - cy, to[2] - cz, colors[axis]);
            }
        }
        Tessellator.instance.draw();
    }

    private static boolean corner(int[] block, Vector3i pos1, Vector3i pos2) {
        return block[0] == pos1.x && block[1] == pos1.y && block[2] == pos1.z || block[0] == pos2.x && block[1] == pos2.y && block[2] == pos2.z;
    }

    /** RenderUtils.renderAreaSides: translucent quads 0.002 outside the area. */
    private void areaSides(Vector3i pos1, Vector3i pos2, float[] color) {
        double e = 0.002;
        double x0 = Math.min(pos1.x, pos2.x) - e - cx, y0 = Math.min(pos1.y, pos2.y) - e - cy, z0 = Math.min(pos1.z, pos2.z) - e - cz;
        double x1 = Math.max(pos1.x, pos2.x) + 1 + e - cx, y1 = Math.max(pos1.y, pos2.y) + 1 + e - cy, z1 = Math.max(pos1.z, pos2.z) + 1 + e - cz;
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.setColorRGBA_F(color[0], color[1], color[2], color[3]);
        t.addVertex(x0, y0, z0); t.addVertex(x1, y0, z0); t.addVertex(x1, y0, z1); t.addVertex(x0, y0, z1);
        t.addVertex(x0, y1, z0); t.addVertex(x0, y1, z1); t.addVertex(x1, y1, z1); t.addVertex(x1, y1, z0);
        t.addVertex(x0, y0, z0); t.addVertex(x0, y1, z0); t.addVertex(x1, y1, z0); t.addVertex(x1, y0, z0);
        t.addVertex(x0, y0, z1); t.addVertex(x1, y0, z1); t.addVertex(x1, y1, z1); t.addVertex(x0, y1, z1);
        t.addVertex(x0, y0, z0); t.addVertex(x0, y0, z1); t.addVertex(x0, y1, z1); t.addVertex(x0, y1, z0);
        t.addVertex(x1, y0, z0); t.addVertex(x1, y1, z0); t.addVertex(x1, y1, z1); t.addVertex(x1, y0, z1);
        t.draw();
    }

    private void cuboidEdges(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, float[] colorX, float[] colorY, float[] colorZ) {
        double x0 = minX - cx, y0 = minY - cy, z0 = minZ - cz, x1 = maxX - cx, y1 = maxY - cy, z1 = maxZ - cz;
        for (double y : new double[] {y0, y1}) for (double z : new double[] {z0, z1}) line(x0, y, z, x1, y, z, colorX);
        for (double x : new double[] {x0, x1}) for (double z : new double[] {z0, z1}) line(x, y0, z, x, y1, z, colorY);
        for (double x : new double[] {x0, x1}) for (double y : new double[] {y0, y1}) line(x, y, z0, x, y, z1, colorZ);
    }

    private static void lines(float width) {
        GL11.glLineWidth(width);
        Tessellator.instance.startDrawing(GL11.GL_LINES);
    }

    private static void line(double x0, double y0, double z0, double x1, double y1, double z1, float[] color) {
        Tessellator t = Tessellator.instance;
        t.setColorRGBA_F(color[0], color[1], color[2], color[3]);
        t.addVertex(x0, y0, z0);
        t.addVertex(x1, y1, z1);
    }

    private static float[] rgb(int color) { return new float[] {(color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, 1f}; }

    private static float[] argb(int color) { return new float[] {(color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, (color >>> 24) / 255f}; }

    private static float[] alpha(float[] color, float alpha) { return new float[] {color[0], color[1], color[2], alpha}; }
}

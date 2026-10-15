// 3D schematic preview for the file browsers, after the idea of Tech Utils' renderPreview and QuickCraft's 3D preview, by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.browser;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;

import org.lwjgl.opengl.GL11;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.client.renderer.SchematicRenderPass;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.reference.Reference;

/**
 * A loaded schematic drawn into a square of a GUI: its blocks are compiled into display lists one 16^3 section at a
 * time within a per-frame budget. Navigation is local to the preview widget.
 * Blocks drawn only by tile entity renderers (chests, signs) are not shown.
 */
final class SchematicPreview3D {
    static final long MAX_VOLUME = 4_000_000L;
    private static final int SECTION = 16;
    private static final long BUDGET_NANOS = 6_000_000L;

    private final ISchematic schematic;
    private PreviewWorld world;
    private final int sectionsX, sectionsY, sectionsZ;
    private final List<int[]> lists = new ArrayList<>();
    private int next;
    private boolean failed;
    final PreviewCamera camera;

    SchematicPreview3D(ISchematic schematic) {
        this.schematic = schematic;
        sectionsX = (schematic.getWidth() + SECTION - 1) / SECTION;
        sectionsY = (schematic.getHeight() + SECTION - 1) / SECTION;
        sectionsZ = (schematic.getLength() + SECTION - 1) / SECTION;
        camera = new PreviewCamera((float) Math.sqrt((double) schematic.getWidth() * schematic.getWidth()
            + (double) schematic.getHeight() * schematic.getHeight() + (double) schematic.getLength() * schematic.getLength()));
    }

    private int sections() { return sectionsX * sectionsY * sectionsZ; }

    /** Builds display lists until the frame budget is used; true while sections are left. */
    private boolean build() {
        if (next >= sections()) return false;
        long deadline = System.nanoTime() + BUDGET_NANOS;
        if (world == null) world = new PreviewWorld(schematic);
        RenderBlocks renderer = new RenderBlocks(world);
        Tessellator tessellator = Tessellator.instance;
        tessellator.setTranslation(0, 0, 0);
        Minecraft mc = Minecraft.getMinecraft();
        int ambientOcclusion = mc.gameSettings.ambientOcclusion;
        mc.gameSettings.ambientOcclusion = 0;
        try {
            while (next < sections() && System.nanoTime() < deadline) {
                int index = next++;
                int sx = index % sectionsX, sy = index / sectionsX % sectionsY, sz = index / (sectionsX * sectionsY);
                int[] passes = {-1, -1};
                for (int pass = 0; pass < 2; pass++) {
                    int list = GL11.glGenLists(1);
                    GL11.glNewList(list, GL11.GL_COMPILE);
                    boolean any = false;
                    try (SchematicRenderPass context = new SchematicRenderPass(pass)) {
                        tessellator.startDrawingQuads();
                        for (int y = sy * SECTION; y < Math.min(schematic.getHeight(), (sy + 1) * SECTION); y++) {
                            for (int z = sz * SECTION; z < Math.min(schematic.getLength(), (sz + 1) * SECTION); z++) {
                                for (int x = sx * SECTION; x < Math.min(schematic.getWidth(), (sx + 1) * SECTION); x++) {
                                    any |= drawBlock(renderer, pass, x, y, z);
                                }
                            }
                        }
                        tessellator.draw();
                    } finally {
                        GL11.glEndList();
                    }
                    if (any) passes[pass] = list;
                    else GL11.glDeleteLists(list, 1);
                }
                if (passes[0] >= 0 || passes[1] >= 0) lists.add(passes);
            }
        } finally {
            mc.gameSettings.ambientOcclusion = ambientOcclusion;
        }
        return next < sections();
    }

    private boolean drawBlock(RenderBlocks renderer, int pass, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        if (block.getMaterial() == net.minecraft.block.material.Material.air || block.getRenderType() < 0) return false;
        if (!block.canRenderInPass(pass)) return false;
        renderer.clearOverrideBlockTexture();
        renderer.lockBlockBounds = false;
        renderer.renderAllFaces = false;
        renderer.renderFromInside = false;
        renderer.flipTexture = false;
        renderer.enableAO = false;
        renderer.uvRotateTop = renderer.uvRotateBottom = renderer.uvRotateEast = renderer.uvRotateWest = 0;
        renderer.uvRotateNorth = renderer.uvRotateSouth = 0;
        try {
            return renderer.renderBlockByRenderType(block, x, y, z);
        } catch (RuntimeException | LinkageError error) {
            Reference.logger.debug("Could not draw {} in the schematic preview", block, error);
            return false;
        }
    }

    boolean failed() { return failed; }

    /** Uses a separate perspective viewport, retaining the GUI's matrices and clipping. */
    void draw(Minecraft mc, int x, int y, int width, int height) {
        if (failed) return;
        boolean building;
        try {
            building = build();
        } catch (RuntimeException error) {
            failed = true;
            Reference.logger.warn("Could not build the schematic preview", error);
            return;
        }
        ScaledResolution scaled = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int factor = scaled.getScaleFactor();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_SCISSOR_BIT | GL11.GL_COLOR_BUFFER_BIT
            | GL11.GL_CURRENT_BIT | GL11.GL_VIEWPORT_BIT | GL11.GL_TRANSFORM_BIT | GL11.GL_TEXTURE_BIT);
        int left = x * factor, bottom = mc.displayHeight - (y + height) * factor;
        int right = left + width * factor, top = bottom + height * factor;
        if (GL11.glIsEnabled(GL11.GL_SCISSOR_TEST)) {
            java.nio.IntBuffer clip = org.lwjgl.BufferUtils.createIntBuffer(16);
            GL11.glGetInteger(GL11.GL_SCISSOR_BOX, clip);
            left = Math.max(left, clip.get(0)); bottom = Math.max(bottom, clip.get(1));
            right = Math.min(right, clip.get(0) + clip.get(2)); top = Math.min(top, clip.get(1) + clip.get(3));
        }
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(left, bottom, Math.max(0, right - left), Math.max(0, top - bottom));
        GL11.glViewport(x * factor, mc.displayHeight - (y + height) * factor, width * factor, height * factor);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        double near = 0.05, half = near * PreviewCamera.TAN_HALF_FOV, aspect = (double) width / Math.max(1, height);
        GL11.glFrustum(-half * aspect, half * aspect, -half, half, near,
            camera.extent * 8 + camera.distance() + Math.abs(camera.z));
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        try {
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glDepthMask(true);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_FOG);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f);
            GL11.glColor4f(1, 1, 1, 1);
            mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
            GL11.glTranslatef(camera.x, camera.y, camera.z - camera.distance());
            GL11.glRotatef(camera.pitch, 1, 0, 0);
            GL11.glRotatef(camera.yaw, 0, 1, 0);
            GL11.glTranslatef(-schematic.getWidth() / 2f, -schematic.getHeight() / 2f, -schematic.getLength() / 2f);
            for (int[] list : lists) if (list[0] >= 0) call(mc, list[0]);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDepthMask(false);
            for (int[] list : lists) if (list[1] >= 0) call(mc, list[1]);
            GL11.glDepthMask(true);
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        } finally {
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
        if (building) {
            String text = (next * 100 / Math.max(1, sections())) + "%";
            mc.fontRenderer.drawStringWithShadow(text, x + 3, y + height - 11, 0xFFAAAAAA);
        }
    }

    /** A block renderer may have bound another texture inside a list. */
    private static void call(Minecraft mc, int list) {
        mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        GL11.glCallList(list);
    }

    void delete() {
        for (int[] list : lists) for (int id : list) if (id >= 0) GL11.glDeleteLists(id, 1);
        lists.clear();
        next = sections();
    }

    /**
     * The schematic as a world, so block entities are bound and get their saved look back as in placements; full light
     * and air outside it.
     */
    private static final class PreviewWorld extends SchematicWorld {
        PreviewWorld(ISchematic schematic) { super(schematic); }
        private boolean inside(int x, int y, int z) {
            return x >= 0 && y >= 0 && z >= 0 && x < getWidth() && y < getHeight() && z < getLength();
        }
        @Override public Block getBlock(int x, int y, int z) {
            if (!inside(x, y, z)) return Blocks.air;
            Block block = super.getBlock(x, y, z);
            return block == null ? Blocks.air : block;
        }
        @Override public TileEntity getTileEntity(int x, int y, int z) { return inside(x, y, z) ? super.getTileEntity(x, y, z) : null; }
        @Override public int getBlockMetadata(int x, int y, int z) { return inside(x, y, z) ? super.getBlockMetadata(x, y, z) : 0; }
        @Override public int getLightBrightnessForSkyBlocks(int x, int y, int z, int light) { return 15 << 20 | 15 << 4; }
    }
}

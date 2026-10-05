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
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.reference.Reference;

/**
 * A loaded schematic drawn into a square of a GUI: its blocks are compiled into display lists one 16^3 section at a
 * time within a per-frame budget, then shown turning slowly; dragging inside the square turns it, the wheel zooms.
 * Blocks drawn only by tile entity renderers (chests, signs) are not shown.
 */
final class SchematicPreview3D {
    static final long MAX_VOLUME = 4_000_000L;
    private static final int SECTION = 16;
    private static final long BUDGET_NANOS = 6_000_000L;

    private final ISchematic schematic;
    private final IBlockAccess access;
    private final int sectionsX, sectionsY, sectionsZ;
    private final List<int[]> lists = new ArrayList<>();
    private int next;
    private boolean failed;
    private float yaw = 45, pitch = 30, zoom = 1;
    private int dragX = -1, dragY;
    private long lastFrame;

    SchematicPreview3D(ISchematic schematic) {
        this.schematic = schematic;
        this.access = new Access(schematic);
        sectionsX = (schematic.getWidth() + SECTION - 1) / SECTION;
        sectionsY = (schematic.getHeight() + SECTION - 1) / SECTION;
        sectionsZ = (schematic.getLength() + SECTION - 1) / SECTION;
    }

    private int sections() { return sectionsX * sectionsY * sectionsZ; }

    /** Builds display lists until the frame budget is used; true while sections are left. */
    private boolean build() {
        long deadline = System.nanoTime() + BUDGET_NANOS;
        RenderBlocks renderer = new RenderBlocks(access);
        Tessellator tessellator = Tessellator.instance;
        tessellator.setTranslation(0, 0, 0);
        while (next < sections() && System.nanoTime() < deadline) {
            int index = next++;
            int sx = index % sectionsX, sy = index / sectionsX % sectionsY, sz = index / (sectionsX * sectionsY);
            int[] passes = {-1, -1};
            for (int pass = 0; pass < 2; pass++) {
                int list = GL11.glGenLists(1);
                GL11.glNewList(list, GL11.GL_COMPILE);
                tessellator.startDrawingQuads();
                boolean any = false;
                for (int y = sy * SECTION; y < Math.min(schematic.getHeight(), (sy + 1) * SECTION); y++) {
                    for (int z = sz * SECTION; z < Math.min(schematic.getLength(), (sz + 1) * SECTION); z++) {
                        for (int x = sx * SECTION; x < Math.min(schematic.getWidth(), (sx + 1) * SECTION); x++) {
                            Block block = access.getBlock(x, y, z);
                            if (block.getMaterial() == net.minecraft.block.material.Material.air || block.getRenderType() < 0) continue;
                            if (!block.canRenderInPass(pass)) continue;
                            try {
                                any |= renderer.renderBlockByRenderType(block, x, y, z);
                            } catch (RuntimeException | LinkageError error) {
                                Reference.logger.debug("Could not draw {} in the schematic preview", block, error);
                            }
                        }
                    }
                }
                tessellator.draw();
                GL11.glEndList();
                if (any) passes[pass] = list;
                else GL11.glDeleteLists(list, 1);
            }
            if (passes[0] >= 0 || passes[1] >= 0) lists.add(passes);
        }
        return next < sections();
    }

    /** Draws the preview into the square (GUI coordinates), building more of it first. */
    void draw(Minecraft mc, int x, int y, int size, int mouseX, int mouseY) {
        if (failed) return;
        boolean building;
        try {
            building = build();
        } catch (RuntimeException error) {
            failed = true;
            Reference.logger.warn("Could not build the schematic preview", error);
            return;
        }
        long now = System.currentTimeMillis();
        boolean inside = mouseX >= x && mouseY >= y && mouseX < x + size && mouseY < y + size;
        if (inside && Mouse.isButtonDown(0)) {
            if (dragX >= 0) { yaw += (mouseX - dragX) * 1.5f; pitch = Math.max(-90, Math.min(90, pitch + (mouseY - dragY) * 1.5f)); }
            dragX = mouseX; dragY = mouseY;
        } else {
            dragX = -1;
            if (lastFrame > 0) yaw += (now - lastFrame) * 0.02f;
        }
        lastFrame = now;

        ScaledResolution scaled = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int factor = scaled.getScaleFactor();
        float extent = (float) Math.sqrt((double) schematic.getWidth() * schematic.getWidth()
            + (double) schematic.getHeight() * schematic.getHeight() + (double) schematic.getLength() * schematic.getLength());
        float scale = size / Math.max(1, extent) * zoom;

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_SCISSOR_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * factor, mc.displayHeight - (y + size) * factor, size * factor, size * factor);
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f);
        GL11.glColor4f(1, 1, 1, 1);
        mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        GL11.glPushMatrix();
        GL11.glTranslatef(x + size / 2f, y + size / 2f, 0);
        GL11.glScalef(scale, -scale, scale);
        GL11.glRotatef(pitch, 1, 0, 0);
        GL11.glRotatef(yaw, 0, 1, 0);
        GL11.glTranslatef(-schematic.getWidth() / 2f, -schematic.getHeight() / 2f, -schematic.getLength() / 2f);
        for (int[] list : lists) if (list[0] >= 0) GL11.glCallList(list[0]);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthMask(false);
        for (int[] list : lists) if (list[1] >= 0) GL11.glCallList(list[1]);
        GL11.glDepthMask(true);
        GL11.glPopMatrix();
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glPopAttrib();
        if (building) {
            String text = (next * 100 / Math.max(1, sections())) + "%";
            mc.fontRenderer.drawStringWithShadow(text, x + 3, y + size - 11, 0xFFAAAAAA);
        }
    }

    /** Mouse wheel over the preview: zoom in or out. */
    void zoom(int wheel) { zoom = Math.max(0.25f, Math.min(4f, zoom * (wheel > 0 ? 1.25f : 0.8f))); }

    void delete() {
        for (int[] list : lists) for (int id : list) if (id >= 0) GL11.glDeleteLists(id, 1);
        lists.clear();
        next = sections();
    }

    /** The schematic as a block access for RenderBlocks: air outside it, full light, plains biome. */
    private static final class Access implements IBlockAccess {
        private final ISchematic schematic;
        Access(ISchematic schematic) { this.schematic = schematic; }
        private boolean inside(int x, int y, int z) {
            return x >= 0 && y >= 0 && z >= 0 && x < schematic.getWidth() && y < schematic.getHeight() && z < schematic.getLength();
        }
        @Override public Block getBlock(int x, int y, int z) {
            if (!inside(x, y, z)) return Blocks.air;
            Block block = schematic.getBlock(x, y, z);
            return block == null ? Blocks.air : block;
        }
        @Override public TileEntity getTileEntity(int x, int y, int z) { return inside(x, y, z) ? schematic.getTileEntity(x, y, z) : null; }
        @Override public int getLightBrightnessForSkyBlocks(int x, int y, int z, int light) { return 15 << 20 | 15 << 4; }
        @Override public int getBlockMetadata(int x, int y, int z) { return inside(x, y, z) ? schematic.getBlockMetadata(x, y, z) : 0; }
        @Override public int isBlockProvidingPowerTo(int x, int y, int z, int direction) { return 0; }
        @Override public boolean isAirBlock(int x, int y, int z) { return getBlock(x, y, z).getMaterial() == net.minecraft.block.material.Material.air; }
        @Override public BiomeGenBase getBiomeGenForCoords(int x, int z) { return BiomeGenBase.plains; }
        @Override public int getHeight() { return 256; }
        @Override public boolean extendedLevelsInChunkCache() { return false; }
        @Override public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean fallback) {
            if (!inside(x, y, z)) return fallback;
            try {
                return getBlock(x, y, z).isSideSolid(this, x, y, z, side);
            } catch (RuntimeException error) {
                return fallback;
            }
        }
    }
}

package com.github.lunatrius.schematica.client.gui.framework;

import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public final class MinecraftUiDraw implements UiDraw, AutoCloseable {

    private final Minecraft minecraft;
    private final int scale;
    private final int matrixMode;
    private final Deque<UiBounds> clips = new ArrayDeque<>();

    public MinecraftUiDraw(Minecraft minecraft) {
        this.minecraft = minecraft;
        scale = new ScaledResolution(minecraft, minecraft.displayWidth, minecraft.displayHeight).getScaleFactor();
        matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        UiBounds framebuffer = new UiBounds(0, 0, minecraft.displayWidth, minecraft.displayHeight);
        if (GL11.glIsEnabled(GL11.GL_SCISSOR_TEST)) {
            IntBuffer box = BufferUtils.createIntBuffer(16);
            GL11.glGetInteger(GL11.GL_SCISSOR_BOX, box);
            framebuffer = framebuffer.intersect(new UiBounds(box.get(0), box.get(1), box.get(2), box.get(3)));
        }
        clips.push(framebuffer);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.01F);
    }

    @Override
    public void fill(UiBounds bounds, int color) {
        if (!bounds.isEmpty()) Gui.drawRect(bounds.x, bounds.y, bounds.right(), bounds.bottom(), color);
    }

    private void prepareTextured() {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1, 1, 1, 1);
    }

    @Override
    public void text(String text, int x, int y, int color) {
        prepareTextured();
        minecraft.fontRenderer.drawStringWithShadow(text, x, y, color);
    }

    @Override
    public int textWidth(String text) {
        return minecraft.fontRenderer.getStringWidth(text);
    }

    @Override
    public String trim(String text, int width) {
        return minecraft.fontRenderer.trimStringToWidth(text, Math.max(0, width));
    }

    public void texture(ResourceLocation texture, UiBounds destination, int u, int v, int sourceWidth,
        int sourceHeight, int textureWidth, int textureHeight) {
        prepareTextured();
        minecraft.getTextureManager().bindTexture(texture);
        double left = (double) u / textureWidth;
        double top = (double) v / textureHeight;
        double right = (double) (u + sourceWidth) / textureWidth;
        double bottom = (double) (v + sourceHeight) / textureHeight;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(destination.x, destination.bottom(), 0, left, bottom);
        tessellator.addVertexWithUV(destination.right(), destination.bottom(), 0, right, bottom);
        tessellator.addVertexWithUV(destination.right(), destination.y, 0, right, top);
        tessellator.addVertexWithUV(destination.x, destination.y, 0, left, top);
        tessellator.draw();
    }

    @Override
    public Clip clip(UiBounds bounds) {
        UiBounds scissor = clips.peek().intersect(bounds.toScissor(scale, minecraft.displayWidth, minecraft.displayHeight));
        clips.push(scissor);
        applyClip(scissor);
        return () -> {
            clips.pop();
            applyClip(clips.peek());
        };
    }

    private void applyClip(UiBounds bounds) {
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    @Override
    public void close() {
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
        GL11.glMatrixMode(matrixMode);
    }
}

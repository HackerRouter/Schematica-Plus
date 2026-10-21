// SPDX-License-Identifier: LGPL-3.0-only
// Litematica GuiSchematicManager.PreviewGenerator, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.browser;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.shader.Framebuffer;

import com.github.lunatrius.schematica.client.util.InfoUtils;
import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.util.MessageType;
import com.github.lunatrius.schematica.world.schematic.SchematicMetadata;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/** Sets a schematic preview from the next screenshot key press, or from a thumb.png beside the file. */
public final class SchematicPreview {
    public static final SchematicPreview INSTANCE = new SchematicPreview();
    public static final int SIZE = 120;

    private File pending;
    private boolean keyWasDown;

    private SchematicPreview() {}

    public boolean hasPending() { return pending != null; }

    public void request(File file) { pending = file; }

    public boolean cancel() {
        boolean had = pending != null;
        pending = null;
        return had;
    }

    /** The centered square of an image scaled to size x size, as opaque ARGB pixels. */
    public static int[] square(BufferedImage image, int size) {
        UiBounds crop = crop(image.getWidth(), image.getHeight());
        BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(image, 0, 0, size, size, crop.x, crop.y, crop.right(), crop.bottom(), null);
        graphics.dispose();
        int[] pixels = scaled.getRGB(0, 0, size, size, null, 0, size);
        for (int i = 0; i < pixels.length; i++) pixels[i] |= 0xFF000000;
        return pixels;
    }

    public static UiBounds crop(int width, int height) {
        int side = Math.min(width, height);
        return new UiBounds((width - side) / 2, (height - side) / 2, side, side);
    }

    private void renderFrame(Minecraft mc) {
        UiBounds frame = crop(mc.displayWidth, mc.displayHeight);
        int thickness = 2 * new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaleFactor();
        int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, mc.displayWidth, mc.displayHeight, 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        try {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            Gui.drawRect(frame.x, frame.y, frame.right(), frame.y + thickness, 0xFFFFFFFF);
            Gui.drawRect(frame.x, frame.bottom() - thickness, frame.right(), frame.bottom(), 0xFFFFFFFF);
            Gui.drawRect(frame.x, frame.y, frame.x + thickness, frame.bottom(), 0xFFFFFFFF);
            Gui.drawRect(frame.right() - thickness, frame.y, frame.right(), frame.bottom(), 0xFFFFFFFF);
        } finally {
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glMatrixMode(mode);
            GL11.glPopAttrib();
        }
    }

    public static void store(File file, int[] pixels) throws IOException {
        if (!SchematicMetadata.validPreview(pixels)) throw new IOException("Invalid preview dimensions");
        SchematicFileEdits.edit(file, metadata -> metadata.setIntArray("PreviewImageData", pixels));
    }

    /** Ctrl + Alt + Shift on the button: thumb.png from the schematic's directory. */
    public static void fromThumbnail(File file) throws IOException {
        File image = new File(file.getParentFile(), "thumb.png");
        BufferedImage read = image.isFile() ? ImageIO.read(image) : null;
        if (read == null) throw new IOException("Image 'thumb.png' not found");
        store(file, square(read, SIZE));
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        int key = mc.gameSettings.keyBindScreenshot.getKeyCode();
        boolean down = key > 0 && key < Keyboard.KEYBOARD_SIZE && Keyboard.isKeyDown(key);
        boolean pressed = down && !keyWasDown;
        keyWasDown = down;
        if (pending == null || mc.currentScreen != null || mc.thePlayer == null) return;
        if (!pressed) {
            renderFrame(mc);
            return;
        }
        File file = pending;
        pending = null;
        try {
            store(file, square(screenshot(mc), SIZE));
            InfoUtils.showGuiOrInGameMessage(MessageType.SUCCESS, "litematica.info.schematic_manager.preview.success");
        } catch (IOException | RuntimeException e) {
            Reference.logger.warn("Exception while creating preview image", e);
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_load.cant_read_file", file.getName());
        }
    }

    /** The current frame, read like ScreenShotHelper.saveScreenshot. */
    private static BufferedImage screenshot(Minecraft mc) {
        Framebuffer framebuffer = mc.getFramebuffer();
        boolean fbo = OpenGlHelper.isFramebufferEnabled();
        int width = fbo ? framebuffer.framebufferTextureWidth : mc.displayWidth;
        int height = fbo ? framebuffer.framebufferTextureHeight : mc.displayHeight;
        IntBuffer buffer = BufferUtils.createIntBuffer(width * height);
        int[] pixels = new int[width * height];
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        if (fbo) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, framebuffer.framebufferTexture);
            GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, buffer);
        } else {
            GL11.glReadPixels(0, 0, width, height, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, buffer);
        }
        buffer.get(pixels);
        TextureUtil.func_147953_a(pixels, width, height);
        if (!fbo) {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            image.setRGB(0, 0, width, height, pixels, 0, width);
            return image;
        }
        BufferedImage image = new BufferedImage(framebuffer.framebufferWidth, framebuffer.framebufferHeight, BufferedImage.TYPE_INT_RGB);
        int offset = framebuffer.framebufferTextureHeight - framebuffer.framebufferHeight;
        for (int y = offset; y < framebuffer.framebufferTextureHeight; y++) {
            for (int x = 0; x < framebuffer.framebufferWidth; x++) {
                image.setRGB(x, y - offset, pixels[y * framebuffer.framebufferTextureWidth + x]);
            }
        }
        return image;
    }
}

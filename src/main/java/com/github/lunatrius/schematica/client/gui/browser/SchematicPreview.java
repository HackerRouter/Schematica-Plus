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

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;

import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.world.schematic.SchematicFiles;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Sets a .litematic preview from the next screenshot key press, or from a thumb.png beside the file. */
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
        int side = Math.min(image.getWidth(), image.getHeight());
        int x = (image.getWidth() - side) / 2, y = (image.getHeight() - side) / 2;
        BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(image, 0, 0, size, size, x, y, x + side, y + side, null);
        graphics.dispose();
        int[] pixels = scaled.getRGB(0, 0, size, size, null, 0, size);
        for (int i = 0; i < pixels.length; i++) pixels[i] |= 0xFF000000;
        return pixels;
    }

    public static void store(File file, int[] pixels) throws IOException {
        SchematicFiles.editLitematicMetadata(file, metadata -> metadata.setIntArray("PreviewImageData", pixels));
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
        if (!pressed || pending == null || mc.currentScreen != null || mc.thePlayer == null) return;
        File file = pending;
        pending = null;
        try {
            store(file, square(screenshot(mc), SIZE));
            message(mc, "litematica.info.schematic_manager.preview.success", EnumChatFormatting.GREEN);
        } catch (IOException | RuntimeException e) {
            Reference.logger.warn("Exception while creating preview image", e);
            message(mc, "litematica.error.schematic_load.cant_read_file", EnumChatFormatting.RED, file.getName());
        }
    }

    private static void message(Minecraft mc, String key, EnumChatFormatting color, Object... arguments) {
        ChatComponentTranslation text = new ChatComponentTranslation(key, arguments);
        text.getChatStyle().setColor(color);
        mc.thePlayer.addChatMessage(text);
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

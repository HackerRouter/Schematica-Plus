package com.github.lunatrius.schematica.client.gui.browser;

import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiOpenEvent;

import com.github.lunatrius.schematica.client.gui.framework.UiBounds;

import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class SchematicPreviewTest {
    @After public void tearDown() {
        SchematicPreview.INSTANCE.cancel();
    }

    @Test public void frameAndStoredPreviewUseTheSameCenteredCropAtAnyAspectRatio() {
        for (int[] size : new int[][] {{17, 10}, {10, 17}, {10, 10}}) {
            UiBounds crop = SchematicPreview.crop(size[0], size[1]);
            BufferedImage image = new BufferedImage(size[0], size[1], BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < size[1]; y++) for (int x = 0; x < size[0]; x++) image.setRGB(x, y, x << 16 | y);
            int[] pixels = SchematicPreview.square(image, crop.width);
            assertEquals(Math.min(size[0], size[1]), crop.width);
            assertEquals(crop.width, crop.height);
            assertEquals(image.getRGB(crop.x, crop.y), pixels[0]);
            assertEquals(image.getRGB(crop.right() - 1, crop.bottom() - 1), pixels[pixels.length - 1]);
        }
    }

    @Test public void escapeCancelsPendingPreviewInGameWithoutOpeningPauseMenu() throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        Object unsafe = theUnsafe.get(null);
        Method allocate = unsafeClass.getMethod("allocateInstance", Class.class);

        Minecraft mc = (Minecraft) allocate.invoke(unsafe, Minecraft.class);
        EntityClientPlayerMP player = (EntityClientPlayerMP) allocate.invoke(unsafe, EntityClientPlayerMP.class);
        mc.thePlayer = player;
        mc.currentScreen = null;

        Field theMinecraft = Minecraft.class.getDeclaredField("theMinecraft");
        theMinecraft.setAccessible(true);
        Object previousMc = theMinecraft.get(null);
        theMinecraft.set(null, mc);
        try {
            File target = new File("sample.schematic");
            SchematicPreview.INSTANCE.request(target);

            GuiOpenEvent focusLoss = new GuiOpenEvent(new GuiIngameMenu()) {
                @Override public boolean isCancelable() { return true; }
            };
            assertFalse(SchematicPreview.INSTANCE.handleEscapeGuiOpen(mc, focusLoss, false));
            assertFalse(focusLoss.isCanceled());
            assertTrue(SchematicPreview.INSTANCE.hasPending());

            GuiOpenEvent escapeMenu = new GuiOpenEvent(new GuiIngameMenu()) {
                @Override public boolean isCancelable() { return true; }
            };
            assertTrue(SchematicPreview.INSTANCE.handleEscapeGuiOpen(mc, escapeMenu, true));
            assertTrue(escapeMenu.isCanceled());
            assertFalse(SchematicPreview.INSTANCE.hasPending());
            assertFalse(SchematicPreview.INSTANCE.handleEscapeKey(mc, true));

            SchematicPreview.INSTANCE.request(target);
            mc.currentScreen = new GuiScreen() {};
            assertFalse(SchematicPreview.INSTANCE.handleEscapeKey(mc, true));
            assertTrue(SchematicPreview.INSTANCE.hasPending());

            mc.currentScreen = null;
            assertTrue(SchematicPreview.INSTANCE.handleEscapeKey(mc, true));
            assertFalse(SchematicPreview.INSTANCE.hasPending());
        } finally {
            theMinecraft.set(null, previousMc);
        }
    }
}

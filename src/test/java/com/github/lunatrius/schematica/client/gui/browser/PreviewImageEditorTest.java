package com.github.lunatrius.schematica.client.gui.browser;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

import com.github.lunatrius.schematica.world.schematic.SchematicMetadata;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

public class PreviewImageEditorTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    private static final int RED = 0xFFFF0000;
    private static final int GREEN = 0xFF00FF00;
    private static final int BLUE = 0xFF0000FF;
    private static final int YELLOW = 0xFFFFFF00;

    private static BufferedImage quadrants(int side) {
        BufferedImage image = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
        int half = side / 2;
        for (int y = 0; y < side; y++) {
            for (int x = 0; x < side; x++) {
                int color = y < half ? (x < half ? RED : GREEN) : (x < half ? BLUE : YELLOW);
                image.setRGB(x, y, color);
            }
        }
        return image;
    }

    @Test public void loadsImageFileAndDefaultsToCenteredSquareCrop() throws IOException {
        BufferedImage wide = new BufferedImage(80, 40, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 40; y++) {
            for (int x = 0; x < 80; x++) {
                wide.setRGB(x, y, x >= 20 && x < 60 ? GREEN : RED);
            }
        }
        File file = temporary.newFile("sample.png");
        assertTrue(ImageIO.write(wide, "png", file));

        PreviewImageEditor editor = new PreviewImageEditor();
        editor.loadFile(file);
        assertTrue(editor.hasImage());
        assertEquals(80, editor.sourceWidth());
        assertEquals(40, editor.sourceHeight());
        assertEquals(20.0, editor.cropX(), 0.001);
        assertEquals(0.0, editor.cropY(), 0.001);
        assertEquals(40.0, editor.cropSize(), 0.001);
        assertEquals(PreviewImageEditor.DEFAULT_SIZE, editor.outputSize());

        int[] pixels = editor.renderPixels();
        assertTrue(SchematicMetadata.validPreview(pixels));
        assertEquals(PreviewImageEditor.DEFAULT_SIZE * PreviewImageEditor.DEFAULT_SIZE, pixels.length);
        int side = PreviewImageEditor.DEFAULT_SIZE;
        assertEquals(GREEN, pixels[(side / 2) * side + (side / 2)]);
        assertEquals(GREEN, pixels[(side / 2) * side + 10]);
        assertEquals(GREEN, pixels[(side / 2) * side + side - 10]);
    }

    @Test public void mirrorAndFixedRotationProduceExactQuadrantOrientation() {
        PreviewImageEditor editor = new PreviewImageEditor();
        editor.setImage(quadrants(16));
        editor.setOutputSize(16);

        editor.rotateFixed(90);
        assertEquals(90.0, editor.rotationDegrees(), 0.001);
        BufferedImage rotated = editor.renderResult(true);
        assertEquals(BLUE, rotated.getRGB(3, 3));
        assertEquals(RED, rotated.getRGB(12, 3));
        assertEquals(YELLOW, rotated.getRGB(3, 12));
        assertEquals(GREEN, rotated.getRGB(12, 12));

        editor.flipHorizontal();
        BufferedImage flippedH = editor.renderResult(true);
        assertEquals(RED, flippedH.getRGB(3, 3));
        assertEquals(BLUE, flippedH.getRGB(12, 3));
        assertEquals(GREEN, flippedH.getRGB(3, 12));
        assertEquals(YELLOW, flippedH.getRGB(12, 12));

        editor.flipVertical();
        BufferedImage flippedV = editor.renderResult(true);
        assertEquals(GREEN, flippedV.getRGB(3, 3));
        assertEquals(YELLOW, flippedV.getRGB(12, 3));
        assertEquals(RED, flippedV.getRGB(3, 12));
        assertEquals(BLUE, flippedV.getRGB(12, 12));
    }

    @Test public void freeAngleRotationExpandsOrientedBoundsAndStaysValid() {
        PreviewImageEditor editor = new PreviewImageEditor();
        editor.setImage(quadrants(40));
        editor.setRotationDegrees(45.0);
        assertEquals(45.0, editor.rotationDegrees(), 0.001);
        assertTrue(editor.orientedWidth() > 40);
        assertTrue(editor.orientedHeight() > 40);

        BufferedImage canvas = editor.renderOrientedPreview(64);
        assertNotNull(canvas);
        assertTrue(Math.max(canvas.getWidth(), canvas.getHeight()) <= 64);

        int[] pixels = editor.renderPixels();
        assertTrue(SchematicMetadata.validPreview(pixels));
        assertEquals(0xFF000000, pixels[0] & 0xFF000000);
    }

    @Test public void cropMoveZoomCornerResizeAndFitRemainBounded() {
        PreviewImageEditor editor = new PreviewImageEditor();
        editor.setImage(quadrants(64));
        editor.setOutputSize(32);

        editor.zoomCrop(0.5);
        assertEquals(32.0, editor.cropSize(), 0.001);
        assertEquals(16.0, editor.cropX(), 0.001);
        assertEquals(16.0, editor.cropY(), 0.001);

        editor.moveCrop(-100.0, -100.0);
        assertEquals(0.0, editor.cropX(), 0.001);
        assertEquals(0.0, editor.cropY(), 0.001);
        BufferedImage topLeftCrop = editor.renderResult(true);
        assertEquals(RED, topLeftCrop.getRGB(16, 16));

        editor.resizeCropCorner(PreviewImageEditor.CORNER_BR, 48.0, 48.0);
        assertEquals(48.0, editor.cropSize(), 0.001);
        assertEquals(0.0, editor.cropX(), 0.001);
        assertEquals(0.0, editor.cropY(), 0.001);

        editor.setCropFromDrag(32.0, 32.0, 64.0, 64.0);
        assertEquals(32.0, editor.cropX(), 0.001);
        assertEquals(32.0, editor.cropY(), 0.001);
        assertEquals(32.0, editor.cropSize(), 0.001);
        BufferedImage bottomRightCrop = editor.renderResult(true);
        assertEquals(YELLOW, bottomRightCrop.getRGB(16, 16));

        editor.setImage(new BufferedImage(80, 40, BufferedImage.TYPE_INT_ARGB));
        editor.fitFullImage();
        assertEquals(80.0, editor.cropSize(), 0.001);
        assertEquals(0.0, editor.cropX(), 0.001);
        assertEquals(-20.0, editor.cropY(), 0.001);
    }

    @Test public void outputSizeAndBorderApplyCleanlyAndClampWithinLimits() {
        PreviewImageEditor editor = new PreviewImageEditor();
        editor.setImage(quadrants(64));
        editor.setOutputSize(999);
        assertEquals(PreviewImageEditor.MAX_SIZE, editor.outputSize());
        editor.setOutputSize(4);
        assertEquals(PreviewImageEditor.MIN_SIZE, editor.outputSize());

        editor.setOutputSize(64);
        editor.setBorderWidth(6);
        editor.setBorderColor(0x00123456);
        assertEquals(6, editor.borderWidth());
        assertEquals(0xFF123456, editor.borderColor());

        BufferedImage result = editor.renderResult(true);
        assertEquals(64, result.getWidth());
        assertEquals(64, result.getHeight());
        assertEquals(0xFF123456, result.getRGB(0, 0));
        assertEquals(0xFF123456, result.getRGB(63, 63));
        assertEquals(0xFF123456, result.getRGB(5, 32));
        assertEquals(RED, result.getRGB(10, 10));
        assertEquals(YELLOW, result.getRGB(53, 53));

        editor.setOutputSize(16);
        assertTrue(editor.borderWidth() <= editor.maxBorderWidth());
        assertTrue(editor.outputSize() - 2 * editor.borderWidth() >= 8);
    }

    @Test public void loadsExistingPreviewPixelsAndRejectsInvalidFiles() throws IOException {
        PreviewImageEditor editor = new PreviewImageEditor();
        int[] existing = new int[32 * 32];
        for (int i = 0; i < existing.length; i++) existing[i] = BLUE;
        editor.loadPixels(existing);
        assertTrue(editor.hasImage());
        assertEquals(32, editor.outputSize());
        assertEquals(BLUE, editor.renderPixels()[0]);

        File nonImage = temporary.newFile("not_an_image.txt");
        assertThrows(IOException.class, () -> editor.loadFile(nonImage));
        assertTrue(NativeImagePicker.isSupportedImageName("photo.PNG"));
        assertTrue(NativeImagePicker.isSupportedImageName("thumb.jpeg"));
        assertFalse(NativeImagePicker.isSupportedImageName("build.schematic"));
    }
}

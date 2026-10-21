package com.github.lunatrius.schematica.client.gui.browser;

import java.awt.image.BufferedImage;
import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import org.junit.Test;
import static org.junit.Assert.*;

public class SchematicPreviewTest {
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
}

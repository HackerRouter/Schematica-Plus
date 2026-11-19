package com.github.lunatrius.schematica.client.gui.browser;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

import com.github.lunatrius.schematica.world.schematic.SchematicMetadata;

final class PreviewImageEditor {
    static final int DEFAULT_SIZE = SchematicPreview.SIZE;
    static final int MIN_SIZE = 16;
    static final int MAX_SIZE = SchematicMetadata.MAX_PREVIEW_SIDE;
    static final int MAX_BORDER = 64;
    static final int BACKGROUND_COLOR = 0xFF10151C;
    static final int DEFAULT_BORDER_COLOR = 0xFFFFFFFF;
    static final int CORNER_TL = 0;
    static final int CORNER_TR = 1;
    static final int CORNER_BL = 2;
    static final int CORNER_BR = 3;
    private static final int WORK_MAX_SIDE = 512;
    private static final int MAX_SOURCE_SIDE = 4096;

    private BufferedImage source;
    private BufferedImage previewSource;
    private boolean mirrored;
    private double rotationDegrees;
    private double cropX;
    private double cropY;
    private double cropSize;
    private int outputSize = DEFAULT_SIZE;
    private int borderWidth;
    private int borderColor = DEFAULT_BORDER_COLOR;

    boolean hasImage() {
        return source != null;
    }

    int sourceWidth() {
        return source == null ? 0 : source.getWidth();
    }

    int sourceHeight() {
        return source == null ? 0 : source.getHeight();
    }

    boolean mirrored() {
        return mirrored;
    }

    double rotationDegrees() {
        return rotationDegrees;
    }

    double cropX() {
        return cropX;
    }

    double cropY() {
        return cropY;
    }

    double cropSize() {
        return cropSize;
    }

    int outputSize() {
        return outputSize;
    }

    int borderWidth() {
        return clampedBorderWidth();
    }

    int maxBorderWidth() {
        return Math.min(MAX_BORDER, Math.max(0, outputSize / 2 - 4));
    }

    int borderColor() {
        return borderColor;
    }

    void loadFile(File file) throws IOException {
        if (file == null || !file.isFile() || !file.canRead()) {
            throw new IOException("Image file cannot be read");
        }
        BufferedImage loaded = ImageIO.read(file);
        if (loaded == null || loaded.getWidth() <= 0 || loaded.getHeight() <= 0) {
            throw new IOException("Unsupported or unreadable image file");
        }
        setImage(loaded, false);
    }

    void loadPixels(int[] pixels) {
        if (pixels == null || !SchematicMetadata.validPreview(pixels)) return;
        int side = (int) Math.sqrt(pixels.length);
        BufferedImage image = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, side, side, pixels, 0, side);
        outputSize = clampInt(side, MIN_SIZE, MAX_SIZE);
        setImage(image, true);
    }

    void setImage(BufferedImage image) {
        setImage(image, false);
    }

    private void setImage(BufferedImage image, boolean preserveOutputSize) {
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
            throw new IllegalArgumentException("Image must have positive dimensions");
        }
        source = normalizeImage(image, MAX_SOURCE_SIDE);
        previewSource = Math.max(source.getWidth(), source.getHeight()) > WORK_MAX_SIDE
            ? normalizeImage(source, WORK_MAX_SIDE) : source;
        mirrored = false;
        rotationDegrees = 0.0;
        if (!preserveOutputSize) outputSize = DEFAULT_SIZE;
        borderWidth = 0;
        borderColor = DEFAULT_BORDER_COLOR;
        resetCrop();
    }

    private static BufferedImage normalizeImage(BufferedImage input, int maxSide) {
        int w = input.getWidth();
        int h = input.getHeight();
        int targetW = w;
        int targetH = h;
        if (Math.max(w, h) > maxSide) {
            double scale = (double) maxSide / Math.max(w, h);
            targetW = Math.max(1, (int) Math.round(w * scale));
            targetH = Math.max(1, (int) Math.round(h * scale));
        }
        if (targetW == w && targetH == h && input.getType() == BufferedImage.TYPE_INT_ARGB) {
            BufferedImage copy = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = copy.createGraphics();
            try {
                g.drawImage(input, 0, 0, null);
            } finally {
                g.dispose();
            }
            return copy;
        }
        BufferedImage normalized = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = normalized.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(input, 0, 0, targetW, targetH, null);
        } finally {
            g.dispose();
        }
        return normalized;
    }

    void resetAll() {
        mirrored = false;
        rotationDegrees = 0.0;
        outputSize = DEFAULT_SIZE;
        borderWidth = 0;
        borderColor = DEFAULT_BORDER_COLOR;
        resetCrop();
    }

    void resetCrop() {
        if (source == null) {
            cropX = 0;
            cropY = 0;
            cropSize = 0;
            return;
        }
        int ow = orientedWidth();
        int oh = orientedHeight();
        cropSize = Math.min(ow, oh);
        cropX = (ow - cropSize) / 2.0;
        cropY = (oh - cropSize) / 2.0;
        clampCrop();
    }

    void fitFullImage() {
        if (source == null) return;
        int ow = orientedWidth();
        int oh = orientedHeight();
        cropSize = Math.max(ow, oh);
        cropX = (ow - cropSize) / 2.0;
        cropY = (oh - cropSize) / 2.0;
        clampCrop();
    }

    void flipHorizontal() {
        if (source == null) return;
        int ow = orientedWidth();
        rotationDegrees = normalizeAngle(-rotationDegrees);
        mirrored = !mirrored;
        cropX = ow - cropX - cropSize;
        clampCrop();
    }

    void flipVertical() {
        if (source == null) return;
        int oh = orientedHeight();
        rotationDegrees = normalizeAngle(180.0 - rotationDegrees);
        mirrored = !mirrored;
        cropY = oh - cropY - cropSize;
        clampCrop();
    }

    void rotateFixed(int deltaDegrees) {
        setRotationDegrees(rotationDegrees + deltaDegrees);
    }

    void setRotationDegrees(double degrees) {
        double next = normalizeAngle(degrees);
        if (source == null) {
            rotationDegrees = next;
            return;
        }
        int oldW = orientedWidth();
        int oldH = orientedHeight();
        double relCenterX = oldW > 0 ? (cropX + cropSize / 2.0) / oldW : 0.5;
        double relCenterY = oldH > 0 ? (cropY + cropSize / 2.0) / oldH : 0.5;
        double oldMin = Math.max(1, Math.min(oldW, oldH));
        double relSize = cropSize / oldMin;
        rotationDegrees = next;
        int newW = orientedWidth();
        int newH = orientedHeight();
        double newMin = Math.max(1, Math.min(newW, newH));
        cropSize = relSize * newMin;
        cropX = relCenterX * newW - cropSize / 2.0;
        cropY = relCenterY * newH - cropSize / 2.0;
        clampCrop();
    }

    void moveCrop(double dx, double dy) {
        if (source == null) return;
        cropX += dx;
        cropY += dy;
        clampCrop();
    }

    void zoomCrop(double scaleFactor) {
        if (source == null || scaleFactor <= 0) return;
        double cx = cropX + cropSize / 2.0;
        double cy = cropY + cropSize / 2.0;
        cropSize = clampDouble(cropSize * scaleFactor, minCropSize(), maxCropSize());
        cropX = cx - cropSize / 2.0;
        cropY = cy - cropSize / 2.0;
        clampCrop();
    }

    void resizeCropCorner(int corner, double targetX, double targetY) {
        if (source == null) return;
        double minSize = minCropSize();
        double maxSize = maxCropSize();
        double anchorX;
        double anchorY;
        double size;
        switch (corner) {
            case CORNER_TL:
                anchorX = cropX + cropSize;
                anchorY = cropY + cropSize;
                size = clampDouble(Math.max(anchorX - targetX, anchorY - targetY), minSize, maxSize);
                cropX = anchorX - size;
                cropY = anchorY - size;
                cropSize = size;
                break;
            case CORNER_TR:
                anchorX = cropX;
                anchorY = cropY + cropSize;
                size = clampDouble(Math.max(targetX - anchorX, anchorY - targetY), minSize, maxSize);
                cropX = anchorX;
                cropY = anchorY - size;
                cropSize = size;
                break;
            case CORNER_BL:
                anchorX = cropX + cropSize;
                anchorY = cropY;
                size = clampDouble(Math.max(anchorX - targetX, targetY - anchorY), minSize, maxSize);
                cropX = anchorX - size;
                cropY = anchorY;
                cropSize = size;
                break;
            case CORNER_BR:
            default:
                anchorX = cropX;
                anchorY = cropY;
                size = clampDouble(Math.max(targetX - anchorX, targetY - anchorY), minSize, maxSize);
                cropX = anchorX;
                cropY = anchorY;
                cropSize = size;
                break;
        }
        clampCrop();
    }

    void setCropFromDrag(double startX, double startY, double endX, double endY) {
        if (source == null) return;
        double dx = endX - startX;
        double dy = endY - startY;
        double size = clampDouble(Math.max(Math.abs(dx), Math.abs(dy)), minCropSize(), maxCropSize());
        cropX = dx >= 0 ? startX : startX - size;
        cropY = dy >= 0 ? startY : startY - size;
        cropSize = size;
        clampCrop();
    }

    void setOutputSize(int size) {
        outputSize = clampInt(size, MIN_SIZE, MAX_SIZE);
        borderWidth = clampedBorderWidth();
    }

    void setBorderWidth(int width) {
        borderWidth = clampInt(width, 0, maxBorderWidth());
    }

    void setBorderColor(int color) {
        borderColor = color | 0xFF000000;
    }

    int orientedWidth() {
        if (source == null) return 0;
        int w = source.getWidth();
        int h = source.getHeight();
        double rad = Math.toRadians(rotationDegrees);
        double absCos = Math.abs(Math.cos(rad));
        double absSin = Math.abs(Math.sin(rad));
        if (absSin < 1e-6) return w;
        if (absCos < 1e-6) return h;
        return Math.max(1, (int) Math.ceil(w * absCos + h * absSin));
    }

    int orientedHeight() {
        if (source == null) return 0;
        int w = source.getWidth();
        int h = source.getHeight();
        double rad = Math.toRadians(rotationDegrees);
        double absCos = Math.abs(Math.cos(rad));
        double absSin = Math.abs(Math.sin(rad));
        if (absSin < 1e-6) return h;
        if (absCos < 1e-6) return w;
        return Math.max(1, (int) Math.ceil(w * absSin + h * absCos));
    }

    double minCropSize() {
        if (source == null) return 1.0;
        int minSide = Math.min(orientedWidth(), orientedHeight());
        return Math.max(1.0, Math.min(minSide, Math.max(8.0, minSide / 16.0)));
    }

    double maxCropSize() {
        if (source == null) return 1.0;
        return Math.max(orientedWidth(), orientedHeight());
    }

    BufferedImage renderOrientedPreview(int maxSide) {
        if (source == null) return null;
        int ow = orientedWidth();
        int oh = orientedHeight();
        double scale = Math.max(ow, oh) > maxSide ? (double) maxSide / Math.max(ow, oh) : 1.0;
        int targetW = Math.max(1, (int) Math.round(ow * scale));
        int targetH = Math.max(1, (int) Math.round(oh * scale));
        BufferedImage input = previewSource != null ? previewSource : source;
        BufferedImage canvas = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        try {
            g.setColor(new Color(BACKGROUND_COLOR, true));
            g.fillRect(0, 0, targetW, targetH);
            applyQualityHints(g, !isQuarterTurn());
            AffineTransform transform = new AffineTransform();
            transform.translate(targetW / 2.0, targetH / 2.0);
            transform.scale((double) targetW / ow, (double) targetH / oh);
            appendSourceOrientation(transform, input);
            g.drawImage(input, transform, null);
        } finally {
            g.dispose();
        }
        return canvas;
    }

    BufferedImage renderResult(boolean highQuality) {
        if (source == null) return null;
        int size = outputSize;
        int border = clampedBorderWidth();
        int inner = Math.max(1, size - 2 * border);
        int ow = orientedWidth();
        int oh = orientedHeight();
        BufferedImage input = highQuality || previewSource == null ? source : previewSource;
        BufferedImage result = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        try {
            g.setColor(new Color(BACKGROUND_COLOR, true));
            g.fillRect(0, 0, size, size);
            g.setClip(border, border, inner, inner);
            applyQualityHints(g, highQuality || !isQuarterTurn());
            AffineTransform transform = new AffineTransform();
            transform.translate(border, border);
            transform.scale(inner / Math.max(1.0, cropSize), inner / Math.max(1.0, cropSize));
            transform.translate(-cropX, -cropY);
            transform.translate(ow / 2.0, oh / 2.0);
            appendSourceOrientation(transform, input);
            g.drawImage(input, transform, null);
            g.setClip(null);
            if (border > 0) {
                g.setColor(new Color(borderColor | 0xFF000000, true));
                g.fillRect(0, 0, size, border);
                g.fillRect(0, size - border, size, border);
                g.fillRect(0, border, border, inner);
                g.fillRect(size - border, border, border, inner);
            }
        } finally {
            g.dispose();
        }
        return result;
    }

    int[] renderPixels() {
        BufferedImage result = renderResult(true);
        if (result == null) return null;
        int size = result.getWidth();
        int[] pixels = result.getRGB(0, 0, size, size, null, 0, size);
        for (int i = 0; i < pixels.length; i++) pixels[i] |= 0xFF000000;
        return pixels;
    }

    private void appendSourceOrientation(AffineTransform transform, BufferedImage input) {
        transform.rotate(Math.toRadians(rotationDegrees));
        if (mirrored) transform.scale(-1.0, 1.0);
        transform.scale((double) source.getWidth() / input.getWidth(), (double) source.getHeight() / input.getHeight());
        transform.translate(-input.getWidth() / 2.0, -input.getHeight() / 2.0);
    }

    private void applyQualityHints(Graphics2D g, boolean smooth) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            smooth ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    private boolean isQuarterTurn() {
        double rad = Math.toRadians(rotationDegrees);
        return Math.abs(Math.sin(rad)) < 1e-6 || Math.abs(Math.cos(rad)) < 1e-6;
    }

    private void clampCrop() {
        if (source == null) return;
        int ow = orientedWidth();
        int oh = orientedHeight();
        cropSize = clampDouble(cropSize, minCropSize(), maxCropSize());
        if (cropSize <= ow) cropX = clampDouble(cropX, 0.0, ow - cropSize);
        else cropX = clampDouble(cropX, ow - cropSize, 0.0);
        if (cropSize <= oh) cropY = clampDouble(cropY, 0.0, oh - cropSize);
        else cropY = clampDouble(cropY, oh - cropSize, 0.0);
    }

    private int clampedBorderWidth() {
        return clampInt(borderWidth, 0, maxBorderWidth());
    }

    static double normalizeAngle(double degrees) {
        double value = ((degrees + 180.0) % 360.0 + 360.0) % 360.0 - 180.0;
        if (Math.abs(value) < 1e-9) return 0.0;
        if (Math.abs(value + 180.0) < 1e-9 && degrees > 0) return 180.0;
        return value;
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}

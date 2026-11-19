package com.github.lunatrius.schematica.client.gui.browser;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import com.github.lunatrius.schematica.client.gui.config.ColorPickerPanel;
import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiDraw;
import com.github.lunatrius.schematica.client.gui.framework.UiInput;
import com.github.lunatrius.schematica.client.gui.framework.UiIntegerField;
import com.github.lunatrius.schematica.client.gui.framework.UiLabel;
import com.github.lunatrius.schematica.client.gui.framework.UiPanel;
import com.github.lunatrius.schematica.client.gui.framework.UiScreen;
import com.github.lunatrius.schematica.client.gui.framework.UiTextField;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.gui.framework.UiWidget;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.util.MessageType;

import org.lwjgl.input.Keyboard;

public final class GuiSchematicPreviewEditor extends UiScreen {
    private static final int HANDLE_RADIUS = 6;
    private static final int CANVAS_MAX_TEXTURE = 512;

    private final GuiScreen returnScreen;
    private final File schematicFile;
    private final int[] initialPreview;
    final PreviewImageEditor editor = new PreviewImageEditor();

    private UiButton chooseButton;
    private UiTextField pathField;
    private UiButton loadButton;
    private CropCanvas canvas;
    private ResultPreview resultPreview;
    private UiLabel dimensionLabel;
    private UiButton centerCropButton;
    private UiButton fitCropButton;
    private UiButton flipHButton;
    private UiButton flipVButton;
    private UiButton rotateLeftButton;
    private UiButton rotateRightButton;
    private UiButton rotateZeroButton;
    private IntSlider angleSlider;
    private UiIntegerField angleField;
    private IntSlider sizeSlider;
    private UiIntegerField sizeField;
    private IntSlider borderSlider;
    private UiIntegerField borderField;
    private UiButton borderColorButton;
    private UiButton resetButton;
    private UiLabel statusLabel;
    private UiButton saveButton;
    private UiButton cancelButton;

    private Future<NativeImagePicker.Result> pickerFuture;
    private ResourceLocation orientedTexture;
    private DynamicTexture orientedDynamic;
    private int orientedTexW;
    private int orientedTexH;
    private ResourceLocation resultTexture;
    private DynamicTexture resultDynamic;
    private int resultTexSize;
    private boolean orientedDirty;
    private boolean resultDirty;
    private boolean syncingFields = true;
    private String statusText = "";

    public GuiSchematicPreviewEditor(GuiScreen parent, File schematicFile, int[] initialPreview) {
        super(parent, UiTranslations.format("schematica.ui.preview_editor.title"));
        this.returnScreen = parent;
        this.schematicFile = schematicFile;
        this.initialPreview = initialPreview == null ? null : initialPreview.clone();
    }

    private static String tr(String key, Object... args) {
        return UiTranslations.format("schematica.ui.preview_editor." + key, args);
    }

    @Override
    protected void createWidgets() {
        chooseButton = root.add(new UiButton(() -> tr(pickerFuture != null ? "choosing" : "choose"), button -> {
            if (button == 0) startNativePicker();
        }));
        pathField = root.add(new UiTextField(fontRendererObj, 1024, text -> {}) {
            @Override public boolean keyTyped(char character, int keyCode) {
                if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                    loadFromPathField();
                    return true;
                }
                return super.keyTyped(character, keyCode);
            }
        });
        pathField.setTooltip(tr("path_hint"));
        loadButton = root.add(new UiButton(() -> tr("load"), button -> {
            if (button == 0) loadFromPathField();
        }));

        canvas = root.add(new CropCanvas());
        resultPreview = root.add(new ResultPreview());
        dimensionLabel = root.add(new UiLabel(this::dimensionSummary));

        centerCropButton = root.add(new UiButton(() -> tr("crop_center"), button -> {
            if (button == 0) {
                editor.resetCrop();
                markResultDirty();
            }
        }));
        fitCropButton = root.add(new UiButton(() -> tr("crop_fit"), button -> {
            if (button == 0) {
                editor.fitFullImage();
                markResultDirty();
            }
        }));
        flipHButton = root.add(new UiButton(() -> tr("flip_h"), button -> {
            if (button == 0) {
                editor.flipHorizontal();
                syncFields();
                markAllDirty();
            }
        }));
        flipVButton = root.add(new UiButton(() -> tr("flip_v"), button -> {
            if (button == 0) {
                editor.flipVertical();
                syncFields();
                markAllDirty();
            }
        }));
        rotateLeftButton = root.add(new UiButton(() -> "-90\u00b0", button -> {
            if (button == 0) {
                editor.rotateFixed(-90);
                syncFields();
                markAllDirty();
            }
        }));
        rotateRightButton = root.add(new UiButton(() -> "+90\u00b0", button -> {
            if (button == 0) {
                editor.rotateFixed(90);
                syncFields();
                markAllDirty();
            }
        }));
        rotateZeroButton = root.add(new UiButton(() -> "0\u00b0", button -> {
            if (button == 0) {
                editor.setRotationDegrees(0);
                syncFields();
                markAllDirty();
            }
        }));

        angleSlider = root.add(new IntSlider(
            () -> tr("angle", (int) Math.round(editor.rotationDegrees())),
            () -> (int) Math.round(editor.rotationDegrees()),
            () -> -180,
            () -> 180,
            0,
            value -> {
                editor.setRotationDegrees(value);
                syncFields();
                markAllDirty();
            }));
        angleField = root.add(new UiIntegerField(fontRendererObj, 0, -180, 180, value -> {
            if (syncingFields) return;
            editor.setRotationDegrees(value);
            markAllDirty();
        }));

        sizeSlider = root.add(new IntSlider(
            () -> tr("size", editor.outputSize()),
            editor::outputSize,
            () -> PreviewImageEditor.MIN_SIZE,
            () -> PreviewImageEditor.MAX_SIZE,
            PreviewImageEditor.DEFAULT_SIZE,
            value -> {
                editor.setOutputSize(value);
                syncFields();
                markResultDirty();
            }));
        sizeField = root.add(new UiIntegerField(fontRendererObj, PreviewImageEditor.DEFAULT_SIZE,
            PreviewImageEditor.MIN_SIZE, PreviewImageEditor.MAX_SIZE, value -> {
                if (syncingFields) return;
                editor.setOutputSize(value);
                syncFields();
                markResultDirty();
            }));

        borderSlider = root.add(new IntSlider(
            () -> tr("border", editor.borderWidth()),
            editor::borderWidth,
            () -> 0,
            editor::maxBorderWidth,
            0,
            value -> {
                editor.setBorderWidth(value);
                syncFields();
                markResultDirty();
            }));
        borderField = root.add(new UiIntegerField(fontRendererObj, 0, 0, PreviewImageEditor.MAX_BORDER, value -> {
            if (syncingFields) return;
            editor.setBorderWidth(value);
            syncFields();
            markResultDirty();
        }));
        borderColorButton = root.add(new UiButton(() -> "", button -> {
            if (button == 0) openBorderColorPicker();
        }) {
            @Override public void draw(UiDraw draw, int mouseX, int mouseY) {
                draw.fill(bounds(), isEnabled() ? 0xFFFFFFFF : 0xFF808080);
                draw.fill(bounds().inset(1), 0xFF000000);
                draw.fill(bounds().inset(2), editor.borderColor() | 0xFF000000);
                if (isFocused() && UiInput.focusVisible()) draw.border(bounds(), 0xFFFFFF00);
            }
        });
        borderColorButton.setTooltip(tr("border_color"));

        resetButton = addButton("malilib.gui.button.reset", () -> {
            editor.resetAll();
            syncFields();
            markAllDirty();
        });
        statusLabel = root.add(new UiLabel(() -> statusText, 0xFFFF7777));
        saveButton = addButton("schematica.ui.preview_editor.save", this::save);
        cancelButton = addButton("malilib.gui.button.cancel", this::closeScreen);

        if (initialPreview != null) {
            editor.loadPixels(initialPreview);
            markAllDirty();
        }
        syncFields();
    }

    private String dimensionSummary() {
        if (!editor.hasImage()) return "";
        int size = editor.outputSize();
        int border = editor.borderWidth();
        return border > 0 ? size + "x" + size + " (+" + border + "px)" : size + "x" + size;
    }

    private void openBorderColorPicker() {
        ColorPickerPanel panel = new ColorPickerPanel(fontRendererObj, editor.borderColor(), color -> {
            editor.setBorderColor(color);
            markResultDirty();
        });
        panel.layout(root.bounds());
        input.pushModal(panel);
    }

    private void startNativePicker() {
        if (pickerFuture != null) return;
        statusText = "";
        File startDir = schematicFile == null ? null : schematicFile.getParentFile();
        pickerFuture = NativeImagePicker.pickAsync(tr("title"), startDir);
    }

    void loadFromPathField() {
        String raw = pathField.text().trim();
        if (raw.isEmpty()) return;
        if ((raw.startsWith("\"") && raw.endsWith("\"") || raw.startsWith("'") && raw.endsWith("'")) && raw.length() >= 2) {
            raw = raw.substring(1, raw.length() - 1).trim();
        }
        loadImageFile(new File(raw));
    }

    void loadImageFile(File file) {
        try {
            editor.loadFile(file);
            pathField.setText(file.getAbsolutePath());
            statusText = "";
            syncFields();
            markAllDirty();
        } catch (IOException | RuntimeException e) {
            Reference.logger.warn("Could not load preview image file {}", file, e);
            statusText = tr("load_failed", file.getName());
        }
    }

    boolean saveToFile() {
        if (!editor.hasImage()) return false;
        input.focus(null);
        int[] pixels = editor.renderPixels();
        if (pixels == null) return false;
        try {
            SchematicPreview.store(schematicFile, pixels);
            return true;
        } catch (IOException | RuntimeException e) {
            Reference.logger.warn("Could not save edited preview image to {}", schematicFile, e);
            statusText = UiTranslations.format("litematica.error.schematic_load.cant_read_file", schematicFile.getName());
            return false;
        }
    }

    private void save() {
        if (!saveToFile()) return;
        if (returnScreen instanceof UiScreen) {
            ((UiScreen) returnScreen).addMessage(MessageType.SUCCESS, "litematica.info.schematic_manager.preview.success");
        }
        closeScreen();
    }

    private void syncFields() {
        syncingFields = true;
        try {
            angleField.setValue(Math.round(editor.rotationDegrees()));
            sizeField.setValue(editor.outputSize());
            borderField.setValue(editor.borderWidth());
        } finally {
            syncingFields = false;
        }
    }

    private void markAllDirty() {
        orientedDirty = true;
        resultDirty = true;
    }

    private void markResultDirty() {
        resultDirty = true;
    }

    @Override
    protected void tickScreen() {
        if (pickerFuture != null && pickerFuture.isDone()) {
            Future<NativeImagePicker.Result> finished = pickerFuture;
            pickerFuture = null;
            try {
                NativeImagePicker.Result result = finished.get();
                if (result.file != null) {
                    loadImageFile(result.file);
                } else if (result.unavailable) {
                    statusText = tr("picker_unavailable");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                Reference.logger.warn("Image picker failed", e.getCause());
                statusText = tr("picker_unavailable");
            }
        }
        boolean hasImage = editor.hasImage();
        chooseButton.setEnabled(pickerFuture == null);
        centerCropButton.setEnabled(hasImage);
        fitCropButton.setEnabled(hasImage);
        flipHButton.setEnabled(hasImage);
        flipVButton.setEnabled(hasImage);
        rotateLeftButton.setEnabled(hasImage);
        rotateRightButton.setEnabled(hasImage);
        rotateZeroButton.setEnabled(hasImage);
        angleSlider.setEnabled(hasImage);
        angleField.setEnabled(hasImage);
        sizeSlider.setEnabled(hasImage);
        sizeField.setEnabled(hasImage);
        borderSlider.setEnabled(hasImage && editor.maxBorderWidth() > 0);
        borderField.setEnabled(hasImage && editor.maxBorderWidth() > 0);
        borderColorButton.setEnabled(hasImage);
        resetButton.setEnabled(hasImage);
        saveButton.setEnabled(hasImage);
    }

    @Override
    protected boolean interceptMouse(int x, int y, int button) {
        List<UiPanel> modals = input.modalPanels();
        if (!modals.isEmpty()) {
            UiPanel top = modals.get(modals.size() - 1);
            if (top instanceof ColorPickerPanel && !top.bounds().contains(x, y)) {
                input.popModal();
                return true;
            }
        }
        return false;
    }

    @Override
    protected void layoutWidgets() {
        int margin = 10;
        int topY = 24;
        int chooseW = Math.max(66, fontRendererObj.getStringWidth(chooseButton.label()) + 12);
        int loadW = Math.max(38, fontRendererObj.getStringWidth(loadButton.label()) + 12);
        chooseButton.setBounds(margin, topY, chooseW, 18);
        int pathX = margin + chooseW + 4;
        int pathW = Math.max(40, width - margin - loadW - 4 - pathX);
        pathField.setBounds(pathX, topY, pathW, 18);
        loadButton.setBounds(pathX + pathW + 4, topY, loadW, 18);

        int bottomY = height - 26;
        int resetW = Math.max(46, fontRendererObj.getStringWidth(resetButton.label()) + 12);
        int cancelW = Math.max(46, fontRendererObj.getStringWidth(cancelButton.label()) + 12);
        int saveW = Math.max(60, fontRendererObj.getStringWidth(saveButton.label()) + 12);
        resetButton.setBounds(margin, bottomY, resetW, 20);
        cancelButton.setBounds(width - margin - cancelW, bottomY, cancelW, 20);
        saveButton.setBounds(cancelButton.bounds().x - 4 - saveW, bottomY, saveW, 20);
        int statusX = resetButton.bounds().right() + 6;
        statusLabel.setBounds(statusX, bottomY + 6, Math.max(0, saveButton.bounds().x - 6 - statusX), 12);

        int contentTop = 46;
        int contentHeight = Math.max(40, bottomY - 4 - contentTop);
        int gap = 8;
        int rightW = Math.max(168, Math.min(230, (width - margin * 2 - gap) / 2));
        int canvasW = Math.max(40, width - margin * 2 - gap - rightW);
        int rightX = margin + canvasW + gap;
        canvas.setBounds(margin, contentTop, canvasW, contentHeight);

        int rowH = 18;
        int rowGap = 3;
        int controlsHeight = 6 * rowH + 5 * rowGap;
        int previewAreaH = Math.max(24, contentHeight - controlsHeight - 4);
        int previewSide = Math.max(24, Math.min(124, Math.min(previewAreaH, rightW)));
        int previewX = rightX + (rightW - previewSide) / 2;
        if (rightW - previewSide >= 64) {
            previewX = rightX;
            resultPreview.setBounds(previewX, contentTop, previewSide, previewSide);
            dimensionLabel.setBounds(previewX + previewSide + 6, contentTop + Math.max(0, (previewSide - 10) / 2),
                rightW - previewSide - 6, 12);
        } else {
            resultPreview.setBounds(previewX, contentTop, previewSide, previewSide);
            dimensionLabel.setBounds(rightX, contentTop + previewSide + 2, rightW, 0);
        }

        int rowY = contentTop + Math.max(previewSide + 4, contentHeight - controlsHeight);
        int halfW = (rightW - 4) / 2;
        centerCropButton.setBounds(rightX, rowY, halfW, rowH);
        fitCropButton.setBounds(rightX + halfW + 4, rowY, rightW - halfW - 4, rowH);

        rowY += rowH + rowGap;
        flipHButton.setBounds(rightX, rowY, halfW, rowH);
        flipVButton.setBounds(rightX + halfW + 4, rowY, rightW - halfW - 4, rowH);

        rowY += rowH + rowGap;
        int thirdW = (rightW - 8) / 3;
        rotateLeftButton.setBounds(rightX, rowY, thirdW, rowH);
        rotateRightButton.setBounds(rightX + thirdW + 4, rowY, thirdW, rowH);
        rotateZeroButton.setBounds(rightX + (thirdW + 4) * 2, rowY, rightW - (thirdW + 4) * 2, rowH);

        int fieldW = 36;
        rowY += rowH + rowGap;
        angleSlider.setBounds(rightX, rowY, rightW - fieldW - 4, rowH);
        angleField.setBounds(rightX + rightW - fieldW, rowY, fieldW, rowH);

        rowY += rowH + rowGap;
        sizeSlider.setBounds(rightX, rowY, rightW - fieldW - 4, rowH);
        sizeField.setBounds(rightX + rightW - fieldW, rowY, fieldW, rowH);

        rowY += rowH + rowGap;
        int swatchW = 18;
        int borderFieldW = 32;
        borderSlider.setBounds(rightX, rowY, rightW - borderFieldW - swatchW - 8, rowH);
        borderField.setBounds(borderSlider.bounds().right() + 4, rowY, borderFieldW, rowH);
        borderColorButton.setBounds(borderField.bounds().right() + 4, rowY, swatchW, rowH);
    }

    @Override
    protected void closed() {
        if (pickerFuture != null) {
            pickerFuture.cancel(true);
            pickerFuture = null;
        }
        deleteTextures();
    }

    private void deleteTextures() {
        if (mc == null || mc.getTextureManager() == null) return;
        if (orientedTexture != null) {
            mc.getTextureManager().deleteTexture(orientedTexture);
            orientedTexture = null;
            orientedDynamic = null;
        }
        if (resultTexture != null) {
            mc.getTextureManager().deleteTexture(resultTexture);
            resultTexture = null;
            resultDynamic = null;
        }
    }

    private void refreshOrientedTexture() {
        if (!orientedDirty || !editor.hasImage() || mc == null || mc.getTextureManager() == null) return;
        BufferedImage image = editor.renderOrientedPreview(CANVAS_MAX_TEXTURE);
        if (image == null) return;
        int w = image.getWidth();
        int h = image.getHeight();
        if (orientedDynamic == null || orientedTexW != w || orientedTexH != h) {
            if (orientedTexture != null) mc.getTextureManager().deleteTexture(orientedTexture);
            orientedDynamic = new DynamicTexture(w, h);
            orientedTexture = mc.getTextureManager().getDynamicTextureLocation("schematica_preview_edit_src", orientedDynamic);
            orientedTexW = w;
            orientedTexH = h;
        }
        image.getRGB(0, 0, w, h, orientedDynamic.getTextureData(), 0, w);
        orientedDynamic.updateDynamicTexture();
        orientedDirty = false;
    }

    private void refreshResultTexture() {
        if (!resultDirty || !editor.hasImage() || mc == null || mc.getTextureManager() == null) return;
        BufferedImage image = editor.renderResult(false);
        if (image == null) return;
        int side = image.getWidth();
        if (resultDynamic == null || resultTexSize != side) {
            if (resultTexture != null) mc.getTextureManager().deleteTexture(resultTexture);
            resultDynamic = new DynamicTexture(side, side);
            resultTexture = mc.getTextureManager().getDynamicTextureLocation("schematica_preview_edit_out", resultDynamic);
            resultTexSize = side;
        }
        image.getRGB(0, 0, side, side, resultDynamic.getTextureData(), 0, side);
        resultDynamic.updateDynamicTexture();
        resultDirty = false;
    }

    private final class ResultPreview extends UiWidget {
        @Override public void draw(UiDraw draw, int mouseX, int mouseY) {
            UiBounds b = bounds();
            draw.fill(b, PreviewImageEditor.BACKGROUND_COLOR);
            draw.border(b, 0xFF777777);
            if (!editor.hasImage()) return;
            refreshResultTexture();
            if (resultTexture != null) {
                draw.texture(resultTexture.toString(), b.inset(1), 0, 0, 1, 1, 1, 1);
            }
        }
    }

    private final class CropCanvas extends UiWidget {
        private static final int MODE_NONE = 0;
        private static final int MODE_MOVE = 1;
        private static final int MODE_CORNER = 2;
        private static final int MODE_NEW = 3;

        private int dragMode = MODE_NONE;
        private int dragCorner = -1;
        private int lastX;
        private int lastY;
        private double dragStartImgX;
        private double dragStartImgY;

        private UiBounds imageRect() {
            UiBounds inner = bounds().inset(4);
            int ow = Math.max(1, editor.orientedWidth());
            int oh = Math.max(1, editor.orientedHeight());
            double spanW = Math.max(ow, editor.cropSize());
            double spanH = Math.max(oh, editor.cropSize());
            double scale = Math.min((double) inner.width / Math.max(1.0, spanW), (double) inner.height / Math.max(1.0, spanH));
            int w = Math.max(1, (int) Math.round(ow * scale));
            int h = Math.max(1, (int) Math.round(oh * scale));
            return new UiBounds(inner.x + (inner.width - w) / 2, inner.y + (inner.height - h) / 2, w, h);
        }

        private UiBounds cropRect(UiBounds img) {
            int ow = Math.max(1, editor.orientedWidth());
            int oh = Math.max(1, editor.orientedHeight());
            int x0 = (int) Math.round(img.x + editor.cropX() * img.width / ow);
            int y0 = (int) Math.round(img.y + editor.cropY() * img.height / oh);
            int x1 = (int) Math.round(img.x + (editor.cropX() + editor.cropSize()) * img.width / ow);
            int y1 = (int) Math.round(img.y + (editor.cropY() + editor.cropSize()) * img.height / oh);
            return new UiBounds(x0, y0, Math.max(1, x1 - x0), Math.max(1, y1 - y0));
        }

        private double toImageX(UiBounds img, int screenX) {
            return (screenX - img.x) * (double) Math.max(1, editor.orientedWidth()) / Math.max(1, img.width);
        }

        private double toImageY(UiBounds img, int screenY) {
            return (screenY - img.y) * (double) Math.max(1, editor.orientedHeight()) / Math.max(1, img.height);
        }

        private int hitCorner(UiBounds crop, int x, int y) {
            int[][] corners = {
                {crop.x, crop.y},
                {crop.right(), crop.y},
                {crop.x, crop.bottom()},
                {crop.right(), crop.bottom()}
            };
            int best = -1;
            int bestDist = HANDLE_RADIUS * HANDLE_RADIUS;
            for (int i = 0; i < corners.length; i++) {
                int dx = x - corners[i][0];
                int dy = y - corners[i][1];
                int dist = dx * dx + dy * dy;
                if (dist <= bestDist) {
                    bestDist = dist;
                    best = i;
                }
            }
            return best;
        }

        @Override public List<String> tooltip(int mouseX, int mouseY) {
            if (!editor.hasImage() || dragMode != MODE_NONE) return Collections.emptyList();
            return Collections.singletonList(tr("canvas_hint").replace("\\n", "\n"));
        }

        @Override public void draw(UiDraw draw, int mouseX, int mouseY) {
            UiBounds b = bounds();
            draw.fill(b, PreviewImageEditor.BACKGROUND_COLOR);
            draw.border(b, 0xFF777777);
            if (!editor.hasImage()) {
                String empty = draw.trim(tr("empty"), b.width - 12);
                draw.text(empty, b.x + (b.width - draw.textWidth(empty)) / 2, b.y + (b.height - 8) / 2, 0xFFAAAAAA);
                return;
            }
            refreshOrientedTexture();
            UiBounds img = imageRect();
            if (orientedTexture != null) {
                draw.texture(orientedTexture.toString(), img, 0, 0, 1, 1, 1, 1);
            }
            UiBounds crop = cropRect(img);
            int mx0 = Math.max(img.x, Math.min(img.right(), crop.x));
            int my0 = Math.max(img.y, Math.min(img.bottom(), crop.y));
            int mx1 = Math.max(img.x, Math.min(img.right(), crop.right()));
            int my1 = Math.max(img.y, Math.min(img.bottom(), crop.bottom()));
            int mask = 0x90000000;
            draw.fill(new UiBounds(img.x, img.y, img.width, Math.max(0, my0 - img.y)), mask);
            draw.fill(new UiBounds(img.x, my1, img.width, Math.max(0, img.bottom() - my1)), mask);
            draw.fill(new UiBounds(img.x, my0, Math.max(0, mx0 - img.x), Math.max(0, my1 - my0)), mask);
            draw.fill(new UiBounds(mx1, my0, Math.max(0, img.right() - mx1), Math.max(0, my1 - my0)), mask);

            draw.border(crop, 0xFFFFFFFF);
            if (crop.width >= 18 && crop.height >= 18) {
                for (int i = 1; i <= 2; i++) {
                    int gx = crop.x + crop.width * i / 3;
                    int gy = crop.y + crop.height * i / 3;
                    draw.fill(new UiBounds(gx, crop.y + 1, 1, crop.height - 2), 0x50FFFFFF);
                    draw.fill(new UiBounds(crop.x + 1, gy, crop.width - 2, 1), 0x50FFFFFF);
                }
            }
            int[][] handles = {
                {crop.x, crop.y},
                {crop.right(), crop.y},
                {crop.x, crop.bottom()},
                {crop.right(), crop.bottom()}
            };
            for (int[] handle : handles) {
                UiBounds box = new UiBounds(handle[0] - 3, handle[1] - 3, 6, 6);
                draw.fill(box, 0xFFFFFFFF);
                draw.border(box, 0xFF000000);
            }
        }

        @Override public boolean mouseDown(int x, int y, int button) {
            if (!editor.hasImage() || button != 0) return false;
            UiBounds img = imageRect();
            UiBounds crop = cropRect(img);
            int corner = hitCorner(crop, x, y);
            lastX = x;
            lastY = y;
            if (corner >= 0) {
                dragMode = MODE_CORNER;
                dragCorner = corner;
                return true;
            }
            if (crop.contains(x, y)) {
                dragMode = MODE_MOVE;
                return true;
            }
            dragMode = MODE_NEW;
            dragStartImgX = toImageX(img, x);
            dragStartImgY = toImageY(img, y);
            return true;
        }

        @Override public void mouseDrag(int x, int y, int button) {
            if (!editor.hasImage() || button != 0 || dragMode == MODE_NONE) return;
            UiBounds img = imageRect();
            if (dragMode == MODE_MOVE) {
                double dx = (x - lastX) * (double) Math.max(1, editor.orientedWidth()) / Math.max(1, img.width);
                double dy = (y - lastY) * (double) Math.max(1, editor.orientedHeight()) / Math.max(1, img.height);
                editor.moveCrop(dx, dy);
                lastX = x;
                lastY = y;
                markResultDirty();
            } else if (dragMode == MODE_CORNER) {
                editor.resizeCropCorner(dragCorner, toImageX(img, x), toImageY(img, y));
                markResultDirty();
            } else if (dragMode == MODE_NEW) {
                editor.setCropFromDrag(dragStartImgX, dragStartImgY, toImageX(img, x), toImageY(img, y));
                markResultDirty();
            }
        }

        @Override public void mouseUp(int x, int y, int button) {
            cancelMouse();
        }

        @Override public void cancelMouse() {
            dragMode = MODE_NONE;
            dragCorner = -1;
        }

        @Override public boolean scroll(int x, int y, int amount) {
            if (!editor.hasImage() || amount == 0) return false;
            editor.zoomCrop(amount > 0 ? 0.9 : 1.0 / 0.9);
            markResultDirty();
            return true;
        }
    }

    private static final class IntSlider extends UiWidget {
        private final Supplier<String> label;
        private final IntSupplier getter;
        private final IntSupplier minSupplier;
        private final IntSupplier maxSupplier;
        private final int defaultValue;
        private final IntConsumer setter;

        IntSlider(Supplier<String> label, IntSupplier getter, IntSupplier minSupplier, IntSupplier maxSupplier,
            int defaultValue, IntConsumer setter) {
            this.label = label;
            this.getter = getter;
            this.minSupplier = minSupplier;
            this.maxSupplier = maxSupplier;
            this.defaultValue = defaultValue;
            this.setter = setter;
            setTooltip(tr("slider_reset"));
        }

        @Override public boolean isFocusable() { return true; }

        private double fraction() {
            int min = minSupplier.getAsInt();
            int max = maxSupplier.getAsInt();
            if (max <= min) return 0.0;
            return Math.max(0.0, Math.min(1.0, (double) (getter.getAsInt() - min) / (max - min)));
        }

        private void applyFraction(double value) {
            int min = minSupplier.getAsInt();
            int max = maxSupplier.getAsInt();
            if (max <= min) return;
            double clamped = Math.max(0.0, Math.min(1.0, value));
            setter.accept((int) Math.round(min + clamped * (max - min)));
        }

        private static void bar(UiDraw draw, UiBounds box, int v) {
            int left = box.width / 2;
            int right = box.width - left;
            draw.texture("minecraft:textures/gui/widgets.png", new UiBounds(box.x, box.y, left, box.height),
                0, v, Math.min(left, 200), 20, 256, 256);
            draw.texture("minecraft:textures/gui/widgets.png", new UiBounds(box.x + left, box.y, right, box.height),
                200 - Math.min(right, 200), v, Math.min(right, 200), 20, 256, 256);
        }

        @Override public void draw(UiDraw draw, int mouseX, int mouseY) {
            UiBounds box = bounds();
            bar(draw, new UiBounds(box.x + 1, box.y, Math.max(2, box.width - 2), box.height), 46);
            int knob = Math.max(8, Math.min(14, (box.width - 4) / 6));
            int knobX = box.x + 2 + (int) Math.round(fraction() * Math.max(0, box.width - 4 - knob));
            bar(draw, new UiBounds(knobX, box.y, knob, box.height), isEnabled() ? 66 : 46);
            String text = draw.trim(label.get(), box.width - 6);
            draw.text(text, box.x + (box.width - draw.textWidth(text)) / 2, box.y + (box.height - 8) / 2,
                isEnabled() ? 0xFFFFFFA0 : 0xFFA0A0A0);
            if (isFocused() && isEnabled() && UiInput.focusVisible()) draw.border(box, 0xFFE0E0E0);
        }

        @Override public boolean mouseDown(int x, int y, int button) {
            if (!isEnabled()) return false;
            if (button == 1) {
                setter.accept(defaultValue);
                return true;
            }
            if (button != 0) return false;
            mouseDrag(x, y, button);
            return true;
        }

        @Override public void mouseDrag(int x, int y, int button) {
            if (!isEnabled() || button != 0) return;
            int knob = Math.max(8, Math.min(14, (bounds().width - 4) / 6));
            applyFraction((double) (x - bounds().x - knob / 2) / Math.max(1, bounds().width - knob - 4));
        }

        @Override public boolean scroll(int x, int y, int amount) {
            if (!isEnabled() || amount == 0) return false;
            int step = Integer.signum(amount);
            setter.accept(getter.getAsInt() + step);
            return true;
        }

        @Override public boolean keyTyped(char character, int keyCode) {
            if (!isEnabled()) return false;
            if (keyCode == Keyboard.KEY_LEFT) {
                setter.accept(getter.getAsInt() - 1);
                return true;
            }
            if (keyCode == Keyboard.KEY_RIGHT) {
                setter.accept(getter.getAsInt() + 1);
                return true;
            }
            return false;
        }
    }
}

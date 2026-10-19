package com.github.lunatrius.schematica.client.gui.browser;

import java.io.File;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiDraw;
import com.github.lunatrius.schematica.client.gui.framework.UiInput;
import com.github.lunatrius.schematica.client.gui.framework.UiPanel;
import com.github.lunatrius.schematica.client.gui.framework.UiSlideSwitch;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.gui.framework.UiWidget;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.world.schematic.SchematicFiles;

final class SchematicPreviewPane extends UiPanel {
    private final UiInput input;
    private final boolean expanded;
    private final UiSlideSwitch mode;
    private final UiButton expand, rotate, reset;
    final Viewport viewport;
    private Content content;

    SchematicPreviewPane(UiInput input, boolean expanded, Runnable expandOrClose) {
        this.input = input;
        this.expanded = expanded;
        viewport = add(new Viewport());
        mode = add(new UiSlideSwitch(() -> tr("image"), () -> "3D", () -> content != null && content.showModel(), value -> {
            if (content != null) content.modelSelected = value;
        }));
        expand = add(new UiButton(() -> expanded ? UiTranslations.format("gui.back") : tr("fullscreen"), button -> {
            if (button == 0) expandOrClose.run();
        }));
        rotate = add(new UiButton(() -> tr(model() != null && model().camera.rotating ? "pause" : "resume"), button -> {
            SchematicPreview3D model = model();
            if (button == 0 && model != null) {
                model.camera.rotating = !model.camera.rotating;
                input.focus(viewport);
            }
        }));
        reset = add(new UiButton(() -> tr("reset"), button -> {
            if (button == 0 && model() != null) { model().camera.reset(); input.focus(viewport); }
        }));
        viewport.setTooltip(tr("controls").replace("\\n", "\n").split("\n"));
        reset.setTooltip(tr("reset.hover"));
    }

    private static String tr(String name) { return UiTranslations.format("schematica.ui.preview." + name); }

    void setContent(Content content) {
        if (this.content != content) {
            viewport.cancelMouse();
            viewport.keysArmed = false;
            viewport.lastFrame = 0;
        }
        this.content = content;
    }

    Content content() { return content; }

    private SchematicPreview3D model() { return content != null && content.showModel() && content.canModel() ? content.cache.preview3D(content.file) : null; }

    @Override public void layout(UiBounds screen) {
        if (expanded) setBounds(screen.x + 6, screen.y + 6, screen.width - 12, screen.height - 12);
        UiBounds b = bounds();
        int pad = expanded ? 6 : 0, x = b.x + pad, y = b.y + pad, w = b.width - pad * 2;
        int fullWidth = Math.min(w / 2, expanded ? 65 : 52);
        mode.setBounds(x, y, w - fullWidth - 4, 18);
        expand.setBounds(x + w - fullWidth, y, fullWidth, 18);
        rotate.setBounds(x, y + 21, (w - 4) / 2, 18);
        reset.setBounds(x + (w - 4) / 2 + 4, y + 21, w - (w - 4) / 2 - 4, 18);
        viewport.setBounds(x, y + 43, w, Math.max(1, b.bottom() - pad - (y + 43)));
    }

    @Override public void draw(UiDraw draw, int mouseX, int mouseY) {
        if (content == null) return;
        if (expanded) { draw.fill(bounds(), 0xFF151A20); draw.border(bounds(), 0xFF999999); }
        mode.setVisible(content.image() != null && content.canModel());
        expand.setVisible(expanded || content.canModel());
        SchematicPreview3D model = model();
        rotate.setEnabled(model != null && !model.failed());
        reset.setEnabled(model != null && !model.failed());
        super.draw(draw, mouseX, mouseY);
    }

    static final class Content {
        final File file;
        final long size;
        final SchematicInfoCache cache;
        boolean modelSelected = ConfigurationHandler.schematicPreview3DReplacesImage;

        Content(File file, long size, SchematicInfoCache cache) {
            this.file = file; this.size = size; this.cache = cache;
        }

        boolean canModel() {
            SchematicFiles.Info info = cache.get(file);
            return ConfigurationHandler.schematicPreview3D && size <= (32L << 20) && SchematicBrowserModel.supported(file.getName())
                && (info == null || info.volume <= SchematicPreview3D.MAX_VOLUME);
        }

        ResourceLocation image() {
            SchematicFiles.Info info = cache.get(file);
            return info == null ? null : cache.preview(file, info);
        }

        boolean showModel() { return canModel() && (modelSelected || image() == null); }
        boolean available() { return canModel() || image() != null; }
    }

    final class Viewport extends UiWidget {
        private int dragButton = -1, dragX, dragY;
        private long lastFrame;
        private boolean keysArmed;

        @Override public boolean isFocusable() { return content != null && content.showModel(); }

        @Override protected void focusChanged(boolean focused) { keysArmed = false; if (!focused) cancelMouse(); }

        @Override public void draw(UiDraw draw, int mouseX, int mouseY) {
            UiBounds b = bounds();
            draw.fill(b, 0xFF10151C);
            draw.border(b, isFocused() ? 0xFF9CC9FF : 0xFF777777);
            List<UiPanel> modals = input.modalPanels();
            boolean active = modals.isEmpty() || modals.get(modals.size() - 1) == SchematicPreviewPane.this;
            if (!active) { lastFrame = 0; return; }
            SchematicPreview3D model = model();
            if (content.showModel()) {
                if (model != null && !model.failed()) {
                    long now = System.nanoTime();
                    float elapsed = lastFrame == 0 ? 0 : (now - lastFrame) / 1_000_000_000f;
                    lastFrame = now;
                    model.camera.viewport(b.width - 2, b.height - 2);
                    int right = 0, up = 0, forward = 0;
                    if (isFocused() && Display.isActive() && mouseX >= 0) {
                        right = down(Keyboard.KEY_D) - down(Keyboard.KEY_A);
                        up = down(Keyboard.KEY_SPACE) - Math.max(down(Keyboard.KEY_LSHIFT), down(Keyboard.KEY_RSHIFT));
                        forward = down(Keyboard.KEY_W) - down(Keyboard.KEY_S);
                        if (right == 0 && up == 0 && forward == 0) keysArmed = true;
                        if (!keysArmed) right = up = forward = 0;
                    } else keysArmed = false;
                    model.camera.advance(elapsed, right, up, forward);
                    model.draw(Minecraft.getMinecraft(), b.x + 1, b.y + 1, Math.max(1, b.width - 2), Math.max(1, b.height - 2));
                } else {
                    String text = tr(content.cache.modelDone(content.file) ? "unavailable" : "loading");
                    draw.text(draw.trim(text, b.width - 8), b.x + 4, b.y + 4, 0xFFAAAAAA);
                }
            } else {
                lastFrame = 0;
                ResourceLocation image = content.image();
                if (image != null) {
                    int side = Math.min(b.width, b.height) - 2;
                    draw.texture(image.toString(), new UiBounds(b.x + (b.width - side) / 2, b.y + (b.height - side) / 2, side, side), 0, 0, 1, 1, 1, 1);
                }
            }
        }

        private int down(int key) { return Keyboard.isKeyDown(key) ? 1 : 0; }

        @Override public boolean mouseDown(int x, int y, int button) {
            SchematicPreview3D model = model();
            if (model == null || button < 0 || button > 1) return false;
            model.camera.rotating = false;
            dragButton = button; dragX = x; dragY = y;
            return true;
        }

        @Override public void mouseDrag(int x, int y, int button) {
            SchematicPreview3D model = model();
            if (model == null || dragButton != button) return;
            if (button == 0) model.camera.turn(x - dragX, y - dragY);
            else model.camera.pan(x - dragX, y - dragY, bounds().height);
            dragX = x; dragY = y;
        }

        @Override public void mouseUp(int x, int y, int button) { cancelMouse(); }
        @Override public void cancelMouse() { dragButton = -1; }

        @Override public boolean scroll(int x, int y, int amount) {
            if (model() == null) return false;
            model().camera.zoom(amount);
            return true;
        }

        @Override public boolean keyTyped(char character, int keyCode) {
            return keyCode == Keyboard.KEY_W || keyCode == Keyboard.KEY_A || keyCode == Keyboard.KEY_S || keyCode == Keyboard.KEY_D
                || keyCode == Keyboard.KEY_SPACE || keyCode == Keyboard.KEY_LSHIFT || keyCode == Keyboard.KEY_RSHIFT;
        }
    }
}

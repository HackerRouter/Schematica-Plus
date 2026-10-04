// SPDX-License-Identifier: LGPL-3.0-only
// Litematica/MaLiLib visual conventions, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.framework;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public abstract class UiScreen extends GuiScreen {

    protected final UiPanel root = new UiPanel();
    protected final UiInput input = new UiInput(root, () -> mc.getSoundHandler().playSound(
        PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F)));
    private final GuiScreen parent;
    private final String title;
    private boolean created;
    private boolean repeatOwned;
    private boolean previousRepeat;
    private UiWidget resumeFocus;
    private UiWidget hovered;
    private int lastMouseX;
    private int lastMouseY;
    private long hoverSince;

    protected UiScreen(GuiScreen parent, String title) {
        this.parent = parent;
        this.title = title;
    }

    protected abstract void createWidgets();

    protected abstract void layoutWidgets();

    protected void opened() {}

    protected void tickScreen() {}

    protected int titleRightMargin() { return 30; }

    protected void closed() {}

    protected boolean interceptKey(char character, int keyCode) { return false; }

    protected boolean interceptMouse(int x, int y, int button) { return false; }

    protected boolean handleKey(char character, int keyCode) { return false; }

    protected UiButton addButton(String key, Runnable action) {
        UiButton button = root.add(new UiButton(() -> UiTranslations.format(key), mouseButton -> {
            if (mouseButton == 0) action.run();
        }));
        button.setTooltip(UiTranslations.format(key));
        return button;
    }

    @Override
    public final void initGui() {
        boolean opening = !repeatOwned;
        if (!repeatOwned) {
            previousRepeat = Keyboard.areRepeatEventsEnabled();
            Keyboard.enableRepeatEvents(true);
            repeatOwned = true;
        }
        if (!created) {
            createWidgets();
            created = true;
        }
        root.setBounds(0, 0, width, height);
        if (opening) opened();
        // The first frame already shows the ticked state (hidden/disabled buttons), not the created one
        tickScreen();
        layoutWidgets();
        for (UiPanel panel : input.modalPanels()) panel.layout(root.bounds());
        input.validate();
        if (resumeFocus != null) {
            input.focus(resumeFocus);
            resumeFocus = null;
        }
        if (input.focused() == null) input.focusFirstText();
    }

    @Override
    public final void drawScreen(int mouseX, int mouseY, float partialTicks) {
        input.validate();
        try (MinecraftUiDraw draw = new MinecraftUiDraw(mc); UiDraw.Clip ignored = draw.clip(root.bounds())) {
            draw.fill(root.bounds(), 0xB0000000);
            draw.text(draw.trim(title, width - titleRightMargin()), 20, 10, 0xFFFFFFFF);
            List<UiPanel> modals = input.modalPanels();
            root.draw(draw, modals.isEmpty() ? mouseX : -1, modals.isEmpty() ? mouseY : -1);
            for (int i = 0; i < modals.size(); i++) {
                draw.fill(root.bounds(), 0x99000000);
                boolean top = i == modals.size() - 1;
                modals.get(i).draw(draw, top ? mouseX : -1, top ? mouseY : -1);
            }
            drawTooltip(draw, mouseX, mouseY);
        }
    }

    private void drawTooltip(UiDraw draw, int mouseX, int mouseY) {
        UiWidget target = input.hit(mouseX, mouseY);
        long now = System.nanoTime();
        if (target != hovered || mouseX != lastMouseX || mouseY != lastMouseY) hoverSince = now;
        hovered = target;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        if (target == null || now - hoverSince < 400_000_000L) return;
        if (target.drawTooltip(draw, mouseX, mouseY, root.bounds())) return;
        List<String> lines = new ArrayList<>();
        int maxWidth = Math.max(1, Math.min(320, width - 20));
        for (String text : target.tooltip(mouseX, mouseY)) {
            lines.addAll(fontRendererObj.listFormattedStringToWidth(text, maxWidth));
        }
        int maxLines = Math.max(1, (height - 16) / 11);
        if (lines.size() > maxLines) {
            lines = new ArrayList<>(lines.subList(0, maxLines));
            lines.set(maxLines - 1, "...");
        }
        if (lines.isEmpty()) return;
        int textWidth = 0;
        for (String line : lines) textWidth = Math.max(textWidth, draw.textWidth(line));
        int boxWidth = textWidth + 8;
        int boxHeight = lines.size() * 11 + 6;
        int x = Math.max(4, Math.min(mouseX + 12, width - boxWidth - 4));
        int y = Math.max(4, Math.min(mouseY + 12, height - boxHeight - 4));
        draw.fill(new UiBounds(x, y, boxWidth, boxHeight), UiTheme.PANEL);
        draw.border(new UiBounds(x, y, boxWidth, boxHeight), UiTheme.BORDER);
        for (String line : lines) {
            draw.text(line, x + 4, y + 4, UiTheme.TEXT);
            y += 11;
        }
    }

    @Override
    public final void updateScreen() {
        tickScreen();
        input.validate();
        root.tick();
        for (UiPanel panel : input.modalPanels()) panel.tick();
    }

    @Override
    protected final void mouseClicked(int x, int y, int button) {
        if (!interceptMouse(x, y, button)) input.mouseDown(x, y, button);
    }

    @Override
    protected final void mouseMovedOrUp(int x, int y, int button) {
        if (button >= 0) input.mouseUp(x, y, button);
    }

    @Override
    protected final void mouseClickMove(int x, int y, int button, long duration) {
        input.mouseDrag(x, y, button);
    }

    @Override
    public final void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            int x = Mouse.getEventX() * width / mc.displayWidth;
            int y = height - Mouse.getEventY() * height / mc.displayHeight - 1;
            input.scroll(x, y, wheel);
        }
    }

    @Override
    protected final void keyTyped(char character, int keyCode) {
        if (interceptKey(character, keyCode)) return;
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (!input.popModal()) closeScreen();
        } else if (keyCode == Keyboard.KEY_TAB) {
            input.cycleFocus(isShiftKeyDown());
        } else {
            boolean handled = input.modalPanels().isEmpty() && handleKey(character, keyCode);
            if (!handled && !(Keyboard.isRepeatEvent() && input.focused() instanceof UiButton)) {
                input.keyTyped(character, keyCode);
            }
        }
    }

    protected final void closeScreen() {
        mc.displayGuiScreen(parent);
        if (parent == null) mc.setIngameFocus();
    }

    protected final void mainMenu() {
        GuiScreen screen = parent;
        while (screen instanceof UiScreen) {
            if (screen instanceof com.github.lunatrius.schematica.client.gui.GuiSchematicMainMenu) {
                mc.displayGuiScreen(screen);
                return;
            }
            screen = ((UiScreen) screen).parent;
        }
        mc.displayGuiScreen(new com.github.lunatrius.schematica.client.gui.GuiSchematicMainMenu(null));
    }

    protected final UiButton unavailable(UiButton button) {
        button.setEnabled(false);
        button.setTooltip(UiTranslations.format("schematica.ui.pending"));
        return button;
    }

    protected final void confirm(String title, String message, Runnable confirmed) {
        UiPanel panel = new ConfirmationPanel(title, message, confirmed);
        panel.layout(root.bounds());
        input.pushModal(panel);
    }

    protected final void prompt(String title, String initial, java.util.function.Function<String, String> apply) {
        UiPanel panel = new TextInputPanel(fontRendererObj, input, title, initial, apply);
        panel.layout(root.bounds());
        input.pushModal(panel);
    }

    @Override
    public final void onGuiClosed() {
        resumeFocus = input.focused();
        input.suspend();
        if (repeatOwned) {
            Keyboard.enableRepeatEvents(previousRepeat);
            repeatOwned = false;
        }
        closed();
    }

    @Override
    public final boolean doesGuiPauseGame() {
        return false;
    }

    private final class ConfirmationPanel extends UiPanel {
        private final String dialogTitle;
        private final String message;
        private final UiButton cancel;
        private final UiButton accept;
        private List<String> lines;

        ConfirmationPanel(String title, String message, Runnable confirmed) {
            this.dialogTitle = title;
            this.message = message;
            cancel = add(new UiButton(() -> "\u00a7c" + UiTranslations.format("malilib.gui.button.cancel") + "\u00a7r", button -> {
                if (button == 0) input.popModal();
            }));
            accept = add(new UiButton(() -> "\u00a7a" + UiTranslations.format("malilib.gui.button.ok") + "\u00a7r", button -> {
                if (button == 0) {
                    input.popModal();
                    confirmed.run();
                }
            }));
        }

        @Override
        public void layout(UiBounds screen) {
            int width = Math.max(40, Math.min(400, screen.width - 24));
            lines = fontRendererObj.listFormattedStringToWidth(message, Math.max(1, width - 30));
            int height = Math.min(screen.height - 16, 54 + lines.size() * (fontRendererObj.FONT_HEIGHT + 1));
            setBounds((screen.width - width) / 2, (screen.height - height) / 2, width, height);
            int buttonWidth = Math.max(fontRendererObj.getStringWidth(accept.label()), fontRendererObj.getStringWidth(cancel.label())) + 10;
            accept.setBounds(bounds().x + 10, bounds().bottom() - 24, buttonWidth, 20);
            cancel.setBounds(bounds().x + 20 + buttonWidth, bounds().bottom() - 24, buttonWidth, 20);
        }

        @Override
        public void draw(UiDraw draw, int mouseX, int mouseY) {
            draw.fill(bounds(), 0xF0000000);
            draw.border(bounds(), UiTheme.BORDER);
            draw.text(draw.trim(dialogTitle, bounds().width - 20), bounds().x + 10, bounds().y + 4, UiTheme.TEXT);
            try (UiDraw.Clip ignored = draw.clip(new UiBounds(bounds().x + 10, bounds().y + 20,
                bounds().width - 20, bounds().height - 48))) {
                int y = bounds().y + 20;
                for (String line : lines) {
                    draw.text(line, bounds().x + 10, y, 0xFFC0C0C0);
                    y += fontRendererObj.FONT_HEIGHT + 1;
                }
            }
            super.draw(draw, mouseX, mouseY);
        }
    }
}

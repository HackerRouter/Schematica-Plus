// SPDX-License-Identifier: LGPL-3.0-only
// Litematica/MaLiLib visual conventions, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.framework;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;

import com.github.lunatrius.schematica.client.gui.GuiSchematicMainMenu;
import com.github.lunatrius.schematica.client.util.IssueReport;
import com.github.lunatrius.schematica.util.MessageType;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public abstract class UiScreen extends GuiScreen {

    protected final UiPanel root = new UiPanel();
    protected final UiInput input = new UiInput(root, () -> mc.getSoundHandler().playSound(
        PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F)));
    private final MessageRenderer messages = new MessageRenderer(0xDD000000, 0xFF999999, true, true);
    private final GuiScreen parent;
    private final String title;
    private boolean created;
    private boolean repeatOwned;
    private boolean previousRepeat;
    private UiWidget resumeFocus;

    protected UiScreen(GuiScreen parent, String title) {
        this.parent = parent;
        this.title = title;
    }

    protected abstract void createWidgets();

    public final void addMessage(MessageType type, String translationKey, Object... args) {
        addMessage(type, 5000, translationKey, args);
    }

    public final void addMessage(MessageType type, int lifeTime, String translationKey, Object... args) {
        messages.add(type, lifeTime, UiTranslations.format(translationKey, args));
    }

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
                boolean top = i == modals.size() - 1;
                modals.get(i).draw(draw, top ? mouseX : -1, top ? mouseY : -1);
            }
            drawTooltip(draw, mouseX, mouseY);
            messages.draw(draw, width / 2, height / 2, fontRendererObj.FONT_HEIGHT + 1);
        }
    }

    private void drawTooltip(UiDraw draw, int mouseX, int mouseY) {
        UiWidget target = input.hit(mouseX, mouseY);
        if (target == null) return;
        if (target.drawTooltip(draw, mouseX, mouseY, root.bounds())) return;
        List<String> lines = new ArrayList<>();
        int maxWidth = Math.max(1, width - 16);
        for (String text : target.tooltip(mouseX, mouseY)) {
            for (String line : text.split("\n")) lines.addAll(fontRendererObj.listFormattedStringToWidth(line, maxWidth));
        }
        int lineHeight = fontRendererObj.FONT_HEIGHT + 1;
        int maxLines = Math.max(1, (height - 16) / lineHeight);
        if (lines.size() > maxLines) {
            lines = new ArrayList<>(lines.subList(0, maxLines));
            lines.set(maxLines - 1, "...");
        }
        if (lines.isEmpty()) return;
        int textWidth = 0;
        for (String line : lines) textWidth = Math.max(textWidth, draw.textWidth(line));
        int textHeight = lines.size() * lineHeight - 2;
        int x = mouseX + 4;
        int y = Math.max(8, mouseY - textHeight - 6);
        if (x + textWidth + 6 > width) x = Math.max(2, width - textWidth - 8);
        int left = x - 3, top = y - 3, right = x + textWidth + 3, bottom = y + textHeight + 3;
        int background = 0xF0100010, light = 0x505000FF, dark = 0x5028007F;
        draw.fill(new UiBounds(left, top - 1, right - left, 1), background);
        draw.fill(new UiBounds(left, bottom, right - left, 1), background);
        draw.fill(new UiBounds(left, top, right - left, bottom - top), background);
        draw.fill(new UiBounds(left - 1, top, 1, bottom - top), background);
        draw.fill(new UiBounds(right, top, 1, bottom - top), background);
        draw.colorGrid(new UiBounds(left, top + 1, 1, bottom - top - 2), 1, 1, new int[] {light, light, dark, dark});
        draw.colorGrid(new UiBounds(right - 1, top + 1, 1, bottom - top - 2), 1, 1, new int[] {light, light, dark, dark});
        draw.fill(new UiBounds(left, top, right - left, 1), light);
        draw.fill(new UiBounds(left, bottom - 1, right - left, 1), dark);
        for (String line : lines) {
            draw.text(line, x, y, 0xFFFFFFFF);
            y += lineHeight;
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
            if (screen instanceof GuiSchematicMainMenu) {
                mc.displayGuiScreen(screen);
                return;
            }
            screen = ((UiScreen) screen).parent;
        }
        mc.displayGuiScreen(new GuiSchematicMainMenu(null));
    }

    protected final UiButton unavailable(UiButton button) {
        button.setEnabled(false);
        button.setTooltip(UiTranslations.format("schematica.ui.pending"));
        return button;
    }

    protected final void confirm(String title, String message, Runnable confirmed) {
        confirm(title, message, "malilib.gui.button.ok", confirmed);
    }

    protected final void confirm(String title, String message, String acceptKey, Runnable confirmed) {
        UiPanel panel = new ConfirmationPanel(title, message, acceptKey, confirmed);
        panel.layout(root.bounds());
        input.pushModal(panel);
    }

    protected final UiButton addIssueReportButton() {
        UiButton button = root.add(new UiButton(() -> "", mouseButton -> {
            if (mouseButton != 0) return;
            if (IssueReport.skipIntroduction()) {
                openIssueTracker();
                return;
            }
            UiPanel panel = new ConfirmationPanel(UiTranslations.format("schematica.ui.report.title"),
                UiTranslations.format("schematica.ui.report.message").replace("\\n", "\n")
                    .replace("Pull Request", "Pull\u00a0Request"), "malilib.gui.button.ok",
                "schematica.ui.report.dont_show_again", skip -> {
                    if (skip) IssueReport.rememberSkipIntroduction();
                    openIssueTracker();
                });
            panel.layout(root.bounds());
            input.pushModal(panel);
        })).setSprite(UiSprite.REPORT_ISSUE);
        button.setTooltip(UiTranslations.format("schematica.ui.report.tooltip"));
        return button;
    }

    private void openIssueTracker() {
        IssueReport.open(() -> {
            if (mc.currentScreen == this) {
                confirm(UiTranslations.format("schematica.ui.report.open_failed"), IssueReport.URL,
                    "schematica.ui.report.copy_link", () -> GuiScreen.setClipboardString(IssueReport.URL));
            }
        });
    }

    protected final void prompt(String title, String initial, Function<String, String> apply) {
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
        private final UiCheckBox remember;
        private boolean checked;
        private List<String> lines;
        private UiBounds content;
        private int scroll;
        private boolean dragging;
        private int dragOffset;

        ConfirmationPanel(String title, String message, String acceptKey, Runnable confirmed) {
            this(title, message, acceptKey, null, ignored -> confirmed.run());
        }

        ConfirmationPanel(String title, String message, String acceptKey, String rememberKey, Consumer<Boolean> confirmed) {
            this.dialogTitle = title;
            this.message = message;
            remember = rememberKey == null ? null : add(new UiCheckBox(() -> UiTranslations.format(rememberKey),
                () -> checked, value -> checked = value));
            cancel = add(new UiButton(() -> "\u00a7c" + UiTranslations.format("malilib.gui.button.cancel") + "\u00a7r", button -> {
                if (button == 0) input.popModal();
            }));
            accept = add(new UiButton(() -> "\u00a7a" + UiTranslations.format(acceptKey) + "\u00a7r", button -> {
                if (button == 0) {
                    input.popModal();
                    confirmed.accept(checked);
                }
            }));
        }

        @Override
        public void layout(UiBounds screen) {
            int width = Math.max(40, Math.min(400, screen.width - 24));
            lines = UiTextWrap.wrap(message, Math.max(1, width - 30), fontRendererObj::getStringWidth);
            int buttonWidth = Math.max(fontRendererObj.getStringWidth(accept.label()), fontRendererObj.getStringWidth(cancel.label())) + 10;
            int rememberWidth = remember == null ? 0 : fontRendererObj.getStringWidth(remember.label()) + 15;
            int footer = remember != null && rememberWidth + 2 * buttonWidth + 40 > width ? 18 : 0;
            int height = Math.min(screen.height - 16, 54 + footer + lines.size() * lineHeight());
            setBounds((screen.width - width) / 2, (screen.height - height) / 2, width, height);
            content = new UiBounds(bounds().x + 10, bounds().y + 20, width - 20, height - 48 - footer);
            setScroll(scroll);
            accept.setBounds(bounds().x + 10, bounds().bottom() - 24, buttonWidth, 20);
            cancel.setBounds(accept.bounds().right() + 10, bounds().bottom() - 24, buttonWidth, 20);
            if (remember != null) {
                if (footer == 0) remember.setBounds(cancel.bounds().right() + 10, bounds().bottom() - 21, bounds().right() - cancel.bounds().right() - 20, 14);
                else remember.setBounds(bounds().x + 10, bounds().bottom() - 21 - footer, width - 20, 14);
            }
        }

        private int lineHeight() { return fontRendererObj.FONT_HEIGHT + 1; }
        private int maxScroll() { return Math.max(0, lines.size() * lineHeight() - content.height); }
        private void setScroll(int value) { scroll = Math.max(0, Math.min(maxScroll(), value)); }

        private UiBounds track() { return new UiBounds(content.right() - 6, content.y, 6, content.height); }

        private UiBounds thumb() { return UiScrollBar.thumb(track(), lines.size() * lineHeight(), scroll, maxScroll()); }

        @Override public boolean isFocusable() { return maxScroll() > 0; }

        @Override
        public boolean scroll(int x, int y, int amount) {
            setScroll(scroll - Integer.signum(amount) * lineHeight() * 3);
            return true;
        }

        @Override
        public boolean keyTyped(char character, int keyCode) {
            switch (keyCode) {
                case Keyboard.KEY_UP: setScroll(scroll - lineHeight()); return true;
                case Keyboard.KEY_DOWN: setScroll(scroll + lineHeight()); return true;
                case Keyboard.KEY_PRIOR: setScroll(scroll - content.height); return true;
                case Keyboard.KEY_NEXT: setScroll(scroll + content.height); return true;
                case Keyboard.KEY_HOME: setScroll(0); return true;
                case Keyboard.KEY_END: setScroll(maxScroll()); return true;
                default: return false;
            }
        }

        @Override
        public boolean mouseDown(int x, int y, int button) {
            if (button != 0 || maxScroll() == 0 || !content.contains(x, y) || x < content.right() - 6) return false;
            UiBounds thumb = thumb();
            dragOffset = thumb.contains(x, y) ? y - thumb.y : thumb.height / 2;
            dragging = true;
            mouseDrag(x, y, button);
            return true;
        }

        @Override
        public void mouseDrag(int x, int y, int button) {
            if (dragging) {
                setScroll(UiScrollBar.offset(track(), thumb(), y, dragOffset, maxScroll()));
            }
        }

        @Override public void mouseUp(int x, int y, int button) { dragging = false; }
        @Override public void cancelMouse() { dragging = false; }

        @Override
        public void draw(UiDraw draw, int mouseX, int mouseY) {
            draw.fill(bounds(), 0xF0000000);
            draw.outline(bounds(), 0xFF999999);
            draw.shadowText(draw.trim(dialogTitle, bounds().width - 20), bounds().x + 10, bounds().y + 4, 0xFFFFFFFF);
            try (UiDraw.Clip ignored = draw.clip(new UiBounds(content.x, content.y, content.width - 6, content.height))) {
                int y = content.y - scroll;
                for (String line : lines) {
                    if (y + lineHeight() > content.y && y < content.bottom()) draw.text(line, content.x, y, 0xFFC0C0C0);
                    y += lineHeight();
                }
            }
            if (maxScroll() > 0) UiScrollBar.draw(draw, track(), thumb());
            super.draw(draw, mouseX, mouseY);
        }
    }
}

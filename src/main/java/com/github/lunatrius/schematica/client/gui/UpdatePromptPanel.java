package com.github.lunatrius.schematica.client.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.ToIntFunction;

import net.minecraft.client.gui.FontRenderer;

import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiDraw;
import com.github.lunatrius.schematica.client.gui.framework.UiPanel;
import com.github.lunatrius.schematica.client.gui.framework.UiScrollBar;
import com.github.lunatrius.schematica.client.gui.framework.UiTextWrap;
import com.github.lunatrius.schematica.client.gui.framework.UiTheme;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.update.UpdateManager;
import com.github.lunatrius.schematica.client.update.UpdateRelease;

import org.lwjgl.input.Keyboard;

public final class UpdatePromptPanel extends UiPanel {

    private final UpdateManager manager;
    private final ToIntFunction<String> textWidth;
    private final int fontHeight;
    private final BiFunction<String, Object[], String> translator;
    private final Runnable closeModal;
    private final Runnable openSettings;

    private final UiButton downloadButton;
    private final UiButton dismissSessionButton;
    private final UiButton ignoreVersionButton;
    private final UiButton settingsButton;
    private final UiButton cancelDownloadButton;
    private final UiButton backgroundButton;
    private final UiButton closeGameNowButton;
    private final UiButton laterButton;
    private final UiButton checkAgainButton;
    private final UiButton closeButton;
    private final List<UiButton> allButtons;

    private UiBounds screenBounds = new UiBounds(0, 0, 420, 240);
    private UiBounds content = new UiBounds(0, 0, 0, 0);
    private UiBounds progressBar = new UiBounds(0, 0, 0, 0);
    private List<String> lines = Collections.emptyList();
    private String dialogTitle = "";
    private String message = "";
    private UpdateManager.State lastState;
    private String lastReleaseTag;
    private long lastDownloaded = -1L;
    private long lastTotal = -2L;
    private boolean lastVerifying;
    private String lastError;
    private int scroll;
    private boolean dragging;
    private int dragOffset;

    public UpdatePromptPanel(FontRenderer font, UpdateManager manager, Runnable closeModal, Runnable openSettings) {
        this(font::getStringWidth, font.FONT_HEIGHT, UiTranslations::format, manager, closeModal, openSettings);
    }

    UpdatePromptPanel(ToIntFunction<String> textWidth, int fontHeight, BiFunction<String, Object[], String> translator,
        UpdateManager manager, Runnable closeModal, Runnable openSettings) {
        this.textWidth = textWidth;
        this.fontHeight = fontHeight;
        this.translator = translator;
        this.manager = manager;
        this.closeModal = closeModal;
        this.openSettings = openSettings;

        downloadButton = add(new UiButton(() -> "\u00a7a" + tr("schematica.ui.update.download") + "\u00a7r", button -> {
            if (button == 0) manager.startDownload();
        }));
        dismissSessionButton = add(new UiButton(() -> tr("schematica.ui.update.dismiss_session"), button -> {
            if (button == 0) {
                manager.dismissForSession();
                closeModal.run();
            }
        }));
        ignoreVersionButton = add(new UiButton(() -> tr("schematica.ui.update.ignore_version"), button -> {
            if (button == 0) {
                manager.ignoreAvailableVersion();
                closeModal.run();
            }
        }));
        settingsButton = add(new UiButton(() -> tr("schematica.ui.update.open_settings"), button -> {
            if (button == 0) {
                closeModal.run();
                openSettings.run();
            }
        }));
        cancelDownloadButton = add(new UiButton(() -> "\u00a7c" + tr("malilib.gui.button.cancel") + "\u00a7r", button -> {
            if (button == 0) manager.cancelDownload();
        }));
        backgroundButton = add(new UiButton(() -> tr("schematica.ui.update.background"), button -> {
            if (button == 0) closeModal.run();
        }));
        closeGameNowButton = add(new UiButton(() -> "\u00a7a" + tr("schematica.ui.update.close_game_now") + "\u00a7r", button -> {
            if (button == 0) {
                closeModal.run();
                manager.applyNowAndShutdown();
            }
        }));
        laterButton = add(new UiButton(() -> tr("schematica.ui.update.later"), button -> {
            if (button == 0) {
                manager.deferApplyUntilExit();
                closeModal.run();
            }
        }));
        checkAgainButton = add(new UiButton(() -> "\u00a7a" + tr("schematica.ui.update.check_now") + "\u00a7r", button -> {
            if (button == 0) manager.checkForUpdates(true);
        }));
        closeButton = add(new UiButton(() -> tr("schematica.ui.update.close"), button -> {
            if (button == 0) closeModal.run();
        }));
        allButtons = Arrays.asList(
            downloadButton, dismissSessionButton, ignoreVersionButton, settingsButton,
            cancelDownloadButton, backgroundButton, closeGameNowButton, laterButton,
            checkAgainButton, closeButton);
        syncFromManager(true);
    }

    private String tr(String key, Object... args) {
        return translator.apply(key, args).replace("\\n", "\n");
    }

    @Override
    public void tick() {
        syncFromManager(false);
        super.tick();
    }

    private void syncFromManager(boolean force) {
        UpdateManager.State state = manager.state();
        UpdateRelease release = manager.availableRelease();
        String tag = release == null ? null : release.tag;
        long done = manager.downloadedBytes();
        long total = manager.totalBytes();
        boolean checking = manager.verifying();
        String error = manager.errorMessage();
        boolean stateChanged = state != lastState || !equalsNullable(tag, lastReleaseTag);
        boolean dataChanged = force || stateChanged || done != lastDownloaded || total != lastTotal
            || checking != lastVerifying || !equalsNullable(error, lastError);
        if (!dataChanged) return;

        lastState = state;
        lastReleaseTag = tag;
        lastDownloaded = done;
        lastTotal = total;
        lastVerifying = checking;
        lastError = error;

        if (stateChanged) scroll = 0;

        for (UiButton button : allButtons) button.setVisible(false);
        switch (state) {
            case UPDATE_AVAILABLE:
                dialogTitle = tr("schematica.ui.update.title.available");
                message = buildAvailableMessage(release);
                downloadButton.setVisible(true);
                dismissSessionButton.setVisible(true);
                ignoreVersionButton.setVisible(true);
                settingsButton.setVisible(true);
                break;
            case DOWNLOADING:
                dialogTitle = tr("schematica.ui.update.title.downloading");
                message = buildDownloadingMessage(release, done, total, checking);
                cancelDownloadButton.setVisible(true);
                backgroundButton.setVisible(true);
                break;
            case READY_TO_APPLY:
                dialogTitle = tr("schematica.ui.update.title.ready");
                message = tr("schematica.ui.update.ready", release == null ? "" : release.tag);
                closeGameNowButton.setVisible(true);
                laterButton.setVisible(true);
                break;
            case CHECKING:
                dialogTitle = tr("schematica.ui.update.title.checking");
                message = tr("schematica.ui.update.checking", manager.currentVersion());
                settingsButton.setVisible(true);
                closeButton.setVisible(true);
                break;
            case FAILED:
                dialogTitle = tr("schematica.ui.update.title.failed");
                message = tr("schematica.ui.update.failed", error == null ? "Unknown error" : error);
                if (release != null) downloadButton.setVisible(true);
                else checkAgainButton.setVisible(true);
                settingsButton.setVisible(true);
                closeButton.setVisible(true);
                break;
            case IDLE:
            case UP_TO_DATE:
            default:
                dialogTitle = tr("schematica.ui.update.title.up_to_date");
                message = tr("schematica.ui.update.up_to_date", manager.currentVersion());
                checkAgainButton.setVisible(true);
                settingsButton.setVisible(true);
                closeButton.setVisible(true);
                break;
        }
        if (screenBounds.width > 0 && screenBounds.height > 0) {
            layout(screenBounds);
        }
    }

    private String buildAvailableMessage(UpdateRelease release) {
        if (release == null) return "";
        StringBuilder builder = new StringBuilder();
        builder.append(tr("schematica.ui.update.version_change", manager.currentVersion(), release.tag));
        if (release.assetName != null && !release.assetName.isEmpty()) {
            builder.append("\n").append(tr("schematica.ui.update.asset_info",
                release.assetName, UpdateRelease.formatBytes(release.assetSize)));
        }
        if (!manager.canReplaceInPlace()) {
            builder.append("\n\n").append(tr("schematica.ui.update.dev_warning"));
        }
        builder.append("\n\n").append(tr("schematica.ui.update.notes_header")).append("\n");
        String notes = release.body == null ? "" : release.body.replace("\r\n", "\n").replace('\r', '\n').trim();
        builder.append(notes.isEmpty() ? tr("schematica.ui.update.notes_empty") : notes);
        return builder.toString();
    }

    private String buildDownloadingMessage(UpdateRelease release, long done, long total, boolean checking) {
        String name = release == null ? "" : release.assetName;
        if (checking) return tr("schematica.ui.update.verifying", name);
        if (total > 0L) {
            int percent = (int) Math.max(0L, Math.min(100L, done * 100L / total));
            return tr("schematica.ui.update.downloading_progress", name,
                UpdateRelease.formatBytes(done), UpdateRelease.formatBytes(total), percent);
        }
        return tr("schematica.ui.update.downloading_unknown", name, UpdateRelease.formatBytes(done));
    }

    @Override
    public void layout(UiBounds screen) {
        screenBounds = screen;
        int width = Math.max(220, Math.min(420, screen.width - 24));
        lines = UiTextWrap.wrap(message, Math.max(1, width - 30), textWidth::applyAsInt);
        List<List<UiButton>> buttonRows = buildButtonRows(width - 20);
        int buttonsHeight = buttonRows.isEmpty() ? 0 : buttonRows.size() * 20 + (buttonRows.size() - 1) * 4;
        int progressHeight = lastState == UpdateManager.State.DOWNLOADING ? 16 : 0;
        int chromeHeight = 32 + progressHeight + buttonsHeight;
        int desiredHeight = chromeHeight + Math.max(2, lines.size()) * lineHeight();
        int height = Math.min(screen.height - 16, Math.max(chromeHeight + lineHeight() * 2, desiredHeight));
        setBounds((screen.width - width) / 2, (screen.height - height) / 2, width, height);

        int contentHeight = Math.max(lineHeight(), height - chromeHeight);
        content = new UiBounds(bounds().x + 10, bounds().y + 20, width - 20, contentHeight);
        setScroll(scroll);

        int footerTop = bounds().bottom() - 6 - buttonsHeight;
        if (progressHeight > 0) {
            progressBar = new UiBounds(bounds().x + 10, footerTop - 14, width - 20, 8);
        } else {
            progressBar = new UiBounds(0, 0, 0, 0);
        }

        int rowY = footerTop;
        for (List<UiButton> row : buttonRows) {
            layoutButtonRow(row, bounds().x + 10, rowY, width - 20);
            rowY += 24;
        }
    }

    private List<List<UiButton>> buildButtonRows(int innerWidth) {
        if (lastState == UpdateManager.State.UPDATE_AVAILABLE) {
            return Arrays.asList(
                Arrays.asList(downloadButton, dismissSessionButton),
                Arrays.asList(ignoreVersionButton, settingsButton));
        }
        List<UiButton> visible = new ArrayList<>();
        for (UiButton button : allButtons) {
            if (button.isVisible()) visible.add(button);
        }
        if (visible.isEmpty()) return Collections.emptyList();
        int totalPreferred = 0;
        for (UiButton button : visible) totalPreferred += textWidth.applyAsInt(button.label()) + 14;
        totalPreferred += Math.max(0, visible.size() - 1) * 6;
        if (totalPreferred <= innerWidth || visible.size() <= 2) {
            return Collections.singletonList(visible);
        }
        int split = (visible.size() + 1) / 2;
        return Arrays.asList(visible.subList(0, split), visible.subList(split, visible.size()));
    }

    private void layoutButtonRow(List<UiButton> row, int startX, int y, int innerWidth) {
        int count = row.size();
        if (count == 0) return;
        int gap = 6;
        int cellWidth = Math.max(40, (innerWidth - gap * (count - 1)) / count);
        int x = startX;
        for (int i = 0; i < count; i++) {
            int w = i == count - 1 ? startX + innerWidth - x : cellWidth;
            row.get(i).setBounds(x, y, Math.max(20, w), 20);
            x += cellWidth + gap;
        }
    }

    private int lineHeight() { return fontHeight + 1; }
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
        try (UiDraw.Clip ignored = draw.clip(new UiBounds(content.x, content.y, Math.max(0, content.width - 6), content.height))) {
            int y = content.y - scroll;
            for (String line : lines) {
                if (y + lineHeight() > content.y && y < content.bottom()) draw.text(line, content.x, y, 0xFFC0C0C0);
                y += lineHeight();
            }
        }
        if (maxScroll() > 0) UiScrollBar.draw(draw, track(), thumb());
        if (progressBar.width > 0 && progressBar.height > 0) {
            drawProgressBar(draw);
        }
        super.draw(draw, mouseX, mouseY);
    }

    private void drawProgressBar(UiDraw draw) {
        draw.fill(progressBar, UiTheme.FIELD);
        draw.outline(progressBar, UiTheme.BORDER);
        int innerWidth = Math.max(0, progressBar.width - 2);
        int innerHeight = Math.max(0, progressBar.height - 2);
        if (innerWidth <= 0 || innerHeight <= 0) return;
        if (lastVerifying) {
            draw.fill(new UiBounds(progressBar.x + 1, progressBar.y + 1, innerWidth, innerHeight), 0xFF44AA55);
            return;
        }
        if (lastTotal > 0L) {
            int filled = (int) Math.max(0L, Math.min(innerWidth, lastDownloaded * innerWidth / lastTotal));
            if (filled > 0) {
                draw.fill(new UiBounds(progressBar.x + 1, progressBar.y + 1, filled, innerHeight), 0xFF44AA55);
            }
        }
    }

    UiButton downloadButton() { return downloadButton; }
    UiButton dismissSessionButton() { return dismissSessionButton; }
    UiButton ignoreVersionButton() { return ignoreVersionButton; }
    UiButton settingsButton() { return settingsButton; }
    UiButton closeGameNowButton() { return closeGameNowButton; }
    UiButton laterButton() { return laterButton; }

    private static boolean equalsNullable(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }
}

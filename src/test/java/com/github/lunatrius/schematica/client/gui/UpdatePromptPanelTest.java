package com.github.lunatrius.schematica.client.gui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiInput;
import com.github.lunatrius.schematica.client.gui.framework.UiPanel;
import com.github.lunatrius.schematica.client.update.UpdateHttpClient;
import com.github.lunatrius.schematica.client.update.UpdateManager;
import com.github.lunatrius.schematica.client.update.UpdateManagerTestHelper;
import com.github.lunatrius.schematica.client.update.UpdateRelease;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UpdatePromptPanelTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    private static final String RELEASE_JSON = "[{\"tag_name\":\"1.0.0-beta.6\",\"name\":\"1.0.0-beta.6\","
        + "\"body\":\"Line 1\\nLine 2\",\"html_url\":\"https://example.invalid/r\",\"draft\":false,\"prerelease\":false,"
        + "\"assets\":[{\"name\":\"schematica_plus-1.0.0-beta.6.jar\",\"size\":16,\"browser_download_url\":\"https://example.invalid/b6.jar\"}]}]";

    private static UpdateManager createManager(File modsDir, File updatesDir, AtomicReference<String> ignored,
        AtomicBoolean shutdownCalled) throws IOException {
        File currentJar = new File(modsDir, "schematica_plus-1.0.0-beta.5.jar");
        Files.write(currentJar.toPath(), new byte[] {1});
        return UpdateManagerTestHelper.create(
            new UpdateManager.Transport() {
                @Override public String fetchReleasesJson() { return RELEASE_JSON; }
                @Override public void download(UpdateRelease release, File destination, AtomicBoolean cancelled,
                    UpdateHttpClient.ProgressListener listener) throws IOException {
                    Files.createDirectories(destination.toPath().getParent());
                    Files.write(destination.toPath(), new byte[] {1, 2, 3, 4});
                    if (listener != null) listener.onProgress(4L, 4L, false);
                }
            },
            currentJar,
            updatesDir,
            ignored,
            shutdownCalled);
    }

    private static void click(UiInput input, UiButton button) {
        int x = button.bounds().x + button.bounds().width / 2;
        int y = button.bounds().y + button.bounds().height / 2;
        input.mouseDown(x, y, 0);
        input.mouseUp(x, y, 0);
    }

    @Test public void exposesFourActionsOnAvailableUpdateAndTransitionsToReadyPrompt() throws Exception {
        File modsDir = temporary.newFolder("mods");
        File updatesDir = temporary.newFolder("updates");
        AtomicReference<String> ignored = new AtomicReference<>("");
        AtomicBoolean shutdownCalled = new AtomicBoolean(false);
        UpdateManager manager = createManager(modsDir, updatesDir, ignored, shutdownCalled);

        manager.checkForUpdates(false);
        waitUntil(() -> manager.state() == UpdateManager.State.UPDATE_AVAILABLE);

        UiPanel root = new UiPanel();
        root.setBounds(0, 0, 480, 320);
        UiInput input = new UiInput(root);
        AtomicInteger closed = new AtomicInteger();
        AtomicInteger settingsOpened = new AtomicInteger();

        UpdatePromptPanel panel = new UpdatePromptPanel(text -> text.length() * 6, 9, (key, args) -> key, manager,
            () -> { closed.incrementAndGet(); input.popModal(); },
            settingsOpened::incrementAndGet);
        panel.layout(root.bounds());
        input.pushModal(panel);

        assertTrue(panel.downloadButton().isVisible());
        assertTrue(panel.dismissSessionButton().isVisible());
        assertTrue(panel.ignoreVersionButton().isVisible());
        assertTrue(panel.settingsButton().isVisible());
        assertFalse(panel.closeGameNowButton().isVisible());
        assertFalse(panel.laterButton().isVisible());

        click(input, panel.settingsButton());
        assertEquals(1, closed.get());
        assertEquals(1, settingsOpened.get());

        input.pushModal(panel);
        click(input, panel.downloadButton());
        waitUntil(() -> manager.state() == UpdateManager.State.READY_TO_APPLY);
        panel.tick();

        assertTrue(panel.closeGameNowButton().isVisible());
        assertTrue(panel.laterButton().isVisible());
        assertFalse(panel.downloadButton().isVisible());

        click(input, panel.closeGameNowButton());
        assertTrue(shutdownCalled.get());
        assertTrue(input.modalPanels().isEmpty());
    }

    @Test public void dismissAndIgnoreButtonsUpdateManagerState() throws Exception {
        File modsDir = temporary.newFolder("mods");
        File updatesDir = temporary.newFolder("updates");
        AtomicReference<String> ignored = new AtomicReference<>("");
        AtomicBoolean shutdownCalled = new AtomicBoolean(false);
        UpdateManager manager = createManager(modsDir, updatesDir, ignored, shutdownCalled);

        manager.checkForUpdates(false);
        waitUntil(() -> manager.state() == UpdateManager.State.UPDATE_AVAILABLE);

        UiPanel root = new UiPanel();
        root.setBounds(0, 0, 480, 320);
        UiInput input = new UiInput(root);
        UpdatePromptPanel panel = new UpdatePromptPanel(text -> text.length() * 6, 9, (key, args) -> key, manager,
            input::popModal, () -> {});
        panel.layout(new UiBounds(0, 0, 480, 320));
        input.pushModal(panel);

        click(input, panel.dismissSessionButton());
        assertFalse(manager.shouldAutoPrompt());
        assertTrue(manager.hasBadge());

        input.pushModal(panel);
        click(input, panel.ignoreVersionButton());
        assertEquals("1.0.0-beta.6", ignored.get());
        assertFalse(manager.hasBadge());
    }

    private static void waitUntil(java.util.function.BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + 5000000000L;
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) throw new AssertionError("Timed out waiting for condition");
            Thread.sleep(20L);
        }
    }
}

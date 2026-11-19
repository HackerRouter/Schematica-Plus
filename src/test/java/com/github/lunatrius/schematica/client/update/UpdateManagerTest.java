package com.github.lunatrius.schematica.client.update;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class UpdateManagerTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    private static final String RELEASE_JSON = "[{\"tag_name\":\"1.0.0-beta.6\",\"name\":\"1.0.0-beta.6\","
        + "\"body\":\"Fixes\",\"html_url\":\"https://example.invalid/r\",\"draft\":false,\"prerelease\":false,"
        + "\"assets\":[{\"name\":\"schematica_plus-1.0.0-beta.6.jar\",\"size\":128,\"browser_download_url\":\"https://example.invalid/b6.jar\"}]}]";

    @Test public void tracksPromptDismissIgnoreDownloadAndShutdownApply() throws Exception {
        File modsDir = temporary.newFolder("mods");
        File updatesDir = temporary.newFolder("updates");
        File currentJar = new File(modsDir, "schematica_plus-1.0.0-beta.5.jar");
        Files.write(currentJar.toPath(), new byte[] {1});

        AtomicReference<String> ignored = new AtomicReference<>("");
        AtomicBoolean shutdownCalled = new AtomicBoolean(false);
        AtomicReference<File[]> scheduled = new AtomicReference<>();

        UpdateManager.Transport transport = new UpdateManager.Transport() {
            @Override
            public String fetchReleasesJson() {
                return RELEASE_JSON;
            }

            @Override
            public void download(UpdateRelease release, File destination, AtomicBoolean cancelled,
                UpdateHttpClient.ProgressListener listener) throws IOException {
                Files.createDirectories(destination.toPath().getParent());
                Files.write(destination.toPath(), new byte[] {1, 2, 3});
                if (listener != null) listener.onProgress(3L, 3L, false);
            }
        };

        UpdateManager manager = new UpdateManager(
            transport,
            () -> currentJar,
            (dir, oldJar, stagedJar, targetJar) -> scheduled.set(new File[] {dir, oldJar, stagedJar, targetJar}),
            () -> "1.0.0-beta.5",
            () -> "auto",
            ignored::get,
            ignored::set,
            () -> updatesDir,
            () -> shutdownCalled.set(true),
            false);

        manager.checkForUpdates(false);
        waitUntil(() -> manager.state() == UpdateManager.State.UPDATE_AVAILABLE);
        assertTrue(manager.shouldAutoPrompt());
        assertTrue(manager.hasBadge());

        manager.dismissForSession();
        assertFalse(manager.shouldAutoPrompt());
        assertTrue(manager.hasBadge());

        manager.ignoreAvailableVersion();
        assertEquals("1.0.0-beta.6", ignored.get());
        assertFalse(manager.shouldAutoPrompt());
        assertFalse(manager.hasBadge());

        ignored.set("");
        assertTrue(manager.hasBadge());

        manager.startDownload();
        waitUntil(() -> manager.state() == UpdateManager.State.READY_TO_APPLY);
        assertTrue(manager.shouldAutoPrompt());
        assertTrue(manager.hasBadge());

        manager.deferApplyUntilExit();
        assertFalse(manager.shouldAutoPrompt());
        assertTrue(manager.hasBadge());

        manager.applyNowAndShutdown();
        assertTrue(shutdownCalled.get());

        manager.launchScheduledUpdateOnExit();
        File[] launched = scheduled.get();
        assertNotNull(launched);
        assertEquals(updatesDir, launched[0]);
        assertEquals(currentJar, launched[1]);
        assertEquals(new File(updatesDir, "schematica_plus-1.0.0-beta.6.jar"), launched[2]);
        assertEquals(new File(modsDir, "schematica_plus-1.0.0-beta.6.jar"), launched[3]);
    }

    @Test public void failsGracefullyOutsidePackagedJar() throws Exception {
        File updatesDir = temporary.newFolder("updates");
        UpdateManager manager = new UpdateManager(
            new UpdateManager.Transport() {
                @Override public String fetchReleasesJson() { return RELEASE_JSON; }
                @Override public void download(UpdateRelease release, File destination, AtomicBoolean cancelled,
                    UpdateHttpClient.ProgressListener listener) {}
            },
            () -> null,
            (dir, oldJar, stagedJar, targetJar) -> {},
            () -> "1.0.0-beta.5",
            () -> "auto",
            () -> "",
            value -> {},
            () -> updatesDir,
            () -> {},
            false);

        manager.checkForUpdates(false);
        waitUntil(() -> manager.state() == UpdateManager.State.UPDATE_AVAILABLE);
        manager.startDownload();
        assertEquals(UpdateManager.State.FAILED, manager.state());
        assertTrue(manager.errorMessage().contains("packaged mod JAR"));
    }

    private static void waitUntil(java.util.function.BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + 5000000000L;
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) throw new AssertionError("Timed out waiting for condition");
            Thread.sleep(20L);
        }
    }
}

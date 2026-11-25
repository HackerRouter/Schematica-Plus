package com.github.lunatrius.schematica.update;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.util.PlusDataFiles;

public final class UpdateManager {

    public enum State {
        IDLE,
        CHECKING,
        UP_TO_DATE,
        UPDATE_AVAILABLE,
        DOWNLOADING,
        READY_TO_APPLY,
        FAILED
    }

    public interface Transport {
        String fetchReleasesJson() throws IOException;
        void download(UpdateRelease release, File destination, AtomicBoolean cancelled,
            UpdateHttpClient.ProgressListener listener) throws IOException;
    }

    public interface ApplicatorStarter {
        void start(File updatesDir, File oldJar, File stagedJar, File targetJar) throws IOException;
    }

    public static final UpdateManager INSTANCE = new UpdateManager(
        new ManifestWithFallbackTransport(GitHubReleaseParser.MANIFEST_URL, GitHubReleaseParser.RELEASES_API_URL),
        ModJarLocator::locateCurrentModJar,
        (updatesDir, oldJar, stagedJar, targetJar) -> UpdateApplicatorLauncher.applyOnExit(updatesDir, oldJar, stagedJar, targetJar, 60000L),
        () -> Reference.VERSION,
        () -> ConfigurationHandler.updateChannel,
        () -> ConfigurationHandler.ignoredUpdateVersion,
        ConfigurationHandler::setIgnoredUpdateVersion,
        () -> PlusDataFiles.file("updates"),
        () -> {},
        true);

    static final class ManifestWithFallbackTransport implements Transport {
        private final String manifestUrl;
        private final String apiUrl;
        private String manifestEtag;
        private String manifestBody;
        private String apiEtag;
        private String apiBody;

        ManifestWithFallbackTransport(String manifestUrl, String apiUrl) {
            this.manifestUrl = manifestUrl;
            this.apiUrl = apiUrl;
        }

        @Override
        public synchronized String fetchReleasesJson() throws IOException {
            try {
                UpdateHttpClient.CachedResponse response = UpdateHttpClient.fetchTextCached(
                    manifestUrl, "application/json", manifestEtag, manifestBody);
                manifestEtag = response.etag;
                manifestBody = response.body;
                return response.body;
            } catch (IOException manifestError) {
                Reference.logger.debug("Could not fetch update manifest, falling back to GitHub Releases API: {}",
                    manifestError.getMessage());
            }
            UpdateHttpClient.CachedResponse fallback = UpdateHttpClient.fetchTextCached(
                apiUrl, "application/vnd.github+json", apiEtag, apiBody);
            apiEtag = fallback.etag;
            apiBody = fallback.body;
            return fallback.body;
        }

        @Override
        public void download(UpdateRelease release, File destination, AtomicBoolean cancelled,
            UpdateHttpClient.ProgressListener listener) throws IOException {
            UpdateHttpClient.downloadVerifiedJar(release.downloadUrl, destination, release.assetSize,
                release.sha256, cancelled, listener);
        }
    }

    private final Transport transport;
    private final Supplier<File> jarLocator;
    private final ApplicatorStarter applicatorStarter;
    private final Supplier<String> currentVersionSupplier;
    private volatile Supplier<String> channelSupplier;
    private final Supplier<String> ignoredVersionSupplier;
    private final Consumer<String> ignoredVersionSaver;
    private final Supplier<File> updatesDirSupplier;
    private volatile Runnable shutdownAction;
    private final boolean registerJvmShutdownHook;

    private final Object lock = new Object();
    private final AtomicBoolean shutdownHookRegistered = new AtomicBoolean(false);
    private final AtomicBoolean downloadCancelled = new AtomicBoolean(false);

    private State state = State.IDLE;
    private UpdateRelease availableRelease;
    private File stagedFile;
    private File scheduledOldJar;
    private File scheduledTargetJar;
    private long downloadedBytes;
    private long totalBytes = -1L;
    private boolean verifying;
    private String errorMessage;
    private String dismissedTagThisSession;
    private boolean deferredApplyPrompt;

    UpdateManager(Transport transport, Supplier<File> jarLocator, ApplicatorStarter applicatorStarter,
        Supplier<String> currentVersionSupplier, Supplier<String> channelSupplier,
        Supplier<String> ignoredVersionSupplier, Consumer<String> ignoredVersionSaver,
        Supplier<File> updatesDirSupplier, Runnable shutdownAction, boolean registerJvmShutdownHook) {
        this.transport = transport;
        this.jarLocator = jarLocator;
        this.applicatorStarter = applicatorStarter;
        this.currentVersionSupplier = currentVersionSupplier;
        this.channelSupplier = channelSupplier;
        this.ignoredVersionSupplier = ignoredVersionSupplier;
        this.ignoredVersionSaver = ignoredVersionSaver;
        this.updatesDirSupplier = updatesDirSupplier;
        this.shutdownAction = shutdownAction;
        this.registerJvmShutdownHook = registerJvmShutdownHook;
    }

    public void configureRuntime(Runnable shutdownAction, Supplier<String> channelSupplier) {
        if (shutdownAction != null) this.shutdownAction = shutdownAction;
        if (channelSupplier != null) this.channelSupplier = channelSupplier;
    }

    public void onClientStartup() {
        startCleanupThread();
        if (ConfigurationHandler.checkForUpdatesOnStartup) {
            checkForUpdates(false);
        }
    }

    public void onServerStartup(boolean checkOnStartup) {
        startCleanupThread();
        if (checkOnStartup) {
            checkForUpdates(false, () -> {
                UpdateRelease release = availableRelease();
                if (state() == State.UPDATE_AVAILABLE && release != null) {
                    Reference.logger.info("A newer version of Schematica Plus ({}) is available (current: {}). Run '/schematicaplus update download' to stage it.",
                        release.tag, currentVersion());
                }
            });
        }
    }

    private void startCleanupThread() {
        Thread cleanup = new Thread(() -> UpdateApplicatorLauncher.cleanupStaleFiles(updatesDirSupplier.get(), stagedFile),
            "Schematica Plus update cleanup");
        cleanup.setDaemon(true);
        cleanup.start();
    }

    public void checkForUpdates(boolean manual) {
        checkForUpdates(manual, null);
    }

    public boolean checkForUpdates(boolean manual, Runnable onFinished) {
        synchronized (lock) {
            if (state == State.CHECKING || state == State.DOWNLOADING || state == State.READY_TO_APPLY) return false;
            state = State.CHECKING;
            errorMessage = null;
            if (manual) dismissedTagThisSession = null;
        }
        Thread thread = new Thread(() -> {
            runCheck();
            if (onFinished != null) onFinished.run();
        }, "Schematica Plus update check");
        thread.setDaemon(true);
        thread.start();
        return true;
    }

    void runCheck() {
        try {
            String json = transport.fetchReleasesJson();
            UpdateRelease found = GitHubReleaseParser.findLatestUpdate(json, currentVersionSupplier.get(), channelSupplier.get());
            synchronized (lock) {
                if (state != State.CHECKING) return;
                if (found != null) {
                    availableRelease = found;
                    state = State.UPDATE_AVAILABLE;
                    Reference.logger.info("Found Schematica Plus update {} (current: {})", found.tag, currentVersionSupplier.get());
                } else {
                    availableRelease = null;
                    state = State.UP_TO_DATE;
                }
            }
        } catch (Exception error) {
            Reference.logger.warn("Failed to check for Schematica Plus updates: {}", error.getMessage());
            synchronized (lock) {
                if (state != State.CHECKING) return;
                errorMessage = describeError(error);
                state = State.FAILED;
            }
        }
    }

    public void startDownload() {
        startDownload(null);
    }

    public boolean startDownload(Runnable onFinished) {
        UpdateRelease release;
        File currentJar;
        File targetJar;
        File destination;
        synchronized (lock) {
            if (state != State.UPDATE_AVAILABLE && state != State.FAILED) return false;
            release = availableRelease;
            if (release == null) return false;
            currentJar = jarLocator.get();
            if (currentJar == null) {
                errorMessage = "Automatic update requires running from a packaged mod JAR file.";
                state = State.FAILED;
                if (onFinished != null) onFinished.run();
                return false;
            }
            targetJar = ModJarLocator.resolveTargetJar(currentJar, release.assetName);
            if (targetJar == null) {
                errorMessage = "Invalid update filename received from release metadata.";
                state = State.FAILED;
                if (onFinished != null) onFinished.run();
                return false;
            }
            destination = new File(updatesDirSupplier.get(), targetJar.getName());
            downloadCancelled.set(false);
            downloadedBytes = 0L;
            totalBytes = release.assetSize;
            verifying = false;
            errorMessage = null;
            state = State.DOWNLOADING;
        }
        Thread thread = new Thread(() -> {
            runDownload(release, currentJar, targetJar, destination);
            if (onFinished != null) onFinished.run();
        }, "Schematica Plus update download");
        thread.setDaemon(true);
        thread.start();
        return true;
    }

    void runDownload(UpdateRelease release, File currentJar, File targetJar, File destination) {
        try {
            transport.download(release, destination, downloadCancelled, (done, total, checking) -> {
                synchronized (lock) {
                    downloadedBytes = done;
                    totalBytes = total;
                    verifying = checking;
                }
            });
            synchronized (lock) {
                if (downloadCancelled.get()) {
                    tryDelete(destination);
                    state = State.UPDATE_AVAILABLE;
                    verifying = false;
                    return;
                }
                stagedFile = destination;
                scheduledOldJar = currentJar;
                scheduledTargetJar = targetJar;
                verifying = false;
                deferredApplyPrompt = false;
                state = State.READY_TO_APPLY;
            }
            registerShutdownHookIfNeeded();
            Reference.logger.info("Downloaded Schematica Plus update {} to {}", release.tag, destination.getName());
        } catch (CancellationException cancelled) {
            synchronized (lock) {
                verifying = false;
                state = State.UPDATE_AVAILABLE;
            }
        } catch (Exception error) {
            Reference.logger.warn("Failed to download Schematica Plus update {}: {}", release.tag, error.getMessage());
            synchronized (lock) {
                verifying = false;
                errorMessage = describeError(error);
                state = State.FAILED;
            }
        }
    }

    public void cancelDownload() {
        downloadCancelled.set(true);
    }

    public void dismissForSession() {
        synchronized (lock) {
            if (availableRelease != null) dismissedTagThisSession = availableRelease.tag;
        }
    }

    public void ignoreAvailableVersion() {
        String tag;
        synchronized (lock) {
            if (availableRelease == null) return;
            tag = availableRelease.tag;
            dismissedTagThisSession = tag;
        }
        ignoredVersionSaver.accept(tag);
    }

    public void deferApplyUntilExit() {
        synchronized (lock) {
            deferredApplyPrompt = true;
        }
    }

    public void applyNowAndShutdown() {
        synchronized (lock) {
            if (state != State.READY_TO_APPLY || stagedFile == null || !stagedFile.isFile()) return;
        }
        registerShutdownHookIfNeeded();
        shutdownAction.run();
    }

    public boolean shouldAutoPrompt() {
        synchronized (lock) {
            if (state == State.READY_TO_APPLY) return !deferredApplyPrompt;
            if (state != State.UPDATE_AVAILABLE || availableRelease == null) return false;
            if (availableRelease.tag.equals(dismissedTagThisSession)) return false;
            String ignored = ignoredVersionSupplier.get();
            return ignored == null || !availableRelease.tag.equalsIgnoreCase(ignored.trim());
        }
    }

    public boolean hasBadge() {
        synchronized (lock) {
            if (state == State.READY_TO_APPLY) return true;
            if (state != State.UPDATE_AVAILABLE || availableRelease == null) return false;
            String ignored = ignoredVersionSupplier.get();
            return ignored == null || !availableRelease.tag.equalsIgnoreCase(ignored.trim());
        }
    }

    public State state() {
        synchronized (lock) { return state; }
    }

    public UpdateRelease availableRelease() {
        synchronized (lock) { return availableRelease; }
    }

    public long downloadedBytes() {
        synchronized (lock) { return downloadedBytes; }
    }

    public long totalBytes() {
        synchronized (lock) { return totalBytes; }
    }

    public boolean verifying() {
        synchronized (lock) { return verifying; }
    }

    public String errorMessage() {
        synchronized (lock) { return errorMessage; }
    }

    public String currentVersion() {
        return currentVersionSupplier.get();
    }

    public String currentChannel() {
        return channelSupplier.get();
    }

    public boolean canReplaceInPlace() {
        return jarLocator.get() != null;
    }

    private void registerShutdownHookIfNeeded() {
        if (!registerJvmShutdownHook) return;
        if (shutdownHookRegistered.compareAndSet(false, true)) {
            Runtime.getRuntime().addShutdownHook(new Thread(this::launchScheduledUpdateOnExit, "Schematica Plus update applicator"));
        }
    }

    void launchScheduledUpdateOnExit() {
        File oldJar;
        File staged;
        File target;
        synchronized (lock) {
            oldJar = scheduledOldJar;
            staged = stagedFile;
            target = scheduledTargetJar;
        }
        if (oldJar == null || staged == null || target == null || !staged.isFile()) return;
        try {
            applicatorStarter.start(updatesDirSupplier.get(), oldJar, staged, target);
        } catch (IOException error) {
            Reference.logger.error("Failed to launch Schematica Plus update applicator on exit: {}", error.getMessage());
        }
    }

    private static void tryDelete(File file) {
        if (file == null) return;
        try {
            java.nio.file.Files.deleteIfExists(file.toPath());
        } catch (IOException ignored) {
        }
    }

    private static String describeError(Exception error) {
        String message = error.getMessage();
        return message == null || message.trim().isEmpty() ? error.getClass().getSimpleName() : message;
    }
}

package com.github.lunatrius.schematica.client.update;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.zip.ZipFile;

public final class UpdateApplicatorMain {

    private static final long DEFAULT_TIMEOUT_MS = 60000L;
    private static final long POLL_INTERVAL_MS = 250L;

    private UpdateApplicatorMain() {}

    public static void main(String[] args) {
        if (args.length < 4) {
            System.exit(2);
            return;
        }
        Path oldJar = Paths.get(args[0]);
        Path stagedJar = Paths.get(args[1]);
        Path targetJar = Paths.get(args[2]);
        Path logFile = Paths.get(args[3]);
        long timeoutMs = DEFAULT_TIMEOUT_MS;
        if (args.length >= 5) {
            try {
                timeoutMs = Math.max(1000L, Long.parseLong(args[4]));
            } catch (NumberFormatException ignored) {
                timeoutMs = DEFAULT_TIMEOUT_MS;
            }
        }
        int exitCode = apply(oldJar, stagedJar, targetJar, logFile, timeoutMs);
        System.exit(exitCode);
    }

    static int apply(Path oldJar, Path stagedJar, Path targetJar, Path logFile, long timeoutMs) {
        if (!isValidArchive(stagedJar)) {
            writeLog(logFile, "Aborted update: staged file is missing or not a valid JAR archive: " + stagedJar);
            return 1;
        }
        long deadline = System.nanoTime() + timeoutMs * 1000000L;
        IOException lastDeleteError = null;
        while (true) {
            try {
                Files.deleteIfExists(oldJar);
                if (!Files.exists(oldJar)) break;
            } catch (IOException error) {
                lastDeleteError = error;
            }
            if (System.nanoTime() >= deadline) {
                String reason = lastDeleteError == null ? "file still exists" : lastDeleteError.getMessage();
                writeLog(logFile, "Timed out waiting to delete old mod JAR (" + oldJar + "): " + reason);
                return 1;
            }
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                writeLog(logFile, "Interrupted while waiting to delete old mod JAR: " + oldJar);
                return 1;
            }
        }
        try {
            Path targetParent = targetJar.toAbsolutePath().getParent();
            if (targetParent != null) Files.createDirectories(targetParent);
            moveReplacing(stagedJar, targetJar);
            writeLog(logFile, "Applied Schematica Plus update: removed " + oldJar.getFileName() + " and installed " + targetJar.getFileName());
            return 0;
        } catch (IOException error) {
            writeLog(logFile, "Failed to move staged update into mods directory: " + error.getMessage());
            return 1;
        }
    }

    private static void moveReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException moveError) {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            try {
                Files.deleteIfExists(source);
            } catch (IOException ignored) {
            }
        }
    }

    private static boolean isValidArchive(Path file) {
        if (file == null || !Files.isRegularFile(file)) return false;
        try (ZipFile zip = new ZipFile(file.toFile())) {
            return zip.size() > 0;
        } catch (IOException error) {
            return false;
        }
    }

    private static void writeLog(Path logFile, String message) {
        if (logFile == null) return;
        try {
            Path parent = logFile.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
            String line = message + System.lineSeparator();
            Files.write(logFile, line.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }
}

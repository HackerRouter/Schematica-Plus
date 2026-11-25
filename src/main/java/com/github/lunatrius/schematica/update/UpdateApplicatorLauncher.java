package com.github.lunatrius.schematica.update;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Locale;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public final class UpdateApplicatorLauncher {

    private static final String HELPER_PREFIX = "applicator-";
    private static final String HELPER_SUFFIX = ".jar";
    private static final String LOG_NAME = "last_update.log";

    private UpdateApplicatorLauncher() {}

    public static void applyOnExit(File updatesDir, File oldJar, File stagedJar, File targetJar, long timeoutMs) throws IOException {
        if (updatesDir == null || oldJar == null || stagedJar == null || targetJar == null) {
            throw new IllegalArgumentException("Update paths must not be null");
        }
        Files.createDirectories(updatesDir.toPath());
        if (!isWindows()) {
            File logFile = new File(updatesDir, LOG_NAME);
            int inlineResult = UpdateApplicatorMain.apply(oldJar.toPath(), stagedJar.toPath(), targetJar.toPath(),
                logFile.toPath(), Math.min(timeoutMs, 5000L));
            if (inlineResult == 0) return;
        }
        launch(updatesDir, oldJar, stagedJar, targetJar, timeoutMs);
    }

    public static Process launch(File updatesDir, File oldJar, File stagedJar, File targetJar, long timeoutMs) throws IOException {
        if (updatesDir == null || oldJar == null || stagedJar == null || targetJar == null) {
            throw new IllegalArgumentException("Update paths must not be null");
        }
        Files.createDirectories(updatesDir.toPath());
        File helperJar = writeHelperJar(updatesDir);
        File logFile = new File(updatesDir, LOG_NAME);
        File javaExecutable = locateJavaExecutable();
        ProcessBuilder builder = new ProcessBuilder(
            javaExecutable.getAbsolutePath(),
            "-jar",
            helperJar.getAbsolutePath(),
            oldJar.getAbsolutePath(),
            stagedJar.getAbsolutePath(),
            targetJar.getAbsolutePath(),
            logFile.getAbsolutePath(),
            Long.toString(timeoutMs));
        builder.directory(updatesDir);
        File nullSink = isWindows() ? new File("NUL") : new File("/dev/null");
        builder.redirectInput(nullSink);
        builder.redirectOutput(nullSink);
        builder.redirectError(nullSink);
        return builder.start();
    }

    public static File writeHelperJar(File updatesDir) throws IOException {
        Files.createDirectories(updatesDir.toPath());
        byte[] bytecode = readApplicatorBytecode();
        File helperJar = new File(updatesDir, HELPER_PREFIX + System.currentTimeMillis() + HELPER_SUFFIX);
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, UpdateApplicatorMain.class.getName());
        String entryName = UpdateApplicatorMain.class.getName().replace('.', '/') + ".class";
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(helperJar), manifest)) {
            out.putNextEntry(new JarEntry(entryName));
            out.write(bytecode);
            out.closeEntry();
        }
        return helperJar;
    }

    public static void cleanupStaleFiles(File updatesDir, File activeStagedFile) {
        if (updatesDir == null || !updatesDir.isDirectory()) return;
        File[] files = updatesDir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (!file.isFile()) continue;
            if (activeStagedFile != null && file.equals(activeStagedFile)) continue;
            String name = file.getName().toLowerCase(Locale.ROOT);
            if ((name.startsWith(HELPER_PREFIX) && name.endsWith(HELPER_SUFFIX)) || name.endsWith(".part")) {
                try {
                    Files.deleteIfExists(file.toPath());
                } catch (IOException ignored) {
                }
            }
        }
    }

    static File locateJavaExecutable() throws IOException {
        String javaHome = System.getProperty("java.home");
        if (javaHome == null || javaHome.trim().isEmpty()) {
            throw new IOException("java.home system property is not set");
        }
        File bin = new File(javaHome, "bin");
        if (isWindows()) {
            File javaw = new File(bin, "javaw.exe");
            if (javaw.isFile()) return javaw;
            File java = new File(bin, "java.exe");
            if (java.isFile()) return java;
        } else {
            File java = new File(bin, "java");
            if (java.isFile()) return java;
        }
        throw new IOException("Could not locate Java executable under " + bin.getAbsolutePath());
    }

    private static byte[] readApplicatorBytecode() throws IOException {
        String resource = UpdateApplicatorMain.class.getSimpleName() + ".class";
        try (InputStream stream = UpdateApplicatorMain.class.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IOException("Missing update applicator bytecode resource: " + resource);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = stream.read(buffer)) >= 0) {
                if (read > 0) out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name", "");
        return os.toLowerCase(Locale.ROOT).contains("win");
    }
}

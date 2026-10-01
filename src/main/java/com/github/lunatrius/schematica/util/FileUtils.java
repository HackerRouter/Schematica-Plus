package com.github.lunatrius.schematica.util;

import java.io.File;
import java.io.IOException;

import com.github.lunatrius.schematica.reference.Reference;

public class FileUtils {

    public static void migrateLegacyConfiguration(File configFile) throws IOException {
        if (configFile.exists()) return;
        for (String name : new String[] {"Schematica.cfg", "schematica.cfg"}) {
            File legacy = new File(configFile.getAbsoluteFile().getParentFile(), name);
            if (legacy.isFile()) {
                java.nio.file.Files.copy(legacy.toPath(), configFile.toPath());
                return;
            }
        }
    }

    public static void writeUtf8Atomically(File file, String text) throws IOException {
        java.nio.file.Path target = file.toPath().toAbsolutePath();
        java.nio.file.Path temporary = java.nio.file.Files.createTempFile(target.getParent(), ".schematica-", ".tmp");
        try {
            java.nio.file.Files.write(temporary, text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            try {
                java.nio.file.Files.move(temporary, target, java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                java.nio.file.Files.move(temporary, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            java.nio.file.Files.deleteIfExists(temporary);
        }
    }

    public static File resolveSchematicFile(File directory, String filename) throws IOException {
        if (directory == null || filename == null || filename.isEmpty() || filename.equals(".")
            || filename.equals("..") || filename.indexOf('/') >= 0 || filename.indexOf('\\') >= 0
            || filename.indexOf(':') >= 0 || filename.indexOf('\0') >= 0) {
            throw new IOException("Invalid schematic filename");
        }
        String lowerName = filename.toLowerCase(java.util.Locale.ROOT);
        if (!lowerName.endsWith(".schematic") && !lowerName.endsWith(".schemplus") && !lowerName.endsWith(".litematic")
            && !lowerName.endsWith(".nbt")) {
            throw new IOException("Unsupported schematic output extension");
        }
        File file = new File(directory, filename).getCanonicalFile();
        if (!contains(directory, file)) {
            throw new IOException("Schematic path leaves its directory");
        }
        return file;
    }

    public static File findSchematicFile(File directory, String name, boolean preferExtended) throws IOException {
        if (name == null) throw new IOException("Invalid schematic filename");
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        if (lower.endsWith(".schematic") || lower.endsWith(".schemplus")) return resolveSchematicFile(directory, name);
        File preferred = resolveSchematicFile(directory, name + (preferExtended ? ".schemplus" : ".schematic"));
        if (preferred.isFile()) return preferred;
        File alternate = resolveSchematicFile(directory, name + (preferExtended ? ".schematic" : ".schemplus"));
        return alternate.isFile() ? alternate : preferred;
    }

    // http://stackoverflow.com/a/3758880/1166946
    public static String humanReadableByteCount(final long bytes) {
        final int unit = 1024;
        if (bytes < unit) {
            return bytes + " B";
        }

        int exp = (int) (Math.log(bytes) / Math.log(unit));
        final String pre = "KMGTPE".charAt(exp - 1) + "i";

        return String.format("%3.0f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public static boolean contains(final File root, final String filename) {
        return contains(root, new File(root, filename));
    }

    // http://stackoverflow.com/q/18227634/1166946
    public static boolean contains(final File root, final File file) {
        try {
            return file.getCanonicalPath()
                .startsWith(root.getCanonicalPath() + File.separator);
        } catch (IOException e) {
            Reference.logger.error("", e);
        }

        return false;
    }
}

package com.github.lunatrius.schematica.client.gui.browser;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.LinkOption;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class SchematicBrowserModel {

    private final Path root;
    private Path directory;
    private List<Entry> entries = Collections.emptyList();

    public SchematicBrowserModel(File root) throws IOException {
        this.root = root.getCanonicalFile().toPath();
        this.directory = this.root;
    }

    public File root() {
        return root.toFile();
    }

    public File directory() {
        return directory.toFile();
    }

    public String relativeDirectory() {
        return directory.equals(root) ? "/" : "/" + root.relativize(directory).toString().replace('\\', '/');
    }

    public boolean canGoUp() {
        return !directory.equals(root);
    }

    public List<Entry> entries() {
        return entries;
    }

    public void refresh() throws IOException {
        entries = Collections.emptyList();
        Path current = checked(directory);
        List<Entry> found = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(current)) {
            for (Path path : stream) {
                try {
                    Path resolved = checked(path);
                    BasicFileAttributes attributes = Files.readAttributes(resolved, BasicFileAttributes.class);
                    if (attributes.isDirectory() || attributes.isRegularFile() && supported(path.getFileName().toString())) {
                        found.add(new Entry(path.toFile(), attributes));
                    }
                } catch (IOException ignored) {}
            }
        }
        found.sort(Comparator.comparing((Entry entry) -> !entry.directory)
            .thenComparing(entry -> entry.name().toLowerCase(Locale.ROOT)).thenComparing(Entry::name));
        entries = Collections.unmodifiableList(found);
    }

    public void navigate(File target) throws IOException {
        Path next = checked(target.toPath());
        if (!Files.isDirectory(next)) throw new IOException("Not a directory: " + target);
        directory = next;
        refresh();
    }

    public void up() throws IOException {
        if (canGoUp()) navigate(directory.getParent().toFile());
    }

    public File readableFile(Entry entry) throws IOException {
        if (entry == null || entry.directory || !entries.contains(entry)) throw new IOException("No file selected");
        Path target = checked(entry.file.toPath());
        if (!Files.isRegularFile(target) || !Files.isReadable(target) || !supported(entry.name())) {
            throw new IOException("File is not readable: " + entry.name());
        }
        return entry.file;
    }

    public File createDirectory(String name) throws IOException {
        validateName(name);
        Path target = checked(directory).resolve(name);
        requireAbsent(target);
        return Files.createDirectory(target).toFile();
    }

    public File rename(Entry entry, String name) throws IOException {
        Path source = currentFile(entry);
        Path target = destination(source, name);
        if (source.equals(target) && entry.name().equals(name)) return source.toFile();
        requireAbsent(target);
        SchematicSourceIndex update = SchematicSourceIndex.prepare(root.toFile(), source.toFile(), target.toFile());
        Files.move(source, target);
        try {
            update.save();
        } catch (IOException e) {
            try {
                Files.move(target, source);
            } catch (IOException rollback) {
                e.addSuppressed(rollback);
            }
            throw e;
        }
        return target.toFile();
    }

    public File copy(Entry entry, String name) throws IOException {
        Path source = currentFile(entry);
        Path target = destination(source, name);
        requireAbsent(target);
        Path temporary = Files.createTempFile(source.getParent(), ".schematica-copy-", ".tmp");
        try {
            Files.copy(source, temporary, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            currentFile(entry);
            Files.move(temporary, target);
            return target.toFile();
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public void delete(Entry entry) throws IOException {
        Files.delete(currentFile(entry));
    }

    private Path currentFile(Entry entry) throws IOException {
        if (entry == null || entry.directory || !entries.contains(entry)) throw new FileOperationException("selection");
        Path source = entry.file.toPath().toAbsolutePath().normalize();
        if (!checked(source).equals(source)) throw new FileOperationException("link");
        BasicFileAttributes now = Files.readAttributes(source, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!now.isRegularFile() || now.size() != entry.size || !now.lastModifiedTime().equals(entry.modifiedTime)
            || !Objects.equals(now.fileKey(), entry.fileKey)) throw new FileOperationException("changed");
        return source;
    }

    private Path destination(Path source, String name) throws IOException {
        validateName(name);
        String extension = source.getFileName().toString().substring(source.getFileName().toString().lastIndexOf('.'));
        if (!name.toLowerCase(Locale.ROOT).endsWith(extension.toLowerCase(Locale.ROOT))) {
            throw new FileOperationException("extension");
        }
        return source.getParent().resolve(name);
    }

    private static void requireAbsent(Path target) throws IOException {
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) throw new FileAlreadyExistsException(target.toString());
    }

    private static void validateName(String name) throws IOException {
        if (name == null || name.trim().isEmpty() || name.length() > 255 || name.endsWith(".") || name.endsWith(" ")
            || name.equals(".") || name.equals("..")) throw FileOperationException.name(name);
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c < 32 || "<>:\"/\\|?*".indexOf(c) >= 0) throw FileOperationException.name(name);
        }
        String stem = name.split("\\.", 2)[0].toUpperCase(Locale.ROOT);
        if (stem.matches("CON|PRN|AUX|NUL|COM[1-9¹²³]|LPT[1-9¹²³]")) throw FileOperationException.name(name);
    }

    public static final class FileOperationException extends IOException {
        public final String translationKey;
        public final Object[] arguments;

        FileOperationException(String reason) {
            this(reason, "schematica.ui.files.error." + reason);
        }

        private FileOperationException(String reason, String translationKey, Object... arguments) {
            super(reason);
            this.translationKey = translationKey;
            this.arguments = arguments;
        }

        static FileOperationException name(String name) {
            return new FileOperationException("name", "malilib.message.error.illegal_characters_in_file_name", String.valueOf(name));
        }
    }

    private Path checked(Path path) throws IOException {
        Path real = path.toRealPath();
        if (!real.startsWith(root)) throw new IOException("Path leaves the schematic directory");
        return real;
    }

    public static boolean supported(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".schematic") || lower.endsWith(".schemplus") || lower.endsWith(".litematic");
    }

    public static final class Entry {
        public final File file;
        public final boolean directory;
        public final long size;
        public final long modified;

        private final FileTime modifiedTime;
        private final Object fileKey;

        private Entry(File file, BasicFileAttributes attributes) {
            this.file = file;
            this.directory = attributes.isDirectory();
            this.size = attributes.size();
            this.modifiedTime = attributes.lastModifiedTime();
            this.modified = modifiedTime.toMillis();
            this.fileKey = attributes.fileKey();
        }

        public String name() {
            return file.getName();
        }

        public String label() {
            return directory ? "[+] " + name() + "/" : name();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Entry && file.equals(((Entry) other).file)
                && directory == ((Entry) other).directory;
        }

        @Override
        public int hashCode() {
            return file.hashCode() * 31 + Boolean.hashCode(directory);
        }
    }
}

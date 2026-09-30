package com.github.lunatrius.schematica.client.gui.browser;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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
                        found.add(new Entry(path.toFile(), attributes.isDirectory(), attributes.size(),
                            attributes.lastModifiedTime().toMillis()));
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

        private Entry(File file, boolean directory, long size, long modified) {
            this.file = file;
            this.directory = directory;
            this.size = size;
            this.modified = modified;
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

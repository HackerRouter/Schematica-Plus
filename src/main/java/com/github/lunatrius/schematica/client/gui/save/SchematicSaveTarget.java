package com.github.lunatrius.schematica.client.gui.save;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import com.github.lunatrius.schematica.util.FileUtils;

public final class SchematicSaveTarget {

    private SchematicSaveTarget() {}

    public static final String[] FORMATS = {".schematic", ".schemplus", ".litematic"};

    public static String filename(String input, boolean extended) {
        return filename(input, extended ? ".schemplus" : ".schematic");
    }

    /** The file name for a save in one of FORMATS; a typed schematic extension is replaced. */
    public static String filename(String input, String extension) {
        if (!java.util.Arrays.asList(FORMATS).contains(extension)) throw new IllegalArgumentException("Invalid schematic format");
        String name = input.trim();
        String lower = name.toLowerCase(Locale.ROOT);
        for (String format : FORMATS) {
            if (lower.endsWith(format)) {
                name = name.substring(0, name.length() - format.length());
                break;
            }
        }
        if (name.isEmpty() || name.endsWith(".") || name.endsWith(" ") || name.length() > 200) {
            throw new IllegalArgumentException("Invalid schematic filename");
        }
        for (int i = 0; i < name.length(); i++) {
            if (name.charAt(i) < 32 || "<>:\"/\\|?*;".indexOf(name.charAt(i)) >= 0) {
                throw new IllegalArgumentException("Invalid schematic filename");
            }
        }
        String stem = name.split("\\.", 2)[0].toUpperCase(Locale.ROOT);
        if (stem.matches("CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9]")) {
            throw new IllegalArgumentException("Reserved schematic filename");
        }
        return name + extension;
    }

    public static File resolve(File root, File directory, String input, boolean extended) throws IOException {
        return resolve(root, directory, input, extended ? ".schemplus" : ".schematic");
    }

    public static File resolve(File root, File directory, String input, String extension) throws IOException {
        File canonicalRoot = root.getCanonicalFile();
        File canonicalDirectory = directory.getCanonicalFile();
        if (!canonicalDirectory.toPath().startsWith(canonicalRoot.toPath()) || !canonicalDirectory.isDirectory()) {
            throw new IOException("Invalid schematic output directory");
        }
        File file = FileUtils.resolveSchematicFile(canonicalDirectory, filename(input, extension));
        if (file.exists() && !file.isFile()) throw new IOException("Output path is not a regular file");
        return file;
    }

    public static File sourceCopy(File root, File directory, String input, String extension) throws IOException {
        if (!extension.equals(".schematic") && !extension.equals(".schemplus") && !extension.equals(".litematic") && !extension.equals(".nbt")) {
            throw new IllegalArgumentException("Invalid source format");
        }
        String name = input.trim();
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(extension)) name = name.substring(0, name.length() - extension.length());
        else if (lower.endsWith(".schematic") || lower.endsWith(".schemplus") || lower.endsWith(".litematic") || lower.endsWith(".nbt")) {
            throw new IllegalArgumentException("Keep the original source extension");
        }
        filename(name, true);
        File parent = directory.getCanonicalFile();
        if (!parent.toPath().startsWith(root.getCanonicalFile().toPath()) || !parent.isDirectory()) {
            throw new IOException("Invalid source output directory");
        }
        File file = new File(parent, name + extension);
        if (!file.getCanonicalFile().equals(file) || file.exists() && !file.isFile()) {
            throw new IOException("Invalid source output file");
        }
        return file;
    }
}

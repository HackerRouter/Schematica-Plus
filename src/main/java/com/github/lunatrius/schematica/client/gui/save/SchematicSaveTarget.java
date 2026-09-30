package com.github.lunatrius.schematica.client.gui.save;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import com.github.lunatrius.schematica.util.FileUtils;

public final class SchematicSaveTarget {

    private SchematicSaveTarget() {}

    public static String filename(String input, boolean extended) {
        String name = input.trim();
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".schematic")) name = name.substring(0, name.length() - 10);
        else if (lower.endsWith(".schemplus")) name = name.substring(0, name.length() - 10);
        else if (lower.endsWith(".litematic")) throw new IllegalArgumentException("Litematic export is not supported");
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
        return name + (extended ? ".schemplus" : ".schematic");
    }

    public static File resolve(File root, File directory, String input, boolean extended) throws IOException {
        File canonicalRoot = root.getCanonicalFile();
        File canonicalDirectory = directory.getCanonicalFile();
        if (!canonicalDirectory.toPath().startsWith(canonicalRoot.toPath()) || !canonicalDirectory.isDirectory()) {
            throw new IOException("Invalid schematic output directory");
        }
        File file = FileUtils.resolveSchematicFile(canonicalDirectory, filename(input, extended));
        if (file.exists() && !file.isFile()) throw new IOException("Output path is not a regular file");
        return file;
    }
}

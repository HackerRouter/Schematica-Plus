package com.github.lunatrius.schematica;

import java.io.File;
import java.io.FileFilter;
import java.util.Locale;

public class FileFilterSchematic implements FileFilter {

    private final boolean directory;

    public FileFilterSchematic(boolean dir) {
        this.directory = dir;
    }

    @Override
    public boolean accept(File file) {
        if (this.directory) {
            return file.isDirectory();
        }
        final String name = file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".litematic") || name.endsWith(".schemplus") || name.endsWith(".schematic") || name.endsWith(".nbt") || name.endsWith(".schem");
    }
}

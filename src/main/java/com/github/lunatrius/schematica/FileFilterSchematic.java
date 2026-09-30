package com.github.lunatrius.schematica;

import java.io.File;
import java.io.FileFilter;


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
        final String name = file.getName().toLowerCase(java.util.Locale.ROOT);
        return name.endsWith(".litematic") || name.endsWith(".schemplus") || name.endsWith(".schematic");
    }
}

package com.github.lunatrius.schematica;

import java.io.File;
import java.io.FileFilter;

import com.github.lunatrius.schematica.handler.ConfigurationHandler;

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
        final String name = file.getName().toLowerCase();
        // Always accept .litematic files regardless of format setting
        if (name.endsWith(".litematic")) {
            return true;
        }
        if (ConfigurationHandler.useSchematicplusFormat) {
            return name.endsWith(".schemplus");
        } else {
            return name.endsWith(".schematic");
        }
    }
}

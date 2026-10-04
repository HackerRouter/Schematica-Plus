package com.github.lunatrius.schematica.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.reference.Reference;

/**
 * Per-world data (placements, selections, render layers, projects) lives in config/schematica_plus like Litematica's
 * config/litematica, not in the schematic directory where its folders showed up in the schematic browser. Files
 * and folders of older versions are moved over the first time they are used.
 */
public final class PlusDataFiles {
    private PlusDataFiles() {}

    public static File directory() {
        File config = ConfigurationHandler.configuration == null ? null : ConfigurationHandler.configuration.getConfigFile();
        File base = config != null && config.getParentFile() != null ? config.getParentFile() : ConfigurationHandler.schematicDirectory;
        return new File(base, "schematica_plus");
    }

    /** A data file or folder, moved from the schematic directory when only the old one exists. */
    public static File file(String name) {
        File target = new File(directory(), name);
        File legacy = new File(ConfigurationHandler.schematicDirectory, name);
        if (!target.exists() && legacy.exists() && !legacy.equals(target)) {
            try {
                Files.createDirectories(target.getParentFile().toPath());
                Files.move(legacy.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException | RuntimeException atomic) {
                try {
                    Files.move(legacy.toPath(), target.toPath());
                } catch (IOException | RuntimeException error) {
                    Reference.logger.warn("Could not move {} to {}; using the old location", legacy, target, error);
                    return legacy;
                }
            }
            Reference.logger.info("Moved {} to {}", legacy, target);
        }
        return target;
    }
}

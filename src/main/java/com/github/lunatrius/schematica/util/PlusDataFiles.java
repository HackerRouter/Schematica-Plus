package com.github.lunatrius.schematica.util;

import java.io.File;

import com.github.lunatrius.schematica.handler.ConfigurationHandler;

/**
 * Per-world data (placements, selections, render layers, projects) lives in config/schematica_plus like Litematica's
 * config/litematica, not in the schematic directory where its folders showed up in the schematic browser.
 */
public final class PlusDataFiles {
    private PlusDataFiles() {}

    public static File directory() {
        File config = ConfigurationHandler.configuration == null ? null : ConfigurationHandler.configuration.getConfigFile();
        File base = config != null && config.getParentFile() != null ? config.getParentFile() : ConfigurationHandler.schematicDirectory;
        return new File(base, "schematica_plus");
    }

    public static File file(String name) {
        return new File(directory(), name);
    }
}

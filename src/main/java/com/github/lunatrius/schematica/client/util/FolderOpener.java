package com.github.lunatrius.schematica.client.util;

import java.io.File;

import net.minecraft.util.Util;

import org.lwjgl.Sys;

import com.github.lunatrius.schematica.reference.Reference;

/** Opens a directory in the system file manager, the way the resource pack screen's "Open folder" does. */
public final class FolderOpener {
    private FolderOpener() {}

    public static void open(File directory) {
        String path = directory.getAbsolutePath();
        try {
            switch (Util.getOSType()) {
                case OSX: Runtime.getRuntime().exec(new String[] {"/usr/bin/open", path}); return;
                case WINDOWS: Runtime.getRuntime().exec(new String[] {"explorer.exe", path}); return;
                default:
                    if (java.awt.Desktop.isDesktopSupported()) {
                        java.awt.Desktop.getDesktop().open(directory);
                        return;
                    }
                    Runtime.getRuntime().exec(new String[] {"xdg-open", path});
                    return;
            }
        } catch (Exception error) {
            Reference.logger.debug("Could not open {} directly", path, error);
        }
        try {
            Sys.openURL(directory.toURI().toString());
        } catch (RuntimeException error) {
            Reference.logger.error("Could not open {}", path, error);
        }
    }
}

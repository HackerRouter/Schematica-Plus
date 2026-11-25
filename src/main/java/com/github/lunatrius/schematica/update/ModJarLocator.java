package com.github.lunatrius.schematica.update;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.util.Locale;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.reference.Reference;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;

public final class ModJarLocator {

    private ModJarLocator() {}

    public static File locateCurrentModJar() {
        try {
            Loader loader = Loader.instance();
            if (loader != null) {
                ModContainer container = loader.getIndexedModList().get(Reference.MODID);
                if (container != null && isPackagedJar(container.getSource())) {
                    return container.getSource();
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            CodeSource source = SchematicaPlus.class.getProtectionDomain().getCodeSource();
            URL location = source == null ? null : source.getLocation();
            if (location != null && "file".equalsIgnoreCase(location.getProtocol())) {
                File file = new File(location.toURI());
                if (isPackagedJar(file)) return file;
            }
        } catch (URISyntaxException | SecurityException ignored) {
        }
        return null;
    }

    public static boolean isPackagedJar(File file) {
        return file != null && file.isFile() && file.getName().toLowerCase(Locale.ROOT).endsWith(".jar");
    }

    public static File resolveTargetJar(File currentJar, String assetName) {
        if (currentJar == null || currentJar.getParentFile() == null || assetName == null) return null;
        String safeName = new File(assetName).getName().trim();
        if (safeName.isEmpty() || !safeName.toLowerCase(Locale.ROOT).endsWith(".jar")) return null;
        return new File(currentJar.getParentFile(), safeName);
    }
}

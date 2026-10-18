package com.github.lunatrius.schematica.client.cloud;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;

import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary;
import com.github.lunatrius.schematica.client.util.WorldServerName;
import com.github.lunatrius.schematica.cloud.CloudFile;
import com.github.lunatrius.schematica.cloud.CloudPath;
import com.github.lunatrius.schematica.cloud.CloudStorage;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;

public final class CloudCache {
    public static final String DIRECTORY = ".server_schematics";
    private static final Map<String, Hashed> HASHES = new HashMap<>();

    private CloudCache() {}

    private static final class Hashed {
        final long size, modified;
        final String hash;

        Hashed(long size, long modified, String hash) { this.size = size; this.modified = modified; this.hash = hash; }
    }

    public static File root() {
        Minecraft mc = Minecraft.getMinecraft();
        String key = mc.theWorld == null ? "unknown" : WorldServerName.worldServerName(mc, mc.theWorld);
        if (key.contains("|")) key = key.substring(0, key.lastIndexOf('|'));
        String name = AreaSelectionLibrary.safeFileName(key);
        return new File(new File(ConfigurationHandler.schematicDirectory, DIRECTORY), name.isEmpty() ? "_" : name);
    }

    public static File file(CloudPath path) {
        return path.resolve(new File(root(), path.scope));
    }

    public static File shared(String hash, String extension) {
        return new File(new File(root(), "shared"), hash + extension);
    }

    public static boolean cached(CloudFile file) {
        return matches(file(file.path), file.size, file.hash);
    }

    public static boolean matches(File local, long size, String hash) {
        if (!local.isFile() || local.length() != size) return false;
        try {
            return hash(local).equals(hash);
        } catch (IOException e) {
            return false;
        }
    }

    static synchronized String hash(File file) throws IOException {
        String key = file.getAbsolutePath();
        Hashed known = HASHES.get(key);
        if (known != null && known.size == file.length() && known.modified == file.lastModified()) return known.hash;
        String hash = CloudStorage.hash(file);
        HASHES.put(key, new Hashed(file.length(), file.lastModified(), hash));
        return hash;
    }
}

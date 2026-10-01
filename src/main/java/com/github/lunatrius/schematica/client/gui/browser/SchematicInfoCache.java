// SPDX-License-Identifier: LGPL-3.0-only
// Litematica WidgetSchematicBrowser metadata and preview caches, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.browser;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.world.schematic.SchematicFiles;

/** Reads file metadata off the client thread and keeps preview textures until the browser closes. */
final class SchematicInfoCache {
    private static final ExecutorService READER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Schematica Plus schematic info reader");
        thread.setDaemon(true);
        return thread;
    });

    private final Map<File, Future<SchematicFiles.Info>> infos = new HashMap<>();
    private final Map<File, ResourceLocation> previews = new HashMap<>();

    /** The metadata of a .litematic or .nbt file once read, else null (also for other formats and unreadable files). */
    SchematicFiles.Info get(File file) {
        Future<SchematicFiles.Info> future = infos.computeIfAbsent(file, key -> READER.submit(() -> SchematicFiles.info(key)));
        if (!future.isDone()) return null;
        try {
            return future.get();
        } catch (Exception e) {
            Reference.logger.debug("Could not read schematic metadata of {}", file, e);
            return null;
        }
    }

    /** A texture of the square preview image, or null when the file has none. */
    ResourceLocation preview(File file, SchematicFiles.Info info) {
        if (previews.containsKey(file)) return previews.get(file);
        ResourceLocation location = null;
        int[] pixels = info.preview;
        int size = pixels == null ? 0 : (int) Math.sqrt(pixels.length);
        if (size > 0 && size * size == pixels.length) {
            DynamicTexture texture = new DynamicTexture(size, size);
            System.arraycopy(pixels, 0, texture.getTextureData(), 0, pixels.length);
            texture.updateDynamicTexture();
            location = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("schematica_plus_preview", texture);
        }
        previews.put(file, location);
        return location;
    }

    void clear() {
        for (ResourceLocation location : previews.values()) {
            if (location != null) Minecraft.getMinecraft().getTextureManager().deleteTexture(location);
        }
        previews.clear();
        infos.clear();
    }
}

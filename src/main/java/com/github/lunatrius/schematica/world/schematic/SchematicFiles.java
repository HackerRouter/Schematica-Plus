// SPDX-License-Identifier: LGPL-3.0-only
// Litematica schematic metadata, file editing and WorldUtils conversions, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.world.schematic;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.function.Consumer;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants.NBT;

import com.github.lunatrius.schematica.api.ISchematic;

/** Schematic file metadata for the browsers, in-place .litematic metadata edits and format conversions. */
public final class SchematicFiles {

    /** What the browser shows about a .litematic or vanilla structure file. */
    public static final class Info {
        public final boolean litematic;
        public String name = "", author = "", description = "";
        public long created, modified, volume, blocks = -1;
        public int regions = 1, version = -1, dataVersion = -1, sizeX, sizeY, sizeZ;
        public int[] preview;

        Info(boolean litematic) { this.litematic = litematic; }

        public boolean hasBeenModified() { return modified > created; }
    }

    private SchematicFiles() {}

    private static boolean extension(File file, String extension) {
        return file.getName().toLowerCase(Locale.ROOT).endsWith(extension);
    }

    /** Metadata of a .litematic or .nbt file, or null for other formats. */
    public static Info info(File file) throws IOException {
        boolean litematic = extension(file, ".litematic");
        if (!litematic && !extension(file, ".nbt")) return null;
        try {
            NBTTagCompound root = LitematicaNBTReader.readFromFile(file);
            Info info = new Info(litematic);
            if (litematic) {
                NBTTagCompound metadata = root.getCompoundTag("Metadata");
                info.name = metadata.getString("Name");
                info.author = metadata.getString("Author");
                info.description = metadata.getString("Description");
                info.created = metadata.getLong("TimeCreated");
                info.modified = metadata.getLong("TimeModified");
                info.regions = metadata.getInteger("RegionCount");
                info.volume = metadata.getLong("TotalVolume");
                info.blocks = metadata.hasKey("TotalBlocks") ? metadata.getLong("TotalBlocks") : -1;
                NBTTagCompound size = metadata.getCompoundTag("EnclosingSize");
                info.sizeX = size.getInteger("x"); info.sizeY = size.getInteger("y"); info.sizeZ = size.getInteger("z");
                if (metadata.hasKey("PreviewImageData", NBT.TAG_INT_ARRAY)) info.preview = metadata.getIntArray("PreviewImageData");
                info.version = root.getInteger("Version");
                info.dataVersion = root.hasKey("MinecraftDataVersion") ? root.getInteger("MinecraftDataVersion") : -1;
            } else {
                if (!VanillaStructure.isStructure(root)) return null;
                NBTTagList size = (NBTTagList) root.getTagList("size", NBT.TAG_INT).copy();
                if (size.tagCount() != 3) return null;
                info.sizeZ = ((NBTTagInt) size.removeTag(2)).func_150287_d();
                info.sizeY = ((NBTTagInt) size.removeTag(1)).func_150287_d();
                info.sizeX = ((NBTTagInt) size.removeTag(0)).func_150287_d();
                info.name = file.getName().substring(0, file.getName().length() - 4);
                info.author = root.getString("author");
                info.volume = (long) info.sizeX * info.sizeY * info.sizeZ;
                info.created = info.modified = file.lastModified();
                info.dataVersion = root.hasKey("DataVersion") ? root.getInteger("DataVersion") : -1;
            }
            return info;
        } finally {
            LitematicaNBTReader.clearLongArrayStore();
        }
    }

    /** Changes the Metadata of a .litematic file in place, keeping every other tag (TimeModified is updated). */
    public static void editLitematicMetadata(File file, Consumer<NBTTagCompound> edit) throws IOException {
        if (!extension(file, ".litematic")) throw new IOException("Not a litematic file");
        try {
            NBTTagCompound root = LitematicaNBTReader.readFromFile(file);
            if (!SchematicLitematica.isLitematicFormat(root)) throw new IOException("Not a litematic schematic");
            if (LitematicaNBTReader.droppedData()) throw new IOException("The file holds data this version cannot rewrite");
            NBTTagCompound metadata = root.getCompoundTag("Metadata");
            edit.accept(metadata);
            metadata.setLong("TimeModified", System.currentTimeMillis());
            root.setTag("Metadata", metadata);
            SchematicFileWriter.write(file, new LitematicExport.Document(root, LitematicaNBTReader.longArrays()));
        } finally {
            LitematicaNBTReader.clearLongArrayStore();
        }
    }

    private static NBTTagCompound litematicMetadata(File file) {
        try {
            return LitematicaNBTReader.readFromFile(file).getCompoundTag("Metadata");
        } catch (IOException | RuntimeException e) {
            return null;
        } finally {
            LitematicaNBTReader.clearLongArrayStore();
        }
    }

    /**
     * Converts a readable schematic file to the target's format (.litematic, .schematic or .nbt) with block entities,
     * and entities unless ignored. A .litematic source keeps its name, author, description, preview and creation time.
     * Returns the written file; a .schematic needing extended IDs becomes .schemplus.
     */
    public static File convert(File source, File target, boolean includeEntities, String author) throws IOException {
        ISchematic schematic = SchematicFormat.readFromFile(source);
        if (schematic == null) throw new IOException("Unreadable schematic " + source.getName());
        NBTTagCompound sourceMetadata = extension(source, ".litematic") ? litematicMetadata(source) : null;
        String sourceAuthor = sourceMetadata != null && !sourceMetadata.getString("Author").isEmpty() ? sourceMetadata.getString("Author") : author;
        if (extension(target, ".litematic")) {
            String stem = target.getName().substring(0, target.getName().length() - ".litematic".length());
            LitematicExport.Document document = LitematicExport.encode(schematic, null, true, includeEntities, stem, sourceAuthor,
                System.currentTimeMillis());
            if (sourceMetadata != null) {
                NBTTagCompound metadata = document.root.getCompoundTag("Metadata");
                for (String key : new String[] {"Name", "Description"}) metadata.setString(key, sourceMetadata.getString(key));
                if (sourceMetadata.hasKey("TimeCreated")) metadata.setLong("TimeCreated", sourceMetadata.getLong("TimeCreated"));
                if (sourceMetadata.hasKey("PreviewImageData", NBT.TAG_INT_ARRAY)) {
                    metadata.setIntArray("PreviewImageData", sourceMetadata.getIntArray("PreviewImageData"));
                }
            }
            return SchematicFileWriter.write(target, document);
        }
        File written = SchematicFormat.saveToFile(target, schematic, null, true, includeEntities, sourceAuthor);
        if (written == null) throw new IOException("Could not write " + target.getName());
        return written;
    }
}

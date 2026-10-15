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

/** Schematic file metadata for the browsers, in-place metadata edits and format conversions. */
public final class SchematicFiles {

    /** What the browser shows about a writable schematic file. */
    public static final class Info {
        public final boolean litematic;
        public String format;
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

    public static boolean editable(File file) {
        return extension(file, ".litematic") || extension(file, ".schematic")
            || extension(file, ".schemplus") || extension(file, ".nbt");
    }

    private static boolean valid(NBTTagCompound root, File file) {
        if (extension(file, ".litematic")) return SchematicLitematica.isLitematicFormat(root);
        if (extension(file, ".nbt")) return VanillaStructure.isStructure(root);
        if (!"Alpha".equals(root.getString("Materials"))) return false;
        int width = root.getShort("Width"), height = root.getShort("Height"), length = root.getShort("Length");
        long volume = (long) width * height * length;
        return width > 0 && height > 0 && length > 0 && volume <= com.github.lunatrius.schematica.util.SchematicLimits.MAX_BLOCKS
            && root.getByteArray("Blocks").length == volume && root.getByteArray("Data").length == volume;
    }

    public static Info info(File file) throws IOException {
        if (!editable(file)) return null;
        try {
            NBTTagCompound root = LitematicaNBTReader.readFromFile(file);
            if (!valid(root, file)) return null;
            boolean litematic = extension(file, ".litematic"), structure = extension(file, ".nbt");
            NBTTagCompound metadata = SchematicMetadata.read(root, litematic);
            Info info = new Info(litematic);
            info.format = structure ? "nbt" : litematic ? "litematic" : extension(file, ".schemplus") ? "schemplus" : "schematic";
            info.name = metadata.hasKey("Name", NBT.TAG_STRING) ? metadata.getString("Name") : SchematicMetadata.stem(file.getName());
            info.author = metadata.getString("Author");
            info.description = metadata.getString("Description");
            info.created = metadata.hasKey("TimeCreated", NBT.TAG_ANY_NUMERIC) ? metadata.getLong("TimeCreated") : file.lastModified();
            info.modified = metadata.hasKey("TimeModified", NBT.TAG_ANY_NUMERIC) ? metadata.getLong("TimeModified") : info.created;
            info.regions = metadata.hasKey("RegionCount") ? metadata.getInteger("RegionCount") : 1;
            info.blocks = metadata.hasKey("TotalBlocks") ? metadata.getLong("TotalBlocks") : -1;
            if (litematic) {
                NBTTagCompound size = metadata.getCompoundTag("EnclosingSize");
                info.sizeX = size.getInteger("x"); info.sizeY = size.getInteger("y"); info.sizeZ = size.getInteger("z");
                info.version = root.getInteger("Version");
                info.dataVersion = root.hasKey("MinecraftDataVersion") ? root.getInteger("MinecraftDataVersion") : -1;
            } else if (structure) {
                NBTTagList size = (NBTTagList) root.getTagList("size", NBT.TAG_INT).copy();
                if (size.tagCount() != 3) return null;
                info.sizeZ = ((NBTTagInt) size.removeTag(2)).func_150287_d();
                info.sizeY = ((NBTTagInt) size.removeTag(1)).func_150287_d();
                info.sizeX = ((NBTTagInt) size.removeTag(0)).func_150287_d();
                info.dataVersion = root.hasKey("DataVersion") ? root.getInteger("DataVersion") : -1;
            } else {
                info.sizeX = root.getShort("Width"); info.sizeY = root.getShort("Height"); info.sizeZ = root.getShort("Length");
            }
            info.volume = metadata.hasKey("TotalVolume") ? metadata.getLong("TotalVolume") : (long) info.sizeX * info.sizeY * info.sizeZ;
            int[] preview = metadata.getIntArray("PreviewImageData");
            if (SchematicMetadata.validPreview(preview)) info.preview = preview;
            return info;
        } finally {
            LitematicaNBTReader.clearLongArrayStore();
        }
    }

    public static void editLitematicMetadata(File file, Consumer<NBTTagCompound> edit) throws IOException {
        if (!extension(file, ".litematic")) throw new IOException("Not a litematic file");
        editMetadata(file, edit);
    }

    /** Rewrites only descriptive fields; preserves all block data, unknown tags and the file's encoding. */
    public static NBTTagCompound editMetadata(File file, Consumer<NBTTagCompound> edit) throws IOException {
        if (!editable(file)) throw new IOException("Unsupported metadata format");
        try {
            NBTTagCompound root = LitematicaNBTReader.readFromFile(file);
            if (!valid(root, file)) throw new IOException("Not a valid schematic");
            if (LitematicaNBTReader.droppedData()) throw new IOException("The file holds data this version cannot rewrite");
            boolean litematic = extension(file, ".litematic");
            NBTTagCompound metadata = litematic ? root.getCompoundTag("Metadata")
                : root.getCompoundTag("ExtendedMetadata").getCompoundTag(SchematicMetadata.EXTENSION);
            if (VanillaStructure.isStructure(root) && root.hasKey("author", NBT.TAG_STRING)) metadata.setString("Author", root.getString("author"));
            SchematicMetadata.defaults(metadata, SchematicMetadata.stem(file.getName()), root.getString("author"), file.lastModified());
            edit.accept(metadata);
            metadata.setLong("TimeModified", System.currentTimeMillis());
            SchematicMetadata.write(root, metadata, litematic);
            SchematicFileWriter.write(file, new LitematicExport.Document(root, LitematicaNBTReader.longArrays()));
            return (NBTTagCompound) metadata.copy();
        } finally {
            LitematicaNBTReader.clearLongArrayStore();
        }
    }

    /**
     * Converts a readable schematic file to the target's format (.litematic, .schematic, .schemplus or .nbt) with block entities,
     * and entities unless ignored. Descriptive metadata follows the source across the writable formats.
     * Returns the written file; a .schematic needing extended IDs becomes .schemplus.
     */
    public static File convert(File source, File target, boolean includeEntities, String author) throws IOException {
        ISchematic schematic = SchematicFormat.readFromFile(source);
        if (schematic == null) throw new IOException("Unreadable schematic " + source.getName());
        File written = SchematicFormat.saveToFile(target, schematic, null, true, includeEntities, author);
        if (written == null) throw new IOException("Could not write " + target.getName());
        return written;
    }
}

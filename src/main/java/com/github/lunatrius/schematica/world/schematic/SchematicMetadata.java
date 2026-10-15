package com.github.lunatrius.schematica.world.schematic;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants.NBT;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.reference.Names;

/** Descriptive information, independent of block IDs and tile entity data. */
public final class SchematicMetadata {
    public static final String EXTENSION = "SchematicaPlus";
    public static final int MAX_DESCRIPTION = 8192;
    public static final int MAX_PREVIEW_SIDE = 512;

    private SchematicMetadata() {}

    public static NBTTagCompound read(NBTTagCompound root, boolean litematic) {
        NBTTagCompound metadata = litematic ? root.getCompoundTag("Metadata")
            : root.getCompoundTag(Names.NBT.EXTENDED_METADATA).getCompoundTag(EXTENSION);
        metadata = (NBTTagCompound) metadata.copy();
        if (!litematic && root.hasKey("author", NBT.TAG_STRING)
            && (VanillaStructure.isStructure(root) || !metadata.hasKey("Author", NBT.TAG_STRING))) metadata.setString("Author", root.getString("author"));
        return metadata;
    }

    public static void write(NBTTagCompound root, NBTTagCompound metadata, boolean litematic) {
        if (litematic) root.setTag("Metadata", metadata);
        else {
            NBTTagCompound extended = root.getCompoundTag(Names.NBT.EXTENDED_METADATA);
            extended.setTag(EXTENSION, metadata);
            root.setTag(Names.NBT.EXTENDED_METADATA, extended);
            if (VanillaStructure.isStructure(root)) root.setString("author", metadata.getString("Author"));
        }
    }

    public static void defaults(NBTTagCompound metadata, String name, String author, long time) {
        if (!metadata.hasKey("Name", NBT.TAG_STRING) && !name.isEmpty()) metadata.setString("Name", name);
        if (!metadata.hasKey("Author", NBT.TAG_STRING) && !author.isEmpty()) metadata.setString("Author", author);
        if (!metadata.hasKey("Description", NBT.TAG_STRING)) metadata.setString("Description", "");
        if (!metadata.hasKey("TimeCreated", NBT.TAG_ANY_NUMERIC)) metadata.setLong("TimeCreated", time);
    }

    public static NBTTagCompound forSave(ISchematic schematic, String name, String author, long time) {
        NBTTagCompound metadata = schematic.getSchematicMetadata();
        defaults(metadata, name, author, time);
        metadata.setLong("TimeModified", time);
        return metadata;
    }

    public static void size(NBTTagCompound metadata, ISchematic schematic, long blocks) {
        metadata.setInteger("RegionCount", Math.max(1, schematic.getRegions().size()));
        metadata.setLong("TotalVolume", (long) schematic.getWidth() * schematic.getHeight() * schematic.getLength());
        metadata.setLong("TotalBlocks", blocks);
        metadata.setTag("EnclosingSize", LitematicExport.vector(schematic.getWidth(), schematic.getHeight(), schematic.getLength()));
    }

    public static boolean validPreview(int[] pixels) {
        int side = (int) Math.sqrt(pixels.length);
        return side > 0 && side <= MAX_PREVIEW_SIDE && side * side == pixels.length;
    }

    public static void updateDescription(NBTTagCompound target, NBTTagCompound source) {
        for (String key : new String[] {"Name", "Author", "Description", "TimeCreated", "TimeModified", "PreviewImageData"}) {
            if (source.hasKey(key)) target.setTag(key, source.getTag(key).copy());
            else target.removeTag(key);
        }
    }

    public static String stem(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}

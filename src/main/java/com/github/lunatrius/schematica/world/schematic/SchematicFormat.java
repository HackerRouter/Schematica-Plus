package com.github.lunatrius.schematica.world.schematic;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.api.event.PostSchematicCaptureEvent;
import com.github.lunatrius.schematica.reference.Names;
import com.github.lunatrius.schematica.reference.Reference;

public abstract class SchematicFormat {

    public static final Map<String, SchematicFormat> FORMATS = new HashMap<>();
    public static String FORMAT_DEFAULT;

    /** When true, tile entity NBT data will be included in saved schematics. On by default. */
    public static boolean saveNBT = true;
    /** When true, entities will be included in saved schematics. Off by default. */
    public static boolean saveEntities = false;

    public abstract ISchematic readFromNBT(NBTTagCompound tagCompound);

    public abstract boolean writeToNBT(NBTTagCompound tagCompound, ISchematic schematic, World backupWorld);

    public boolean writeToNBT(NBTTagCompound tagCompound, ISchematic schematic, World backupWorld,
        boolean includeNBT, boolean includeEntities) {
        return writeToNBT(tagCompound, schematic, backupWorld);
    }

    public static ISchematic readFromFile(File file) {
        try {
            // Check for .litematic format first — needs custom NBT reader for TAG_Long_Array
            final String fileName = file.getName().toLowerCase();
            if (fileName.endsWith(".litematic")) {
                Reference.logger.info("Detected .litematic file: {}", file.getName());
                final NBTTagCompound tagCompound = LitematicaNBTReader.readFromFile(file);
                final SchematicFormat litematicFormat = FORMATS.get("Litematica");
                if (litematicFormat != null) {
                    try {
                        return litematicFormat.readFromNBT(tagCompound);
                    } finally {
                        // Free side-channel long array storage after reading is complete
                        LitematicaNBTReader.clearLongArrayStore();
                    }
                } else {
                    LitematicaNBTReader.clearLongArrayStore();
                    Reference.logger.error("Litematica format handler not registered!");
                    return null;
                }
            }

            // Standard .schematic format path
            final NBTTagCompound tagCompound = SchematicUtil.readTagCompoundFromFile(file);
            if (fileName.endsWith(".schemplus") && !tagCompound.hasKey(SchematicBlockIds.ENCODING)) {
                tagCompound.setString(SchematicBlockIds.ENCODING, SchematicBlockIds.EXTENDED);
            }
            final String format = tagCompound.getString(Names.NBT.MATERIALS);
            final SchematicFormat schematicFormat = FORMATS.get(format);

            if (schematicFormat == null) {
                throw new UnsupportedFormatException(format);
            }

            return schematicFormat.readFromNBT(tagCompound);
        } catch (Exception ex) {
            Reference.logger.error("Failed to read schematic!", ex);
        }

        return null;
    }

    public static ISchematic readFromFile(File directory, String filename) {
        return readFromFile(new File(directory, filename));
    }

    public static boolean writeToFile(File file, ISchematic schematic, World backupWorld) {
        return writeToFile(file, schematic, backupWorld, saveNBT, saveEntities);
    }

    public static boolean writeToFile(File file, ISchematic schematic, World backupWorld,
        boolean includeNBT, boolean includeEntities) {
        return saveToFile(file, schematic, backupWorld, includeNBT, includeEntities) != null;
    }

    public static File saveToFile(File file, ISchematic schematic, World backupWorld,
        boolean includeNBT, boolean includeEntities) {
        try {
            if (schematic == null) return null;
            final PostSchematicCaptureEvent event = new PostSchematicCaptureEvent(schematic);
            MinecraftForge.EVENT_BUS.post(event);

            NBTTagCompound tagCompound = new NBTTagCompound();

            SchematicFormat format = FORMATS.get(FORMAT_DEFAULT);
            boolean written = format instanceof SchematicAlpha
                ? ((SchematicAlpha) format).writeToNBT(tagCompound, schematic, backupWorld, includeNBT, includeEntities,
                    file.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".schemplus"))
                : format.writeToNBT(tagCompound, schematic, backupWorld, includeNBT, includeEntities);
            if (!written) return null;
            return SchematicFileWriter.write(file, tagCompound);
        } catch (Exception ex) {
            Reference.logger.error("Failed to write schematic!", ex);
        }
        return null;
    }

    public static boolean writeToFile(File directory, String filename, ISchematic schematic, World backupWorld) {
        return writeToFile(directory, filename, schematic, backupWorld, saveNBT, saveEntities);
    }

    public static boolean writeToFile(File directory, String filename, ISchematic schematic, World backupWorld,
        boolean includeNBT, boolean includeEntities) {
        return saveToFile(directory, filename, schematic, backupWorld, includeNBT, includeEntities) != null;
    }

    public static File saveToFile(File directory, String filename, ISchematic schematic, World backupWorld,
        boolean includeNBT, boolean includeEntities) {
        try {
            return saveToFile(com.github.lunatrius.schematica.util.FileUtils.resolveSchematicFile(directory, filename),
                schematic, backupWorld, includeNBT, includeEntities);
        } catch (java.io.IOException e) {
            Reference.logger.warn("Rejected schematic filename", e);
            return null;
        }
    }

    static {
        FORMATS.put(Names.NBT.FORMAT_ALPHA, new SchematicAlpha());
        FORMATS.put("Litematica", new SchematicLitematica());

        FORMAT_DEFAULT = Names.NBT.FORMAT_ALPHA;
    }
}

package com.github.lunatrius.schematica.world.schematic;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.nbt.CompressedStreamTools;
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
        Path temporary = null;
        try {
            if (schematic == null) return false;
            final PostSchematicCaptureEvent event = new PostSchematicCaptureEvent(schematic);
            MinecraftForge.EVENT_BUS.post(event);

            NBTTagCompound tagCompound = new NBTTagCompound();

            SchematicFormat format = FORMATS.get(FORMAT_DEFAULT);
            boolean written = format instanceof SchematicAlpha
                ? ((SchematicAlpha) format).writeToNBT(tagCompound, schematic, backupWorld, includeNBT, includeEntities,
                    file.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".schemplus"))
                : format.writeToNBT(tagCompound, schematic, backupWorld, includeNBT, includeEntities);
            if (!written) return false;

            // Use CompressedStreamTools.writeCompressed which writes using the new NBT
            // format (func_152446_a). This matches what readCompressed (func_152456_a)
            // expects on the read side. The old func_150298_a wrote using the legacy
            // format which is incompatible with readCompressed, causing NPE.
            Path target = file.toPath().toAbsolutePath();
            temporary = Files.createTempFile(target.getParent(), ".schematica-", ".tmp");
            try (FileOutputStream output = new FileOutputStream(temporary.toFile())) {
                CompressedStreamTools.writeCompressed(tagCompound, output);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }

            return true;
        } catch (Exception ex) {
            Reference.logger.error("Failed to write schematic!", ex);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (java.io.IOException e) {
                    Reference.logger.warn("Could not remove temporary schematic", e);
                }
            }
        }

        return false;
    }

    public static boolean writeToFile(File directory, String filename, ISchematic schematic, World backupWorld) {
        return writeToFile(directory, filename, schematic, backupWorld, saveNBT, saveEntities);
    }

    public static boolean writeToFile(File directory, String filename, ISchematic schematic, World backupWorld,
        boolean includeNBT, boolean includeEntities) {
        try {
            return writeToFile(com.github.lunatrius.schematica.util.FileUtils.resolveSchematicFile(directory, filename),
                schematic, backupWorld, includeNBT, includeEntities);
        } catch (java.io.IOException e) {
            Reference.logger.warn("Rejected schematic filename", e);
            return false;
        }
    }

    static {
        FORMATS.put(Names.NBT.FORMAT_ALPHA, new SchematicAlpha());
        FORMATS.put("Litematica", new SchematicLitematica());

        FORMAT_DEFAULT = Names.NBT.FORMAT_ALPHA;
    }
}

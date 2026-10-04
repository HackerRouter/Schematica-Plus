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
            return decode(LitematicaNBTReader.readFromFile(file), file.getName());
        } catch (Exception ex) {
            Reference.logger.error("Failed to read schematic!", ex);
            return null;
        } finally {
            LitematicaNBTReader.clearLongArrayStore();
        }
    }

    public static ISchematic readFromSnapshot(SchematicFileSnapshot snapshot) throws java.io.IOException {
        try {
            ISchematic data = decode(snapshot.readNBT(), snapshot.extension());
            if (data == null) throw new java.io.IOException("Unable to decode schematic source");
            return data;
        } catch (RuntimeException ex) {
            throw new java.io.IOException("Unable to decode schematic source", ex);
        } finally {
            LitematicaNBTReader.clearLongArrayStore();
        }
    }

    private static ISchematic decode(NBTTagCompound tag, String filename) {
        String name = filename.toLowerCase(java.util.Locale.ROOT);
        if (name.endsWith(".litematic")) return FORMATS.get("Litematica").readFromNBT(tag);
        if (name.endsWith(".nbt")) return FORMATS.get("Structure").readFromNBT(tag);
        if (name.endsWith(".schem") || !tag.hasKey(Names.NBT.MATERIALS) && SpongeSchematic.isSponge(tag)) return FORMATS.get("Sponge").readFromNBT(tag);
        if (name.endsWith(".schemplus") && !tag.hasKey(SchematicBlockIds.ENCODING)) {
            tag.setString(SchematicBlockIds.ENCODING, SchematicBlockIds.EXTENDED);
        }
        String format = tag.getString(Names.NBT.MATERIALS);
        SchematicFormat reader = FORMATS.get(format);
        if (reader == null) throw new IllegalArgumentException("Unsupported schematic format: " + format);
        return reader.readFromNBT(tag);
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
        return saveToFile(file, schematic, backupWorld, includeNBT, includeEntities, "");
    }

    /** Writes by extension: .litematic and .nbt in their Minecraft 1.12.2 layouts (with the author), else Alpha/.schemplus. */
    public static File saveToFile(File file, ISchematic schematic, World backupWorld,
        boolean includeNBT, boolean includeEntities, String author) {
        try {
            if (schematic == null) return null;
            final PostSchematicCaptureEvent event = new PostSchematicCaptureEvent(schematic);
            MinecraftForge.EVENT_BUS.post(event);

            String fileName = file.getName();
            if (fileName.toLowerCase(java.util.Locale.ROOT).endsWith(".litematic")) {
                LitematicExport.Document document = LitematicExport.encode(schematic, backupWorld, includeNBT, includeEntities,
                    fileName.substring(0, fileName.length() - ".litematic".length()), author, System.currentTimeMillis());
                return SchematicFileWriter.write(file, document);
            }
            if (fileName.toLowerCase(java.util.Locale.ROOT).endsWith(".nbt")) {
                return SchematicFileWriter.write(file, VanillaStructure.encode(schematic, backupWorld, includeEntities, author));
            }

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
        FORMATS.put("Structure", new VanillaStructure());
        FORMATS.put("Sponge", new SpongeSchematic());

        FORMAT_DEFAULT = Names.NBT.FORMAT_ALPHA;
    }
}

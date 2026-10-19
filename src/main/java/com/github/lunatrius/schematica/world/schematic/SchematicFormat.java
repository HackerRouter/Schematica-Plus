package com.github.lunatrius.schematica.world.schematic;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.api.event.PostSchematicCaptureEvent;
import com.github.lunatrius.schematica.reference.Names;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.util.FileUtils;

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

    public static ISchematic readFromSnapshot(SchematicFileSnapshot snapshot) throws IOException {
        LitematicaNBTReader.Parsed parsed;
        try {
            parsed = snapshot.parse();
        } catch (RuntimeException ex) {
            throw new IOException("Unable to decode schematic source", ex);
        }
        return readFromSnapshot(snapshot, parsed);
    }

    public static ISchematic readFromSnapshot(SchematicFileSnapshot snapshot, LitematicaNBTReader.Parsed parsed) throws IOException {
        try {
            ISchematic data = decode(LitematicaNBTReader.restore(parsed), snapshot.extension());
            if (data == null) throw new IOException("Unable to decode schematic source");
            return data;
        } catch (RuntimeException ex) {
            throw new IOException("Unable to decode schematic source", ex);
        } finally {
            LitematicaNBTReader.clearLongArrayStore();
        }
    }

    private static ISchematic decode(NBTTagCompound tag, String filename) {
        ImportReport.begin();
        try {
            PortableSchematicData.read(tag);
            ISchematic schematic = decodeFormat(tag, filename);
            schematic.setSchematicMetadata(SchematicMetadata.read(tag, SchematicLitematica.isLitematicFormat(tag)));
            return schematic;
        } finally {
            ImportReport.end();
        }
    }

    private static ISchematic decodeFormat(NBTTagCompound tag, String filename) {
        String name = filename.toLowerCase(Locale.ROOT);
        if (name.endsWith(".litematic")) return FORMATS.get("Litematica").readFromNBT(tag);
        if (name.endsWith(".nbt")) return FORMATS.get("Structure").readFromNBT(tag);
        if (name.endsWith(".schem") || !tag.hasKey(Names.NBT.MATERIALS) && SpongeSchematic.isSponge(tag)) return FORMATS.get("Sponge").readFromNBT(tag);
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

            LitematicExport.Document document = encode(file, schematic, backupWorld, includeNBT, includeEntities, author);
            PortableSchematicData.write(document.root);
            return file.getName().toLowerCase(Locale.ROOT).endsWith(".litematic")
                ? SchematicFileWriter.write(file, document) : SchematicFileWriter.write(file, document.root);
        } catch (Exception ex) {
            Reference.logger.error("Failed to write schematic!", ex);
        }
        return null;
    }

    static LitematicExport.Document encode(File file, ISchematic schematic, World backupWorld,
        boolean includeNBT, boolean includeEntities, String author) {
        String fileName = file.getName();
        if (fileName.toLowerCase(Locale.ROOT).endsWith(".litematic")) {
            LitematicExport.Document document = LitematicExport.encode(schematic, backupWorld, includeNBT, includeEntities,
                fileName.substring(0, fileName.length() - ".litematic".length()), author, System.currentTimeMillis());
            return document;
        }
        if (fileName.toLowerCase(Locale.ROOT).endsWith(".nbt")) {
            NBTTagCompound tag = VanillaStructure.encode(schematic, backupWorld, includeNBT, includeEntities, author);
            NBTTagCompound metadata = SchematicMetadata.forSave(schematic, SchematicMetadata.stem(fileName), author, System.currentTimeMillis());
            SchematicMetadata.size(metadata, schematic, -1);
            SchematicMetadata.write(tag, metadata, false);
            return new LitematicExport.Document(tag, Collections.emptyMap());
        }

        NBTTagCompound tagCompound = new NBTTagCompound();

        SchematicFormat format = FORMATS.get(FORMAT_DEFAULT);
        boolean written = format instanceof SchematicAlpha
            ? ((SchematicAlpha) format).writeToNBT(tagCompound, schematic, backupWorld, includeNBT, includeEntities,
                file.getName().toLowerCase(Locale.ROOT).endsWith(".schemplus"))
            : format.writeToNBT(tagCompound, schematic, backupWorld, includeNBT, includeEntities);
        if (!written) throw new IllegalStateException("Unable to encode schematic");
        NBTTagCompound metadata = SchematicMetadata.read(tagCompound, false);
        SchematicMetadata.defaults(metadata, SchematicMetadata.stem(fileName), author, System.currentTimeMillis());
        SchematicMetadata.write(tagCompound, metadata, false);
        return new LitematicExport.Document(tagCompound, Collections.emptyMap());
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
            return saveToFile(FileUtils.resolveSchematicFile(directory, filename),
                schematic, backupWorld, includeNBT, includeEntities);
        } catch (IOException e) {
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

package com.github.lunatrius.schematica.world.schematic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.util.SchematicLimits;

public final class SchematicFileSnapshot {

    private final byte[] bytes;
    private final String extension;

    private SchematicFileSnapshot(byte[] bytes, String extension) {
        this.bytes = bytes;
        this.extension = extension;
    }

    public static SchematicFileSnapshot read(File file) throws IOException {
        String name = file.getName().toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        String extension = dot < 0 ? "" : name.substring(dot);
        if (!extension.equals(".schematic") && !extension.equals(".schemplus") && !extension.equals(".litematic") && !extension.equals(".nbt") && !extension.equals(".schem")) {
            throw new IOException("Unsupported source extension");
        }
        if (Files.size(file.toPath()) > SchematicLimits.MAX_NBT_BYTES) throw new IOException("Source file exceeds memory limit");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (InputStream input = Files.newInputStream(file.toPath())) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                if ((long) output.size() + count > SchematicLimits.MAX_NBT_BYTES) throw new IOException("Source file exceeds memory limit");
                output.write(buffer, 0, count);
            }
        }
        return new SchematicFileSnapshot(output.toByteArray(), extension);
    }

    public String extension() { return extension; }

    public SchematicFileSnapshot withMetadata(NBTTagCompound metadata) throws IOException {
        try {
            NBTTagCompound root = readNBT();
            if (LitematicaNBTReader.droppedData()) throw new IOException("Unsupported source data");
            boolean litematic = extension.equals(".litematic");
            NBTTagCompound current = litematic ? root.getCompoundTag("Metadata")
                : root.getCompoundTag("ExtendedMetadata").getCompoundTag(SchematicMetadata.EXTENSION);
            SchematicMetadata.updateDescription(current, metadata);
            SchematicMetadata.write(root, current, litematic);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            LitematicaNBTWriter.writeCompressed(root, LitematicaNBTReader.longArrays(), output);
            if (output.size() > SchematicLimits.MAX_NBT_BYTES) throw new IOException("Source file exceeds memory limit");
            return new SchematicFileSnapshot(output.toByteArray(), extension);
        } finally {
            LitematicaNBTReader.clearLongArrayStore();
        }
    }

    public static SchematicFileSnapshot capture(ISchematic schematic) throws IOException {
        return capture(schematic, "", "");
    }

    public static SchematicFileSnapshot capture(ISchematic schematic, String name, String author) throws IOException {
        NBTTagCompound tag = new NBTTagCompound();
        if (!new SchematicAlpha().writeToNBT(tag, schematic, null, true, true, true)) throw new IOException("Unable to encode edited schematic");
        NBTTagCompound metadata = SchematicMetadata.read(tag, false);
        SchematicMetadata.defaults(metadata, name, author, System.currentTimeMillis());
        SchematicMetadata.write(tag, metadata, false);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CompressedStreamTools.writeCompressed(tag, bytes);
        if (bytes.size() > SchematicLimits.MAX_NBT_BYTES) throw new IOException("Edited schematic exceeds file limit");
        return new SchematicFileSnapshot(bytes.toByteArray(), ".schemplus");
    }
    public int size() { return bytes.length; }

    public NBTTagCompound readNBT() throws IOException {
        return LitematicaNBTReader.readFromStream(new ByteArrayInputStream(bytes));
    }

    public LitematicaNBTReader.Parsed parse() throws IOException {
        return LitematicaNBTReader.parse(new ByteArrayInputStream(bytes));
    }

    public void write(File target, boolean replace) throws IOException {
        if (!target.getName().toLowerCase(Locale.ROOT).endsWith(extension)) throw new IOException("Keep the original source extension");
        Path destination = target.toPath().toAbsolutePath();
        Path temporary = Files.createTempFile(destination.getParent(), ".schematica-source-", ".tmp");
        try {
            Files.write(temporary, bytes);
            if (replace) {
                try {
                    Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            } else Files.move(temporary, destination);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}

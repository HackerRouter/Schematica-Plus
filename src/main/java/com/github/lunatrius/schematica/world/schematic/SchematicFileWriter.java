package com.github.lunatrius.schematica.world.schematic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

final class SchematicFileWriter {
    private SchematicFileWriter() {}

    static File write(File requested, NBTTagCompound tag) throws IOException {
        Path target = requested.toPath().toAbsolutePath();
        String name = target.getFileName().toString();
        boolean upgrade = name.toLowerCase(Locale.ROOT).endsWith(".schematic")
            && SchematicBlockIds.EXTENDED.equals(tag.getString(SchematicBlockIds.ENCODING));
        Path temporary = Files.createTempFile(target.getParent(), ".schematica-", ".tmp");
        try {
            try (FileOutputStream output = new FileOutputStream(temporary.toFile())) {
                CompressedStreamTools.writeCompressed(tag, output);
            }
            if (upgrade) {
                String stem = name.substring(0, name.length() - ".schematic".length());
                for (int suffix = 0; suffix < 10000; suffix++) {
                    Path candidate = target.resolveSibling(stem + (suffix == 0 ? "" : "-" + suffix) + ".schemplus");
                    try {
                        Files.move(temporary, candidate);
                        return candidate.toFile();
                    } catch (FileAlreadyExistsException ignored) {}
                }
                throw new IOException("No unused extended schematic filename available");
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return target.toFile();
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}

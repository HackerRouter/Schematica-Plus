package com.github.lunatrius.schematica.client.gui.browser;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import net.minecraft.nbt.NBTTagCompound;

import com.github.lunatrius.schematica.client.world.Placements;
import com.github.lunatrius.schematica.client.world.SchematicLibrary;
import com.github.lunatrius.schematica.client.world.SchematicSourceData;
import com.github.lunatrius.schematica.world.schematic.SchematicFiles;

public final class SchematicFileEdits {
    private SchematicFileEdits() {}

    public static void edit(File file, Consumer<NBTTagCompound> edit) throws IOException {
        NBTTagCompound metadata = SchematicFiles.editMetadata(file, edit);
        SchematicLibrary.Source<SchematicSourceData> source = Placements.LIBRARY.find(file);
        if (source != null) source.data().updateMetadata(metadata);
    }
}

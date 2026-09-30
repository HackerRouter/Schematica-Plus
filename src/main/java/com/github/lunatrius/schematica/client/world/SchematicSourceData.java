package com.github.lunatrius.schematica.client.world;

import java.io.File;
import java.io.IOException;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.util.SchematicLimits;
import com.github.lunatrius.schematica.world.schematic.SchematicFileSnapshot;
import com.github.lunatrius.schematica.world.schematic.SchematicFormat;

public final class SchematicSourceData {

    public final SchematicFileSnapshot snapshot;
    public final int width;
    public final int height;
    public final int length;

    private SchematicSourceData(SchematicFileSnapshot snapshot, ISchematic data) {
        this.snapshot = snapshot;
        width = data.getWidth();
        height = data.getHeight();
        length = data.getLength();
        SchematicLimits.volume(width, height, length);
    }

    public static SchematicSourceData read(File file) throws IOException {
        SchematicFileSnapshot snapshot = SchematicFileSnapshot.read(file);
        return new SchematicSourceData(snapshot, SchematicFormat.readFromSnapshot(snapshot));
    }

    public ISchematic instantiate() throws IOException { return SchematicFormat.readFromSnapshot(snapshot); }
}

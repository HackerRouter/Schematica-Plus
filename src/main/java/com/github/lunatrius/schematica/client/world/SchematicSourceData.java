package com.github.lunatrius.schematica.client.world;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.api.SchematicRegion;
import com.github.lunatrius.schematica.util.MessageException;
import com.github.lunatrius.schematica.util.SchematicLimits;
import com.github.lunatrius.schematica.world.schematic.SchematicFileSnapshot;
import com.github.lunatrius.schematica.world.schematic.SchematicFormat;
import com.github.lunatrius.schematica.world.storage.Schematic;
import com.github.lunatrius.schematica.world.storage.SchematicCopies;

public final class SchematicSourceData {

    public final SchematicFileSnapshot snapshot;
    public final int width;
    public final int height;
    public final int length;
    private Schematic edited;
    private int revision;

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

    public ISchematic instantiate() throws IOException {
        return edited == null ? SchematicFormat.readFromSnapshot(snapshot) : SchematicCopies.copy(edited);
    }

    public boolean modified() { return edited != null; }
    public int revision() { return revision; }
    public String saveExtension() { return modified() ? ".schemplus" : snapshot.extension(); }

    /** The in-memory working copy edited by the rebuild tool; the original file snapshot is never changed. */
    public Schematic editable() throws IOException {
        if (edited == null) {
            ISchematic data = SchematicFormat.readFromSnapshot(snapshot);
            edited = data instanceof Schematic ? (Schematic) data : SchematicCopies.copy(data);
        }
        return edited;
    }

    public void changed() { revision++; }

    public void save(File file, boolean replace) throws IOException {
        if (edited == null) { snapshot.write(file, replace); return; }
        if (SchematicCopies.independent(edited) && overlapping(edited.getRegions())) throw new MessageException("schematica.message.rebuild.save_overlapping");
        SchematicFileSnapshot.capture(edited).write(file, replace);
    }

    static boolean overlapping(List<SchematicRegion> regions) {
        for (int i = 0; i < regions.size(); i++) for (int j = i + 1; j < regions.size(); j++) {
            SchematicRegion a = regions.get(i), b = regions.get(j);
            if (a.minX <= b.maxX && b.minX <= a.maxX && a.minY <= b.maxY && b.minY <= a.maxY && a.minZ <= b.maxZ && b.minZ <= a.maxZ) return true;
        }
        return false;
    }
}

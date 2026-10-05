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
    private int savedRevision;
    private long modifiedTime;
    private final java.util.Map<String, String> unknownBlocks;
    private boolean unknownBlocksReported;
    private com.github.lunatrius.schematica.world.schematic.ItemIdMaps.Result itemIds;

    private SchematicSourceData(SchematicFileSnapshot snapshot, ISchematic data) {
        this.snapshot = snapshot;
        this.unknownBlocks = com.github.lunatrius.schematica.world.schematic.ImportReport.last();
        this.itemIds = com.github.lunatrius.schematica.world.schematic.ImportReport.lastItems();
        width = data.getWidth();
        height = data.getHeight();
        length = data.getLength();
        SchematicLimits.volume(width, height, length);
    }

    public static SchematicSourceData read(File file) throws IOException {
        return of(SchematicFileSnapshot.read(file));
    }

    public static SchematicSourceData of(SchematicFileSnapshot snapshot) throws IOException {
        return new SchematicSourceData(snapshot, SchematicFormat.readFromSnapshot(snapshot));
    }

    public ISchematic instantiate() throws IOException {
        return !modified() ? SchematicFormat.readFromSnapshot(snapshot) : SchematicCopies.copy(edited);
    }

    /** Block names or states of the file this game does not have, to the block that replaced them; taken once. */
    public java.util.Map<String, String> takeUnknownBlocks() {
        if (unknownBlocksReported) return java.util.Collections.emptyMap();
        unknownBlocksReported = true;
        return unknownBlocks;
    }

    /** What loading did with the item ids of the file; taken once, then null. */
    public com.github.lunatrius.schematica.world.schematic.ItemIdMaps.Result takeItemIds() {
        com.github.lunatrius.schematica.world.schematic.ItemIdMaps.Result result = itemIds;
        itemIds = null;
        return result;
    }

    public boolean modified() { return revision > 0; }
    public int revision() { return revision; }
    /** Whether edits exist that were not written to any file yet. */
    public boolean unsaved() { return revision != savedRevision; }
    public long modifiedTime() { return modifiedTime; }
    public String saveExtension() { return modified() ? ".schemplus" : snapshot.extension(); }

    /** The in-memory working copy edited by the rebuild tool; the original file snapshot is never changed. */
    public Schematic editable() throws IOException {
        if (edited == null) {
            ISchematic data = SchematicFormat.readFromSnapshot(snapshot);
            edited = data instanceof Schematic ? (Schematic) data : SchematicCopies.copy(data);
        }
        return edited;
    }

    public void changed() {
        revision++;
        modifiedTime = System.currentTimeMillis();
    }

    public void save(File file, boolean replace) throws IOException {
        if (!modified()) { snapshot.write(file, replace); return; }
        SchematicFileSnapshot.capture(edited).write(file, replace);
        savedRevision = revision;
    }
}

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

    public SchematicFileSnapshot snapshot;
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
    /** The schematic decoded while reading, handed to the first placement instead of decoding the file again. */
    private java.lang.ref.SoftReference<ISchematic> decoded;

    private SchematicSourceData(SchematicFileSnapshot snapshot, ISchematic data) {
        this.snapshot = snapshot;
        this.unknownBlocks = com.github.lunatrius.schematica.world.schematic.ImportReport.last();
        this.itemIds = com.github.lunatrius.schematica.world.schematic.ImportReport.lastItems();
        width = data.getWidth();
        height = data.getHeight();
        length = data.getLength();
        SchematicLimits.volume(width, height, length);
        decoded = new java.lang.ref.SoftReference<>(data);
    }

    public static SchematicSourceData read(File file) throws IOException {
        return of(SchematicFileSnapshot.read(file));
    }

    public static SchematicSourceData of(SchematicFileSnapshot snapshot) throws IOException {
        return new SchematicSourceData(snapshot, SchematicFormat.readFromSnapshot(snapshot));
    }

    public synchronized ISchematic instantiate() throws IOException {
        ISchematic first = decoded == null ? null : decoded.get();
        decoded = null;
        if (modified()) return SchematicCopies.copy(edited);
        return first != null ? first : SchematicFormat.readFromSnapshot(snapshot);
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

    /** The working copy edited by REBUILD; the snapshot's block contents stay unchanged. */
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
        save(file, replace, "");
    }

    public void save(File file, boolean replace, String author) throws IOException {
        if (!modified()) { snapshot.write(file, replace); return; }
        SchematicFileSnapshot.capture(edited,
            com.github.lunatrius.schematica.world.schematic.SchematicMetadata.stem(file.getName()), author).write(file, replace);
        savedRevision = revision;
    }

    public synchronized void updateMetadata(net.minecraft.nbt.NBTTagCompound metadata) throws IOException {
        snapshot = snapshot.withMetadata(metadata);
        if (edited != null) edited.setSchematicMetadata(metadata);
        ISchematic first = decoded == null ? null : decoded.get();
        if (first != null) first.setSchematicMetadata(metadata);
    }
}

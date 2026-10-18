package com.github.lunatrius.schematica.cloud;

import net.minecraft.nbt.NBTTagCompound;

import com.github.lunatrius.schematica.world.schematic.SchematicFiles;

public final class CloudFile {
    public final CloudPath path;
    public final long size, modified;
    public final String hash, owner;
    public String name = "", author = "", format = "";
    public long created, volume, blocks = -1;
    public int regions = 1, version = -1, dataVersion = -1, sizeX, sizeY, sizeZ;

    public CloudFile(CloudPath path, long size, long modified, String hash, String owner) {
        this.path = path; this.size = size; this.modified = modified; this.hash = hash; this.owner = owner == null ? "" : owner;
    }

    public CloudFile describe(SchematicFiles.Info info) {
        if (info == null) return this;
        name = info.name; author = info.author; format = info.format; created = info.created;
        volume = info.volume; blocks = info.blocks; regions = info.regions; version = info.version; dataVersion = info.dataVersion;
        sizeX = info.sizeX; sizeY = info.sizeY; sizeZ = info.sizeZ;
        return this;
    }

    public CloudFile withOwner(String owner) {
        return copyInto(new CloudFile(path, size, modified, hash, owner));
    }

    private CloudFile copyInto(CloudFile file) {
        file.name = name; file.author = author; file.format = format; file.created = created; file.volume = volume; file.blocks = blocks;
        file.regions = regions; file.version = version; file.dataVersion = dataVersion; file.sizeX = sizeX; file.sizeY = sizeY; file.sizeZ = sizeZ;
        return file;
    }

    NBTTagCompound write() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Path", path.toString());
        tag.setLong("Size", size);
        tag.setLong("Modified", modified);
        tag.setString("Hash", hash);
        tag.setString("Owner", owner);
        tag.setString("Name", name);
        tag.setString("Author", author);
        tag.setString("Format", format);
        tag.setLong("Created", created);
        tag.setLong("Volume", volume);
        tag.setLong("Blocks", blocks);
        tag.setInteger("Regions", regions);
        tag.setInteger("Version", version);
        tag.setInteger("DataVersion", dataVersion);
        tag.setIntArray("Dimensions", new int[] {sizeX, sizeY, sizeZ});
        return tag;
    }

    static CloudFile read(NBTTagCompound tag) {
        CloudPath path = CloudPath.parse(tag.getString("Path"));
        if (path.isRoot() || !CloudPath.schematicName(path.name())) throw new IllegalArgumentException("Invalid file");
        CloudFile file = new CloudFile(path, tag.getLong("Size"), tag.getLong("Modified"), tag.getString("Hash"), tag.getString("Owner"));
        file.name = tag.getString("Name");
        file.author = tag.getString("Author");
        file.format = tag.getString("Format");
        file.created = tag.getLong("Created");
        file.volume = tag.getLong("Volume");
        file.blocks = tag.getLong("Blocks");
        file.regions = tag.getInteger("Regions");
        file.version = tag.getInteger("Version");
        file.dataVersion = tag.getInteger("DataVersion");
        int[] dimensions = tag.getIntArray("Dimensions");
        if (dimensions.length == 3) { file.sizeX = dimensions[0]; file.sizeY = dimensions[1]; file.sizeZ = dimensions[2]; }
        return file;
    }
}

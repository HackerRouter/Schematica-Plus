package com.github.lunatrius.schematica.cloud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants;

public final class CloudCatalog {
    public static final int MAX_ENTRIES = 4096;
    public final List<CloudPath> directories;
    public final List<CloudFile> files;
    public final long used, quota;
    public final boolean upload, publish, manage;

    public CloudCatalog(List<CloudPath> directories, List<CloudFile> files, long used, long quota, boolean upload, boolean publish, boolean manage) {
        this.directories = Collections.unmodifiableList(new ArrayList<>(directories));
        this.files = Collections.unmodifiableList(new ArrayList<>(files));
        this.used = used; this.quota = quota; this.upload = upload; this.publish = publish; this.manage = manage;
    }

    public NBTTagCompound write() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList directories = new NBTTagList();
        for (CloudPath directory : this.directories) directories.appendTag(new NBTTagString(directory.toString()));
        NBTTagList files = new NBTTagList();
        for (CloudFile file : this.files) files.appendTag(file.write());
        tag.setTag("Directories", directories);
        tag.setTag("Files", files);
        tag.setLong("Used", used);
        tag.setLong("Quota", quota);
        tag.setBoolean("Upload", upload);
        tag.setBoolean("Publish", publish);
        tag.setBoolean("Manage", manage);
        return tag;
    }

    public static CloudCatalog read(NBTTagCompound tag) {
        NBTTagList directoryTags = tag.getTagList("Directories", Constants.NBT.TAG_STRING);
        NBTTagList fileTags = tag.getTagList("Files", Constants.NBT.TAG_COMPOUND);
        if (directoryTags.tagCount() + fileTags.tagCount() > MAX_ENTRIES * 2) throw new IllegalArgumentException("Catalog too large");
        List<CloudPath> directories = new ArrayList<>();
        for (int i = 0; i < directoryTags.tagCount(); i++) directories.add(CloudPath.parse(directoryTags.getStringTagAt(i)));
        List<CloudFile> files = new ArrayList<>();
        for (int i = 0; i < fileTags.tagCount(); i++) files.add(CloudFile.read(fileTags.getCompoundTagAt(i)));
        return new CloudCatalog(directories, files, tag.getLong("Used"), tag.getLong("Quota"),
            tag.getBoolean("Upload"), tag.getBoolean("Publish"), tag.getBoolean("Manage"));
    }
}
